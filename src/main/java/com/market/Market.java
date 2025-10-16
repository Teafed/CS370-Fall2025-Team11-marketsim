package com.market;

import com.etl.FinnhubClient;
import com.etl.TradeSource;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


// Will hold all open stock objects
// each account will reference stocks held in the market
public class Market implements TradeListener {

    private Map<String, TradeItem> stocks;
    private DatabaseManager dbManager;
    private TradeSource client;

    private String[] initialSymbols = {
            "AAPL", // Apple
            "MSFT", // Microsoft
            "GOOGL", // Alphabet
            "NVDA", // NVIDIA
            "AMZN", // Amazon
            "META", // Meta Platforms
            "TSLA", // Tesla
            "AVGO", // Broadcom
            "TSM",  // Taiwan Semiconductor Manufacturing Company
            "BRK.B" // Berkshire Hathaway
    };

    public Market(DatabaseManager db, TradeSource client) throws Exception {
        stocks =  new LinkedHashMap<>();
        this.dbManager = db;
        this.client = client;
        client.setTradeListener(this);

        this.add(initialSymbols);
    }

    /**
     * This method opens a socket through the etl clients and pulls data for each stock.
     */



    public void add(String symbol) throws Exception {
        // start client for symbol
        client.subscribe(symbol);
        Stock stock = new Stock("name", symbol);
        stocks.put(symbol, stock);
        Thread.sleep(200);

        // instantiate symbol using database


    }


    public void add(String[] symbols) throws Exception {
        // pass off list
        for (String symbol : symbols) {
            add(symbol);
        }

    }

    public void updateStock(String symbol, double p) {
        // Get the stock from the map
        TradeItem stock = stocks.get(symbol);

        //TODO Maybe change this later
        // convert price from double to int
        int price = (int) (p * 100);
        //TODO

        // Update its price
        stock.updatePrice(price);
    }


    @Override
    public void onTrade(String symbol, double price, long timestamp, long volume) {
        updateStock(symbol, price);
    }
}