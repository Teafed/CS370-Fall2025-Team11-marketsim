package com.tools;

import com.etl.TradeSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.market.DatabaseManager;
import com.market.TradeListener;
import jakarta.websocket.OnMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockFinnhubClient implements TradeSource {
    private final DatabaseManager db;
    private TradeListener listener;
    private final List<String> subscribedSymbols = new ArrayList<>();
    private final Random rand = new Random();

    public MockFinnhubClient(DatabaseManager db){
        this.db = db;
    }

    public void setTradeListener(TradeListener listener) {
        this.listener = listener;
    }

    @OnMessage
    public void onMessage(String msg) {
        System.out.println(msg);
    }

    public void subscribe(String symbol) {
        System.out.println("[Mock] Subscribed to " + symbol);
        subscribedSymbols.add(symbol);
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                JsonArray trades = new JsonArray();
                long baseTimestamp = System.currentTimeMillis();

                for (String symbol : subscribedSymbols) {
                    double price = 100 + rand.nextDouble() * 50;
                    long timestamp = baseTimestamp + rand.nextInt(5); // slight jitter
                    long volume = rand.nextInt(500) + 50;

                    JsonObject trade = new JsonObject();
                    trade.addProperty("p", price);
                    trade.addProperty("t", timestamp);
                    trade.addProperty("v", volume);
                    trade.addProperty("s", symbol);
                    trades.add(trade);

                    // Insert into DB
//                    try {
//                        db.insertPrice(symbol, timestamp, price, price, price, price, volume);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }

                    // Notify listener
                    if (listener != null) {
                        listener.onTrade(symbol, price, timestamp, volume);
                    }
                }

                JsonObject message = new JsonObject();
                message.addProperty("type", "trade");
                message.add("data", trades);

                System.out.println("[Mock] Emitting: " + message);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {}
            }
        }).start();
    }
}


