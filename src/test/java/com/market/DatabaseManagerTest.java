package com.market;

import com.etl.FinnhubClient;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseTest {

    @Test
    void testDatabase() {
        DatabaseManager db;
        Thread thread;
        // initialize database
        try {
            db = new DatabaseManager("data/market.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        // startup FinnhubClient
        try {
            FinnhubClient.start(db, "AAPL");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        
    }
}
