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
            System.out.println("Usage: PolygonRestBackfill <dbFile> <TICKER> <from:YYYY-MM-DD> <to:YYYY-MM-DD> [span:minute|hour|day]");
            System.exit(1);
        }
        String dbFile = args[0];
        String symbol = args[1];
        LocalDate from = LocalDate.parse(args[2]);
        LocalDate to   = LocalDate.parse(args[3]);
        String spanArg = (args.length >= 5) ? args[4].toLowerCase() : "day";
        int interval   = (args.length >= 6) ? Integer.parseInt(args[5]) : 1;

        HistoricalService.Span span = switch (spanArg) {
            case "minute" -> HistoricalService.Span.MINUTE;
            case "hour"   -> HistoricalService.Span.HOUR;
            default       -> HistoricalService.Span.DAY;
        };

        try (com.market.DatabaseManager db = new com.market.DatabaseManager(dbFile)) {
            HistoricalService svc = new HistoricalService(db);
            svc.backfillRange(symbol, from, to, span, interval);
        }
        System.out.println("Backfill complete for " + symbol + " " + from + " → " + to);
    }
}

