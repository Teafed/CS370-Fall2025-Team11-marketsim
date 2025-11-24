package com.models;

import com.etl.HistoricalService;
import com.etl.finnhub.ClientFacade;
import com.models.market.*;
import com.models.market.TradeItem;
import com.models.profile.*;

import javax.swing.*;
import java.sql.SQLException;
import java.util.ArrayList;
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

    private final java.util.Set<String> subscribed = java.util.concurrent.ConcurrentHashMap.newKeySet();
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
                fireWatchlistChanged(items, getPortfolioItems());
            }
        });
        this.hist = new HistoricalService(db);
    }

    // listeners
    public void addListener(ModelListener l) { listeners.add(l); }
    public void removeListener(ModelListener l) { listeners.remove(l); }
    private void fireQuotesUpdated() { onEDT(() -> listeners.forEach(ModelListener::onQuotesUpdated)); }
    private void fireWatchlistChanged(List<TradeItem> watchlist, List<TradeItem> portfolio) { onEDT(() -> listeners.forEach(l -> l.onWatchlistChanged(watchlist, portfolio))); }
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

    // MARKET - queries
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
    public List<TradeItem> getWatchlist() {
        return profile.getActiveAccount().getWatchlist().getWatchlist(); }
    public List<TradeItem> getPortfolioItems() {
        List<String> symbols = profile.getActiveAccount().getPortfolio().getPortfolioItems();
        List<TradeItem> items = new ArrayList<>();
        symbols.forEach(symbol -> {items.add(market.get(symbol));});
        return items;
    }
    public long getLatestTimestamp(String symbol) throws SQLException {
        return db.getLatestTimestamp(symbol);
    }
    // MARKET - commands
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
        CompanyProfile cp = fetchAndCacheCompanyProfile(key);

        String logo = (cp == null ? null : cp.getLogo());
        if (logo != null && !logo.isBlank() && !"unknown".equals(logo)) {
            logoCache.put(key, logo);
        } else {
            logoCache.put(key, null);
        }
        return logoCache.get(key);
    }

    // ACCOUNT - queries
    public long getProfileId() {
        return profile.getId(); // you already use profile.getId() elsewhere
    }
    public String getProfileName() {
        return profile == null ? "" : profile.getOwner();
    }
    public AccountDTO getAccountDTO() throws SQLException {
        Account a = profile.getActiveAccount();
        Map<java.lang.String, Integer> positions = db.getPositions(a.getId());
        return new AccountDTO(a.getId(), a.getCash(), positions);
    }
    public Long getDefaultAccountId() {
        try {
            return db.getDefaultAccountId(profile.getId());
        } catch (Exception e) {
            return null;
        }
    }
    public boolean isDefaultAccount(Account a) {
        if (a == null) return false;
        Long d = getDefaultAccountId();
        return d != null && d == a.getId();
    }
    public List<Account> listAccounts() { return profile.getAccounts(); }
    public Account getActiveAccount() { return profile.getActiveAccount(); }
    public String getActiveAccountName() {
        var a = profile.getActiveAccount();
        return a == null ? "" : a.getName();
    }
    public double getAccountTotalValue() {
        Account a = profile.getActiveAccount();
        double portfolioValue = a.getPortfolio().computeMarketValue(this::getPrice);
        return a.getCash() + portfolioValue;
    }
    public List<TradeItem> getWatchlistView() {
        var wl = profile.getActiveAccount().getWatchlist().getWatchlist();
        ArrayList<TradeItem> out = new ArrayList<>(wl.size());
        for (var ti : wl) {
            var canonical = market.get(ti.getSymbol());
            if (canonical != null) out.add(canonical);
        }
        return out;
    }
    public List<TradeItem> getHoldingsView() {
        try {
            var a = profile.getActiveAccount();
            if (a == null) return List.of();

            var pos = db.getPositions(a.getId()); // Map<String, Integer>
            java.util.Set<String> holdingSyms = pos.entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0)
                    .map(e -> e.getKey().trim().toUpperCase())
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            java.util.Set<String> wlSyms = a.getWatchlist().getWatchlist().stream()
                    .map(TradeItem::getSymbol)
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase())
                    .collect(java.util.stream.Collectors.toSet());
            holdingSyms.removeAll(wlSyms);
            if (holdingSyms.isEmpty()) return List.of();

            java.util.ArrayList<TradeItem> out = new java.util.ArrayList<>(holdingSyms.size());
            for (String s : holdingSyms) {
                var canon = ensureCanonical(s);
                if (canon != null) out.add(canon);
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }
    public List<TradeRow> getRecentTrades(int limit) throws Exception {
        Account a = profile.getActiveAccount();
        if (a == null)
            return List.of();
        return db.listRecentTrades(a.getId(), limit);
    }
    // ACCOUNT - commands
    public void createAccount(String accountName, double initialDeposit) throws Exception {
        if (accountName == null || accountName.isBlank()) throw new IllegalArgumentException("Account name is required.");
        if (initialDeposit < 0) throw new IllegalArgumentException("Initial deposit cannot be negative.");

        long id = db.getOrCreateAccount(accountName.trim(), "USD");
        Account a = new Account(id, accountName.trim());

        if (initialDeposit > 0) {
            db.depositCash(id, initialDeposit, System.currentTimeMillis(), "Initial deposit");
        }
        a.setCash(db.getAccountCash(id));

        // make sure it's profile list if not already
        boolean exists = profile.getAccounts().stream().anyMatch(acc -> acc.getId() == id);
        if (!exists) {
            profile.getAccounts().add(a);
        }

        ensureWatchlistPopulated(a);
        try {
            var positions = db.getPositions(id);
            a.getPortfolio().setFromDb(positions);
        } catch (Exception ignored) { }

        setActiveAccount(a);
    }
    public void setActiveAccount(Account account) throws Exception {
        // don't reload if same account
        var current = profile.getActiveAccount();
        if (current != null && current.getId() == account.getId()) {
            market.addFromWatchlist(account.getWatchlist());
            fireWatchlistChanged(getWatchlistView(), getPortfolioItems());
            fireAccountChanged();
            return;
        }

        profile.setActiveAccount(account);
        ensureWatchlistPopulated(account);
        market.addFromWatchlist(account.getWatchlist());
        market.addFromPortfolio(account.getPortfolio());
        fireWatchlistChanged(getWatchlist(), getPortfolioItems());
        fireAccountChanged();
        System.out.printf("[Model] Account set to %s (ID %d)%n", account.getName(), account.getId());
    }
    public void setDefaultAccount(Account a) throws Exception {
        if (a == null) throw new IllegalArgumentException("Account required");
        db.setDefaultAccountId(profile.getId(), a.getId());
    }
    public void switchAccount(long accountId) throws Exception {
        Account target = profile.getAccounts().stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        setActiveAccount(target);
    }
    public void addToWatchlist(String symbol) throws Exception {
        Account a = profile.getActiveAccount();
        if (a.getPortfolio().hasShare(symbol)) {
            return;
        }
        String sym = (symbol == null ? "" : symbol.trim().toUpperCase());
        if (sym.isEmpty()) throw new IllegalArgumentException("Symbol required");

        TradeItem ti = new TradeItem(sym);
        CompanyProfile cp = fetchAndCacheCompanyProfile(sym);
        if (cp != null) ti.setCompanyProfile(cp); else ti.setNameLookup(ti);

        a.getWatchlist().addWatchlistItem(ti);
        db.saveWatchlistSymbols(a.getId(), "Default", a.getWatchlistItems());
        market.add(ti);

        fireWatchlistChanged(getWatchlistView(), getPortfolioItems());
    }
    public void removeFromWatchlist(TradeItem ti) throws Exception {
        Account a = profile.getActiveAccount();
        a.getWatchlist().removeWatchlistItem(ti);
        db.saveWatchlistSymbols(a.getId(), "Default", a.getWatchlistItems());

        market.remove(ti.getSymbol());
        fireWatchlistChanged(getWatchlistView(), getPortfolioItems());
    }
    public void executeTrade(String symbol, boolean isBuy, int shares) {
        executeTrade(symbol, isBuy, shares, System.currentTimeMillis());
    }
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
            Order order = new Order(a, symbol,
                    isBuy ? Order.side.BUY : Order.side.SELL, shares, price, ts);
            db.recordOrder(order);
            // update in-memory cash/positions and portfolio
            a.setCash(db.getAccountCash(a.getId()));

            // update watchlist
            TradeItem ti = market.get(symbol);
            if (isBuy) {
                //removeFromWatchlist(ti);
            } else {
                addToWatchlist(symbol);
            }
            try {
                var positions = db.getPositions(a.getId()); // symbol -> qty
                a.getPortfolio().setFromDb(positions);
            } catch (Exception e) {
                // non-fatal
            }

            fireAccountChanged();
            fireWatchlistChanged(getWatchlist(), getPortfolioItems());
        } catch (Exception e) {
            fireError("Failed to place order", e);
        }
    }
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

    // DATABASE - commands
    public void close() throws SQLException { db.close(); }

    // helpers
    private CompanyProfile fetchAndCacheCompanyProfile(String symbol) {
        String key = symbol.trim().toUpperCase();
        try {
            CompanyProfile cp = db.getCompanyProfile(key);
            if (cp != null) return cp;

            // fetch from remote
            CompanyProfile fetched = client.fetchInfo(key);
            if (fetched != null) {
                db.upsertCompanyProfile(key, fetched, System.currentTimeMillis());
            }
            return fetched;
        } catch (Exception e) { return null; }
    }
    private void ensureWatchlistPopulated(Account a) throws Exception {
        List<TradeItem> dbSymbols = a.getWatchlistItems();
        if (dbSymbols == null || dbSymbols.isEmpty()) {
            // add defaults and persist
            List<TradeItem> defaults = Watchlist.getDefaultWatchlist();

            for (TradeItem ti : defaults) {
                CompanyProfile cp = fetchAndCacheCompanyProfile(ti.getSymbol());
                if (cp != null) ti.setCompanyProfile(cp); else ti.setNameLookup(ti);
            }
            db.saveWatchlistSymbols(a.getId(), "Default", defaults);
            a.getWatchlist().clearList();
            for (TradeItem ti : defaults) a.getWatchlist().addWatchlistItem(ti);
            System.out.println("[Model] Loaded default watchlist -> DB (" + defaults.size() + ")");
        } else {
            // hydrate in-memory list from DB
            for (TradeItem ti : dbSymbols) {
                if (ti.getCompanyProfile() == null) {
                    CompanyProfile cp = fetchAndCacheCompanyProfile(ti.getSymbol());
                    if (cp != null) ti.setCompanyProfile(cp); else ti.setNameLookup(ti);
                }
            }
            a.getWatchlist().clearList();
            for (TradeItem ti : dbSymbols) a.getWatchlist().addWatchlistItem(ti);
            db.saveWatchlistSymbols(a.getId(), "User List", dbSymbols);
            System.out.println("[Model] Loaded watchlist from DB (" + dbSymbols.size() + ")");
        }
    }
    private TradeItem ensureCanonical(String sym) {
        var ti = market.get(sym);
        if (ti != null) return ti;
        try {
            market.add(new com.models.market.TradeItem(sym));
        } catch (Exception ignore) {}
        return market.get(sym); // may still be null if add failed; callers should null-check
    }

    // HistoricalService
    public record CandlePoint(long t, double close) { }
    public record Range(HistoricalService.Timespan timespan, int mult, long startMs, long endMs) { }
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
}