package com.etl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.DatabaseManager;
import com.market.TradeListener;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Client for interacting with the Finnhub API to fetch stock price data.
 * This class handles polling the API at regular intervals and processing the responses.
 */
public class FinnhubClient implements TradeSource {
    private static final String BASE_URL = "https://finnhub.io/api/v1";
    private static final String LOG_PREFIX = "[FinnhubClient]";

    private final String apiKey;
    private final HttpClient httpClient;
    private final Set<String> subscribed = new ConcurrentSkipListSet<>();

    private volatile DatabaseManager dbManager;
    private volatile TradeListener listener;
    private ScheduledExecutorService scheduler;
    private Consumer<Double> priceUpdateCallback;
    private volatile boolean running = false;

    public FinnhubClient() {
        this.apiKey = getApiKey();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    private String getApiKey() {
        String key = System.getenv("FINNHUB_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("The environment variable 'FINNHUB_API_KEY' is not set.");
        }
        return key;
    }

    @Override
    public void setTradeListener(TradeListener listener) {
        this.listener = listener;
    }

    @Override
    public void subscribe(String symbol) {
        if (symbol == null || symbol.isBlank()) return;
        subscribed.add(symbol);
        System.out.println(LOG_PREFIX + " Subscribed to " + symbol);
        ensurePolling();
    }

    private synchronized void ensurePolling() {
        if (running) return;
        running = true;
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::pollAll, 0, 5, TimeUnit.SECONDS);
        System.out.println(LOG_PREFIX + " Polling every " + 5 + "s");
    }

    private void pollAll() {
        if (!running) return;
        if (subscribed.isEmpty()) return;

        for (String sym : subscribed) {
            fetchQuote(sym);
            // Finnhub rate limits vary; if needed, sleep a bit between symbols:
            // try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
    }

    public synchronized void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        System.out.println(LOG_PREFIX + " Stopped.");
    }
    /**
     * Stops the polling process.
     * This method attempts to gracefully shut down the scheduler, waiting up to 5 seconds
     * for tasks to complete before forcing a shutdown.
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
            System.out.println(LOG_PREFIX + " Stopped polling");
        }
    }


    /**
     * Fetch quote from Finnhub REST API
     */
    private void fetchQuote(String symbol) {
        if (!running) return;

        String url = BASE_URL + "/quote?symbol=" + symbol + "&token=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int sc = response.statusCode();
            if (sc < 200 || sc >= 300) {
                System.err.println(LOG_PREFIX + " API request failed: " + sc);
                return;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                System.err.println(LOG_PREFIX + " Empty response body received");
                return;
            }

            parseAndStoreQuote(symbol, body);
        } catch (IOException | InterruptedException e) {
            System.err.println(LOG_PREFIX + " Error fetching quote: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Parse and store quote data
     */
    private void parseAndStoreQuote(String symbol, String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

            // Check if all required fields exist
            if (!jsonObject.has("c") || !jsonObject.has("o") || !jsonObject.has("h") ||
                    !jsonObject.has("l") || !jsonObject.has("t")) {
                System.err.println(LOG_PREFIX + " Missing required fields in response: " + responseBody);
                return;
            }

            double currentPrice = jsonObject.get("c").getAsDouble(); // current price
            double openPrice = jsonObject.get("o").getAsDouble();    // open price
            double highPrice = jsonObject.get("h").getAsDouble();    // high price
            double lowPrice = jsonObject.get("l").getAsDouble();     // low price
            long timestamp = jsonObject.get("t").getAsLong();        // timestamp

            // Notify market
            if (listener != null && currentPrice > 0) {
                listener.onTrade(symbol, currentPrice);
            }

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
    private static void insertPriceSafely(DatabaseManager db, String symbol, long timestamp,
                                          double open, double high, double low, double close, long volume) {
//        try {
//            db.insertCandle(symbol, timestamp, open, high, low, close, volume);
//        } catch (Exception e) {
//            System.err.println(LOG_PREFIX + " Failed to insert price into DB: " + e.getMessage());
//        }
    }

    public static TradeSource start() {
        return new FinnhubClient();
    }
}