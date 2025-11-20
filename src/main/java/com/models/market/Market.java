package com.models.market;

import com.etl.finnhub.ClientFacade;
import com.models.profile.Watchlist;

import java.util.*;

/**
 * Manages the collection of active stocks (TradeItems) in the market.
 * Handles subscriptions to real-time data and notifies listeners of updates.
 */
public class Market implements TradeListener {
    private final Map<String, TradeItem> stocks = new LinkedHashMap<>();
    private ClientFacade clientFacade;
    private MarketListener listener;

    /**
     * Constructs a new Market instance.
     *
     * @param clientFacade The facade for accessing market data APIs.
     * @throws Exception If an error occurs during initialization.
     */
    public Market(ClientFacade clientFacade) throws Exception {
        this.clientFacade = Objects.requireNonNull(clientFacade);
        clientFacade.setTradeListener(this);
    }

    /**
     * Adds a stock to the market and subscribes to real-time updates.
     *
     * @param item The TradeItem to add.
     * @throws Exception If an error occurs during subscription.
     */
    public synchronized void add(TradeItem item) throws Exception {
        if (item == null)
            return;
        String sym = normalize(item.getSymbol());
        if (stocks.containsKey(sym))
            return;

        clientFacade.subscribe(sym);
        item.setOpen(clientFacade.fetchQuote(sym));
        stocks.put(sym, item);
        if (listener != null)
            listener.loadSymbols(new ArrayList<>(stocks.values()));
    }

    /**
     * Removes a stock from the market.
     *
     * @param symbol The symbol of the stock to remove.
     */
    public synchronized void remove(String symbol) {
        String sym = normalize(symbol);
        if (stocks.remove(sym) != null && listener != null) {
            listener.loadSymbols(new ArrayList<>(stocks.values()));
        }
        // clientFacade.unsubscribe(sym);
    }

    /**
     * Adds all stocks from a watchlist to the market.
     *
     * @param wl The Watchlist containing the stocks.
     * @throws Exception If an error occurs during subscription.
     */
    public void addFromWatchlist(Watchlist wl) throws Exception {
        if (wl == null)
            return;
        for (TradeItem ti : wl.getWatchlist())
            add(ti);
    }

    /**
     * Updates the price of a specific stock.
     *
     * @param symbol The stock symbol.
     * @param price  The new price.
     */
    public synchronized void updateStock(String symbol, double price) {
        TradeItem ti = stocks.get(normalize(symbol));
        if (ti != null)
            ti.updatePrice(price);
    }

    /**
     * Callback for trade events. Updates the stock price and notifies the listener.
     *
     * @param symbol The stock symbol.
     * @param price  The trade price.
     */
    @Override
    public void onTrade(String symbol, double price) {
        updateStock(symbol, price);
        if (listener != null)
            listener.onMarketUpdate();
    }

    /**
     * Sets the listener for market updates.
     *
     * @param l The MarketListener.
     */
    public synchronized void setMarketListener(MarketListener l) {
        this.listener = l;
        if (l != null)
            l.loadSymbols(new ArrayList<>(stocks.values()));
    }

    /**
     * Convenience accessor for current market price of a symbol.
     * Returns Double.NaN if symbol unknown.
     */
    public double getPrice(String symbol) {
        TradeItem ti = stocks.get(normalize(symbol));
        return ti == null ? Double.NaN : ti.getCurrentPrice();
    }

    /**
     * Searches for symbols matching the query.
     *
     * @param symbol The search query.
     * @return A 2D array of search results.
     */
    public String[][] searchSymbol(String symbol) {
        return clientFacade.searchSymbol(symbol);
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}