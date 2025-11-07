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
    private double portfolioValue; // The total value of the portfolio
    // Optional positions map for tracking average cost and richer position info.
    // Keyed by symbol for easy lookup. This is kept in sync when using the new
    // position-aware APIs but existing APIs that use portfolioItems remain functional.
    private Map<String, Position> positions;

    // implement options, ETFs, other as necessary

    public Portfolio() {
        portfolioItems = new HashMap<>();
        this.positions = new HashMap<>();
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

    /**
     * Add a trade item while recording cost-basis information.
     * This keeps the legacy portfolioItems map updated and also maintains a Position
     * entry to store average cost.
     * @param tradeItem item to add
     * @param n quantity added
     * @param pricePaid per-share price paid
     * @return true on success
     */
    public boolean addPosition(TradeItem tradeItem, int n, double pricePaid) {
        boolean ok = addTradeItem(tradeItem, n);
        if (!ok) return false;
        String sym = tradeItem.getSymbol();
        Position pos = positions.get(sym);
        if (pos == null) {
            pos = new Position(tradeItem, n, pricePaid);
            positions.put(sym, pos);
        } else {
            pos.addShares(n, pricePaid);
        }
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

        // Update positions map if present
        Position p = positions.get(tradeItem.getSymbol());
        if (p != null) {
            p.removeShares(n);
            if (p.getQuantity() == 0) positions.remove(tradeItem.getSymbol());
        }

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

    /**
     * Get the Position for the provided TradeItem if available (position-aware API).
     * @param tradeItem query item
     * @return Position or null if unknown
     */
    public Position getPosition(TradeItem tradeItem) {
        if (tradeItem == null) return null;
        return positions.get(tradeItem.getSymbol());
    }

    /**
     * Get average cost if tracked for this trade item.
     * @param tradeItem query item
     * @return average cost or null if not available
     */
    public Double getAverageCost(TradeItem tradeItem) {
        Position p = getPosition(tradeItem);
        return p == null ? null : p.getAverageCost();
    }

    /*
        Updates the portfolio value by looping through all holdings.
     */
    public double getPortfolioValue() {
        double aggregateValue = 0;
        for (Map.Entry<TradeItem, Integer> entry : portfolioItems.entrySet()) {
            double sharePrice = entry.getKey().getCurrentPrice();
            int shares = entry.getValue();
            double totalValue = sharePrice * shares;
            aggregateValue += totalValue;
        }
        this.portfolioValue = aggregateValue;
        return portfolioValue;
    }


    // TODO implement individual maps for Stock, ETF, Option, etc?


}
