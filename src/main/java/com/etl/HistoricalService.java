package com.etl;

import com.market.DatabaseManager;

import java.net.http.*;

public class HistoricalService {
    public static enum Span {
        MINUTE("minute"),
        HOUR("hour"),
        DAY("day");

        public final String token;
        Span(String token) { this.token = token; }
    }
    private final DatabaseManager db;
    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;

    public HistoricalService(DatabaseManager db) {
        this(db, HttpClient.newHttpClient(), System.getenv("POLYGON_API_KEY"),
                "https://api.polygon.io"); // default
    }

    // package-private for tests
    HistoricalService(DatabaseManager db, HttpClient http, String apiKey, String baseUrl) {
        this.db = db;
        this.http = http;
        this.baseUrl = baseUrl;

        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
        String k = (apiKey == null || apiKey.isBlank()) ? dotenv.get("POLYGON_API_KEY") : apiKey;
        if (k == null || k.isBlank()) throw new IllegalStateException("Set POLYGON_API_KEY");
        this.apiKey = k;
    }

    /**
     *  backfill the last N days using 1/DAY bars by default
     * @param symbol
     * @param daysBack
     * @throws Exception
     */
    public void ensureSeedData(String symbol, int daysBack) throws Exception {
        ensureSeedData(symbol, daysBack, Span.DAY, 1);
    }

    /** overload that lets callers choose span/interval
     * backfill the last N days using 1/DAY bars by default
     * @param symbol
     * @param daysBack
     * @param span
     * @param interval
     * @throws Exception
     */
    public void ensureSeedData(String symbol, int daysBack, Span span, int interval) throws Exception {
        long nowMs = System.currentTimeMillis();

        // if we already have data, don’t re-fetch the same window
        long haveUntil = db.getLatestTimestamp(symbol); // 0 if none
        java.time.LocalDate to = java.time.Instant.ofEpochMilli(nowMs)
                .atZone(java.time.ZoneOffset.UTC).toLocalDate();

        java.time.LocalDate desiredFrom = to.minusDays(Math.max(daysBack, 0));
        java.time.LocalDate actualFrom;

        if (haveUntil > 0) {
            var haveDate = java.time.Instant.ofEpochMilli(haveUntil)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate();

            // Start the next request after the latest bar we have
            actualFrom = haveDate.plusDays(1);
            if (actualFrom.isAfter(to)) return; // up to date
            if (actualFrom.isAfter(desiredFrom)) {
                // only fetch the gap
            } else {
                // fetch the union of [desiredFrom..to], but starting after what we have
                actualFrom = desiredFrom;
            }
        } else {
            actualFrom = desiredFrom;
        }

        backfillRange(symbol, actualFrom, to, span, interval);
    }

    /**
     * format url string for aggregates
     * @param symbol
     * @param interval
     * @param span
     * @param from
     * @param to
     * @return
     */
    private String buildCandlesUrl(String symbol, int interval, Span span,
                                   java.time.LocalDate from, java.time.LocalDate to) {
        return String.format(
                "%s/v2/aggs/ticker/%s/range/%d/%s/%s/%s?adjusted=true&sort=asc&limit=50000&apiKey=%s",
                baseUrl, symbol, interval, span.token, from, to, apiKey
        );
    }

    /**
     * convenience method for chart loading weeee!
     * @param symbol
     * @param start
     * @param end
     * @return
     * @throws Exception
     */
    public java.util.List<ReadData.Row> loadForChart(String symbol, long start, long end) throws Exception {
        ensureSeedData(symbol, 30); // e.g., ensure 30 days present
        return new ReadData("removeThis", db).loadSeries(symbol, start, end);
    }

    /**
     * pulls [from..to] in chunks, parses polygon json, and batch-inserts
     * @param symbol
     * @param from
     * @param to
     * @param span
     * @param interval
     * @throws Exception
     */
    public void backfillRange(String symbol,
                              java.time.LocalDate from,
                              java.time.LocalDate to,
                              Span span,
                              int interval) throws Exception {
        if (to.isBefore(from)) return;

        // choose chunk size: bigger for DAY, smaller for intraday
        final int maxChunkDays = switch (span) {
            case DAY    -> 30;
            case HOUR   -> 14;
            case MINUTE -> 7;
        };

        java.time.LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            java.time.LocalDate chunkEnd = cursor.plusDays(maxChunkDays - 1);
            if (chunkEnd.isAfter(to)) chunkEnd = to;

            String url = buildCandlesUrl(symbol, interval, span, cursor, chunkEnd);
            var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                throw new RuntimeException("Polygon: " + resp.statusCode() + " " + resp.body());
            }

            var root = com.google.gson.JsonParser.parseString(resp.body()).getAsJsonObject();
            // Some Polygon responses can be "OK" with empty results; others can return error JSON.
            if (!root.has("status") || !"OK".equals(root.get("status").getAsString())) {
                // advance cursor anyway; you can add retries/backoff if you like
                cursor = chunkEnd.plusDays(1);
                continue;
            }

            var rows = new java.util.ArrayList<DatabaseManager.CandleData>();
            if (root.has("results")) {
                for (var e : root.getAsJsonArray("results")) {
                    var r = e.getAsJsonObject();
                    long   t = r.get("t").getAsLong();      // ms since epoch
                    double o = r.get("o").getAsDouble();
                    double h = r.get("h").getAsDouble();
                    double l = r.get("l").getAsDouble();
                    double c = r.get("c").getAsDouble();
                    long   v = r.get("v").getAsLong();
                    rows.add(new DatabaseManager.CandleData(symbol, t, o, h, l, c, v));
                }
            }

            if (!rows.isEmpty()) db.insertCandlesBatch(rows);

            cursor = chunkEnd.plusDays(1);
        }
    }


}
