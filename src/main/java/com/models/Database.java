package com.models;

import com.models.market.TradeItem;
import com.models.profile.Account;
import com.models.profile.Profile;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database implements AutoCloseable {
    private final Connection conn;

    public enum StartupState {
        FIRST_RUN,           // no profile exists
        PROFILE_NO_ACCOUNTS, // profile exists, but no accounts yet
        READY                // profile + >=1 account
    }

    // used to insert a candle entry into database
    public record CandleData(String symbol, long timestamp,
                             double open, double high, double low, double close, double volume) { }
    // for getting info about a position
    public static record PositionView(String symbol, int quantity, double avgCost) { }

    public Database(String dbFile) throws SQLException {
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
        ensurePortfolioSchema();
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
    private void ensurePortfolioSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            // trades
            st.execute("""
                CREATE TABLE IF NOT EXISTS trades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id INTEGER NOT NULL,
                symbol TEXT NOT NULL,
                timestamp_ms INTEGER NOT NULL,
                side TEXT NOT NULL CHECK (side IN ('BUY','SELL')),
                quantity INTEGER NOT NULL,          -- whole shares; use REAL if you support fractional shares
                price REAL NOT NULL,                -- trade price per share
                FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_trades_acct_time ON trades(account_id, timestamp_ms)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_trades_acct_symbol ON trades(account_id, symbol)");

            // 2) cash_ledger
            st.execute("""
                CREATE TABLE IF NOT EXISTS cash_ledger (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id INTEGER NOT NULL,
                timestamp_ms INTEGER NOT NULL,
                delta REAL NOT NULL,            -- +deposit, -withdrawal, +sell proceeds, -buy cost
                reason TEXT NOT NULL CHECK (reason IN ('DEPOSIT','WITHDRAWAL','TRADE')),
                ref_trade_id INTEGER,           -- nullable; points to trades row when reason=TRADE
                note TEXT,
                FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE,
                FOREIGN KEY(ref_trade_id) REFERENCES trades(id) ON DELETE SET NULL
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_cash_acct_time ON cash_ledger(account_id, timestamp_ms)");

            // positions (quantity + average cost)
            st.execute("""
                CREATE TABLE IF NOT EXISTS positions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id INTEGER NOT NULL,
                symbol TEXT NOT NULL,
                quantity INTEGER NOT NULL,       -- whole shares; match trades.quantity type
                avg_cost REAL NOT NULL,          -- average cost per share for remaining shares
                last_updated_ms INTEGER NOT NULL,
                UNIQUE(account_id, symbol),
                FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
                )
            """);
            st.execute("CREATE INDEX IF NOT EXISTS idx_positions_acct ON positions(account_id)");
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
    public List<Long> listTimestamps(String symbol, int multiplier, String timespan,
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
                                   List<CandleData> rows) throws SQLException {
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

    // profile/account
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
    public long getOrCreateAccount(String accountName, String baseCurrency) throws SQLException {
        long profileId = getSingletonProfileId();
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
    public List<TradeItem> loadWatchlistSymbols(long accountId) throws SQLException {
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
                ArrayList<TradeItem> out = new ArrayList<>();
                while (rs.next()) {
                    String symbol = rs.getString(1);
                    TradeItem ti = new TradeItem("Unknown Name", symbol);
                    ti.setNameLookup(ti);
                    out.add(ti);
                }
                return out;
            }
        }
    }
    public void saveWatchlistSymbols(long accountId, String watchlistName, List<TradeItem> symbols) throws SQLException {
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
                    for (TradeItem sym : symbols) {
                        if (sym == null) continue;
                        String s = sym.getSymbol();
                        if (s == null || s.isBlank()) continue; // skip invalid
                        ins.setLong(1, watchlistId);
                        ins.setString(2, s);
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
    public String getProfileName(long profileId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT name FROM profiles WHERE id=?")) {
            ps.setLong(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
                throw new SQLException("No profile found for id=" + profileId);
            }
        }
    }
    public List<Account> listAccounts(long profileId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name FROM accounts WHERE profile_id=? ORDER BY name")) {
            ps.setLong(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                ArrayList<Account> out = new ArrayList<>();
                while (rs.next()) out.add(new Account(rs.getLong(1), rs.getString(2)));
                return out;
            }
        }
    }

    // portfolio helpers
    public long depositCash(long accountId, double amount, long ts, String note) throws SQLException {
        return recordCash(accountId, ts, +Math.abs(amount), "DEPOSIT", note);
    }
    public long withdrawCash(long accountId, double amount, long ts, String note) throws SQLException {
        return recordCash(accountId, ts, -Math.abs(amount), "WITHDRAWAL", note);
    }
    public double getAccountCash(long accountId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT COALESCE(SUM(delta), 0.0) FROM cash_ledger WHERE account_id=?
        """)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0.0; }
        }
    }
    private long recordCash(long accountId, long ts, double delta, String reason, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO cash_ledger(account_id, timestamp_ms, delta, reason, note)
            VALUES(?,?,?,?,?)
        """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, accountId);
            ps.setLong(2, ts);
            ps.setDouble(3, delta);
            ps.setString(4, reason);
            ps.setString(5, note);
            ps.executeUpdate();
            try (ResultSet ks = ps.getGeneratedKeys()) { return ks.next() ? ks.getLong(1) : 0L; }
        }
    }
    private void upsertPositionFromTrade(long accountId, String symbol, String side,
                                         int qty, double price, long ts) throws SQLException {
        int curQty = 0;
        double curAvg = 0.0;

        try (PreparedStatement sel = conn.prepareStatement("""
            SELECT quantity, avg_cost FROM positions
            WHERE account_id=? AND symbol=?
        """)) {
            sel.setLong(1, accountId);
            sel.setString(2, symbol);
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    curQty = rs.getInt(1);
                    curAvg = rs.getDouble(2);
                }
            }
        }

        int newQty;
        double newAvg;

        if ("BUY".equals(side)) {
            newQty = curQty + qty;
            if (newQty == 0) { // shouldn’t happen with buy, but guard anyway
                newAvg = 0.0;
            } else {
                // weighted average cost
                newAvg = ((curQty * curAvg) + (qty * price)) / newQty;
            }
        } else { // SELL
            newQty = curQty - qty;
            if (newQty < 0) throw new SQLException("Sell exceeds position for " + symbol);
            newAvg = (newQty == 0) ? 0.0 : curAvg; // avg cost unchanged for remaining shares
        }

        if (newQty == 0) {
            try (PreparedStatement del = conn.prepareStatement("""
                DELETE FROM positions WHERE account_id=? AND symbol=?
            """)) {
                del.setLong(1, accountId);
                del.setString(2, symbol);
                del.executeUpdate();
            }
        } else {
            try (PreparedStatement up = conn.prepareStatement("""
            INSERT INTO positions(account_id, symbol, quantity, avg_cost, last_updated_ms)
            VALUES(?,?,?,?,?)
            ON CONFLICT(account_id, symbol)
            DO UPDATE SET quantity=excluded.quantity,
                          avg_cost=excluded.avg_cost,
                          last_updated_ms=excluded.last_updated_ms
        """)) {
                up.setLong(1, accountId);
                up.setString(2, symbol);
                up.setInt(3, newQty);
                up.setDouble(4, newAvg);
                up.setLong(5, ts);
                up.executeUpdate();
            }
        }
    }

    public long recordOrder(com.models.market.Order order) throws SQLException {
        // single entry point for an executed order
        long accountId = order.account().getId();
        String symbol = order.tradeItem().getSymbol();
        long ts = order.ts();
        String side = order.side().name();  // "BUY" or "SELL"
        int qty = order.shares();
        double price = order.price();
        return recordTrade(accountId, symbol, ts, side, qty, price);
    }

    public void applyFill(long accountId, String symbol, int deltaShares, double price, long ts) throws SQLException {
        // compatibility wrapper used by ModelFacade.placeOrder(...)
        if (deltaShares == 0) return;
        String side = (deltaShares > 0) ? "BUY" : "SELL";
        int qty = Math.abs(deltaShares);
        recordTrade(accountId, symbol, ts, side, qty, price);
    }
    public long recordTrade(long accountId, String symbol, long ts, String side,
                            int quantity, double price) throws SQLException {
        if (!"BUY".equals(side) && !"SELL".equals(side)) {
            throw new IllegalArgumentException("side must be BUY or SELL");
        }
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");

        boolean prev = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (PreparedStatement insTrade = conn.prepareStatement("""
                INSERT INTO trades(account_id, symbol, timestamp_ms, side, quantity, price)
                VALUES(?,?,?,?,?,?)
            """, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insCash = conn.prepareStatement("""
                INSERT INTO cash_ledger(account_id, timestamp_ms, delta, reason, ref_trade_id, note)
                VALUES(?,?,?,?,?,?)
            """)) {

            // 1) insert trade
            insTrade.setLong(1, accountId);
            insTrade.setString(2, symbol);
            insTrade.setLong(3, ts);
            insTrade.setString(4, side);
            insTrade.setInt(5, quantity);
            insTrade.setDouble(6, price);
            insTrade.executeUpdate();

            long tradeId;
            try (ResultSet ks = insTrade.getGeneratedKeys()) {
                if (!ks.next()) throw new SQLException("No trade id");
                tradeId = ks.getLong(1);
            }

            // 2) cash impact: BUY = -qty*price; SELL = +qty*price
            double delta = ("BUY".equals(side) ? -1.0 : 1.0) * (quantity * price);
            insCash.setLong(1, accountId);
            insCash.setLong(2, ts);
            insCash.setDouble(3, delta);
            insCash.setString(4, "TRADE");
            insCash.setLong(5, tradeId);
            insCash.setString(6, symbol + " " + side);
            insCash.executeUpdate();

            // 3) update positions (avg cost method)
            upsertPositionFromTrade(accountId, symbol, side, quantity, price, ts);

            conn.commit();
            return tradeId;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(prev);
        }
    }
    public java.util.Map<String, Integer> getPositions(long accountId) throws SQLException {
        String sql = "SELECT symbol, quantity FROM positions WHERE account_id=? ORDER BY symbol";
        java.util.LinkedHashMap<String, Integer> out = new java.util.LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return out;
    }

    // startup helpers
    public long getExistingProfileIdOrZero() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM profiles ORDER BY id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
    public long ensureSingletonProfile(String name) throws SQLException {
        long existing = getExistingProfileIdOrZero();
        if (existing != 0L) return existing;

        try (PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO profiles(name) VALUES(?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ins.setString(1, name);
            ins.executeUpdate();
            try (ResultSet ks = ins.getGeneratedKeys()) {
                if (ks.next()) return ks.getLong(1);
                throw new SQLException("Failed to create singleton profile");
            }
        }
    }
    public long getSingletonProfileId() throws SQLException {
        long id = getExistingProfileIdOrZero();
        if (id == 0L) throw new SQLException("No profile found");
        return id;
    }
    public boolean profileHasAccounts(long profileId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT EXISTS(SELECT 1 FROM accounts WHERE profile_id=?)")) {
            ps.setLong(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }
    public Profile buildProfile(long profileId) throws SQLException {
        String profileName = getProfileName(profileId);

        ArrayList<Account> accounts = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT id, name
                FROM accounts
                WHERE profile_id=?
                ORDER BY name
            """)) {
            ps.setLong(1, profileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long accountId = rs.getLong(1);
                    String accountName = rs.getString(2);
                    Account a = new Account(accountId, accountName);

                    // balance
                    double balance = getAccountCash(accountId);
                    a.setCash(balance);

                    // watchlist
                    List<TradeItem> wl = loadWatchlistSymbols(accountId);
                    if (!wl.isEmpty()) {
                        a.getWatchlist().clearList();
                        for (TradeItem ti : wl) {
                            a.getWatchlist().addWatchlistItem(ti);
                        }
                    }

                    // TODO: load up the portfolios for each account
                    // a.setPortfolio(loadPositions(accountId));

                    accounts.add(a);
                }
            }
        }
        Profile p = new Profile(accounts);
        p.setOwner(profileName);
        return p;
    }
    public StartupState determineStartupState() throws SQLException {
        long profileId = getExistingProfileIdOrZero();
        if (profileId == 0L) return StartupState.FIRST_RUN;
        return profileHasAccounts(profileId) ? StartupState.READY : StartupState.PROFILE_NO_ACCOUNTS;
    }
}
