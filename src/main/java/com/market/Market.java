package com.market;

import com.accountmanager.Account;
import com.etl.finnhub.ClientFacade;

import java.util.*;


// Will hold all open stock objects
// each account will reference stocks held in the market
public class Market implements TradeListener {

    private Map<String, TradeItem> stocks;
    private DatabaseManager dbManager;
    private ClientFacade clientFacade;
    private Account account;
    private MarketListener marketListener;
    private boolean ready = false;
    public Market(Map<String, TradeItem> stocks, DatabaseManager dbManager) {}

    public Market(ClientFacade clientFacade, DatabaseManager db, Account account) throws Exception {
        this.clientFacade = clientFacade;
        clientFacade.setTradeListener(this);
        stocks = new LinkedHashMap<>();
        setDatabase(db);
        setAccount(account);
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
        clientFacade.subscribe(symbol);
        double open = clientFacade.fetchQuote(symbol);
        Stock stock = new Stock("name", symbol);
        stock.setOpen(open);
        stocks.put(symbol, stock);
        //Thread.sleep(200);
    }


    public void add(String[] symbols) throws Exception {
        // pass off list
        for (String symbol : symbols) {
            add(symbol);
        }

    }

    public void addFromWatchlist(com.accountmanager.Watchlist watchlist) throws Exception {
        for (TradeItem stock : watchlist.getWatchlist()) {
            String sym = stock.getSymbol();
            if (!stocks.containsKey(sym)) {
                clientFacade.subscribe(sym);
                double open = clientFacade.fetchQuote(sym);
                stock.setOpen(open);
                System.out.println("Open: " + open);
                stocks.put(sym, stock);
                initChangeBaseline(stock);
            }
        }
    }

    private void initChangeBaseline(TradeItem stock) {
        try {
            double[] lp = dbManager.latestAndPrevClose(stock.getSymbol());
            double last = lp[0], prev = lp[1];

            double baseline = (!Double.isNaN(prev) && prev > 0) ? prev :
                    (!Double.isNaN(last) && last > 0) ? last : Double.NaN;

            if (!Double.isNaN(baseline) && baseline > 0) {
                stock.setPrevClose(baseline);
            }
        } catch (Exception e) {
            //
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