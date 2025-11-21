package com.models.profile;

import com.models.market.TradeItem;

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
    private final Map<java.lang.String, Integer> positions = new HashMap<>();

    public Portfolio() { }

    /**
     * Add a share to the portfolio.
     * @param symbol
     * @param n The number of items to add, must be >0
     * @return True if successfule, false if not
     */
    boolean addShares(String symbol, int n) {
        if (symbol == null || symbol.isBlank() || n <= 0) {
            return false;
        }
        // add symbol to map with value, if it already exists, update value by adding new value
        positions.merge(symbol.toUpperCase(), n, Integer::sum);
        return true;
    }

    /*
        Remove a symbol from a portfolio. Removes the entry if entire value is removed.
        Otherwise, subtracts from existing value.
        @param symbol The item to be removed.
        @param n The amount of the item to be removed. If -1 removes all that item.
     */
    boolean removeShares(String symbol, int n) {
        if (symbol == null || n <= 0 || symbol.isBlank()) return false;

        String key = symbol.toUpperCase();
        Integer current = positions.get(key);

        if (current == null || current < n) return false;
        int left = current - n;
        if (left == 0) positions.remove(key); else positions.put(key, left);
        return true;
    }

    // argument should be from db.getPositions(accountId)
    public void setFromDb(Map<String,Integer> symbolToQty) {
        positions.clear();
        if (symbolToQty != null) {
            symbolToQty.forEach((k,v) -> {
                if (v != null && v > 0) positions.put(k.toUpperCase(), v);
            });
        }
    }

    /** Compatibility: accept a generic TradeItem (from tests) */
    public boolean hasShare(String symbol) {
        if (symbol == null) return false;
        return positions.containsKey(symbol);
    }

    /**
     * Gets the number of shares held for a particular TradeItem
     * @param symbol The item queried.
     * @return The number of shares held.
     */
    public int getNumberOfShares(String symbol) {
        if (symbol == null) return 0;
        return positions.getOrDefault(symbol.toUpperCase(), 0);
    }

    public Map<String,Integer> asMap() {
        return java.util.Collections.unmodifiableMap(positions);
    }

    public double computeMarketValue(java.util.function.ToDoubleFunction<String> priceFn) {
        double total = 0;
        for (var e : positions.entrySet()) {
            double px = priceFn.applyAsDouble(e.getKey());
            if (!Double.isNaN(px)) total += px * e.getValue();
        }
        return total;
    }

    public ArrayList<String> getPortfolioItems() {
        return new ArrayList<>(positions.keySet());
    }
}
