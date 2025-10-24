package com.etl;

import com.market.DatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.sql.ResultSet;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PolygonRestBackfillIT {

    @Test
    @DisabledIfEnvironmentVariable(named = "POLYGON_API_KEY", matches = "^$")
    @Timeout(90)
    void backfillsAndPersistsRows() throws Exception {
        String dbFile = "data/marketsim.db";
        String symbol = "AAPL";
        LocalDate from = LocalDate.now().minusDays(5); // small window
        LocalDate to   = LocalDate.now().minusDays(1);

        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            HistoricalService svc = new HistoricalService(db);
            svc.backfillRange(symbol, from, to, HistoricalService.Span.DAY, 1);
        }

        // Verify by reading the same DB
        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            long latest = db.getLatestTimestamp(symbol);
            assertTrue(latest > 0, "Expected at least one candle written");

            long startMs = from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
            long endMs   = to.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() - 1;

            int count = 0;
            try (ResultSet rs = db.getCandles(symbol, startMs, endMs)) {
                while (rs.next()) count++;
            }
            assertTrue(count > 0, "Expected candles in requested window, got 0");
        }
    }
}
