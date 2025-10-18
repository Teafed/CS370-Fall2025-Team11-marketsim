package com.etl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.market.DatabaseManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FinnhubClient {
    private static final String API_URL = "https://finnhub.io/api/v1/stock/candle";

    private final String apiKey;
    private final String symbol;
    private final HttpClient httpClient;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    public FinnhubClient(String symbol) {
        this.symbol = symbol;
        this.apiKey = System.getenv("FINNHUB_API_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("The environment variable 'FINNHUB_API_KEY' is not set.");
        }
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FinnhubClient-poller-" + symbol);
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Start polling Finnhub for candle data. The onData consumer receives a deserialized CandleResponse.
     * onError receives any Exception that occurs while polling or parsing.
     */
    public void startPolling(Consumer<CandleResponse> onData, Consumer<Exception> onError) {
        Runnable task = () -> {
            try {
                long to = Instant.now().getEpochSecond();
                long from = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
                String resolution = "1"; // 1 minute candles

                String url = String.format("%s?symbol=%s&resolution=%s&from=%d&to=%d&token=%s",
                        API_URL, symbol, resolution, from, to, apiKey);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenAccept(body -> {
                            try {
                                CandleResponse resp = gson.fromJson(body, CandleResponse.class);
                                if (resp != null && "ok".equalsIgnoreCase(resp.getS())) {
                                    onData.accept(resp);
                                } else {
                                    onError.accept(new RuntimeException("Finnhub returned non-ok status or empty response: " + body));
                                }
                            } catch (Exception e) {
                                onError.accept(e);
                            }
                        })
                        .exceptionally(ex -> {
                            onError.accept(new RuntimeException(ex));
                            return null;
                        });

            } catch (Exception e) {
                onError.accept(e);
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, 60, TimeUnit.SECONDS);
    }

    public void stopPolling() {
        try {
            scheduler.shutdownNow();
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static FinnhubClient start(DatabaseManager db, String symbol) {
        FinnhubClient client = new FinnhubClient(symbol);

        client.startPolling(candle -> {
            long[] times = candle.getT();
            double[] opens = candle.getO();
            double[] highs = candle.getH();
            double[] lows = candle.getL();
            double[] closes = candle.getC();
            double[] vols = candle.getV();

            if (times == null) return;

            for (int i = 0; i < times.length; i++) {
                long ts = times[i] * 1000L;
                double open = (opens != null && i < opens.length) ? opens[i] : closes[i];
                double high = (highs != null && i < highs.length) ? highs[i] : closes[i];
                double low = (lows != null && i < lows.length) ? lows[i] : closes[i];
                double close = (closes != null && i < closes.length) ? closes[i] : open;
                long vol = (vols != null && i < vols.length) ? (long) vols[i] : 0L;
                try {
                    db.insertPrice(symbol, ts, open, high, low, close, vol);
                } catch (Exception e) {
                    System.err.println("[FinnhubClient] Failed to insert candle into DB: " + e.getMessage());
                }
            }
        }, ex -> {
            System.err.println("[FinnhubClient] polling error: " + ex.getMessage());
        });

        return client;
    }

    static void parseAndStore(String msg, DatabaseManager db) {
        try {
            JsonObject obj = new com.google.gson.JsonParser().parse(msg).getAsJsonObject();
            if (!obj.has("data")) return;

            for (JsonElement e : obj.getAsJsonArray("data")) {
                JsonObject trade = e.getAsJsonObject();
                double price = trade.get("p").getAsDouble();
                long timestamp = trade.get("t").getAsLong();
                long volume = trade.get("v").getAsLong();
                String s = trade.get("s").getAsString();
                try {
                    db.insertPrice(s, timestamp, price, price, price, price, volume);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}