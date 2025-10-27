package com.etl;

import com.market.DatabaseManager;

import java.sql.SQLException;
import com.google.gson.JsonParser;
import java.time.LocalDate;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.*;
import java.time.*;

public class HistoricalService {
    private static final Logger log = LoggerFactory.getLogger(HistoricalService.class);

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
        LocalDate todayUtc = java.time.Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(java.time.ZoneOffset.UTC).toLocalDate();

        LocalDate reqTo = (requested.to == null || requested.to.isAfter(todayUtc)) ? todayUtc : requested.to;
        LocalDate reqFrom = (requested.from == null) ? reqTo : requested.from;

        if (requested.timespan == Timespan.DAY) {
            reqTo = reqTo.minusDays(1);
            // nudge weekends to previous friday
            if (reqTo.getDayOfWeek() == java.time.DayOfWeek.SATURDAY) reqTo = reqTo.minusDays(1);
            if (reqTo.getDayOfWeek() == java.time.DayOfWeek.SUNDAY)   reqTo = reqTo.minusDays(2);
        } else {
            // HOUR/MINUTE: clamp "to" to the previous trading day so we don't request weekends
            reqTo = prevTradingDay(reqTo);
            // And bump "from" forward if it starts on a weekend
            reqFrom = nextTradingDay(reqFrom);
            // Safety: if the range collapses (e.g., from==to and it's still weekend), expand a bit
            if (reqFrom.isAfter(reqTo)) {
                // heuristics: for 1W hour-bars it's reasonable to pull the last 5 trading days
                reqTo = prevTradingDay(todayUtc);
                reqFrom = reqTo.minusDays(7);
                reqFrom = nextTradingDay(reqFrom);
            }
        }

        if (reqTo.isBefore(reqFrom)) return null;

        long latestMs = db.getLatestTimestamp(symbol, requested.multiplier, requested.timespan.token);
        long earliestMs = db.getEarliestTimestamp(symbol, requested.multiplier, requested.timespan.token);

        if (latestMs == 0L || earliestMs == 0L) {
            // no data yet for this symbol/timeframe: fetch the whole requested window
            return new Range(requested.timespan, requested.multiplier, reqFrom, reqTo);
        }

        LocalDate haveLatest = java.time.Instant.ofEpochMilli(latestMs).atZone(java.time.ZoneOffset.UTC).toLocalDate();
        LocalDate haveEarliest = java.time.Instant.ofEpochMilli(earliestMs).atZone(java.time.ZoneOffset.UTC).toLocalDate();

        // forward fill
        LocalDate forwardFrom = haveLatest.plusDays(1);
        if (!forwardFrom.isAfter(reqTo)) {
            LocalDate from = (reqFrom.isAfter(forwardFrom)) ? reqFrom : forwardFrom;
            if (!reqTo.isBefore(from)) return new Range(requested.timespan, requested.multiplier, from, reqTo);
        }

        // backfill (if there's older data missing)
        LocalDate backfillTo = haveEarliest.minusDays(1);
        if (!reqFrom.isAfter(backfillTo)) {
            LocalDate to = (reqTo.isBefore(backfillTo)) ? reqTo : backfillTo;
            if (!to.isBefore(reqFrom)) return new Range(requested.timespan, requested.multiplier, reqFrom, to);
        }

        // if there's a hole (may be null if none found)
        Range hole = findInteriorHole(symbol, new Range(requested.timespan, requested.multiplier, reqFrom, reqTo));

        return hole;
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
    public int backfillRange(String symbol, Range requested) throws Exception {
        final int MAX_PASSES = 6;  // enough to cover: forward, backward, and multiple holes
        int totalInsertedAll = 0;

        for (int pass = 1; pass <= MAX_PASSES; pass++) {
            Range range = ensureRange(symbol, requested);
            if (range == null || range.to.isBefore(range.from)) {
                System.out.printf("[HistoricalService] Covered %s %d/%s after %d pass(es), totalInserted=%d%n",
                        symbol, requested.multiplier, requested.timespan, (pass - 1), totalInsertedAll);
                break;
            }

            final int maxChunkDays = switch (range.timespan) {
                case DAY    -> 30;
                case HOUR   -> 14;
                case MINUTE -> 7;
            };

            System.out.printf("[HistoricalService] Pass %d: fetching %s %d/%s %s→%s%n",
                    pass, symbol, range.multiplier, range.timespan, range.from, range.to);

            int totalInsertedThisPass = 0;
            LocalDate cursor = range.from;

            while (!cursor.isAfter(range.to)) {
                LocalDate chunkEnd = cursor.plusDays(maxChunkDays - 1);

                LocalDate reqStart = nextTradingDay(cursor);
                LocalDate reqEnd   = prevTradingDay(chunkEnd);

                if (reqEnd.isBefore(reqStart)) {
                    cursor = chunkEnd.plusDays(1);
                    continue;
                }

                String url = buildCandlesUrl(symbol, range.multiplier, range.timespan, cursor, chunkEnd);

                int attempts = 0;
                while (true) {
                    attempts++;
                    var req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();
                    var resp = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                    int sc = resp.statusCode();

                    if (sc == 429) {
                        System.out.println("[HistoricalService] 429 rate limit; backing off for a moment...");
                        Thread.sleep(15000L * attempts); // light backoff
                        if (attempts < 3) continue; // retry a couple of times <3
                        System.out.println("[HistoricalService] giving up on this chunk due to 429");
                        break; // break the retry loop; advance to next chunk (hole will be found later)
                    }
                    if (sc != 200) {
                        throw new RuntimeException("[HistoricalService] " + sc + " " + resp.body());
                    }

                    var root = JsonParser.parseString(resp.body()).getAsJsonObject();
                    String status = root.has("status") ? root.get("status").getAsString() : "(missing)";
                    if (!"OK".equals(status)) {
                        String err = root.has("error") ? root.get("error").getAsString() : "(none)";
                        System.out.printf("[HistoricalService] status=%s error=%s for %s %s→%s%n",
                                status, err, symbol, cursor, chunkEnd);
                        // DELAYED/empty: just skip this chunk; if rows exist below, we still insert them
                    }

                    var rows = new java.util.ArrayList<DatabaseManager.CandleData>();
                    if (root.has("results")) {
                        for (var e : root.getAsJsonArray("results")) {
                            var r = e.getAsJsonObject();
                            long   t = r.get("t").getAsLong();
                            double o = r.get("o").getAsDouble();
                            double h = r.get("h").getAsDouble();
                            double l = r.get("l").getAsDouble();
                            double c = r.get("c").getAsDouble();
                            double v = r.get("v").getAsDouble();
                            rows.add(new DatabaseManager.CandleData(symbol, t, o, h, l, c, v));
                        }
                    }

                    if (!rows.isEmpty()) {
                        db.insertCandlesBatch(symbol, range.multiplier, range.timespan.token, rows);
                        totalInsertedThisPass += rows.size();
                        totalInsertedAll += rows.size();
                        System.out.printf("[HistoricalService] Inserted %d rows for %s %s→%s (passTotal=%d, all=%d)%n",
                                rows.size(), symbol, cursor, chunkEnd, totalInsertedThisPass, totalInsertedAll);
                    } else {
                        System.out.printf("[HistoricalService] No rows for %s %s→%s%n", symbol, cursor, chunkEnd);
                        Thread.sleep(100);
                    }
                    break; // exit retry loop on 200 (whatever the status text)
                }

                // be nicer to the API
                try { Thread.sleep(400); } catch (InterruptedException ignore) {}

                cursor = chunkEnd.plusDays(1);
            }
        }
        return totalInsertedAll;
    }

    /** Count trading days (Mon–Fri) strictly between a and b, assuming a <= b. */
    private static int businessDaysBetween(LocalDate a, LocalDate b) {
        int days = 0;
        LocalDate d = a.plusDays(1);
        while (!d.isAfter(b.minusDays(1))) {
            if (!isWeekend(d)) days++;
            d = d.plusDays(1);
        }
        return Math.max(0, days);
    }

    /** If there is a real trading-day hole inside [from..to], return that subrange (inclusive). Else null. */
    private Range findInteriorHole(String symbol, Range req) throws SQLException {
        long startMs = req.from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endMs   = req.to.atTime(23,59,59).toInstant(ZoneOffset.UTC).toEpochMilli();

        var ts = db.listTimestamps(symbol, req.multiplier, req.timespan.token, startMs, endMs);
        if (ts.isEmpty()) {
            // nothing in window — just fetch it all
            return new Range(req.timespan, req.multiplier, req.from, req.to);
        }

        // We’ll detect holes using dates (UTC) at the day granularity,
        // even for HOUR/MINUTE, which is fine: we backfill full-day spans.
        LocalDate prevDate = Instant.ofEpochMilli(ts.get(0)).atZone(ZoneOffset.UTC).toLocalDate();

        for (int i = 1; i < ts.size(); i++) {
            LocalDate currDate = Instant.ofEpochMilli(ts.get(i)).atZone(ZoneOffset.UTC).toLocalDate();

            int missingTradingDays = businessDaysBetween(prevDate, currDate);
            if (missingTradingDays >= 1) {
                // Bound the hole to trading days only
                LocalDate holeFrom = nextTradingDay(prevDate);
                LocalDate holeTo   = prevTradingDay(currDate);

                // Constrain to the requested window
                if (holeFrom.isBefore(req.from)) holeFrom = req.from;
                if (holeTo.isAfter(req.to))       holeTo   = req.to;

                // Skip if it collapses (e.g., weekend-only)
                if (!holeTo.isBefore(holeFrom)) {
                    return new Range(req.timespan, req.multiplier, holeFrom, holeTo);
                }
            }
            prevDate = currDate;
        }
        return null;
    }

    private static boolean isWeekend(LocalDate d) {
        var w = d.getDayOfWeek();
        return w == java.time.DayOfWeek.SATURDAY || w == java.time.DayOfWeek.SUNDAY;
    }
    private static LocalDate prevTradingDay(LocalDate d) {
        while (isWeekend(d)) d = d.minusDays(1);
        return d;
    }
    private static LocalDate nextTradingDay(LocalDate d) {
        while (isWeekend(d)) d = d.plusDays(1);
        return d;
    }

}
