package com.market;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager implements AutoCloseable {
    private final Connection conn;

    // used to insert a candle entry into database
    public record CandleData(String symbol, long timestamp,
                             double open, double high, double low, double close, double volume) { }

    public DatabaseManager(String dbFile) throws SQLException {
        String url = "jdbc:sqlite:" + dbFile + "?busy_timeout=5000"; // 5s
        this.conn = DriverManager.getConnection(url);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
        createSchema();
    }

    @Override public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    private void createSchema() throws SQLException {
        ensurePricesSchema();
        ensureUserSchema();
    }

    private void ensurePricesSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
            CREATE TABLE IF NOT EXISTS prices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                symbol TEXT NOT NULL,
                timespan TEXT NOT NULL,
                multiplier INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                open REAL, high REAL, low REAL, close REAL,
                volume REAL,
                UNIQUE(symbol, timespan, multiplier, timestamp)
                )
            """);
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_prices_symbol_tf_ts
                ON prices(symbol, timespan, multiplier, timestamp)
            """);
        }
    }

    private void ensureUserSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
            CREATE TABLE IF NOT EXISTS profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s','now')*1000)
                )
            """);

            st.execute("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                profile_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                base_currency TEXT NOT NULL DEFAULT 'USD',
                UNIQUE(profile_id, name),
                FOREIGN KEY(profile_id) REFERENCES profiles(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
            CREATE TABLE IF NOT EXISTS watchlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id INTEGER NOT NULL,
                name TEXT NOT NULL DEFAULT 'Default',
                UNIQUE(account_id, name),
                FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
            CREATE TABLE IF NOT EXISTS watchlist_items (
                watchlist_id INTEGER NOT NULL,
                symbol TEXT NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY (watchlist_id, symbol),
                UNIQUE (watchlist_id, position),
                FOREIGN KEY(watchlist_id) REFERENCES watchlists(id) ON DELETE CASCADE
                )
            """);
        }
    }

    private boolean tableExists(String name) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
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

    /* old version */
    public ResultSet getCandles(String symbol, long startMs, long endMs) throws SQLException {
        return getCandles(symbol, 1, "day", startMs, endMs);
    }

    public ResultSet getCandles(String symbol, int multiplier, String timespan,
                                long startMs, long endMs) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("""
            SELECT timestamp, open, high, low, close, volume
            FROM prices
            WHERE symbol = ? AND timespan = ? AND multiplier = ?
              AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp ASC
        """);
        ps.setString(1, symbol);
        ps.setString(2, timespan);
        ps.setInt(3, multiplier);
        ps.setLong(4, startMs);
        ps.setLong(5, endMs);
        return ps.executeQuery();
    }

    /* old version */
    public long getLatestTimestamp(String symbol) throws SQLException {
        return getLatestTimestamp(symbol, 1, "day");
    }

    public long getLatestTimestamp(String symbol, int multiplier, String timespan) throws SQLException {
        String sql = "SELECT MAX(timestamp) FROM prices WHERE symbol=? AND timespan=? AND multiplier=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setString(2, timespan);
            ps.setInt(3, multiplier);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    public long getEarliestTimestamp(String symbol, int multiplier, String timespan) throws SQLException {
        String sql = "SELECT MIN(timestamp) FROM prices WHERE symbol=? AND timespan=? AND multiplier=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setString(2, timespan);
            ps.setInt(3, multiplier);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    public java.util.List<Long> listTimestamps(String symbol, int multiplier, String timespan,
                                               long startMs, long endMs) throws SQLException {
        String sql = """
            SELECT timestamp FROM prices
            WHERE symbol=? AND timespan=? AND multiplier=?
              AND timestamp BETWEEN ? AND ?
            ORDER BY timestamp ASC
        """;
        java.util.ArrayList<Long> out = new java.util.ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setString(2, timespan);
            ps.setInt(3, multiplier);
            ps.setLong(4, startMs);
            ps.setLong(5, endMs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getLong(1));
            }
        }
        return out;
    }

    /* old voision */
    public double[] latestAndPrevClose(String symbol) throws SQLException {
        return latestAndPrevClose(symbol, 1, "day");
    }

    public double[] latestAndPrevClose(String symbol, int multiplier, String timespan) throws SQLException {
        String sql = """
            SELECT close FROM prices
            WHERE symbol = ? AND timespan = ? AND multiplier = ?
            ORDER BY timestamp DESC
            LIMIT 2
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setString(2, timespan);
            ps.setInt(3, multiplier);
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


    public void insertCandle(String symbol, int multiplier, String timespan,
                             long timestamp, double open, double high, double low,
                             double close, double volume) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO prices(symbol, timespan, multiplier, timestamp, open, high, low, close, volume)
            VALUES(?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setString(2, timespan);
            ps.setInt(3, multiplier);
            ps.setLong(4, timestamp);
            ps.setDouble(5, open);
            ps.setDouble(6, high);
            ps.setDouble(7, low);
            ps.setDouble(8, close);
            ps.setDouble(9, volume);
            ps.executeUpdate();
        }
    }

    public void insertCandlesBatch(String symbol, int multiplier, String timespan,
                                   java.util.List<CandleData> rows) throws SQLException {
        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);
        String sql = """
            INSERT OR REPLACE INTO prices(symbol, timespan, multiplier, timestamp, open, high, low, close, volume)
            VALUES(?,?,?,?,?,?,?,?,?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (CandleData r : rows) {
                ps.setString(1, symbol);
                ps.setString(2, timespan);
                ps.setInt(3, multiplier);
                ps.setLong(4, r.timestamp());
                ps.setDouble(5, r.open());
                ps.setDouble(6, r.high());
                ps.setDouble(7, r.low());
                ps.setDouble(8, r.close());
                ps.setDouble(9, r.volume());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } finally {
            conn.setAutoCommit(prev);
        }
    }

    // profile

    public long getOrCreateProfile(String name) throws SQLException {
        try (PreparedStatement sel = conn.prepareStatement("SELECT id FROM profiles WHERE name=?")) {
            sel.setString(1, name);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO profiles(name) VALUES(?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ins.setString(1, name);
            ins.executeUpdate();
            try (ResultSet rs = ins.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to create profile: " + name);
    }

    public long getOrCreateAccount(long profileId, String accountName, String baseCurrency) throws SQLException {
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT id FROM accounts WHERE profile_id=? AND name=?")) {
            sel.setLong(1, profileId);
            sel.setString(2, accountName);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO accounts(profile_id, name, base_currency) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ins.setLong(1, profileId);
            ins.setString(2, accountName);
            ins.setString(3, baseCurrency == null ? "USD" : baseCurrency);
            ins.executeUpdate();
            try (ResultSet rs = ins.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to create account: " + accountName);
    }

    public java.util.List<String> loadWatchlistSymbols(long accountId) throws SQLException {
        Long watchlistId = null;
        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT id FROM watchlists WHERE account_id=?")) {
            sel.setLong(1, accountId);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) watchlistId = rs.getLong(1);
            }
        }
        if (watchlistId == null) return java.util.Collections.emptyList();

        try (PreparedStatement sel = conn.prepareStatement(
                "SELECT symbol FROM watchlist_items WHERE watchlist_id=? ORDER BY position ASC")) {
            sel.setLong(1, watchlistId);
            try (ResultSet rs = sel.executeQuery()) {
                java.util.ArrayList<String> out = new java.util.ArrayList<>();
                while (rs.next()) out.add(rs.getString(1));
                return out;
            }
        }
    }

    public void saveWatchlistSymbols(long accountId, String watchlistName, java.util.List<String> symbols) throws SQLException {
        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            long watchlistId;
            // ensure a watchlist row
            try (PreparedStatement sel = conn.prepareStatement(
                    "SELECT id FROM watchlists WHERE account_id=? AND name=?")) {
                sel.setLong(1, accountId);
                sel.setString(2, watchlistName);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) {
                        watchlistId = rs.getLong(1);
                    } else {
                        try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO watchlists(account_id, name) VALUES(?,?)",
                                Statement.RETURN_GENERATED_KEYS)) {
                            ins.setLong(1, accountId);
                            ins.setString(2, watchlistName);
                            ins.executeUpdate();
                            try (ResultSet ks = ins.getGeneratedKeys()) {
                                if (ks.next()) watchlistId = ks.getLong(1);
                                else throw new SQLException("No watchlist id");
                            }
                        }
                    }
                }
            }

            // clear & repopulate
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM watchlist_items WHERE watchlist_id=?")) {
                del.setLong(1, watchlistId);
                del.executeUpdate();
            }

            if (symbols != null) {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO watchlist_items(watchlist_id, symbol, position) VALUES(?,?,?)")) {
                    int pos = 0;
                    for (String sym : symbols) {
                        ins.setLong(1, watchlistId);
                        ins.setString(2, sym);
                        ins.setInt(3, pos++);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(prev);
        }
    }
}

