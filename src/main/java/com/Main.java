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

        // Initialize Database
        DatabaseManager db;
        try {
            db = new DatabaseManager(dbFile);
        } catch (Exception e) {
            throw new SQLException("Failed to open database: " + dbFile, e);
        }

        // Initialize account (demo for now)
        Account account = com.tools.BuildDemoAccount.buildDemoAccount();

        long profileId = db.getOrCreateProfile("Test Profile");
        long accountId = db.getOrCreateAccount(profileId, account.getName(), "USD");
        java.util.List<String> dbSymbols = db.loadWatchlistSymbols(accountId);

        if (dbSymbols.isEmpty()) {
            // add in-memory demo watchlist to database
            java.util.List<String> current = new java.util.ArrayList<>();
            for (com.market.TradeItem ti : account.getWatchList().getWatchlist()) {
                current.add(ti.getSymbol());
            }
            db.saveWatchlistSymbols(accountId, "Default", current);
            System.out.println("[startup] Seeded DB watchlist from demo account (" + current.size() + " symbols)");
        } else {
            // use watchlist from database
            account.getWatchList().clearList();
            for (String sym : dbSymbols) {
                // TODO: get the actual name of the symbol
                account.getWatchList().addWatchlistItem(new com.market.TradeItem(sym, sym));
            }
            System.out.println("[startup] Loaded watchlist from DB (" + dbSymbols.size() + " symbols)");
        }


        // Check if market is open or closed
        System.out.println("Checking market status...");
        boolean marketHours = FinnhubMarketStatus.checkStatus();
        TradeSource client;
        if (marketHours) {
            try {
                System.out.println("Market open, starting Finnhub...");
                client = FinnhubClient.startWebSocket();
                System.out.println("Finnhub started...");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Market closed, starting Mock Client...");
            client = MockFinnhubClient.start();
            System.out.println("Mock client started...");
        }

        // Initialize market
        Market market = new Market(client, db, account);
        market.addFromWatchlist(account.getWatchList());
        while (!market.isReady()) {
            System.out.println("Waiting for Market status...");
        }
        System.out.println("Market started...");

        // Initialize GUI Client
        SwingUtilities.invokeLater(() -> {
            MainWindow mw = new MainWindow(db, account, market);
            market.setMarketListener(mw.getSymbolListPanel());
        });
    }
}
