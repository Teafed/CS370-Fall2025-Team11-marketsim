package com.etl.finnhub;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.models.Database;
import com.models.market.TradeListener;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class QuoteClient {

    private static final String LOG_PREFIX = "[FinnhubQuoteClient]";
    private String apiKey;
    private final String baseUrl = "https://finnhub.io/api/v1/quote?symbol=";
    private final HttpClient httpClient;
    private final Set<String> subscribed = new ConcurrentSkipListSet<>();

    private volatile Database dbManager;
    private volatile TradeListener listener;
    private ScheduledExecutorService scheduler;
    private Consumer<Double> priceUpdateCallback;
    private volatile boolean running = false;


    public QuoteClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetch quote from Finnhub REST API
     */
    public double fetchQuote(String symbol) {

        String url = this.baseUrl + symbol + "&token=" + apiKey;

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
                return 0;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                System.err.println(LOG_PREFIX + " Empty response body received");
                return 0;
            }

            return parseAndStoreQuote(symbol, body);
        } catch (IOException | InterruptedException e) {
            System.err.println(LOG_PREFIX + " Error fetching quote: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    return 0;
    }

    /**
     * Parse and store quote data
     */
    private double parseAndStoreQuote(String symbol, String responseBody) {
        try {
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

            // Check if all required fields exist
            if (!jsonObject.has("c") || !jsonObject.has("o") || !jsonObject.has("h") ||
                    !jsonObject.has("l") || !jsonObject.has("t")) {
                System.err.println(LOG_PREFIX + " Missing required fields in response: " + responseBody);
                return 0;
            }

            double currentPrice = jsonObject.get("c").getAsDouble(); // current price
            double openPrice = jsonObject.get("o").getAsDouble();    // open price
            double highPrice = jsonObject.get("h").getAsDouble();    // high price
            double lowPrice = jsonObject.get("l").getAsDouble();     // low price
            long timestamp = jsonObject.get("t").getAsLong();        // timestamp

            System.out.println(openPrice);
            return openPrice;




        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " Error parsing quote: " + e.getMessage());
        }
    return 0;
    }
}
