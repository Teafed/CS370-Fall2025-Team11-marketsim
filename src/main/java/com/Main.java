package com;


import com.etl.*;
import com.market.*;
import com.gui.*;
import com.tools.MockFinnhubClient;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Arrays;

public class Main {
   public static void main(String[] args) {

           // Initialize database for use
           String dbFile = "data/marketsim-sample1.db";
           boolean force = Arrays.asList(args).contains("--force");
           TradeSource client;
           Market market;


           try (DatabaseManager db = new DatabaseManager(dbFile)) {
    //           if (!force) {
    //               System.out.println("[seed] Already seeded. Use --force to overwrite. DB: " + dbFile);
    //               System.out.println(db.listSymbols());
    //               return;
    //           }

               // Initialize connection to Finnhub during market hours
               //client = FinnhubClient.start(db);


               // Initialize mock finnhubclient after market hours
               client = new MockFinnhubClient(db);
               System.out.println("Mock client initialized...");


               // Initialize market
               market = new Market(db);
               System.out.println("Market started...");



           }
           catch (SQLException e) {
               //TODO
               e.printStackTrace();
               throw new RuntimeException(e);
           }
           catch (Exception e) {
               //TODO
               e.printStackTrace();
               throw new RuntimeException(e);
           }

           new Thread(() -> {
               try {
                   // wait for GUI to start up
                   // probably refactor later with a signal
                   Thread.sleep(50);
               } catch (InterruptedException e) {
                   throw new RuntimeException(e);
               }
               // startup client
               // works for mock client now, need to fix for Finnhub
               client.start();
               try {
                   market.setClient(client);
               } catch (Exception e) {
                   throw new RuntimeException(e);
               }
               System.out.println("Client started...");
           }).start();





           // Initialize GUI Client
       SwingUtilities.invokeLater(() -> {
           try {
               Thread.sleep(1000);            /// ADJUST THIS TO BE LARGER IF IT LOADS BEFORE THE
                                                    ///CLIENT FINISHES INITIALIZING AND NOT ALL 10 STOCKS SHOW
           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }
           MainWindow mw = new MainWindow();

        market.setMarketListener(mw.getSymbolListPanel());
        });
    ;}
}
