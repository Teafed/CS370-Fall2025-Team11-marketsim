package com.etl.finnhub;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for fetching stock quotes from Finnhub.
 */
public class QuoteClient {

    private static final String LOG_PREFIX = "[FinnhubQuoteClient]";
    private String apiKey;
    private final String baseUrl = "https://finnhub.io/api/v1/quote?symbol=";
    private final HttpClient httpClient;

    /**
     * Constructs a new QuoteClient.
     *
     * @param apiKey The Finnhub API key.
     */
    public QuoteClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetches the current quote for a symbol.
     *
     * @param symbol The stock symbol.
     * @return The open price of the stock (as per current implementation).
     */
    public String fetchQuote(String symbol) {
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
                throw new IOException("HTTP " + sc);
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                System.err.println(LOG_PREFIX + " Empty response body received");
                throw new IOException("Empty response body");
            }

            return body;
        } catch (IOException | InterruptedException e) {
            System.err.println(LOG_PREFIX + " Error fetching quote: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        return null;
    }

    public double fetchCurrentQuote(String symbol) {
        String response = fetchQuote(symbol);
        return parseResponse(response, "c");
    }

    public double[] fetchInitializingQuote(String symbol) {
        String response = fetchQuote(symbol);
        double open = parseResponse(response, "o");
        double current = parseResponse(response, "c");
        double previousClose = parseResponse(response, "pc");
        return new double[] {open, current, previousClose};
    }

    /**
     * Parse and store quote data
     */
    private double parseResponse(String responseBody, String data) {
        JSONObject jsonObject = new JSONObject(responseBody);

        return jsonObject.getDouble(data);
    }
}
