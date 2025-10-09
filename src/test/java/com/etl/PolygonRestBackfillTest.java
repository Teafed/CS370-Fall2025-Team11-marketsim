package com.etl;

import com.market.DatabaseManager;

public class PolygonRestBackfillTest {
    public static void main(String[] args) throws Exception {
        String dbFile = "market.db";
        String symbol = "AAPL";
        String from = "2025-09-01";
        String to   = "2025-10-03";

        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            HistoricalService svc = new HistoricalService(db);
            svc.ensureSeedData(symbol, 30); // or call the REST once via the date range if you prefer
            var rows = new ReadData("fkdlsjds", db).loadSeries(symbol, 0, Long.MAX_VALUE);
            System.out.println("Inserted/loaded rows: " + rows.size());
        }
    }
}
