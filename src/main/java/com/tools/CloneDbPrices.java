package com.tools;

import com.models.Database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * Utility to create a "clean" test database that keeps only price-related data.
 *
 * Usage:
 *   java com.tools.PriceDbCloneTool path/to/source.db path/to/clean.db
 *
 * Result:
 *   - clean.db has the full schema
 *   - prices (and company_profiles) are copied over
 *   - no profiles/accounts/trades/cash_ledger/positions/watchlists
 */
public class CloneDbPrices {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: PriceDbCloneTool <source.db> <dest.db>");
            System.exit(1);
        }

        Path srcPath = Paths.get(args[0]);
        Path dstPath = Paths.get(args[1]);

        if (!Files.exists(srcPath)) {
            System.err.println("Source DB does not exist: " + srcPath);
            System.exit(1);
        }
        if (Files.exists(dstPath)) {
            System.err.println("Destination DB already exists (refusing to overwrite): " + dstPath);
            System.exit(1);
        }

        // 1) Create destination DB with full schema (but empty tables)
        System.out.println("Creating destination DB with schema: " + dstPath);
        try (Database ignored = new Database(dstPath.toString())) {
            // constructor ensures schema; we don't need anything else here
        }

        // 2) Open raw JDBC connections to source and dest
        String srcUrl = "jdbc:sqlite:" + srcPath;
        String dstUrl = "jdbc:sqlite:" + dstPath + "?busy_timeout=5000";

        try (Connection src = DriverManager.getConnection(srcUrl);
             Connection dst = DriverManager.getConnection(dstUrl)) {

            // Just to be explicit
            try (Statement st = dst.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            copyPrices(src, dst);
            copyCompanyProfiles(src, dst); // optional but useful

            System.out.println("Done!");
        }
    }

    /**
     * Copy all rows from source.prices to dest.prices,
     * ignoring the autoincrement 'id' and relying on the UNIQUE(symbol, timespan, multiplier, timestamp).
     */
    private static void copyPrices(Connection src, Connection dst) throws SQLException {
        System.out.println("Copying prices...");
        String selectSql = """
                SELECT symbol, timespan, multiplier, timestamp,
                       open, high, low, close, volume
                FROM prices
                """;

        String insertSql = """
                INSERT OR REPLACE INTO prices
                (symbol, timespan, multiplier, timestamp, open, high, low, close, volume)
                VALUES (?,?,?,?,?,?,?,?,?)
                """;

        dst.setAutoCommit(false);
        int count = 0;

        try (Statement s = src.createStatement();
             ResultSet rs = s.executeQuery(selectSql);
             PreparedStatement ins = dst.prepareStatement(insertSql)) {

            while (rs.next()) {
                ins.setString(1, rs.getString("symbol"));
                ins.setString(2, rs.getString("timespan"));
                ins.setInt(3, rs.getInt("multiplier"));
                ins.setLong(4, rs.getLong("timestamp"));
                ins.setObject(5, rs.getObject("open"));   // allows NULL
                ins.setObject(6, rs.getObject("high"));
                ins.setObject(7, rs.getObject("low"));
                ins.setObject(8, rs.getObject("close"));
                ins.setObject(9, rs.getObject("volume"));
                ins.addBatch();
                count++;
            }
            ins.executeBatch();
            dst.commit();
        } catch (SQLException e) {
            dst.rollback();
            throw e;
        }

        System.out.println("Copied " + count + " price rows.");
    }

    /**
     * Copy all rows from source.company_profiles to dest.company_profiles.
     * This is optional, but nice so your app already has cached metadata.
     */
    private static void copyCompanyProfiles(Connection src, Connection dst) throws SQLException {
        // If the source DB doesn't have this table (older DB), just skip
        if (!tableExists(src, "company_profiles")) {
            System.out.println("No company_profiles table in source; skipping.");
            return;
        }

        System.out.println("Copying company_profiles...");
        String selectSql = """
                SELECT symbol, country, currency, exchange, ipo, logo,
                       market_cap, name, shares_outstanding, web_url,
                       last_fetched_ms, last_failed_ms
                FROM company_profiles
                """;

        String insertSql = """
                INSERT OR REPLACE INTO company_profiles
                (symbol, country, currency, exchange, ipo, logo, market_cap,
                 name, shares_outstanding, web_url, last_fetched_ms, last_failed_ms)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """;

        dst.setAutoCommit(false);
        int count = 0;

        try (Statement s = src.createStatement();
             ResultSet rs = s.executeQuery(selectSql);
             PreparedStatement ins = dst.prepareStatement(insertSql)) {

            while (rs.next()) {
                ins.setString(1, rs.getString("symbol"));
                ins.setString(2, rs.getString("country"));
                ins.setString(3, rs.getString("currency"));
                ins.setString(4, rs.getString("exchange"));
                ins.setString(5, rs.getString("ipo"));
                ins.setString(6, rs.getString("logo"));
                ins.setString(7, rs.getString("market_cap"));
                ins.setString(8, rs.getString("name"));
                ins.setString(9, rs.getString("shares_outstanding"));
                ins.setString(10, rs.getString("web_url"));
                ins.setLong(11, rs.getLong("last_fetched_ms"));
                ins.setLong(12, rs.getLong("last_failed_ms"));
                ins.addBatch();
                count++;
            }
            ins.executeBatch();
            dst.commit();
        } catch (SQLException e) {
            dst.rollback();
            throw e;
        }

        System.out.println("Copied " + count + " company_profiles rows.");
    }

    private static boolean tableExists(Connection conn, String name) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
