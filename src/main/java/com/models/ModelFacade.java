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
import java.util.concurrent.CopyOnWriteArrayList;

public class ModelFacade {
    private final Database db;
    private final ClientFacade client;
    private final Market market;
    private final Profile profile;
    private final List<ModelListener> listeners = new CopyOnWriteArrayList<>();
    private final HistoricalService hist;

    public record TradeRow(long id, long timestamp, String side, String symbol, int quantity, double price, int posAfter) { }

    public ModelFacade(Database db, Profile profile) throws Exception {
        this.db = db;
        this.profile = profile;
        this.client = new ClientFacade();
        this.market = new Market(client);
        this.market.setMarketListener(new MarketListener() {
            @Override public void onMarketUpdate() { fireQuotesUpdated(); }
            @Override public void loadSymbols(List<TradeItem> items) { fireWatchlistChanged(items); }
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
    private void fireError(String msg, Throwable t) { onEDT(() -> listeners.forEach(l -> l.onError(msg, t))); }
    private static void onEDT(Runnable r) { if (SwingUtilities.isEventDispatchThread()) r.run(); else SwingUtilities.invokeLater(r); }

    // queries
    public boolean isMarketOpen() { return client.getMarketStatus(); }
    public double getPrice(String symbol) { return getPrice(symbol, System.currentTimeMillis()); }
    public double getPrice(String symbol, long ts) {
        if (symbol == null) return Double.NaN;
        String sym = symbol.trim().toUpperCase();

        // if market is open
        double live = market.getPrice(sym);
        if (!Double.isNaN(live)) return live;

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
        Map<java.lang.String,Integer> positions = db.getPositions(a.getId());
        return new AccountDTO(a.getId(), a.getCash(), positions);
    }
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
        if (a == null) return java.util.List.of();
        return db.listRecentTrades(a.getId(), limit);
    }
    // commands
    public void close() throws SQLException { db.close(); }

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
            System.out.println("Here");
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

    public void addToWatchlist(String symbol) throws Exception {
        Account a = profile.getActiveAccount();
        TradeItem ti = new TradeItem(symbol, symbol);
        a.getWatchlist().addWatchlistItem(ti);
        db.saveWatchlistSymbols(a.getId(), "Default", a.getWatchlistItems());
        client.subscribe(symbol);
        fireWatchlistChanged(getWatchlist());
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
    public Range ensureRange(String symbol, Range requested) throws Exception {
        var utc = java.time.ZoneOffset.UTC;

        var from = java.time.Instant.ofEpochMilli(requested.startMs).atZone(utc).toLocalDate();
        var to = java.time.Instant.ofEpochMilli(requested.endMs).atZone(utc).toLocalDate();
        var r = new HistoricalService.Range(requested.timespan, requested.mult, from, to);
        var missing = hist.ensureRange(symbol, r);
        if (missing == null) return null;

        long startMs = missing.from.atStartOfDay(utc).toInstant().toEpochMilli();
        long endMs   = missing.to.atStartOfDay(utc).toInstant().toEpochMilli();
        return new Range(missing.timespan, missing.multiplier, startMs, endMs);
    }
    public int backfillRange(String symbol, Range missing) throws Exception {
        var utc  = java.time.ZoneOffset.UTC;
        var from = java.time.Instant.ofEpochMilli(missing.startMs).atZone(utc).toLocalDate();
        var to = java.time.Instant.ofEpochMilli(missing.endMs).atZone(utc).toLocalDate();
        var r = new HistoricalService.Range(missing.timespan, missing.mult, from, to);
        return hist.backfillRange(symbol, r);
    }
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
}