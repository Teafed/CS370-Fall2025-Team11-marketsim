package com.etl;

import com.market.DatabaseManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;

class FinnhubClientTest {

    @Test
    void parsesTradeMessageIntoDB() throws Exception {
        DatabaseManager db = new DatabaseManager(":memory:");

        String sample = """
        {"type":"trade","data":[
          {"p": 185.12, "s": "AAPL", "t": 1714060800123, "v": 100},
          {"p": 185.15, "s": "AAPL", "t": 1714060800456, "v": 50}
        ]}
        """;

        FinnhubClient.parseAndStore(sample, db);

        try (ResultSet rs = db.getPrices("AAPL", 0, Long.MAX_VALUE)) {
            assertTrue(rs.next());
            assertEquals(1714060800123L, rs.getLong("timestamp"));
            assertEquals(185.12, rs.getDouble("close"), 1e-9);
            assertEquals(100L, rs.getLong("volume"));

            assertTrue(rs.next());
            assertEquals(1714060800456L, rs.getLong("timestamp"));
            assertEquals(185.15, rs.getDouble("close"), 1e-9);
            assertEquals(50L, rs.getLong("volume"));

            assertFalse(rs.next());
        }
    }

    @Test
    void ignoresMessagesWithoutDataArray() throws Exception {
        try (DatabaseManager db = new DatabaseManager(":memory:")) {
            FinnhubClient.parseAndStore("{\"type\":\"ping\"}", db); // no 'data'
            try (ResultSet rs = db.getPrices("AAPL", 0, Long.MAX_VALUE)) {
                assertFalse(rs.next(), "no rows should be written for non-trade messages");
            }
        }
    }
}
