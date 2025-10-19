package com.accountmanager;

import com.market.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A portfolio manages all trade items for an account. It provides information about which trade items an
 * account owns as well as how many of each. A portfolio knows its total value and provides methods to
 * update and return this value.
 */
public class Portfolio {
    private Map<String, PortfolioItem> holdings;
    private int portfolioValue; // The total value of the portfolio

    public Portfolio() {
        holdings = new HashMap<>();
        this.portfolioValue = 0;
    }

    /**
     * Add a stock to the portfolio
     * @param stock The stock to add
     * @param quantity The number of shares to add, must be >0
     * @param purchasePrice The price per share
     * @return True if successful, false if not
     */
    public boolean addStock(Stock stock, int quantity, double purchasePrice) {
        if (stock == null) {
            return false;
        }
        if (quantity < 1) {
            return false;
        }
        
        String symbol = stock.getSymbol();
        PortfolioItem item = holdings.get(symbol);
        
        if (item == null) {
            // First time buying this stock
            item = new PortfolioItem(stock, quantity, purchasePrice);
            holdings.put(symbol, item);
        } else {
            // Average down the purchase price
            double totalValue = (item.getQuantity() * item.getAveragePurchasePrice()) + 
                               (quantity * purchasePrice);
            int newQuantity = item.getQuantity() + quantity;
            double newAvgPrice = totalValue / newQuantity;
            
            item.setQuantity(newQuantity);
            item.setAveragePurchasePrice(newAvgPrice);
        }
        
        updatePortfolioValue();
        return true;
    }

    /**
     * Remove shares of a stock from the portfolio
     * @param symbol The stock symbol
     * @param quantity The number of shares to remove
     * @return True if successful, false if not enough shares
     */
    public boolean removeStock(String symbol, int quantity) {
        PortfolioItem item = holdings.get(symbol);
        if (item == null || item.getQuantity() < quantity) {
            return false;
        }
        
        int newQuantity = item.getQuantity() - quantity;
        if (newQuantity == 0) {
            holdings.remove(symbol);
        } else {
            item.setQuantity(newQuantity);
        }
        
        updatePortfolioValue();
        return true;
    }

    /**
     * Add a trade item (stock) to the portfolio
     * @param stock The stock to add
     * @param quantity The quantity to add
     * @return True if successful
     */
    public boolean addTradeItem(Stock stock, int quantity) {
        return addStock(stock, quantity, stock.getPrice());
    }

    /**
     * Check if the portfolio has a trade item (stock)
     * @param stock The stock to check
     * @return True if has the stock
     */
    public boolean hasTradeItem(Stock stock) {
        return holdings.containsKey(stock.getSymbol());
    }

    /**
     * Remove a trade item (stock) from the portfolio
     * @param stock The stock to remove
     * @param quantity The quantity to remove
     * @return True if successful
     */
    public boolean removeTradeItem(Stock stock, int quantity) {
        return removeStock(stock.getSymbol(), quantity);
    }
    
    /**
     * Check if the portfolio has enough shares of a stock
     * @param symbol The stock symbol
     * @param quantity The quantity to check
     * @return True if the portfolio has enough shares
     */
    public boolean hasEnoughShares(String symbol, int quantity) {
        PortfolioItem item = holdings.get(symbol);
        return item != null && item.getQuantity() >= quantity;
    }
    
    /**
     * Get the current value of the portfolio
     * @return The total value of all holdings
     */
    public int getPortfolioValue() {
        updatePortfolioValue();
        return portfolioValue;
    }
    
    /**
     * Update the total value of the portfolio based on current stock prices
     */
    private void updatePortfolioValue() {
        portfolioValue = 0;
        for (PortfolioItem item : holdings.values()) {
            int currentPrice = (int) item.getStock().getPrice();
            portfolioValue += currentPrice * item.getQuantity();
        }
    }
    
    /**
     * Get all holdings in the portfolio
     * @return Map of stock symbols to portfolio items
     */
    public Map<String, PortfolioItem> getHoldings() {
        return holdings;
    }
    
    /**
     * Get the quantity of a specific stock
     * @param symbol The stock symbol
     * @return The quantity owned, or 0 if none
     */
    public int getQuantity(String symbol) {
        PortfolioItem item = holdings.get(symbol);
        return item != null ? item.getQuantity() : 0;
    }
    
    /**
     * Get the average purchase price of a specific stock
     * @param symbol The stock symbol
     * @return The average purchase price, or 0 if none
     */
    public double getAveragePurchasePrice(String symbol) {
        PortfolioItem item = holdings.get(symbol);
        return item != null ? item.getAveragePurchasePrice() : 0.0;
    }
    
    /**
     * Get the current value of a specific holding
     * @param symbol The stock symbol
     * @return The current value of the holding
     */
    public double getCurrentValue(String symbol) {
        PortfolioItem item = holdings.get(symbol);
        if (item == null) {
            return 0.0;
        }
        return item.getStock().getPrice() * item.getQuantity();
    }
    
    /**
     * Get the profit/loss for a specific holding
     * @param symbol The stock symbol
     * @return The profit/loss amount
     */
    public double getProfitLoss(String symbol) {
        PortfolioItem item = holdings.get(symbol);
        if (item == null) {
            return 0.0;
        }
        double currentValue = getCurrentValue(symbol);
        double costBasis = item.getAveragePurchasePrice() * item.getQuantity();
        return currentValue - costBasis;
    }
    
    /**
     * Inner class to represent a holding in the portfolio
     */
    public static class PortfolioItem {
        private final Stock stock;
        private int quantity;
        private double averagePurchasePrice;
        
        public PortfolioItem(Stock stock, int quantity, double purchasePrice) {
            this.stock = stock;
            this.quantity = quantity;
            this.averagePurchasePrice = purchasePrice;
        }
        
        public Stock getStock() {
            return stock;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        
        public double getAveragePurchasePrice() {
            return averagePurchasePrice;
        }
        
        public void setAveragePurchasePrice(double price) {
            this.averagePurchasePrice = price;
        }
    }
}
