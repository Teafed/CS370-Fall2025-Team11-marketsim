package com.etl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.DatabaseManager;
import com.market.TradeListener;
import io.github.cdimascio.dotenv.Dotenv;

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
<<<<<<< Updated upstream
public class FinnhubClient implements TradeSource {
=======
public class FinnhubClient {
    private static final String BASE_URL = "https://finnhub.io/api/v1";
>>>>>>> Stashed changes
    private static final String LOG_PREFIX = "[FinnhubClient]";

    private final String apiKey;
<<<<<<< Updated upstream
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Set<String> subscribed = new ConcurrentSkipListSet<>();

    private volatile DatabaseManager dbManager;
    private volatile TradeListener listener;
    private ScheduledExecutorService scheduler;
    private Consumer<Double> priceUpdateCallback;
    private volatile boolean running = false;

    FinnhubClient() {
        this(HttpClient.newHttpClient(), System.getenv("FINNHUB_API_KEY"), "https://finnhub.io/api/v1");
=======
    private final String symbol;
    private final OkHttpClient httpClient;
    private volatile DatabaseManager dbManager;
    private ScheduledExecutorService scheduler;
    private Consumer<Double> priceUpdateCallback;
    private volatile boolean running = false;
    
    /**
     * Creates a new FinnhubClient for the specified stock symbol.
     * 
     * @param symbol The stock symbol to fetch data for
     * @param priceUpdateCallback Callback function that will be called with updated price data
     */
    public FinnhubClient(String symbol, Consumer<Double> priceUpdateCallback) {
        this.symbol = symbol;
        this.apiKey = getApiKey();
        this.priceUpdateCallback = priceUpdateCallback;
        this.httpClient = new OkHttpClient();
>>>>>>> Stashed changes
    }

    public FinnhubClient(HttpClient httpClient, String apiKey, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = (apiKey == null || apiKey.isBlank()) ? dotenv.get("FINNHUB_API_KEY") : apiKey;
        if (k == null || k.isBlank()) {
            throw new IllegalStateException("The environment variable 'FINNHUB_API_KEY' is not set.");
        }
        this.apiKey = k;
    }
<<<<<<< Updated upstream

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
=======
    
    /**
     * Starts polling the Finnhub REST API for price data at regular intervals.
     * The polling occurs every 5 seconds and continues until stopPolling() is called.
     * If polling is already in progress, this method does nothing.
     */
    public void startPolling() {
>>>>>>> Stashed changes
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

        String url = this.baseUrl + "/quote?symbol=" + symbol + "&token=" + apiKey;

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
<<<<<<< Updated upstream

            String body = response.body();
            if (body == null || body.isBlank()) {
                System.err.println(LOG_PREFIX + " Empty response body received");
                return;
            }

            parseAndStoreQuote(symbol, body);
        } catch (IOException | InterruptedException e) {
=======
            
            if (response.body() == null) {
                System.err.println(LOG_PREFIX + " Empty response body received");
                return;
            }
            
            String responseBody = response.body().string();
            parseAndStoreQuote(responseBody);
            
        } catch (IOException e) {
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream

            // Check if all required fields exist
            if (!jsonObject.has("c") || !jsonObject.has("o") || !jsonObject.has("h") ||
                    !jsonObject.has("l") || !jsonObject.has("t")) {
                System.err.println(LOG_PREFIX + " Missing required fields in response: " + responseBody);
                return;
            }

=======
            
            // Check if all required fields exist
            if (!jsonObject.has("c") || !jsonObject.has("o") || !jsonObject.has("h") || 
                !jsonObject.has("l") || !jsonObject.has("t")) {
                System.err.println(LOG_PREFIX + " Missing required fields in response: " + responseBody);
                return;
            }
            
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
    private static void insertPriceSafely(DatabaseManager db, String symbol, long timestamp,
                                          double open, double high, double low, double close, long volume) {
//        try {
//            db.insertCandle(symbol, timestamp, open, high, low, close, volume);
//        } catch (Exception e) {
//            System.err.println(LOG_PREFIX + " Failed to insert price into DB: " + e.getMessage());
//        }
=======
    /**
     * Factory method to create, configure, and start a FinnhubClient in one step.
     * 
     * @param db The database manager for storing price data
     * @param symbol The stock symbol to fetch data for
     * @param priceUpdateCallback Callback function that will be called with updated price data
     * @return A configured and running FinnhubClient instance
     */
    public static FinnhubClient start(DatabaseManager db, String symbol, Consumer<Double> priceUpdateCallback) {
        FinnhubClient client = new FinnhubClient(symbol, priceUpdateCallback);
        client.dbManager = db;
        client.startPolling();
        return client;
>>>>>>> Stashed changes
    }

    public static TradeSource start() {
        return new FinnhubClient();
    }
}