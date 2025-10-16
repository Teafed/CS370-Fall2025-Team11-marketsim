package com;


import com.etl.FinnhubClient;
import com.market.DatabaseManager;
import com.market.Market;
import com.tools.MockFinnhubClient;

import java.sql.SQLException;
import java.util.Arrays;

public class Main {
   public static void main(String[] args) {

       // Initialize database for use
       String dbFile = "data/marketsim-sample1.db";
       boolean force = Arrays.asList(args).contains("--force");

       try (DatabaseManager db = new DatabaseManager(dbFile)) {
//           if (!force) {
//               System.out.println("[seed] Already seeded. Use --force to overwrite. DB: " + dbFile);
//               System.out.println(db.listSymbols());
//               return;
//           }

           // Initialize connection to Finnhub during market hours
           //FinnhubClient client = FinnhubClient.start(db);

           // Initialize mock finnhubclient after market hours
           MockFinnhubClient client = new MockFinnhubClient(db);
           client.start();


           System.out.println(db);
           System.out.println(client);

           // Initialize market
           System.out.println("starting market");
           Market market = new Market(db, client);
           new java.util.concurrent.CountDownLatch(1).await();


       } catch (SQLException e) {
           //TODO
           e.printStackTrace();
           throw new RuntimeException(e);
       } catch (Exception e) {
           //TODO
           e.printStackTrace();
           throw new RuntimeException(e);
       }
       System.out.println("[seed] Done: " + dbFile);


   }
}
