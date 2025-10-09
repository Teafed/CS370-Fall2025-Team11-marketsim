// src/main/java/com/etl/PolygonRestBackfill.java
package com.etl;

import com.google.gson.*;
import com.market.DatabaseManager;
import io.github.cdimascio.dotenv.Dotenv;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;

public class PolygonRestBackfill {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: PolygonRestBackfill <dbFile> <TICKER> <from:YYYY-MM-DD> <to:YYYY-MM-DD>");
            System.exit(1);
        }
        String dbFile = args[0];           // e.g. "market.db" or ":memory:"
        String ticker = args[1];           // e.g. "AAPL"
        LocalDate from = LocalDate.parse(args[2]);
        LocalDate to   = LocalDate.parse(args[3]);

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String apiKey = getenvOr(dotenv, "POLYGON_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set POLYGON_API_KEY");
        }

        String url = String.format(
                "https://api.polygon.io/v2/aggs/ticker/%s/range/1/min/%s/%s?adjusted=true&sort=asc&limit=50000&apiKey=%s",
                ticker, from, to, apiKey);

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) throw new RuntimeException("Polygon error: " + resp.statusCode() + " " + resp.body());

        JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (!"OK".equals(root.get("status").getAsString())) throw new RuntimeException("Unexpected status: " + root);

        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            if (root.has("results")) {
                for (JsonElement e : root.getAsJsonArray("results")) {
                    JsonObject r = e.getAsJsonObject();
                    long t   = r.get("t").getAsLong();      // epoch millis
                    double o = r.get("o").getAsDouble();
                    double h = r.get("h").getAsDouble();
                    double l = r.get("l").getAsDouble();
                    double c = r.get("c").getAsDouble();
                    long v   = r.get("v").getAsLong();
                    db.insertPrice(ticker, t, o, h, l, c, v);
                }
            }
        }
        System.out.println("Backfill complete for " + ticker + " " + from + " → " + to);
    }

    private static String getenvOr(Dotenv dotenv, String key) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? dotenv.get(key) : v;
    }
}
