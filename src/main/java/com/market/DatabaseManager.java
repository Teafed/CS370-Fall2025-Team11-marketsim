package com.market;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager implements AutoCloseable {
    private final Connection conn;

    public record PriceRow(String symbol, long timestamp,
                           double open, double high, double low, double close, long volume) { }

    public DatabaseManager(String dbFile) throws SQLException {
        String url = "jdbc:sqlite:" + dbFile + "?busy_timeout=5000"; // 5s
        this.conn = DriverManager.getConnection(url);
        createSchema();
    }

    @Override public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    private void createSchema() throws SQLException {
        String table = "CREATE TABLE IF NOT EXISTS prices ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "symbol TEXT NOT NULL,"
                + "timestamp INTEGER NOT NULL,"
                + "open REAL, high REAL, low REAL, close REAL,"
                + "volume INTEGER,"
                + "UNIQUE(symbol, timestamp)"
                + ");";
        try (Statement st = conn.createStatement()) {
            st.execute(table);
        }

        String index = "CREATE INDEX IF NOT EXISTS idx_prices_symbol_ts "
                + "ON prices(symbol, timestamp)";
        try (Statement st = conn.createStatement()) {
            st.execute(index);
        }
    }

    public Connection getConnection() {
        return conn;
    }

    /**
     * distinct symbols in alphabetical order (for SymbolListPanel)
     * @return String list of symbols
     * @throws SQLException
     */
    public List<String> listSymbols() throws SQLException {
        String sql = "SELECT DISTINCT symbol FROM prices ORDER BY symbol ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> out = new ArrayList<>();
            while (rs.next()) out.add(rs.getString(1));
            return out;
        }
    }

    /**
     * candles in [startMs, endMs], ordered by time asc (for ChartPanel)
     * @param symbol
     * @param startMs
     * @param endMs
     * @return
     * @throws SQLException
     */
    public ResultSet getPrices(String symbol, long startMs, long endMs) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("""
            SELECT timestamp, open, high, low, close, volume
            FROM prices
            WHERE symbol = ? AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp ASC
        """);
        ps.setString(1, symbol);
        ps.setLong(2, startMs);
        ps.setLong(3, endMs);
        // Caller will iterate and then close the ResultSet (closing ps auto-closes rs).
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

    /**
     * get latest close and previous close for % change (NaN if not available)
     * @param symbol
     * @return
     * @throws SQLException
     */
    public double[] latestAndPrevClose(String symbol) throws SQLException {
        String sql = """
            SELECT close FROM prices
            WHERE symbol = ?
            ORDER BY timestamp DESC
            LIMIT 2
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            try (ResultSet rs = ps.executeQuery()) {
                Double last = null, prev = null;
                if (rs.next()) last = rs.getDouble(1);
                if (rs.next()) prev = rs.getDouble(1);
                return new double[] {
                        last == null ? Double.NaN : last,
                        prev == null ? Double.NaN : prev
                };
            }
        }
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

    public void insertPricesBatch(java.util.List<PriceRow> rows) throws SQLException {
        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);
        String sql = "INSERT OR REPLACE INTO prices(symbol, timestamp, open, high, low, close, volume) "
                + "VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (PriceRow r : rows) {
                ps.setString(1, r.symbol());
                ps.setLong(2, r.timestamp());
                ps.setDouble(3, r.open());
                ps.setDouble(4, r.high());
                ps.setDouble(5, r.low());
                ps.setDouble(6, r.close());
                ps.setLong(7, r.volume());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    public List<PriceRow> listRecentPrices(String symbol, int limit) throws SQLException {
        String sql = """
        SELECT symbol, timestamp, open, high, low, close, volume
        FROM prices
        WHERE symbol = ?
        ORDER BY timestamp DESC
        LIMIT ?
    """;
        List<PriceRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new PriceRow(
                            rs.getString("symbol"),
                            rs.getLong("timestamp"),
                            rs.getDouble("open"),
                            rs.getDouble("high"),
                            rs.getDouble("low"),
                            rs.getDouble("close"),
                            rs.getLong("volume")
                    ));
                }
            }
        }
        return rows;
    }

}

