package com.etl;

import com.models.Database;
import com.etl.finnhub.WebSocketClient;
import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;

class FinnhubClientParserTest {

    @Test
    void parsesTradeMessageIntoDB() throws Exception {
        try (Database db = new Database(":memory:")) {

            String sample = """
            {"type":"trade","data":[
              {"p": 185.12, "s": "AAPL", "t": 1714060800123, "v": 100},
              {"p": 185.15, "s": "AAPL", "t": 1714060800456, "v": 50}
            ]}
            """;

            // parse sample and store into the in-memory DB for assertions
            WebSocketClient.parseAndStore(sample, db);

            try (ResultSet rs = db.getCandles("AAPL", 1, "day", 0, Long.MAX_VALUE)) {
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
    }

}
