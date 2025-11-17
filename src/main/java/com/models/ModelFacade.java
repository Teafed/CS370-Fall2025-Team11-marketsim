package com.models;

import com.etl.finnhub.ClientFacade;
import com.models.market.*;
import com.models.profile.*;

import javax.swing.*;
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

        public ModelFacade(Database db, Profile profile) throws Exception {
            this.db = db;
            this.profile = profile;
            this.client = new ClientFacade();
            this.market = new Market(client);
            this.market.setMarketListener(new MarketListener() {
                @Override public void onMarketUpdate() { fireQuotesUpdated(); }
                @Override public void loadSymbols(List<TradeItem> items) { fireWatchlistChanged(items); }
            });

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
    public double getPrice(String symbol) { return market.getPrice(symbol); }
    public Account getActiveAccount() { return profile.getActiveAccount(); }
    public List<Account> listAccounts() { return profile.getAccounts(); }
    public List<TradeItem> getWatchlist() { return profile.getActiveAccount().getWatchlist().getWatchlist(); }
    public AccountDTO getAccountDTO() throws SQLException {
            Account a = profile.getActiveAccount();
            Map<String,Integer> positions = db.getPositions(a.getId());
            return new AccountDTO(a.getId(), a.getCash(), positions);
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
                System.out.println("[facade] Loaded watchlist from DB (" + dbSymbols.size() + ")");
            }
        }

        public void addToWatchlist(String symbol) throws Exception {
            Account a = profile.getActiveAccount();
            TradeItem ti = new TradeItem(symbol, symbol);
            db.saveWatchlistSymbols(a.getId(), "Default", List.of(ti));
            a.getWatchlist().addWatchlistItem(ti);
            client.subscribe(symbol);
            fireWatchlistChanged(getWatchlist());
        }

        public void placeOrder(String symbol, boolean isBuy, int shares) {
            placeOrder(symbol, isBuy, shares, System.currentTimeMillis());
        }

        public void placeOrder(String symbol, boolean isBuy, int shares, long ts) {
            // if ts is today and market is open get price from there
            // otherwise price is gotten from db:
            //    order times outside of range available in db should just get latest/earliest price
            try {
                Account a = profile.getActiveAccount();
                double price = getPrice(symbol);
                if (Double.isNaN(price)) throw new IllegalStateException("No live price for " + symbol);

                Order.side side = isBuy ? Order.side.BUY : Order.side.SELL;
                Order order = new Order(a, new TradeItem(symbol, symbol), side, shares, price, ts);

                // persist + apply
                db.recordOrder(order);
                db.applyFill(a.getId(), symbol, shares * (isBuy ? +1 : -1), price, ts);
                // update in-memory cash/positions
                a.setCash(db.getAccountCash(a.getId()));
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
}