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

public class FinnhubRESTClient {
    private static final String API_URL = "https://finnhub.io/api/v1/stock/candle";
    private static final String LOG_PREFIX = "[FinnhubClient]";
    private static final int POLL_INTERVAL_SECONDS = 60;

    private final String apiKey;
    private final String symbol;
    private final HttpClient httpClient;
    private final Gson gson;
    private final ScheduledExecutorService scheduler;

    public FinnhubRESTClient(String symbol) {
        this.symbol = symbol;
        this.apiKey = getApiKey();
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.scheduler = createScheduler(symbol);
    }

    private String getApiKey() {
        String key = System.getenv("FINNHUB_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("The environment variable 'FINNHUB_API_KEY' is not set.");
        }
        return key;
    }

    private ScheduledExecutorService createScheduler(String symbol) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
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
        Runnable task = () -> fetchCandleData(onData, onError);
        scheduler.scheduleAtFixedRate(task, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void fetchCandleData(Consumer<CandleResponse> onData, Consumer<Exception> onError) {
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
                    .thenAccept(body -> processResponse(body, onData, onError))
                    .exceptionally(ex -> {
                        onError.accept(new RuntimeException(ex));
                        return null;
                    });
        } catch (Exception e) {
            onError.accept(e);
        }
    }

    private void processResponse(String body, Consumer<CandleResponse> onData, Consumer<Exception> onError) {
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
    }

    public void stopPolling() {
        try {
            scheduler.shutdownNow();
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static FinnhubRESTClient start(DatabaseManager db, String symbol) {
        FinnhubRESTClient client = new FinnhubRESTClient(symbol);

        client.startPolling(candle -> {
            processCandle(candle, symbol, db);
        }, ex -> {
            System.err.println(LOG_PREFIX + " polling error: " + ex.getMessage());
        });

        return client;
    }

    private static void processCandle(CandleResponse candle, String symbol, DatabaseManager db) {
        long[] times = candle.getT();
        if (times == null) return;

        double[] opens = candle.getO();
        double[] highs = candle.getH();
        double[] lows = candle.getL();
        double[] closes = candle.getC();
        double[] vols = candle.getV();

        for (int i = 0; i < times.length; i++) {
            long ts = times[i] * 1000L;
            double close = (closes != null && i < closes.length) ? closes[i] : 0;
            double open = (opens != null && i < opens.length) ? opens[i] : close;
            double high = (highs != null && i < highs.length) ? highs[i] : close;
            double low = (lows != null && i < lows.length) ? lows[i] : close;
            long vol = (vols != null && i < vols.length) ? (long) vols[i] : 0L;
            
            insertPriceSafely(db, symbol, ts, open, high, low, close, vol);
        }
    }

    private static void insertPriceSafely(DatabaseManager db, String symbol, long timestamp, 
                                         double open, double high, double low, double close, long volume) {
        try {
            db.insertPrice(symbol, timestamp, open, high, low, close, volume);
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + " Failed to insert candle into DB: " + e.getMessage());
        }
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
                String symbol = trade.get("s").getAsString();
                
                insertPriceSafely(db, symbol, timestamp, price, price, price, price, volume);
            }
        } catch (Exception ex) {
            System.err.println(LOG_PREFIX + " Error parsing trade data: " + ex.getMessage());
        }
    }
}