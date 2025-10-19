package com.market;

/**
 * Represents a trade request in the market
 */
public class TradeRequest {
    private final int accountId;
    private final String symbol;
    private final int quantity;
    private final double price;
    private final boolean isBuyOrder;
    private final Trader trader;
    private final long timestamp;
    
    /**
     * Create a new trade request
     * @param accountId The account ID making the request
     * @param symbol The stock symbol
     * @param quantity The quantity to trade
     * @param price The price to trade at
     * @param isBuyOrder True if this is a buy order, false for sell
     * @param trader The trader making the request
     */
    public TradeRequest(int accountId, String symbol, int quantity, double price, boolean isBuyOrder, Trader trader) {
        this.accountId = accountId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.isBuyOrder = isBuyOrder;
        this.trader = trader;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get the account ID
     * @return The account ID
     */
    public int getAccountId() {
        return accountId;
    }
    
    /**
     * Get the stock symbol
     * @return The stock symbol
     */
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * Get the quantity to trade
     * @return The quantity
     */
    public int getQuantity() {
        return quantity;
    }
    
    /**
     * Get the price to trade at
     * @return The price
     */
    public double getPrice() {
        return price;
    }
    
    /**
     * Check if this is a buy order
     * @return True if this is a buy order, false for sell
     */
    public boolean isBuyOrder() {
        return isBuyOrder;
    }
    
    /**
     * Get the trader making the request
     * @return The trader
     */
    public Trader getTrader() {
        return trader;
    }
    
    /**
     * Get the timestamp of the request
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
}
