// src/test/java/com/tools/SampleDbSmokeIT.java
package com.tools;

import com.market.DatabaseManager;
import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;

class SampleDbTest {

    @Test
    void sampleDbHasDataForSymbols() throws Exception {
        String dbFile = "data/marketsim-sample.db";

        // If missing, build it (no network)
        BuildSampleDb.main(new String[0]);

        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            var syms = db.listSymbols();
            assertTrue(syms.contains("AAPL") && syms.contains("MSFT") && syms.contains("SPY"),
                    "Expected seeded symbols present");

            for (String s : syms) {
                assertTrue(db.getLatestTimestamp(s) > 0, "Latest timestamp missing for " + s);

                int count = 0;
                try (ResultSet rs = db.getPrices(s, 0, System.currentTimeMillis())) {
                    while (rs.next()) count++;
                }
                assertTrue(count >= 60, s + " should have at least ~60 rows");
            }
        }
    }
}
