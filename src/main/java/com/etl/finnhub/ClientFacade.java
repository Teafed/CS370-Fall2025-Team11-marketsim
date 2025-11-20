package com.etl.finnhub;

import com.etl.CompanyProfile;
import com.etl.TradeSource;
import com.models.market.TradeListener;
import com.tools.MockFinnhubClient;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * A facade that unifies various Finnhub clients (WebSocket, Quote, Info,
 * Search) into a single interface.
 * Acts as the main entry point for fetching market data.
 */
public class ClientFacade implements TradeListener, TradeSource {

    String apiKey;
    QuoteClient quoteClient;
    TradeSource webSocketClient;
    InfoClient infoClient;
    SearchClient searchClient;

    TradeListener tradeListener;

    /**
     * Constructs a new ClientFacade.
     * Initializes sub-clients and determines whether to use the real WebSocket or a
     * mock based on market status.
     *
     * @throws Exception If an error occurs during initialization.
     */
    public ClientFacade() throws Exception {
        setAPIKey();
        quoteClient = new QuoteClient(apiKey);
        infoClient = new InfoClient(apiKey);
        searchClient = new SearchClient(apiKey);

        if (getMarketStatus()) {
            webSocketClient = WebSocketClient.start(apiKey);
        } else {
            webSocketClient = MockFinnhubClient.start();
        }
        webSocketClient.setTradeListener(this);

    }

    /**
     * Checks if the US market is currently open.
     *
     * @return True if the market is open, false otherwise.
     */
    public boolean getMarketStatus() {
        return MarketStatusClient.checkStatus();
    }

    /**
     * Fetches the current quote for a symbol.
     *
     * @param symbol The stock symbol.
     * @return The current price (open price is returned by QuoteClient currently).
     */
    public double fetchQuote(String symbol) {
        return quoteClient.fetchQuote(symbol);
    }

    /**
     * Fetches company profile information for a symbol.
     *
     * @param symbol The stock symbol.
     * @return The CompanyProfile.
     */
    public CompanyProfile fetchInfo(String symbol) {
        return infoClient.fetchInfo(symbol);
    }

    /**
     * Callback for trade events. Forwards the event to the registered listener.
     *
     * @param symbol The stock symbol.
     * @param price  The trade price.
     */
    @Override
    public void onTrade(String symbol, double price) {
        tradeListener.onTrade(symbol, price);
    }

    /**
     * Sets the listener to receive trade updates.
     *
     * @param listener The TradeListener.
     */
    @Override
    public void setTradeListener(TradeListener listener) {
        this.tradeListener = listener;
    }

    /**
     * Subscribes to real-time updates for a symbol.
     *
     * @param symbol The stock symbol.
     * @throws Exception If an error occurs during subscription.
     */
    @Override
    public void subscribe(String symbol) throws Exception {
        webSocketClient.subscribe(symbol);
    }

    /**
     * Searches for symbols matching the query.
     *
     * @param symbol The search query.
     * @return A 2D array of [symbol, description] pairs.
     */
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
