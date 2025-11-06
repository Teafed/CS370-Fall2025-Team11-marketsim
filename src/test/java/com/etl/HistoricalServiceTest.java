package com.etl;

import com.market.Database;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistoricalServiceTest {

    @Test
    void ensureRange_insertsDailyBars() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResp = (HttpResponse<String>) mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(200);
        // Build a tiny Polygon-like payload with two “daily candles”
        JsonObject root = new JsonObject();
        root.addProperty("status", "OK");
        JsonArray results = new JsonArray();

        long d1 = Instant.parse("2025-09-01T00:00:00Z").toEpochMilli();
        long d2 = Instant.parse("2025-09-02T00:00:00Z").toEpochMilli();

        results.add(candle(d1, 100, 110, 90, 105, 1_000_000));
        results.add(candle(d2, 106, 115, 101, 112, 1_200_000));
        root.add("results", results);

        when(mockResp.body()).thenReturn(root.toString());
        when(mockHttp.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        try (Database db = new Database(":memory:")) {
            HistoricalService svc = new HistoricalService(db, mockHttp, "test-key", "https://fake");

            HistoricalService.Range range = new HistoricalService.Range(
                    HistoricalService.Timespan.DAY, 1,
                    LocalDate.parse("2025-09-01"),
                    LocalDate.parse("2025-09-10")
            );

            svc.backfillRange("AAPL", range);

            long latest = db.getLatestTimestamp("AAPL");
            assertEquals(d2, latest);

            try (var rs = db.getCandles("AAPL", 0, Long.MAX_VALUE)) {
                assertTrue(rs.next());
                assertEquals(d1, rs.getLong("timestamp"));
                assertEquals(105.0, rs.getDouble("close"), 1e-9);
                assertTrue(rs.next());
                assertEquals(d2, rs.getLong("timestamp"));
                assertEquals(112.0, rs.getDouble("close"), 1e-9);
                assertFalse(rs.next());
            }
        }
    }

    @Test
    void ensureRange_throwsOnNon200() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResp = (HttpResponse<String>) mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(403);
        when(mockResp.body()).thenReturn("{\"status\":\"NOT_AUTHORIZED\"}");
        when(mockHttp.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(mockResp);

        try (Database db = new Database(":memory:")) {
            HistoricalService svc =
                    new HistoricalService(db, mockHttp, "test-key", "https://fake");

            HistoricalService.Range range = new HistoricalService.Range(
                    HistoricalService.Timespan.DAY, 1,
                    LocalDate.parse("2025-09-01"),
                    LocalDate.parse("2025-09-02")
            );

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> svc.backfillRange("AAPL", range));

            assertTrue(ex.getMessage().contains("[HistoricalService] 403"));
        }
    }

    private static JsonObject candle(long t, double o, double h, double l, double c, long v) {
        var obj = new JsonObject();
        obj.addProperty("t", t);
        obj.addProperty("o", o);
        obj.addProperty("h", h);
        obj.addProperty("l", l);
        obj.addProperty("c", c);
        obj.addProperty("v", v);
        return obj;
    }
}
