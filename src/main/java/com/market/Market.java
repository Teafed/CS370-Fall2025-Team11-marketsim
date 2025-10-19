package com.market;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central market class that holds all available stocks and manages market operations.
 * Each account will reference stocks held in this market.
 */
public class Market {
    private static Market instance;
    private final Map<String, Stock> stocks;
    private final DatabaseManager databaseManager;
    private final TradeManager tradeManager;
    
    private Market() {
        this.stocks = new ConcurrentHashMap<>();
        this.databaseManager = new DatabaseManager();
        this.tradeManager = new TradeManager(this);
        
        // Initialize the database
        databaseManager.initializeDatabase();
    }
    
    /**
     * Get the singleton instance of the market
     * @return The market instance
     */
    public static synchronized Market getInstance() {
        if (instance == null) {
            instance = new Market();
        }
        return instance;
    }
    
    /**
     * Add a stock to the market
     * @param symbol The stock symbol
     * @param stock The stock object
     */
    public void addStock(String symbol, Stock stock) {
        stocks.put(symbol, stock);
    }
    
    /**
     * Get a stock from the market
     * @param symbol The stock symbol
     * @return The stock object or null if not found
     */
    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }
    
    /**
     * Get all available stock symbols
     * @return Set of stock symbols
     */
    public Set<String> getAvailableStocks() {
        return stocks.keySet();
    }
    
    /**
     * Get the database manager
     * @return The database manager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Get the trade manager
     * @return The trade manager
     */
    public TradeManager getTradeManager() {
        return tradeManager;
    }
    
    /**
     * Initialize the market with default stocks
     */
    public void initialize() {
        // Add some default stocks
        addStock("AAPL", new Stock("AAPL", "Apple Inc."));
        addStock("MSFT", new Stock("MSFT", "Microsoft Corporation"));
        addStock("GOOGL", new Stock("GOOGL", "Alphabet Inc."));
        addStock("AMZN", new Stock("AMZN", "Amazon.com, Inc."));
        addStock("META", new Stock("META", "Meta Platforms, Inc."));
        
        // Start data collection for each stock
        for (String symbol : stocks.keySet()) {
            stocks.get(symbol).startDataCollection(databaseManager);
        }
    }
}
