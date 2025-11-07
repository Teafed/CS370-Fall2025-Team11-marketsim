package com.etl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Small helper to fetch current quote prices from Finnhub with a simple in-memory cache.
 * Provides async and sync methods. Respects environment variable FINNHUB_API_KEY.
 */
public class MarketPriceFetcher {
    private static final int DEFAULT_CACHE_SECONDS = 5;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;
    private final String baseUrl;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MarketPriceFetcher-cleaner");
        t.setDaemon(true);
        return t;
    });

    public MarketPriceFetcher() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = System.getenv("FINNHUB_API_KEY");
        if (k == null || k.isBlank()) k = dotenv.get("FINNHUB_API_KEY");
        if (k == null || k.isBlank()) throw new IllegalStateException("FINNHUB_API_KEY not set");
        this.apiKey = k;
        this.baseUrl = "https://finnhub.io/api/v1";

        cleaner.scheduleAtFixedRate(this::cleanupCache, DEFAULT_CACHE_SECONDS, DEFAULT_CACHE_SECONDS, TimeUnit.SECONDS);
    }

    private static class CacheEntry {
        final double price;
        final long ts;
        CacheEntry(double price, long ts) { this.price = price; this.ts = ts; }
    }

    private void cleanupCache() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> (now - e.getValue().ts) > DEFAULT_CACHE_SECONDS * 1000L);
    }

    public CompletableFuture<Double> fetchCurrentPriceAsync(String symbol) {
        if (symbol == null || symbol.isBlank()) return CompletableFuture.completedFuture(null);
        // Return cached value if fresh
        CacheEntry ce = cache.get(symbol);
        if (ce != null && (System.currentTimeMillis() - ce.ts) < DEFAULT_CACHE_SECONDS * 1000L) {
            return CompletableFuture.completedFuture(ce.price);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Double p = fetchCurrentPrice(symbol);
                return p;
            } catch (Exception e) {
                return null;
            }
        });
    }

    public Double fetchCurrentPrice(String symbol) throws IOException, InterruptedException {
        if (symbol == null || symbol.isBlank()) return null;
        String url = this.baseUrl + "/quote?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) + "&token=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) return null;

        String body = resp.body();
        if (body == null || body.isBlank()) return null;

        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            if (obj.has("c")) {
                double current = obj.get("c").getAsDouble();
                cache.put(symbol, new CacheEntry(current, System.currentTimeMillis()));
                return current;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void shutdown() {
        cleaner.shutdownNow();
    }
}
