package com.market;


import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    @Test
    void insertsAndReadsBack() throws Exception {
        DatabaseManager db = new DatabaseManager(":memory:");

        db.insertPrice("AAPL", 1000L, 10, 11, 9, 10.5, 123);
        db.insertPrice("AAPL", 2000L, 10.6, 12, 10.2, 11.9, 456);

        assertEquals(2000L, db.getLatestTimestamp("AAPL"));

        try (ResultSet rs = db.getPrices("AAPL", 0L, 3000L)) {
            assertTrue(rs.next());
            assertEquals(1000L, rs.getLong("timestamp"));
            assertEquals(10.5, rs.getDouble("close"), 1e-9);

            assertTrue(rs.next());
            assertEquals(2000L, rs.getLong("timestamp"));
            assertEquals(11.9, rs.getDouble("close"), 1e-9);

            assertFalse(rs.next());
        }

        db.close();
    }

    @Test
    void uniqueConstraintPreventsDupes() throws Exception {
        DatabaseManager db = new DatabaseManager(":memory:");
        db.insertPrice("MSFT", 12345L, 1,1,1,1,1);
        // REPLACE behavior means this will overwrite rather than duplicate
        db.insertPrice("MSFT", 12345L, 2,2,2,2,2);

        try (ResultSet rs = db.getPrices("MSFT", 0L, 99999L)) {
            assertTrue(rs.next());
            assertEquals(2, rs.getDouble("open"), 1e-9); // updated row
            assertFalse(rs.next());
        }
        db.close();
    }
}
