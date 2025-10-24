package com;


import com.etl.*;
import com.market.*;
import com.gui.*;
import com.tools.MockFinnhubClient;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {

        // Initialize database for use
        String dbFile = "data/marketsim-sample1.db";
        boolean force = Arrays.asList(args).contains("--force");
        TradeSource client;
        Market market;

        // Check if market is open or closed
        boolean marketHours;
        System.out.println("Checking market status...");
        marketHours = FinnhubMarketStatus.checkStatus();

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
        market = new Market();
        market.setClient(client);
        while (!market.isReady()) {
            System.out.println("Waiting for Market status...");
        }
        System.out.println("Market started...");

        // Initialize Database
        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            market.setDataBase(db);
        } catch (SQLException e) {
            //TODO
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Exception e) {
            //TODO
            e.printStackTrace();
            throw new RuntimeException(e);
        }


        // Initialize GUI Client
        SwingUtilities.invokeLater(() -> {
            MainWindow mw = new MainWindow();
            market.setMarketListener(mw.getSymbolListPanel());
        });
    }
}
