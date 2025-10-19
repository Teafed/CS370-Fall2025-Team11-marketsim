package com.etl;

import com.market.DatabaseManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinnhubClientParserTest {

    @Test
    void testClientCreation() throws Exception {
        DatabaseManager db = new DatabaseManager(":memory:");

        // Test that client can be created
        double[] priceUpdate = {0.0};
        FinnhubClient client = new FinnhubClient("AAPL", p -> priceUpdate[0] = p);

        assertNotNull(client);

        db.close();
    }

    @org.junit.jupiter.api.Disabled("This test requires a valid FINNHUB_API_KEY environment variable and network access.")
    @Test
    void liveFinnhubSmokeTest() throws Exception {
        DatabaseManager db = new DatabaseManager(":memory:");
        double[] priceUpdate = {0.0};

        FinnhubClient client = FinnhubClient.start(db, "AAPL", p -> priceUpdate[0] = p);

        // give it ~10 seconds to poll and receive something
        Thread.sleep(10_000);

        long ts = db.getLatestTimestamp("AAPL");
        assertTrue(ts > 0, "expected at least one quote");

        client.stopPolling();
        db.close();
    }
}
