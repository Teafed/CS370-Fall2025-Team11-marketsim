package com.etl.finnhub;

import com.etl.CompanyProfile;
import org.json.JSONObject;
import com.market.Database;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class InfoClient {

    private static final String LOG_PREFIX = "[InfoClient]";
    private String apiKey;
    private final String baseUrl = "https://finnhub.io/api/v1/stock/profile2?symbol=";
    private final HttpClient httpClient;
    private final Set<String> subscribed = new ConcurrentSkipListSet<>();

    private volatile Database dbManager;
    private volatile TradeListener listener;
    private ScheduledExecutorService scheduler;
    private Consumer<Double> priceUpdateCallback;
    private volatile boolean running = false;


    public InfoClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Fetch quote from Finnhub REST API
     */
    public CompanyProfile fetchInfo(String symbol) {

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
                return null;
            }

            String body = response.body();

            if (body == null || body.isBlank()) {
                System.err.println(LOG_PREFIX + " Empty response body received");
                return null;
            }

            return parseAndBuildProfile(body);

        } catch (IOException | InterruptedException e) {
            System.err.println(LOG_PREFIX + " Error fetching quote: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Parse and store quote data
     */
    private CompanyProfile parseAndBuildProfile(String responseBody) {
        JSONObject jsonObject = new JSONObject(responseBody);

        String country = jsonObject.optString("country","unknown");
        String currency = jsonObject.optString("currency","unknown");
        String exchange = jsonObject.optString("exchange","unknown");
        String ipo = jsonObject.optString("ipo","unknown");
        String logo = jsonObject.optString("logo","unknown");
        String marketCapitalization = jsonObject.optString("marketCapitalization","unknown");
        String name = jsonObject.optString("name","unknown");
        String sharesOutstanding = jsonObject.optString("sharesOutstanding","unknown");
        String weburl = jsonObject.optString("weburl","unknown");

        CompanyProfile companyProfile = new CompanyProfile();
        companyProfile.setCountry(country);
        companyProfile.setCurrency(currency);
        companyProfile.setExchange(exchange);
        companyProfile.setIpo(ipo);
        companyProfile.setLogo(logo);
        companyProfile.setMarketCapitalization(marketCapitalization);
        companyProfile.setName(name);
        companyProfile.setSharesOutstanding(sharesOutstanding);
        companyProfile.setWeburl(weburl);

        return companyProfile;
    }
}
