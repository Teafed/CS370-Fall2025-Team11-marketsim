package com.market;

import com.etl.FinnhubClient;
import java.sql.*;

public class FinnhubTest {
    public static void main(String[] args) {
        DatabaseManager db;
        try {
            db = new DatabaseManager("data/market.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        String symbol = "AAPL";

        try {
            FinnhubClient.start(db, symbol);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("Listening for real-time trades for " + symbol + "...");
        System.out.println("Waiting for messages...");

        Thread dbChecker = getThread(db, symbol);
        dbChecker.start();

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Thread getThread(DatabaseManager db, String symbol) {
        Thread dbChecker = new Thread(() -> {
            while (true) {
                try {
                    ResultSet rs = db.getPrices(symbol, System.currentTimeMillis() - 3600_000, System.currentTimeMillis());
                    int count = 0;
                    while (rs.next()) count++;

                    if (count > 0) {
                        System.out.println("[DB Check] " + count + " trades recorded in the last hour");
                    } else {
                        System.out.println("[DB Check] No trades recorded yet...");
                    }

                    Thread.sleep(5000); // check every 5 seconds
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        dbChecker.setDaemon(true); // doesn't block JVM exit
        return dbChecker;
    }
}