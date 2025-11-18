package com.models.market;

import com.etl.finnhub.ClientFacade;
import com.models.profile.Watchlist;

import java.util.*;


// Will hold all open stock objects
// each account will reference stocks held in the market
public class Market implements TradeListener {
    private final Map<java.lang.String, TradeItem> stocks = new LinkedHashMap<>();
    private ClientFacade clientFacade;
    private MarketListener listener;

    public Market(ClientFacade clientFacade) throws Exception {
        this.clientFacade = Objects.requireNonNull(clientFacade);
        clientFacade.setTradeListener(this);
    }

    public synchronized void add(TradeItem item) throws Exception {
        if (item == null) return;
        java.lang.String sym = normalize(item.getSymbol());
        if (stocks.containsKey(sym)) return;

        clientFacade.subscribe(sym);
        item.setOpen(clientFacade.fetchQuote(sym));
        stocks.put(sym, item);
        if (listener != null) listener.loadSymbols(new ArrayList<>(stocks.values()));
    }

    public synchronized void remove(java.lang.String symbol) {
        java.lang.String sym = normalize(symbol);
        if (stocks.remove(sym) != null && listener != null) {
            listener.loadSymbols(new ArrayList<>(stocks.values()));
        }
        // clientFacade.unsubscribe(sym);
    }

    public void addFromWatchlist(Watchlist wl) throws Exception {
        if (wl == null) return;
        for (TradeItem ti : wl.getWatchlist()) add(ti);
    }

    public synchronized void updateStock(java.lang.String symbol, double price) {
        TradeItem ti = stocks.get(normalize(symbol));
        if (ti != null) ti.updatePrice(price);
    }

    @Override
    public void onTrade(java.lang.String symbol, double price) {
        updateStock(symbol, price);
        if (listener != null) listener.onMarketUpdate();
    }

    public synchronized void setMarketListener(MarketListener l) {
        this.listener = l;
        if (l != null) l.loadSymbols(new ArrayList<>(stocks.values()));
    }

    /**
     * Convenience accessor for current market price of a symbol.
     * Returns Double.NaN if symbol unknown.
     */
    public double getPrice(java.lang.String symbol) {
        TradeItem ti = stocks.get(normalize(symbol));
        return ti == null ? Double.NaN : ti.getCurrentPrice();
    }

    private static java.lang.String normalize(java.lang.String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}