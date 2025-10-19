package com.market;

import com.etl.FinnhubClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Stock extends TradeItem {
    private final String symbol;      // the symbol the stock trades under
    private final String name;        // the full name of the company
    private final AtomicReference<Double> currentPrice;  // the current price of the stock
    private double openPrice;         // opening price for the day
    private double highPrice;         // highest price for the day
    private double lowPrice;          // lowest price for the day
    private long volume;              // trading volume
    private List<Option> options;     // options for this stock
    private FinnhubClient dataClient; // client for real-time data
    
    public Stock(String symbol, String name) {
        super(name, symbol);
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = new AtomicReference<>(0.0);
        this.openPrice = 0.0;
        this.highPrice = 0.0;
        this.lowPrice = 0.0;
        this.volume = 0;
        this.options = new ArrayList<>();
    }
    
    /**
     * Start collecting real-time data for this stock
     * @param dbManager Database manager to store price data
     */
    public void startDataCollection(DatabaseManager dbManager) {
        this.dataClient = FinnhubClient.start(dbManager, symbol, this::setPrice);
        
        // Initialize price from database if available
        Double lastPrice = dbManager.getLastPrice(symbol);
        if (lastPrice != null) {
            setPrice(lastPrice);
        }
    }
    
    /**
     * Stop collecting data for this stock
     */
    public void stopDataCollection() {
        if (dataClient != null) {
            dataClient.stopPolling();
        }
    }
    
    /**
     * Get the current stock price
     * @return Current price
     */
    public double getPrice() {
        return currentPrice.get();
    }
    
    /**
     * Set the current stock price
     * @param price New price
     */
    public void setPrice(double price) {
        currentPrice.set(price);
        
        // Update high/low prices
        if (price > highPrice || highPrice == 0.0) {
            highPrice = price;
        }
        if (price < lowPrice || lowPrice == 0.0) {
            lowPrice = price;
        }
    }
    
    /**
     * Get the stock symbol
     * @return Stock symbol
     */
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * Get the company name
     * @return Company name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the opening price
     * @return Opening price
     */
    public double getOpenPrice() {
        return openPrice;
    }
    
    /**
     * Set the opening price
     * @param openPrice Opening price
     */
    public void setOpenPrice(double openPrice) {
        this.openPrice = openPrice;
    }
    
    /**
     * Get the highest price of the day
     * @return Highest price
     */
    public double getHighPrice() {
        return highPrice;
    }
    
    /**
     * Get the lowest price of the day
     * @return Lowest price
     */
    public double getLowPrice() {
        return lowPrice;
    }
    
    /**
     * Get the trading volume
     * @return Trading volume
     */
    public long getVolume() {
        return volume;
    }
    
    /**
     * Set the trading volume
     * @param volume Trading volume
     */
    public void setVolume(long volume) {
        this.volume = volume;
    }
    
    /**
     * Add an option for this stock
     * @param option Option to add
     */
    public void addOption(Option option) {
        options.add(option);
    }
    
    /**
     * Get all options for this stock
     * @return List of options
     */
    public List<Option> getOptions() {
        return options;
    }
}
