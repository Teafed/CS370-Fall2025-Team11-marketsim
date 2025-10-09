package com.market;

import java.sql.Connection;
import java.sql.*;

public class DatabaseManager implements AutoCloseable {
    private final Connection conn;

    public DatabaseManager(String dbFile) throws SQLException {
        String url = "jdbc:sqlite:" + dbFile + "?busy_timeout=5000"; // 5s
        this.conn = DriverManager.getConnection(url);
        createSchema();
    }

    public void close() throws SQLException { conn.close(); }

    private void createSchema() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS prices ("
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "    symbol TEXT NOT NULL,"
                + "    timestamp INTEGER NOT NULL," // store as epoch millis
                + "    open REAL,"
                + "    high REAL,"
                + "    low REAL,"
                + "    close REAL,"
                + "    volume INTEGER,"
                + "    UNIQUE(symbol, timestamp)"   // prevent duplicates
                + ");";
        conn.createStatement().execute(sql);
    }

    public void insertPrice(String symbol, long timestamp,
                            double open, double high, double low,
                            double close, long volume) throws SQLException {
        String sql = "INSERT OR REPLACE INTO prices(symbol, timestamp, open, high, low, close, volume) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setLong(2, timestamp);
            ps.setDouble(3, open);
            ps.setDouble(4, high);
            ps.setDouble(5, low);
            ps.setDouble(6, close);
            ps.setLong(7, volume);
            ps.executeUpdate();
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public ResultSet getPrices(String symbol, long start, long end) throws SQLException {
        String sql = "SELECT * FROM prices WHERE symbol=? AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, symbol);
        ps.setLong(2, start);
        ps.setLong(3, end);
        return ps.executeQuery();
    }

    public long getLatestTimestamp(String symbol) throws SQLException {
        String sql = "SELECT MAX(timestamp) FROM prices WHERE symbol=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
}

