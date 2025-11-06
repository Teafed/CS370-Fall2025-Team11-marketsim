package com.etl;

import com.market.Database;

import java.sql.SQLException;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.net.http.*;
import java.time.*;

import static java.lang.Long.parseLong;

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
    }

    // not really useful anymore since we're storing everything in the day range
    public enum Timespan {
        MINUTE("minute"),
        HOUR("hour"),
        DAY("day");

        public final String token;
        Timespan(String token) { this.token = token; }
    }

    private final Database db;
    private final HttpClient http;
    private final String apiKey;
    private final String baseUrl;

    // ensure only one caller can backfill
    private static final ConcurrentHashMap<String, Object> KEY_LOCKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> IN_FLIGHT = new ConcurrentHashMap<>();
    private static String key(String s, Range r){ return s+"|"+r.multiplier+"|"+r.timespan.token; }

    // rate limiter - for acquiring tokens
    private static final Semaphore TOKENS = new Semaphore(1);
    private static long last = System.nanoTime();

    public HistoricalService(Database db) {
        this(db, HttpClient.newHttpClient(), System.getenv("POLYGON_API_KEY"),
                "https://api.polygon.io"); // default
    }

    // package-private for tests
    HistoricalService(Database db, HttpClient http, String apiKey, String baseUrl) {
        this.db = db;
        this.http = http;
        this.baseUrl = baseUrl;

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = (apiKey == null || apiKey.isBlank()) ? dotenv.get("POLYGON_API_KEY") : apiKey;
        if (k == null || k.isBlank()) throw new IllegalStateException("Set POLYGON_API_KEY");
        this.apiKey = k;
    }

    public Range ensureRange(String symbol, Range requested) throws SQLException {
        LocalDate todayUtc = Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(ZoneOffset.UTC).toLocalDate();

        LocalDate reqTo = (requested.to == null || requested.to.isAfter(todayUtc)) ? todayUtc : requested.to;
        LocalDate reqFrom = (requested.from == null) ? reqTo : requested.from;

        requested.timespan = Timespan.DAY;

        reqTo = prevTradingDay(reqTo.minusDays(1));

        if (reqTo.isBefore(reqFrom)) return null;

        long latestMs = db.getLatestTimestamp(symbol, requested.multiplier, requested.timespan.token);
        long earliestMs = db.getEarliestTimestamp(symbol, requested.multiplier, requested.timespan.token);

        if (latestMs == 0L || earliestMs == 0L) {
            System.out.printf("[HS.ensureRange] No data for %s %d/%s; fetching %s - %s%n",
                    symbol, requested.multiplier, requested.timespan, reqFrom, reqTo);
            return new Range(requested.timespan, requested.multiplier, reqFrom, reqTo);
        }

        LocalDate haveLatest = Instant.ofEpochMilli(latestMs).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate haveEarliest = Instant.ofEpochMilli(earliestMs).atZone(ZoneOffset.UTC).toLocalDate();

        // forward fill (newer data missing)
        LocalDate forwardFrom = nextTradingDay(haveLatest.plusDays(1));
        if (!forwardFrom.isAfter(reqTo)) {
            LocalDate from = (reqFrom.isAfter(forwardFrom)) ? reqFrom : forwardFrom;
            if (!reqTo.isBefore(from)) {
                System.out.printf("[HS.ensureRange] Forward fill %s %d/%s; fetching %s - %s%n",
                        symbol, requested.multiplier, requested.timespan, from, reqTo);
                return new Range(requested.timespan, requested.multiplier, from, reqTo);
            }
        }

        // backfill (older data missing)
        LocalDate backfillTo = prevTradingDay(haveEarliest.minusDays(1));
        if (!reqFrom.isAfter(backfillTo)) {
            LocalDate to = (reqTo.isBefore(backfillTo)) ? reqTo : backfillTo;
            if (!to.isBefore(reqFrom)) {
                System.out.printf("[HS.ensureRange] Backfill %s %d/%s; fetching %s - %s%n",
                        symbol, requested.multiplier, requested.timespan, reqFrom, to);
                return new Range(requested.timespan, requested.multiplier, reqFrom, to);
            }
        }

        // if there's a hole (null if none found)
        Range hole = findInteriorHole(symbol, new Range(requested.timespan, requested.multiplier, reqFrom, reqTo));
        if (hole != null) System.out.printf("[HS.ensureRange] Hole fill %s %d/%s; fetching %s - %s%n",
                symbol, requested.multiplier, requested.timespan, hole.from, hole.to);

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

    public int backfillRange(String symbol, Range requested) throws Exception {
        String k = key(symbol, requested);
        Object lock = KEY_LOCKS.computeIfAbsent(k, s -> new Object());
        synchronized (lock) {
            if (Boolean.TRUE.equals(IN_FLIGHT.putIfAbsent(k, true))) return 0;
            try {
                return doBackfillRange(symbol, requested); // extract existing logic into this
            } finally { IN_FLIGHT.remove(k); }
        }
    }

    /**
     * pulls range in chunks, parses polygon json, and batch-inserts
     * @param symbol the symbol to fetch
     * @param requested requested range (ensured valid)
     * @throws Exception throws if status code != 200
     */
    public int doBackfillRange(String symbol, Range requested) throws Exception {
        final int MAX_PASSES = 6; // enough to cover: forward, backward, and multiple holes
        int totalInsertedAll = 0;

        Range prevRange = null;

        for (int pass = 1; pass <= MAX_PASSES; pass++) {
            Range range;
            if (pass == 1) {
                range = requested;
            } else {
                range = ensureRange(symbol, requested);
            }

            if (range == null) {
                System.out.printf("[HS.backfillRange] ensureRange returned NULL for %s %d/%s (req=%s - %s)%n",
                        symbol, requested.multiplier, requested.timespan, requested.from, requested.to);
            } else {
                System.out.printf("[HS.backfillRange] ensureRange returned %s - %s%n", range.from, range.to);
            }

            if (range == null || range.to.isBefore(range.from)) {
                System.out.printf("[HS.backfillRange] Covered %s %d/%s after %d pass%s, totalInserted=%d%n",
                        symbol, requested.multiplier, requested.timespan, (pass - 1), (pass - 1) == 1 ? "" : "es", totalInsertedAll);
                break;
            }

            // if ensureRange keeps returning the same span but we didn't insert anything last time, stop
            if (prevRange != null
                    && prevRange.from.equals(range.from)
                    && prevRange.to.equals(range.to)) {
                System.out.printf("[HS.backfillRange] Same range returned twice; stopping. %s %d/%s %s - %s%n",
                        symbol, range.multiplier, range.timespan, range.from, range.to);
                break;
            }
            prevRange = range;

            final int maxChunkDays = switch (range.timespan) {
                case DAY    -> 30;
                case HOUR   -> 14;
                case MINUTE -> 7;
            };

            System.out.printf("[HS.backfillRange] Pass %d: fetching %s %d/%s %s - %s%n",
                    pass, symbol, range.multiplier, range.timespan, range.from, range.to);

            int totalInsertedThisPass = 0;
            LocalDate cursor = range.from;

            while (!cursor.isAfter(range.to)) {
                LocalDate chunkEnd = cursor.plusDays(maxChunkDays - 1);

                LocalDate reqStart = nextTradingDay(cursor);
                LocalDate reqEnd = prevTradingDay(chunkEnd);

                if (reqEnd.isBefore(reqStart)) {
                    System.out.printf("[HS.chunk] skip empty window (cursor=%s chunkEnd=%s reqStart=%s reqEnd=%s)%n",
                            cursor, chunkEnd, reqStart, reqEnd);
                    cursor = chunkEnd.plusDays(1);
                    continue;
                }

                String url = buildCandlesUrl(symbol, range.multiplier, range.timespan, reqStart, reqEnd);

                int attempts;
                for (attempts = 1; ; attempts++) {
                    var req = HttpRequest.newBuilder(java.net.URI.create(url)).GET().build();

                    // request only when you have a token (for rate limit, ~5/min)
                    acquireToken();

                    var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                    int sc = resp.statusCode();

                    if (sc == 429) {
                        long base = parseLong(resp.headers().firstValue("Retry-After").orElse("5"), 5) * 1000L;
                        long sleepMs = (long)(base * Math.pow(1.8, attempts-1) + (Math.random()*250));
                        System.out.printf("[HS.chunk] 429 rate limit for %s %s - %s; sleeping %dms (attempt %d)%n",
                                symbol, reqStart, reqEnd, sleepMs, attempts);
                        Thread.sleep(sleepMs);
                        if (attempts < 3) continue; // retry a couple of times <3
                        Thread.sleep(sleepMs); // cool-off before next chunk
                        System.out.println("[HS.chunk] giving up on this chunk due to 429");
                        break;
                    }
                    if (sc != 200) {
                        throw new RuntimeException("[HS.chunk] " + sc + " " + resp.body());
                    }

                    var root = JsonParser.parseString(resp.body()).getAsJsonObject();
                    String status = root.has("status") ? root.get("status").getAsString() : "(missing)";
                    if (!"OK".equals(status)) {
                        String err = root.has("error") ? root.get("error").getAsString() : "(none)";
                        System.out.printf("[HS.chunk] status=%s error=%s for %s %s - %s%n",
                                status, err, symbol, cursor, chunkEnd);
                    }

                    var rows = new java.util.ArrayList<Database.CandleData>();
                    if (root.has("results")) {
                        for (var e : root.getAsJsonArray("results")) {
                            var r = e.getAsJsonObject();
                            long   t = r.get("t").getAsLong();
                            double o = r.get("o").getAsDouble();
                            double h = r.get("h").getAsDouble();
                            double l = r.get("l").getAsDouble();
                            double c = r.get("c").getAsDouble();
                            double v = r.get("v").getAsDouble();
                            rows.add(new Database.CandleData(symbol, t, o, h, l, c, v));
                        }
                    }

                    if (!rows.isEmpty()) {
                        db.insertCandlesBatch(symbol, range.multiplier, range.timespan.token, rows);
                        totalInsertedThisPass += rows.size();
                        totalInsertedAll += rows.size();
                        System.out.printf("[HS.chunk] Inserted %d rows for %s %s - %s (passTotal=%d, all=%d)%n",
                                rows.size(), symbol, cursor, chunkEnd, totalInsertedThisPass, totalInsertedAll);
                    } else {
                        System.out.printf("[HS.chunk] No rows for %s %s - %s%n", symbol, cursor, chunkEnd);
                    }
                    break; // success, exit retry loop
                }

                // be nicer to the API between chunks
                try { Thread.sleep(400); } catch (InterruptedException ignore) {}

                cursor = reqEnd.plusDays(1);
            }

            // if this pass did nothing, there's probably nothing left (or we’re rate-limited)
            if (totalInsertedThisPass == 0) {
                System.out.printf("[HS.backfillRange] Pass %d inserted 0 rows; stopping early%n", pass);
                break;
            }
        }
        System.out.println("[HistoricalService] Done.");
        return totalInsertedAll;
    }

    private static void acquireToken() throws InterruptedException {
        // refill token every 12s (polygon free tier is 5 calls/minute)
        while (true) {
            long now = System.nanoTime();
            long elapsedMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(now - last);
            if (elapsedMs >= 12000 && TOKENS.availablePermits() == 0) {
                TOKENS.release();
                last = now;
            }
            if (TOKENS.tryAcquire()) return;
            Thread.sleep(100);
        }
    }

    /* search for subrange in case there's an interior hole */
    private Range findInteriorHole(String symbol, Range req) throws SQLException {
        long startMs = req.from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long endMs = req.to.atTime(23,59,59).toInstant(ZoneOffset.UTC).toEpochMilli();

        var ts = db.listTimestamps(symbol, req.multiplier, req.timespan.token, startMs, endMs);
        if (ts.isEmpty()) {
            // nothing in window, just fetch everything
            return new Range(req.timespan, req.multiplier, req.from, req.to);
        }

        LocalDate prevDate = Instant.ofEpochMilli(ts.getFirst()).atZone(ZoneOffset.UTC).toLocalDate();

        for (int i = 1; i < ts.size(); i++) {
            LocalDate currDate = Instant.ofEpochMilli(ts.get(i)).atZone(ZoneOffset.UTC).toLocalDate();

            int missingTradingDays = businessDaysBetween(prevDate, currDate);
            if (missingTradingDays >= 1) {
                LocalDate holeStart = nextTradingDay(prevDate);
                LocalDate holeEnd = prevTradingDay(currDate);

                if (holeStart.isBefore(req.from)) holeStart = req.from;
                if (holeEnd.isAfter(req.to)) holeEnd = req.to;

                if (!holeEnd.isBefore(holeStart)) {
                    // System.out.printf("[HS.findInteriorHole] holeFrom = %s, holeTo = %s%n", holeStart, holeEnd);
                    return new Range(req.timespan, req.multiplier, holeStart, holeEnd);
                }
            }
            prevDate = currDate;
        }
        return null;
    }

    /* count trading days (Mon–Fri) strictly between a and b, assuming a <= b. */
    private static int businessDaysBetween(LocalDate a, LocalDate b) {
        int days = 0;
        LocalDate d = a.plusDays(1);
        while (!d.isAfter(b.minusDays(1))) {
            if (isTradingDay(d)) days++;
            d = d.plusDays(1);
        }
        return Math.max(0, days);
    }

    private static boolean isTradingDay(LocalDate d) {
        return !isWeekend(d) && !isHoliday(d);
    }
    private static boolean isWeekend(LocalDate d) {
        var w = d.getDayOfWeek();
        return w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY;
    }
    private static LocalDate prevTradingDay(LocalDate d) {
        while (!isTradingDay(d)) d = d.minusDays(1);
        return d;
    }
    private static LocalDate nextTradingDay(LocalDate d) {
        while (!isTradingDay(d)) d = d.plusDays(1);
        return d;
    }
    private static boolean isHoliday(LocalDate d) {
        // fixed-date holidays
        final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
                MonthDay.of(1, 1),   // New Year's Day
                MonthDay.of(1, 9),   // Day of Mourning for Jimmy Carter (2025)
                MonthDay.of(6, 19),  // Juneteenth
                MonthDay.of(7, 4),   // Independence Day
                MonthDay.of(12, 25)  // Christmas
        );
        for (MonthDay md : FIXED_HOLIDAYS) {
            LocalDate actual = md.atYear(d.getYear());
            LocalDate observed = switch (actual.getDayOfWeek()) {
                case SATURDAY -> actual.minusDays(1);
                case SUNDAY -> actual.plusDays(1);
                default -> actual;
            };
            if (d.equals(observed)) return true;
        }

        // moving holidays
        int y = d.getYear();
        if (d.equals(nthWeekdayOfMonth(y, Month.JANUARY, DayOfWeek.MONDAY, 3))) return true;   // MLK Day
        if (d.equals(nthWeekdayOfMonth(y, Month.FEBRUARY, DayOfWeek.MONDAY, 3))) return true;  // Presidents’ Day
        if (d.equals(lastWeekdayOfMonth(y, Month.MAY, DayOfWeek.MONDAY))) return true;         // Memorial Day
        if (d.equals(nthWeekdayOfMonth(y, Month.SEPTEMBER, DayOfWeek.MONDAY, 1))) return true; // Labor Day
        if (d.equals(nthWeekdayOfMonth(y, Month.NOVEMBER, DayOfWeek.THURSDAY, 4))) return true;// Thanksgiving

        // Good Friday
        if (d.equals(easterSunday(y).minusDays(2))) return true;

        return false;
    }
    private static LocalDate nthWeekdayOfMonth(int y, Month m, DayOfWeek dow, int n) {
        LocalDate d = LocalDate.of(y, m, 1);
        int shift = (dow.getValue() - d.getDayOfWeek().getValue() + 7) % 7;
        return d.plusDays(shift + (n - 1) * 7L);
    }
    private static LocalDate lastWeekdayOfMonth(int y, Month m, DayOfWeek dow) {
        LocalDate d = LocalDate.of(y, m, m.length(Year.isLeap(y)));
        int shiftBack = (d.getDayOfWeek().getValue() - dow.getValue() + 7) % 7;
        return d.minusDays(shiftBack);
    }
    private static LocalDate easterSunday(int y) {
        int a = y % 19;
        int b = y / 100, c = y % 100;
        int d = b / 4, e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(y, month, day);
    }
}
