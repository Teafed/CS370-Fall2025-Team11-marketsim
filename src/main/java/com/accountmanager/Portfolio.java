package com.accountmanager;

import com.market.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    A portfolio manages all trade items for an account. It provides information about which trade items an
    account owns as well as how many of each. A portfolio knows its total value and provides methods to
    update and return this value.
 */
public class Portfolio {

    private Map<TradeItem, Integer> portfolioItems;
    private int portfolioValue; // The total value of the portfolio

    // implement options, ETFs, other as necessary

    public Portfolio() {
        portfolioItems = new HashMap<>();
        this.portfolioValue = 0;
    }


    /**
     * Add a TradeItem to the portfolio.
     * @param tradeItem
     * @param n The number of items to add, must be >0
     * @return True if successfule, false if not
     */
    boolean addTradeItem(TradeItem tradeItem, int n) {
        if (tradeItem == null) {
            // TODO Error
            return false;
        }
        if (n < 1) {
            return false;
        }
        // add tradeItem to map with value, if it already exists, update value by adding new value
        portfolioItems.merge(tradeItem, n, Integer::sum);
        return true;
    }

    /*
        Remove a TradeItem from a portfolio. Removes the entry if entire value is removed.
        Otherwise, subtracts from existing value.
        @param tradeItem The item to be removed.
        @param n The amount of the item to be removed. If -1 removes all that item.
     */
    boolean removeTradeItem(TradeItem tradeItem, int n) {
        if (tradeItem == null | n < 1 | !portfolioItems.containsKey(tradeItem)) {
            //TODO Handle error
            return false;
        }
        if (portfolioItems.get(tradeItem) == n) {
            portfolioItems.remove(tradeItem);
        }
        else
            portfolioItems.merge(tradeItem, -n, Integer::sum);

        return true;
    }

    public boolean hasTradeItem(TradeItem tradeItem) {
        return portfolioItems.containsKey(tradeItem);
    }

    public List<TradeItem> listTradeItems() {
        return new ArrayList<TradeItem>(portfolioItems.keySet());
    }


    /**
     * Gets the number of shares held for a particular TradeItem
     * @param tradeItem The item queried.
     * @return The number of shares held.
     */
    public int getNumberOfShares(TradeItem tradeItem) {
        return portfolioItems.get(tradeItem);
    }

    /*
        Updates the portfolio value by looping through all holdings.
     */
    public int getPortfolioValue() {
        int aggregateValue = 0;
        for (Map.Entry<TradeItem, Integer> entry : portfolioItems.entrySet()) {
            int sharePrice = entry.getKey().getCurrentPrice();
            int shares = entry.getValue();
            int totalValue = sharePrice * shares;
            aggregateValue += totalValue;
        }
        this.portfolioValue = aggregateValue;
        return portfolioValue;
    }


    // TODO implement individual maps for Stock, ETF, Option, etc?


}
