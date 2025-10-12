package com.etl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class PolygonRestBackfillIT {

    @Test
    @DisabledIfEnvironmentVariable(named = "POLYGON_API_KEY", matches = "^$")
    @Timeout(90)
    void runsCliAndWritesRows() throws Exception {
        Path tmp = Files.createTempFile("market", ".db");
        String dbFile = tmp.toString();

        // Run the CLI for a tiny daily window to avoid plan issues
        String[] args = { dbFile, "AAPL", "2025-09-01", "2025-09-03" };
        PolygonRestBackfill.main(args); // uses HistoricalService under the hood now

        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM prices WHERE symbol='AAPL'");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) > 0, "expected at least one bar from the CLI backfill");
        }
    }
}
