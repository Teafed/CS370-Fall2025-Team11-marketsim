package com.accountmanager;

import com.market.*;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;


/*
    A portfolio manages all trade items for an account. It provides information about which trade items an
    account owns as well as how many of each. A portfolio knows its total value and provides methods to
    update and return this value.
 */
public class Watchlist {

    private LinkedHashSet<TradeItem> watchlist;
    private final int maxSize = 50;


    public Watchlist() {
        watchlist = new LinkedHashSet<>();
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
        }
        else
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

    public void clearList(){
        watchlist.clear();
    }

    public int  getWatchlistSize() {
        return watchlist.size();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public boolean hasTradeItem(TradeItem tradeItem) {
        return watchlist.contains(tradeItem);
    }

    public List<TradeItem> getWatchlist() {
        return new ArrayList<>(watchlist);
    }

    public static List<TradeItem> getDefaultWatchlist() {
        TradeItem[] initialSymbols = {
                new TradeItem("Apple", "AAPL"),
                new TradeItem("Microsoft", "MSFT"),
                new TradeItem("Alphabet", "GOOGL"),
                new TradeItem("NVIDIA", "NVDA"),
                new TradeItem("Amazon", "AMZN"),
                new TradeItem("Meta Platforms", "META"),
                new TradeItem("Tesla", "TSLA"),
                new TradeItem("Broadcom", "AVGO"),
                new TradeItem("Taiwan Semiconductor Manufacturing Company", "TSM"),
                new TradeItem("Berkshire Hathaway", "BRK.B")
        };
        List<TradeItem> wl = new java.util.ArrayList<>();
        wl.addAll(Arrays.asList(initialSymbols));
        return wl;
    }

}