package com.accountmanager;

import com.market.Stock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a watchlist of stocks that a user is interested in
 */
public class Watchlist {
    private final Map<String, Stock> watchedStocks;
    
    public Watchlist() {
        this.watchedStocks = new HashMap<>();
    }
    
    /**
     * Add a stock to the watchlist
     * @param stock The stock to add
     * @return True if added successfully, false if already in watchlist
     */
    public boolean addStock(Stock stock) {
        if (stock == null) {
            return false;
        }
        
        String symbol = stock.getSymbol();
        if (watchedStocks.containsKey(symbol)) {
            return false;
        }
        
        watchedStocks.put(symbol, stock);
        return true;
    }
    
    /**
     * Remove a stock from the watchlist
     * @param symbol The stock symbol to remove
     * @return True if removed, false if not in watchlist
     */
    public boolean removeStock(String symbol) {
        return watchedStocks.remove(symbol) != null;
    }
    
    /**
     * Check if a stock is in the watchlist
     * @param symbol The stock symbol
     * @return True if in watchlist
     */
    public boolean containsStock(String symbol) {
        return watchedStocks.containsKey(symbol);
    }
    
    /**
     * Get all stocks in the watchlist
     * @return List of watched stocks
     */
    public List<Stock> getStocks() {
        return new ArrayList<>(watchedStocks.values());
    }
    
    /**
     * Get the number of stocks in the watchlist
     * @return Number of watched stocks
     */
    public int size() {
        return watchedStocks.size();
    }
    
    /**
     * Clear the watchlist
     */
    public void clear() {
        watchedStocks.clear();
    }
}