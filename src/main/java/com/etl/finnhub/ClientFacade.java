package com.etl.finnhub;

import com.etl.CompanyProfile;
import com.etl.TradeSource;
import com.models.market.TradeListener;
import com.tools.MockFinnhubClient;
import io.github.cdimascio.dotenv.Dotenv;

public class ClientFacade implements TradeListener, TradeSource {

    String apiKey;
    QuoteClient quoteClient;
    TradeSource webSocketClient;
    InfoClient infoClient;
    SearchClient searchClient;

    TradeListener tradeListener;

    public ClientFacade() throws Exception {
        setAPIKey();
        quoteClient = new QuoteClient(apiKey);
        infoClient = new InfoClient(apiKey);
        searchClient = new SearchClient(apiKey);

        if (getMarketStatus()) {
            webSocketClient = WebSocketClient.start(apiKey);
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

    public CompanyProfile fetchInfo(String symbol) {
        return infoClient.fetchInfo(symbol);
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

    public String[][] searchSymbol(String symbol) {
        return searchClient.searchSymbol(symbol);
    }

    private void setAPIKey() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = (apiKey == null || apiKey.isBlank()) ? dotenv.get("FINNHUB_API_KEY") : apiKey;
        if (k == null || k.isBlank()) {
            throw new IllegalStateException("The environment variable 'FINNHUB_API_KEY' is not set.");
        }
        this.apiKey = k;
    }


}
