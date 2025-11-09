package com.etl.finnhub;

import com.etl.TradeSource;
import com.market.TradeListener;
import com.tools.MockFinnhubClient;

public class ClientFacade implements TradeListener, TradeSource {

    QuoteClient quoteClient;
    TradeSource webSocketClient;
    TradeListener tradeListener;


    public ClientFacade() throws Exception {
        quoteClient = new QuoteClient();

        if (getMarketStatus()) {
            webSocketClient = WebSocketClient.start();
        }
        else{
            webSocketClient = MockFinnhubClient.start();
        }
        webSocketClient.setTradeListener(this);

    }

    public boolean getMarketStatus() {
        return MarketStatusClient.checkStatus();
    }

    public double fetchQuote(String symbol) {
        return quoteClient.fetchQuote(symbol);
    }

    @Override
    public void onTrade(String symbol, double price) {
        tradeListener.onTrade(symbol, price);
    }

    @Override
    public void setTradeListener(TradeListener listener) {
        this.tradeListener = listener;
    }

    @Override
    public void subscribe(String symbol) throws Exception {
        webSocketClient.subscribe(symbol);
    }

}
