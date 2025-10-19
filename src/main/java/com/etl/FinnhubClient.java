package com.etl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.DatabaseManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FinnhubClient {
    private static final String BASE_URL = "https://finnhub.io/api/v1";
    private static final String LOG_PREFIX = "[FinnhubClient]";
    
    private final String apiKey;
    private final String symbol;
    private final OkHttpClient httpClient;
    private DatabaseManager dbManager;
    private ScheduledExecutorService scheduler;
    private Consumer<Double> priceUpdateCallback;
    private volatile boolean running = false;
    
    public FinnhubClient(String symbol, Consumer<Double> priceUpdateCallback) {
        this.symbol = symbol;
        this.apiKey = getApiKey();
        this.priceUpdateCallback = priceUpdateCallback;
        this.httpClient = new OkHttpClient();
    }
    
    private String getApiKey() {
        String key = System.getenv("FINNHUB_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("The environment variable 'FINNHUB_API_KEY' is not set.");
        }
        return key;
    }
    
    /**
     * Start polling Finnhub REST API for price data
     */
    public void startPolling() {
        if (running) return;
        running = true;
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::fetchQuote, 0, 5, TimeUnit.SECONDS);
        System.out.println(LOG_PREFIX + " Started polling for " + symbol);
    }
    
    /**
     * Stop polling
     */
    public void stopPolling() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            System.out.println(LOG_PREFIX + " Stopped polling for " + symbol);
        }
    }
    
    /**
     * Fetch quote from Finnhub REST API
     */
    private void fetchQuote() {
        if (!running) return;
        
        String url = BASE_URL + "/quote?symbol=" + symbol + "&token=" + apiKey;
        
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println(LOG_PREFIX + " API request failed: " + response.code());
                return;
            }
            
            String responseBody = response.body().string();
            parseAndStoreQuote(responseBody);
            
        } catch (IOException e) {
            System.err.println(LOG_PREFIX + " Error fetching quote: " + e.getMessage());
        }
    }
    
    /**
     * Parse and store quote data
     */
    private void parseAndStoreQuote(String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
            
            double currentPrice = jsonObject.get("c").getAsDouble(); // current price
            double openPrice = jsonObject.get("o").getAsDouble();    // open price
            double highPrice = jsonObject.get("h").getAsDouble();    // high price
            double lowPrice = jsonObject.get("l").getAsDouble();     // low price
            long timestamp = jsonObject.get("t").getAsLong();        // timestamp
            
            // Update price via callback
            if (priceUpdateCallback != null) {
                priceUpdateCallback.accept(currentPrice);
            }
            
            // Store in database if available
            if (dbManager != null) {
                // Use current price for all OHLC since it's a quote
                insertPriceSafely(dbManager, symbol, timestamp, openPrice, highPrice, lowPrice, currentPrice, 0);
            }
            
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " Error parsing quote: " + e.getMessage());
        }
    }
    public static FinnhubClient start(DatabaseManager db, String symbol, Consumer<Double> priceUpdateCallback) {
        FinnhubClient client = new FinnhubClient(symbol, priceUpdateCallback);
        client.dbManager = db;
        client.startPolling();
        return client;
    }

    private static void insertPriceSafely(DatabaseManager db, String symbol, long timestamp, 
                                         double open, double high, double low, double close, long volume) {
        try {
            db.insertPrice(symbol, timestamp, open, high, low, close, volume);
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " Failed to insert price into DB: " + e.getMessage());
        }
    }
}