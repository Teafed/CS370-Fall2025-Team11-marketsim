package com.etl;

import com.market.Database;
import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;

class FinnhubClientParserTest {

    @Test
    void parsesTradeMessageIntoDB() throws Exception {
        Database db = new Database(":memory:");

        String sample = """
        {"type":"trade","data":[
          {"p": 185.12, "s": "AAPL", "t": 1714060800123, "v": 100},
          {"p": 185.15, "s": "AAPL", "t": 1714060800456, "v": 50}
        ]}
        """;

//        FinnhubWebSocketClient.parseAndStore(sample, db);

        try (ResultSet rs = db.getCandles("AAPL", 0, Long.MAX_VALUE)) {
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

        db.close();
    }

    // @Disabled("Enable when you have FINNHUB_API_KEY set")
    @Test
    void liveFinnhubSmokeTest() throws Exception {
        Database db = new Database("data/market.db"); // or ":memory:"
        //TradeSource client = WebSocketClient.start();

        // give it ~5–10 seconds to receive something
        Thread.sleep(10_000);

        long ts = db.getLatestTimestamp("AAPL");
        assertTrue(ts > 0, "expected at least one trade");

        //client.stop();
        db.close();
    }
}
