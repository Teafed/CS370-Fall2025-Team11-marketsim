package com.etl.finnhub;

public class ClientFacade {

    MarketStatusClient marketStatusClient;
    QuoteClient quoteClient;
    WebSocketClient webSocketClient;


    public ClientFacade() {
        quoteClient = new QuoteClient();
        webSocketClient = new WebSocketClient();
    }

    public boolean getMarketStatus() {
        return MarketStatusClient.checkStatus();
    }

    public void getQuote() {

    }

}
