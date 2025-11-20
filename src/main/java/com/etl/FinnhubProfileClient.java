package com.etl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Small REST client focused on the Finnhub /stock/profile2 endpoint.
 */
public class FinnhubProfileClient {
    private static final String LOG_PREFIX = "[FinnhubProfile]";

    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;

    /**
     * Constructs a new FinnhubProfileClient using default settings and environment
     * variables.
     */
    public FinnhubProfileClient() {
        this(HttpClient.newHttpClient(), System.getenv("FINNHUB_API_KEY"), "https://finnhub.io/api/v1");
    }

    /**
     * Constructs a new FinnhubProfileClient with custom dependencies.
     *
     * @param httpClient The HttpClient to use for requests.
     * @param apiKey     The Finnhub API key.
     * @param baseUrl    The base URL for the Finnhub API.
     */
    public FinnhubProfileClient(HttpClient httpClient, String apiKey, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl == null ? "https://finnhub.io/api/v1" : baseUrl;

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = (apiKey == null || apiKey.isBlank()) ? dotenv.get("FINNHUB_API_KEY") : apiKey;
        if (k == null || k.isBlank())
            throw new IllegalStateException("The environment variable 'FINNHUB_API_KEY' is not set.");
        this.apiKey = k;
    }

    /**
     * Minimal immutable holder for company profile fields returned by Finnhub
     * /stock/profile2.
     * See: https://finnhub.io/docs/api/company-profile2
     */
    public static record CompanyProfile(
            String country,
            String currency,
            String exchange,
            String finnhubIndustry,
            String ipo,
            Double marketCapitalization,
            String name,
            String phone,
            Double shareOutstanding,
            String ticker,
            String weburl,
            String logo) {
    }

    /**
     * Retrieves the company profile for a given symbol.
     *
     * @param symbol The stock symbol.
     * @return A CompanyProfile object containing the profile data.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the operation is interrupted.
     */
    public CompanyProfile getCompanyProfile(String symbol) throws IOException, InterruptedException {
        if (symbol == null || symbol.isBlank())
            throw new IllegalArgumentException("symbol required");
        String url = this.baseUrl + "/stock/profile2?symbol=" + symbol + "&token=" + apiKey;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        int sc = resp.statusCode();
        if (sc < 200 || sc >= 300) {
            throw new RuntimeException(LOG_PREFIX + " profile request failed: " + sc + " " + resp.body());
        }

        String body = resp.body();
        if (body == null || body.isBlank())
            throw new RuntimeException(LOG_PREFIX + " empty profile response");

        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();

        String country = obj.has("country") ? obj.get("country").getAsString() : null;
        String currency = obj.has("currency") ? obj.get("currency").getAsString() : null;
        String exchange = obj.has("exchange") ? obj.get("exchange").getAsString() : null;
        String finnhubIndustry = obj.has("finnhubIndustry") ? obj.get("finnhubIndustry").getAsString() : null;
        String ipo = obj.has("ipo") ? obj.get("ipo").getAsString() : null;
        Double marketCap = obj.has("marketCapitalization") && !obj.get("marketCapitalization").isJsonNull()
                ? obj.get("marketCapitalization").getAsDouble()
                : null;
        String name = obj.has("name") ? obj.get("name").getAsString() : null;
        String phone = obj.has("phone") ? obj.get("phone").getAsString() : null;
        Double shareOut = obj.has("shareOutstanding") && !obj.get("shareOutstanding").isJsonNull()
                ? obj.get("shareOutstanding").getAsDouble()
                : null;
        String ticker = obj.has("ticker") ? obj.get("ticker").getAsString() : null;
        String weburl = obj.has("weburl") ? obj.get("weburl").getAsString() : null;
        String logo = obj.has("logo") ? obj.get("logo").getAsString() : null;

        return new CompanyProfile(country, currency, exchange, finnhubIndustry, ipo,
                marketCap, name, phone, shareOut, ticker, weburl, logo);
    }
}