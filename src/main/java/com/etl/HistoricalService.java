package com.etl;

import com.market.DatabaseManager;

import java.sql.SQLException;
import com.google.gson.JsonParser;
import java.time.LocalDate;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.http.*;

public class HistoricalService {
    // desired range for
    public static class Range {
        public int multiplier;
        public Timespan timespan;
        public LocalDate from;
        public LocalDate to;

        public Range(Timespan timespan, int multiplier, LocalDate from, LocalDate to) {
            this.timespan = timespan;
            this.multiplier = multiplier;
            this.from = from;
            this.to = to;
        }
        /* might be useful idk
        public Range lastSpans(Timespan timespan, int multiplier, int spansBack) {
            if (spansBack < 1) spansBack = 1;
            LocalDate todayUtc = java.time.Instant.ofEpochMilli(System.currentTimeMillis())
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate();
            int spanMinutes = switch(timespan) {
                case MINUTE -> 1;
                case HOUR   -> 60;
                case DAY    -> 1440;
            };
            long totalMinutes = (long) spansBack * (long) multiplier * (long) spanMinutes;
            int backDays = (int) ((totalMinutes + 1440 - 1) / 1440);
            LocalDate from = todayUtc.minusDays(backDays);
            return new Range(timespan, multiplier, from, todayUtc);
        }
         */
    }

    public enum Timespan {
        MINUTE("minute"),
        HOUR("hour"),
        DAY("day");

        public final String token;
        Timespan(String token) { this.token = token; }
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

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = (apiKey == null || apiKey.isBlank()) ? dotenv.get("POLYGON_API_KEY") : apiKey;
        if (k == null || k.isBlank()) throw new IllegalStateException("Set POLYGON_API_KEY");
        this.apiKey = k;
    }

    Range ensureRange(String symbol, Range requested) throws SQLException {
        long nowMs = System.currentTimeMillis();
        LocalDate todayUtc = java.time.Instant.ofEpochMilli(nowMs)
                .atZone(java.time.ZoneOffset.UTC).toLocalDate();

        LocalDate to = (requested.to != null) ? requested.to : todayUtc;
        LocalDate from = (requested.from != null) ? requested.from : to;
        if (to.isBefore(from)) return null;

        long haveUntil = db.getLatestTimestamp(symbol); // 0 if none
        if (haveUntil > 0) {
            LocalDate haveDate = java.time.Instant.ofEpochMilli(haveUntil)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate();
            LocalDate nextAfterHave = haveDate.plusDays(1);

            if (nextAfterHave.isAfter(to)) {
                // already up to date
                return null;
            }
            // start at the latter of 'from' and what we already have
            from = (from.isBefore(nextAfterHave)) ? nextAfterHave : from;
        }

        return new Range(requested.timespan, requested.multiplier, from, to);
    }

    /* format url string for candles */
    private String buildCandlesUrl(String symbol, int multiplier, Timespan timespan,
                                   LocalDate from, LocalDate to) {
        return String.format(
                "%s/v2/aggs/ticker/%s/range/%d/%s/%s/%s?adjusted=true&sort=asc&limit=50000&apiKey=%s",
                baseUrl, symbol, multiplier, timespan.token, from, to, apiKey
        );
    }

    /**
     * pulls range in chunks, parses polygon json, and batch-inserts
     * @param symbol the symbol to fetch
     * @param requested requested range (ensured valid)
     * @throws Exception throws if status code != 200
     */
    public void backfillRange(String symbol,
                              Range requested) throws Exception {
        Range range = ensureRange(symbol, requested);
        if (range == null || range.to.isBefore(range.from)) return;

        final int maxChunkDays = switch (range.timespan) {
            case DAY    -> 30;
            case HOUR   -> 14;
            case MINUTE -> 7;
        };

        LocalDate cursor = range.from;
        while (!cursor.isAfter(range.to)) {
            LocalDate chunkEnd = cursor.plusDays(maxChunkDays - 1);
            if (chunkEnd.isAfter(range.to)) chunkEnd = range.to;

            String url = buildCandlesUrl(symbol, range.multiplier, range.timespan, cursor, chunkEnd);
            var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
            var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                throw new RuntimeException("[HistoricalService] " + resp.statusCode() + " " + resp.body());
            }

            var root = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (!root.has("status") || !"OK".equals(root.get("status").getAsString())) {
                // note: some Polygon responses can be "OK" with empty results; others can return error
                // ignore not found and advance cursor anyway
                // maybe add retry if this is a problem
                cursor = chunkEnd.plusDays(1);
                continue;
            }

            var rows = new java.util.ArrayList<DatabaseManager.CandleData>();
            if (root.has("results")) {
                for (var e : root.getAsJsonArray("results")) {
                    var res = e.getAsJsonObject();
                    long   t = res.get("t").getAsLong();      // ms since epoch
                    double o = res.get("o").getAsDouble();
                    double h = res.get("h").getAsDouble();
                    double l = res.get("l").getAsDouble();
                    double c = res.get("c").getAsDouble();
                    long   v = res.get("v").getAsLong();
                    rows.add(new DatabaseManager.CandleData(symbol, t, o, h, l, c, v));
                }
            }

            if (!rows.isEmpty()) db.insertCandlesBatch(rows);

            cursor = chunkEnd.plusDays(1);
        }
    }
}
