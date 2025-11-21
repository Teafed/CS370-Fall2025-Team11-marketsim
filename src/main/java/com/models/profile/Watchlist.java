package com.models.profile;

import com.models.market.TradeItem;


import java.util.*;

/*
    A portfolio manages all trade items for an account. It provides information about which trade items an
    account owns as well as how many of each. A portfolio knows its total value and provides methods to
    update and return this value.
 */
/**
 * Manages a list of TradeItems that the user is interested in tracking.
 * Enforces a maximum size for the watchlist.
 */
public class Watchlist {

    private LinkedHashSet<TradeItem> watchlist;
    private final int maxSize = 50;

    /**
     * Constructs a new empty Watchlist.
     */
    public Watchlist() {
        watchlist = new LinkedHashSet<>();
        setDefaultWatchlist();
    }

    /**
     * Adds a specified TradeItem to the watchlist.
     * @param tradeItem The TradeItem to add
     * @return True if successful, False if list is full.
     */
    public boolean addWatchlistItem(TradeItem tradeItem) {
        if (tradeItem == null) {
            // trade item must not be null
            return false;
        }
        if (watchlist.contains(tradeItem)) {
            return false;
        }
        if (watchlist.size() < maxSize) {
            boolean added = watchlist.add(tradeItem);
            return added;
        } else
            // cannot add more than max size
            return false;
    }

    /**
     * Removes a specified TradeItem from the watchlist.
     * @param tradeItem The TradeItem to remove
     * @return True if successful, False if the list did not contain that item.
     */
    public boolean removeWatchlistItem(TradeItem tradeItem) {
        return watchlist.remove(tradeItem);
    }

    /**
     * Clears all items from the watchlist.
     */
    public void clearList() {
        watchlist.clear();
    }

    /**
     * Gets the current number of items in the watchlist.
     *
     * @return The size of the watchlist.
     */
    public int getWatchlistSize() {
        return watchlist.size();
    }

    /**
     * Gets the maximum allowed size of the watchlist.
     *
     * @return The maximum size.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Checks if the watchlist contains a specific TradeItem.
     *
     * @param tradeItem The TradeItem to check.
     * @return True if found, false otherwise.
     */
    public boolean hasTradeItem(TradeItem tradeItem) {
        return watchlist.contains(tradeItem);
    }

    /**
     * Gets a copy of the watchlist as a List.
     *
     * @return A list of TradeItems.
     */
    public List<TradeItem> getWatchlist() {
        return new ArrayList<>(watchlist);
    }

    private void setDefaultWatchlist() {
        watchlist.clear();
        TradeItem[] initialSymbols = {
                new TradeItem("AAPL"),
                new TradeItem("MSFT"),
                new TradeItem("GOOGL"),
                new TradeItem("NVDA"),
                new TradeItem("AMZN"),
                new TradeItem("META"),
                new TradeItem("TSLA"),
                new TradeItem("AVGO"),
                new TradeItem("TSM"),
                new TradeItem("BRK.B")
        };
        Collections.addAll(watchlist, initialSymbols);
    }

    /**
     * Creates a default watchlist with popular tech stocks.
     *
     * @return A list of default TradeItems.
     */
    public static List<TradeItem> getDefaultWatchlist() {
        TradeItem[] initialSymbols = {
                new TradeItem("AAPL"),
                new TradeItem("MSFT"),
                new TradeItem("GOOGL"),
                new TradeItem("NVDA"),
                new TradeItem("AMZN"),
                new TradeItem("META"),
                new TradeItem("TSLA"),
                new TradeItem("AVGO"),
                new TradeItem("TSM"),
                new TradeItem("BRK.B")
        };
        List<TradeItem> wl = new java.util.ArrayList<>();
        wl.addAll(Arrays.asList(initialSymbols));
        return wl;
    }
}