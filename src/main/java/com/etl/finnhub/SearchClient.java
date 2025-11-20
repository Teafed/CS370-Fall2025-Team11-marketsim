package com.etl.finnhub;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for searching stock symbols via Finnhub.
 */
public class SearchClient {

    private static final String LOG_PREFIX = "[FinnhubSearchClient]";
    private String apiKey;
    private final String baseUrl = "https://finnhub.io/api/v1/search?q=";
    private final HttpClient httpClient;

    /**
     * Constructs a new SearchClient.
     *
     * @param apiKey The Finnhub API key.
     */
    public SearchClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Searches for symbols matching the query string.
     *
     * @param symbol The search query (e.g., "AAPL").
     * @return A 2D array where each row is [symbol, description].
     */
    public String[][] searchSymbol(String symbol) {

        String url = this.baseUrl + symbol + "&exchange=US&token=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int sc = response.statusCode();
            if (sc < 200 || sc >= 300) {
                System.err.println(LOG_PREFIX + " API request failed: " + sc);
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                System.err.println(LOG_PREFIX + " Empty response body received");
            }

            return parseAndStoreSearch(body);
        } catch (IOException | InterruptedException e) {
            System.err.println(LOG_PREFIX + " Error fetching quote: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        return new String[0][];
    }

    /**
     * Parses the search results from the JSON response.
     *
     * @param json The JSON response string.
     * @return A 2D array of search results.
     */
    private String[][] parseAndStoreSearch(String json) {
        JSONObject jsonObject = new JSONObject(json);
        JSONArray results = jsonObject.getJSONArray("result");

        String[][] output = new String[results.length()][2];

        for (int i = 0; i < results.length(); i++) {
            JSONObject obj = results.getJSONObject(i);
            output[i][0] = obj.getString("symbol");
            output[i][1] = obj.getString("description");
        }
        return output;
    }
}
