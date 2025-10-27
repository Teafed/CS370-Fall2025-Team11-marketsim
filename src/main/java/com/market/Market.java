package com.market;

import com.accountmanager.Account;
import com.etl.TradeSource;

import java.util.*;


// Will hold all open stock objects
// each account will reference stocks held in the market
public class Market implements TradeListener {

    private Map<String, TradeItem> stocks;
    private DatabaseManager dbManager;
    private TradeSource client;
    private Account account;
    private MarketListener marketListener;
    private boolean ready = false;
    public Market(Map<String, TradeItem> stocks, DatabaseManager dbManager) {}

    public Market(TradeSource client, DatabaseManager db, Account account) throws Exception {
        setClient(client);
        stocks = new LinkedHashMap<>();
        setDatabase(db);
        setAccount(account);
    }

    /**
     * This method opens a socket through the etl clients and pulls data for each stock.
     */
    public void setClient(TradeSource client) throws Exception {
        this.client = client;
        client.setTradeListener(this);
        this.ready = true;
    }

    public void setDatabase(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private void setAccount(Account account) {
        this.account = account;
    }

    public boolean isReady() {
        return ready;
    }

    public void add(String symbol) throws Exception {
        // start client for symbol
        client.subscribe(symbol);
        Stock stock = new Stock("name", symbol);
        stocks.put(symbol, stock);
        //Thread.sleep(200);
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

        // Update its price
        stock.updatePrice(p);

    }

    @Override
    public void onTrade(String symbol, double price) {
        updateStock(symbol, price);
        marketListener.onMarketUpdate();
    }

    public void setMarketListener(MarketListener marketListener) {
        System.out.println("Adding listener");
        this.marketListener = marketListener;
        System.out.println("Adding symbols");
        marketListener.loadSymbols(new ArrayList<>(stocks.values()));
    }
}