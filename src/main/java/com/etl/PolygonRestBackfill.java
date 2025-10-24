// src/main/java/com/etl/PolygonRestBackfill.java
package com.etl;

import java.time.LocalDate;

public class PolygonRestBackfill {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: PolygonRestBackfill <dbFile> <TICKER> <from:YYYY-MM-DD> <to:YYYY-MM-DD> [timespan:minute|hour|day]");
            System.exit(1);
        }
        String dbFile = args[0];
        String symbol = args[1];
        LocalDate from = LocalDate.parse(args[2]);
        LocalDate to   = LocalDate.parse(args[3]);
        String spanArg = (args.length >= 5) ? args[4].toLowerCase() : "day";
        int multiplier   = (args.length >= 6) ? Integer.parseInt(args[5]) : 1;

        HistoricalService.Timespan timespan = switch (spanArg) {
            case "minute" -> HistoricalService.Timespan.MINUTE;
            case "hour"   -> HistoricalService.Timespan.HOUR;
            default       -> HistoricalService.Timespan.DAY;
        };

        try (com.market.DatabaseManager db = new com.market.DatabaseManager(dbFile)) {
            HistoricalService svc = new HistoricalService(db);
            HistoricalService.Range range = new HistoricalService.Range(
                    timespan, multiplier, from, to);
            svc.backfillRange(symbol, range);
        }
        System.out.println("Backfill complete for " + symbol + " " + from + " → " + to);
    }
}

