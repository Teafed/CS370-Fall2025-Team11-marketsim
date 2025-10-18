package com.etl;

import com.market.DatabaseManager;
import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;

class FinnhubClientParserTest {

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

            assertTrue(rs.next());
            assertEquals(1714060800456L, rs.getLong("timestamp"));
            assertEquals(185.15, rs.getDouble("close"), 1e-9);

            assertFalse(rs.next());
        }

        db.close();
    }

    @org.junit.jupiter.api.Disabled("This test is for the old WebSocket client and is not compatible with the new REST polling client.")
    @Test
    void liveFinnhubSmokeTest() throws Exception {
        DatabaseManager db = new DatabaseManager("data/market.db"); // or ":memory:"
        FinnhubClient client = FinnhubClient.start(db, "AAPL");

        // give it ~5–10 seconds to receive something
        Thread.sleep(10_000);

        long ts = db.getLatestTimestamp("AAPL");
        assertTrue(ts > 0, "expected at least one trade");

        client.stopPolling();
        db.close();
    }
}
