package com.models.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    A portfolio manages all trade items for an account. It provides information about which trade items an
    account owns as well as how many of each. A portfolio knows its total value and provides methods to
    update and return this value.
 */
/**
 * Manages the collection of trade items (stocks) owned by an account.
 * Tracks the quantity of each symbol held.
 */
public class Portfolio {
    private final Map<java.lang.String, Integer> positions = new HashMap<>();

    public Portfolio() {
    }

    /**
     * Adds shares to the portfolio.
     *
     * @param symbol The stock symbol.
     * @param n      The number of shares to add (must be > 0).
     * @return True if successful, false otherwise.
     */
    boolean addShares(String symbol, int n) {
        if (symbol == null || symbol.isBlank() || n <= 0) {
            return false;
        }
        // add symbol to map with value, if it already exists, update value by adding
        // new value
        positions.merge(symbol.toUpperCase(), n, Integer::sum);
        return true;
    }

    /**
     * Removes shares from the portfolio.
     *
     * @param symbol The stock symbol.
     * @param n      The number of shares to remove.
     * @return True if successful, false if invalid amount or insufficient shares.
     */
    boolean removeShares(String symbol, int n) {
        if (symbol == null || n <= 0 || symbol.isBlank())
            return false;

        String key = symbol.toUpperCase();
        Integer current = positions.get(key);

        if (current == null || current < n)
            return false;
        int left = current - n;
        if (left == 0)
            positions.remove(key);
        else
            positions.put(key, left);
        return true;
    }

    // argument should be from db.getPositions(accountId)
    /**
     * Populates the portfolio from a database map.
     *
     * @param symbolToQty A map of symbol to quantity.
     */
    public void setFromDb(Map<String, Integer> symbolToQty) {
        positions.clear();
        if (symbolToQty != null) {
            symbolToQty.forEach((k, v) -> {
                if (v != null && v > 0)
                    positions.put(k.toUpperCase(), v);
            });
        }
    }

    /**
     * Checks if the portfolio contains any shares of the given symbol.
     *
     * @param symbol The stock symbol.
     * @return True if shares are held, false otherwise.
     */
    public boolean hasShare(String symbol) {
        if (symbol == null)
            return false;
        return positions.containsKey(symbol);
    }

    /**
     * Gets the number of shares held for a particular symbol.
     *
     * @param symbol The stock symbol.
     * @return The number of shares held.
     */
    public int getNumberOfShares(String symbol) {
        if (symbol == null)
            return 0;
        return positions.getOrDefault(symbol.toUpperCase(), 0);
    }

    /**
     * Returns an unmodifiable view of the positions map.
     *
     * @return A map of symbol to quantity.
     */
    public Map<String, Integer> asMap() {
        return java.util.Collections.unmodifiableMap(positions);
    }

    /**
     * Computes the total market value of the portfolio.
     *
     * @param priceFn A function that provides the current price for a symbol.
     * @return The total value.
     */
    public double computeMarketValue(java.util.function.ToDoubleFunction<String> priceFn) {
        double total = 0;
        for (var e : positions.entrySet()) {
            double px = priceFn.applyAsDouble(e.getKey());
            if (!Double.isNaN(px))
                total += px * e.getValue();
        }
        return total;
    }
}
