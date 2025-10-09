package com.etl;

import com.google.gson.*;
import com.market.DatabaseManager;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

public class HistoricalService {
    private final DatabaseManager db;
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey;

    public HistoricalService(DatabaseManager db) {
        this.db = db;
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = System.getenv("POLYGON_API_KEY");
        this.apiKey = (k == null || k.isBlank()) ? dotenv.get("POLYGON_API_KEY") : k;
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("Set POLYGON_API_KEY");
    }

    /**
     * ensure at least `daysBack` of 1m bars exist for symbol; backfill missing via REST.
     * @param symbol
     * @param daysBack
     * @throws Exception
     */
    public void ensureSeedData(String symbol, int daysBack) throws Exception {
        long now = System.currentTimeMillis();
        long start = now - Duration.ofDays(daysBack).toMillis();

        long haveUntil = db.getLatestTimestamp(symbol); // 0 if none
        long needFrom = Math.min(haveUntil == 0 ? now : haveUntil + 60_000, now);

        if (haveUntil >= start) return; // already have enough

        // backfill in chunks (Polygon returns up to 50k points per call)
        LocalDate cursor = Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate end = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate();

        while (!cursor.isAfter(end)) {
            LocalDate chunkEnd = cursor.plusDays(14); // ~2 weeks per request is plenty for 1m bars
            if (chunkEnd.isAfter(end)) chunkEnd = end;

            String url = String.format(
                    "https://api.polygon.io/v2/aggs/ticker/%s/range/1/day/%s/%s?adjusted=true&sort=asc&limit=50000&apiKey=%s",
                    symbol, cursor, chunkEnd, apiKey);

            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) throw new RuntimeException("Polygon: " + resp.statusCode() + " " + resp.body());

            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (!"OK".equals(root.get("status").getAsString())) {
                cursor = chunkEnd.plusDays(1); // advance to avoid loop if no results
                continue;
            }

            var rows = new ArrayList<com.market.DatabaseManager.PriceRow>();
            if (root.has("results")) {
                for (JsonElement e : root.getAsJsonArray("results")) {
                    JsonObject r = e.getAsJsonObject();
                    long t = r.get("t").getAsLong();
                    double o = r.get("o").getAsDouble();
                    double h = r.get("h").getAsDouble();
                    double l = r.get("l").getAsDouble();
                    double c = r.get("c").getAsDouble();
                    long v = r.get("v").getAsLong();
                    rows.add(new com.market.DatabaseManager.PriceRow(symbol, t, o, h, l, c, v));
                }
            }
            if (!rows.isEmpty()) db.insertPricesBatch(rows);
            cursor = chunkEnd.plusDays(1);
        }
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
}
