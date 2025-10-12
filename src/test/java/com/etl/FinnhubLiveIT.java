package com.etl;

import com.market.DatabaseManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FinnhubLiveIT {

    @Test
    @DisabledIfEnvironmentVariable(named = "FINNHUB_API_KEY", matches = "^$")
    @Timeout(30)
    void liveFinnhubSmokeTest() throws Exception {
        try (DatabaseManager db = new DatabaseManager(":memory:")) {
            FinnhubClient client = FinnhubClient.start(db, "AAPL");
            try {
                // Wait deterministically for the first message using your new helper
                boolean gotOne = client.awaitFirstMessage(java.time.Duration.ofSeconds(20));
                assertTrue(gotOne, "did not receive any message from Finnhub in time");

                long ts = db.getLatestTimestamp("AAPL");
                assertTrue(ts > 0, "expected at least one trade written to DB");
            } finally {
                client.stop();
            }
        }
    }
}
