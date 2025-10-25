package com;


import com.etl.*;
import com.market.*;
import com.gui.*;
import com.accountmanager.*;
import com.tools.MockFinnhubClient;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        String dbFile = "data/marketsim-sample.db";

        // Check if market is open or closed
        System.out.println("Checking market status...");
        boolean marketHours = FinnhubMarketStatus.checkStatus();
        TradeSource client;
        if (marketHours) {
            try {
                System.out.println("Market open, starting Finnhub...");
                client = FinnhubWebSocketClient.start();
                System.out.println("Finnhub started...");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Market closed, starting Mock Client...");
            client = new MockFinnhubClient().start();
            System.out.println("Mock client started...");
        }

        // Initialize market
        Market market = new Market();
        market.setClient(client);
        while (!market.isReady()) {
            System.out.println("Waiting for Market status...");
        }
        System.out.println("Market started...");

        // Initialize Database
        DatabaseManager db;
        try {
            db = new DatabaseManager(dbFile);
        } catch (Exception e) {
            throw new SQLException("Failed to open database: " + dbFile, e);
        }
        market.setDatabase(db);

        // Initialize account (demo for now)
        Account account = com.tools.BuildDemoAccount.buildDemoAccount();

        // Initialize GUI Client
        SwingUtilities.invokeLater(() -> {
            MainWindow mw = new MainWindow(db, account, market);
            market.setMarketListener(mw.getSymbolListPanel());
        });
    }
}
