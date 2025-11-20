package com.models;

import com.etl.HistoricalService;
import com.etl.finnhub.ClientFacade;
import com.models.market.*;
import com.models.market.TradeItem;
import com.models.profile.*;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The central facade for the application model.
 * Coordinates interactions between the database, market data clients, and UI
 * components.
 * Manages the active profile, account state, and market data updates.
 */
public class ModelFacade {
    private final Database db;
    private final ClientFacade client;
    private final Market market;
    private final Profile profile;
    private final List<ModelListener> listeners = new CopyOnWriteArrayList<>();
    private final HistoricalService hist;

    private final Map<String, String> logoCache = new ConcurrentHashMap<>();
    private final Map<String, Long> logoFetchAttempts = new ConcurrentHashMap<>();
    private static final long LOGO_RETRY_DELAY_MS = 60000; // 1 minute before retry

    public record TradeRow(long id, long timestamp, String side, String symbol, int quantity, double price, int posAfter) { }

    /**
     * Constructs a new ModelFacade.
     * Initializes the market client, historical service, and loads the active
     * account's watchlist.
     *
     * @param db      The database instance.
     * @param profile The active user profile.
     * @throws Exception If an error occurs during initialization.
     */
    public ModelFacade(Database db, Profile profile) throws Exception {
        this.db = db;
        this.profile = profile;
        this.client = new ClientFacade();
        this.market = new Market(client);
        this.market.setMarketListener(new MarketListener() {
            @Override
            public void onMarketUpdate() {
                fireQuotesUpdated();
            }
            @Override
            public void loadSymbols(List<TradeItem> items) {
                fireWatchlistChanged(items);
            }
        });
        this.hist = new HistoricalService(db);

        ensureWatchlistPopulated(profile.getActiveAccount());
        market.addFromWatchlist(profile.getActiveAccount().getWatchlist());
    }

    // listeners
    public void addListener(ModelListener l) { listeners.add(l); }
    public void removeListener(ModelListener l) { listeners.remove(l); }
    private void fireQuotesUpdated() { onEDT(() -> listeners.forEach(ModelListener::onQuotesUpdated)); }
    private void fireWatchlistChanged(List<TradeItem> items) { onEDT(() -> listeners.forEach(l -> l.onWatchlistChanged(items))); }
    private void fireAccountChanged() {
        onEDT(() -> listeners.forEach(l -> {
            try {
                l.onAccountChanged(getAccountDTO());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private void fireError(String msg, Throwable t) {
        onEDT(() -> listeners.forEach(l -> l.onError(msg, t)));
    }

    private static void onEDT(Runnable r) {
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);
    }

    // queries
    /**
     * Checks if the market is currently open.
     *
     * @return True if open, false otherwise.
     */
    public boolean isMarketOpen() {
        return client.getMarketStatus();
    }

    public double getPrice(String symbol) {
        return getPrice(symbol, System.currentTimeMillis());
    }

    /**
     * Gets the price of a symbol at a specific timestamp.
     * Tries to get a live price first, then falls back to historical data from the
     * database.
     *
     * @param symbol The stock symbol.
     * @param ts     The timestamp in milliseconds.
     * @return The price, or Double.NaN if unavailable.
     */
    public double getPrice(String symbol, long ts) {
        if (symbol == null)
            return Double.NaN;
        String sym = symbol.trim().toUpperCase();

        // if market is open
        double live = market.getPrice(sym);
        if (!Double.isNaN(live))
            return live;

        // otherwise fallback to db
        try {
            double px = db.getCloseAtOrBefore(sym, ts, 1, "day");
            if (!Double.isNaN(px)) return px;

            px = db.getFirstClose(sym, 1, "day");
            if (!Double.isNaN(px)) return px;

            double[] lp = db.latestAndPrevClose(sym, 1, "day");
            return lp[0];
        } catch (Exception e) {
            return Double.NaN;
        }
    }
    public Account getActiveAccount() { return profile.getActiveAccount(); }
    public List<Account> listAccounts() { return profile.getAccounts(); }
    public List<TradeItem> getWatchlist() { return profile.getActiveAccount().getWatchlist().getWatchlist(); }
    public AccountDTO getAccountDTO() throws SQLException {
        Account a = profile.getActiveAccount();
        Map<java.lang.String, Integer> positions = db.getPositions(a.getId());
        return new AccountDTO(a.getId(), a.getCash(), positions);
    }

    /**
     * Calculates the total value of the active account (cash + portfolio market
     * value).
     *
     * @return The total account value.
     */
    public double getAccountTotalValue() {
        Account a = profile.getActiveAccount();
        double portfolioValue = a.getPortfolio().computeMarketValue(this::getPrice);
        return a.getCash() + portfolioValue;
    }
    public long getLatestTimestamp(String symbol) throws SQLException {
        return db.getLatestTimestamp(symbol);
    }
    public List<TradeRow> getRecentTrades(int limit) throws Exception {
        Account a = profile.getActiveAccount();
        if (a == null)
            return java.util.List.of();
        return db.listRecentTrades(a.getId(), limit);
    }

    // commands
    public void close() throws SQLException { db.close(); }

    /**
     * Sets the active account and updates the model state.
     * Loads the watchlist for the new account and notifies listeners.
     *
     * @param account The account to make active.
     * @throws Exception If an error occurs.
     */
    public void setActiveAccount(Account account) throws Exception {
        profile.setActiveAccount(account);
        ensureWatchlistPopulated(account);
        market.addFromWatchlist(account.getWatchlist());
        fireWatchlistChanged(getWatchlist());
        fireAccountChanged();
    }

    private void ensureWatchlistPopulated(Account a) throws Exception {
        List<TradeItem> dbSymbols = a.getWatchlistItems();
        if (dbSymbols == null || dbSymbols.isEmpty()) {
                // add defaults and persist
                List<TradeItem> defaults = Watchlist.getDefaultWatchlist();
                db.saveWatchlistSymbols(a.getId(), "Default", defaults);
                a.getWatchlist().clearList();
                for (TradeItem ti : defaults) a.getWatchlist().addWatchlistItem(ti);
                System.out.println("[facade] Loaded default watchlist -> DB (" + defaults.size() + ")");
            } else {
                // hydrate in-memory list from DB
                a.getWatchlist().clearList();
                for (TradeItem ti : dbSymbols) a.getWatchlist().addWatchlistItem(ti);
                db.saveWatchlistSymbols(a.getId(), "User List", dbSymbols);
                System.out.println("[facade] Loaded watchlist from DB (" + dbSymbols.size() + ")");
            }
    }

    public void addToWatchlist(String name, String symbol) throws Exception {
    /**
     * Adds a symbol to the active account's watchlist.
     *
     * @param symbol The stock symbol to add.
     * @throws Exception If an error occurs.
     */
        Account a = profile.getActiveAccount();
        TradeItem ti = new TradeItem(name, symbol);
        market.add(ti);
        a.getWatchlist().addWatchlistItem(ti);
        db.saveWatchlistSymbols(a.getId(), "Default", a.getWatchlistItems());
        client.subscribe(symbol);
        fireWatchlistChanged(getWatchlist());
    }

    public void removeFromWatchlist(TradeItem ti) {
        Account a = profile.getActiveAccount();
        a.getWatchlist().removeWatchlistItem(ti);
        try {
            db.saveWatchlistSymbols(a.getId(), "Default", a.getWatchlistItems());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //unsubscribe from client
        fireWatchlistChanged(getWatchlist());
    }

    public void executeTrade(String symbol, boolean isBuy, int shares) {
        executeTrade(symbol, isBuy, shares, System.currentTimeMillis());
    }

    /**
     * Executes a trade for the active account.
     * Validates the trade, records it in the database, and updates the account
     * state.
     *
     * @param symbol The stock symbol.
     * @param isBuy  True for a buy order, false for sell.
     * @param shares The number of shares.
     * @param ts     The timestamp of the trade.
     */
    public void executeTrade(String symbol, boolean isBuy, int shares, long ts) {
        // if ts is today and market is open get price from there
            // otherwise price is gotten from db:
            //    order times outside of range available in db should just get latest/earliest price
        try {
            Account a = profile.getActiveAccount();
            if (a == null) throw new IllegalStateException("No active account");
            symbol = (symbol == null ? "" : symbol.trim().toUpperCase());
            if (symbol.isEmpty()) throw new IllegalArgumentException("Symbol required");
            if (shares <= 0) throw new IllegalArgumentException("Shares must be > 0");

            double price = getPrice(symbol, ts);
            if (Double.isNaN(price)) throw new IllegalStateException("No available price for " + symbol);

            if (isBuy) {
                double needed = shares * price;
                double cash = db.getAccountCash(a.getId());
                if (cash + 1e-6 < needed) {
                    throw new IllegalStateException("Insufficient cash: need " + needed + " have " + cash);
                }
            }

            // persist trade in db
            Order order = new Order(a, new TradeItem(symbol, symbol),
                    isBuy ? Order.side.BUY : Order.side.SELL, shares, price, ts);
            db.recordOrder(order);
            // update in-memory cash/positions and portfolio
            a.setCash(db.getAccountCash(a.getId()));
            try {
                var positions = db.getPositions(a.getId()); // symbol -> qty
                a.getPortfolio().setFromDb(positions);
            } catch (Exception e) {
                // non-fatal
            }

            fireAccountChanged();
        } catch (Exception e) {
            fireError("Failed to place order", e);
        }
    }

    /**
     * Deposits cash into the active account.
     *
     * @param amount The amount to deposit.
     * @param memo   A memo for the transaction.
     */
    public void deposit(double amount, String memo) {
        try {
            Account a = profile.getActiveAccount();
            db.depositCash(a.getId(), amount, System.currentTimeMillis(), memo);
            a.setCash(db.getAccountCash(a.getId()));
            fireAccountChanged();
        } catch (Exception e) {
            fireError("Failed to deposit", e);
        }
    }

    // HistoricalService
    public record CandlePoint(long t, double close) { }
    public static record Range(HistoricalService.Timespan timespan, int mult, long startMs, long endMs) { }

    /**
     * Ensures that historical data exists for the requested range.
     * Checks the database and fetches missing data from the API if necessary.
     *
     * @param symbol    The stock symbol.
     * @param requested The requested data range.
     * @return The range that was actually ensured (may be adjusted), or null if
     *         failed.
     * @throws Exception If an error occurs.
     */
    public Range ensureRange(String symbol, Range requested) throws Exception {
        var utc = java.time.ZoneOffset.UTC;

        var from = java.time.Instant.ofEpochMilli(requested.startMs).atZone(utc).toLocalDate();
        var to = java.time.Instant.ofEpochMilli(requested.endMs).atZone(utc).toLocalDate();
        var r = new HistoricalService.Range(requested.timespan, requested.mult, from, to);
        var missing = hist.ensureRange(symbol, r);
        if (missing == null) return null;

        long startMs = missing.from.atStartOfDay(utc).toInstant().toEpochMilli();
        long endMs = missing.to.atStartOfDay(utc).toInstant().toEpochMilli();
        return new Range(missing.timespan, missing.multiplier, startMs, endMs);
    }

    /**
     * Backfills missing historical data for a range.
     *
     * @param symbol  The stock symbol.
     * @param missing The range of missing data.
     * @return The number of candles added.
     * @throws Exception If an error occurs.
     */
    public int backfillRange(String symbol, Range missing) throws Exception {
        var utc = java.time.ZoneOffset.UTC;
        var from = java.time.Instant.ofEpochMilli(missing.startMs).atZone(utc).toLocalDate();
        var to = java.time.Instant.ofEpochMilli(missing.endMs).atZone(utc).toLocalDate();
        var r = new HistoricalService.Range(missing.timespan, missing.mult, from, to);
        return hist.backfillRange(symbol, r);
    }

    /**
     * Loads close prices for a symbol within a time range, downsampled to a maximum
     * number of points.
     * Useful for charting.
     *
     * @param symbol    The stock symbol.
     * @param startMs   The start timestamp.
     * @param endMs     The end timestamp.
     * @param maxPoints The maximum number of points to return.
     * @return A list of CandlePoint objects.
     * @throws Exception If an error occurs.
     */
    public List<CandlePoint> loadCloses(String symbol, long startMs, long endMs, int maxPoints)
            throws Exception {
        try (var rs = db.getCandles(symbol, 1, "day", startMs, endMs)) {
            java.util.TreeMap<Long, Double> sorted = new java.util.TreeMap<>();
            while (rs.next()) {
                sorted.put(rs.getLong("timestamp"), rs.getDouble("close"));
            }
            if (sorted.isEmpty()) return List.of();

            int dataSize = sorted.size();
            int step = Math.max(1, dataSize / Math.max(1, maxPoints));
            java.util.ArrayList<CandlePoint> out = new java.util.ArrayList<>();
            int i = 0;
            for (var e : sorted.entrySet()) {
                if (i++ % step != 0) continue;
                out.add(new CandlePoint(e.getKey(), e.getValue()));
            }
            return out;
        }
    }

    public String[][] searchSymbol(String symbol) {
        return market.searchSymbol(symbol);
    }

    /**
     * Get the company logo URL for a symbol. Returns null on failure.
     * Uses caching to avoid repeated API calls and rate limiting.
     *
     * @param symbol the stock symbol
     * @return logo URL string or null
     */
    public String getLogoForSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }

        String key = symbol.trim().toUpperCase();

        // Check cache first
        if (logoCache.containsKey(key)) {
            return logoCache.get(key);
        }

        // Check if we recently failed to fetch this logo
        Long lastAttempt = logoFetchAttempts.get(key);
        if (lastAttempt != null) {
            long elapsed = System.currentTimeMillis() - lastAttempt;
            if (elapsed < LOGO_RETRY_DELAY_MS) {
                // Too soon to retry, return null
                return null;
            }
        }

        // Record this fetch attempt
        logoFetchAttempts.put(key, System.currentTimeMillis());

        try {
            com.etl.CompanyProfile profile = client.fetchInfo(key);
            if (profile != null) {
                String logo = profile.getLogo();
                // Check if logo is valid (not null, not blank, not "unknown")
                if (logo != null && !logo.isBlank() && !"unknown".equals(logo)) {
                    logoCache.put(key, logo);
                    return logo;
                }
            }
            // Cache null result to avoid repeated failed lookups
            logoCache.put(key, null);
        } catch (Exception e) {
            // Silently fail and cache null
            logoCache.put(key, null);
        }
        return null;
    }
}