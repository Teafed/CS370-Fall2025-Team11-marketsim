package com.tools;

import com.etl.TradeSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.market.DatabaseManager;
import com.market.TradeListener;
import jakarta.websocket.OnMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MockFinnhubClient implements TradeSource { ;
    private TradeListener listener;
    private static final List<String> subscribedSymbols = new ArrayList<>();
    private static final Random rand = new Random();

    public MockFinnhubClient(){
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            while (true) {
                JsonArray trades = new JsonArray();
                long baseTimestamp = System.currentTimeMillis();

                List<String> symbols = returnRandomSymbolList();
                for (String symbol : symbols) {
                    double price = 100 + rand.nextDouble() * 50;
                    long timestamp = baseTimestamp + rand.nextInt(5); // slight jitter
                    long volume = rand.nextInt(500) + 50;

                    JsonObject trade = new JsonObject();
                    trade.addProperty("p", price);
                    trade.addProperty("t", timestamp);
                    trade.addProperty("v", volume);
                    trade.addProperty("s", symbol);
                    trades.add(trade);

                    // Notify listener
                    if (listener != null) {
                        listener.onTrade(symbol, price);
                    }
                }

                JsonObject message = new JsonObject();
                message.addProperty("type", "trade");
                message.add("data", trades);

//                System.out.println("[Mock] Emitting: " + message);

                try {
                    Thread.sleep(900);
                } catch (InterruptedException ignore) {}
            }
        }, "MockFinnhub-Emitter").start();
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
        if (!subscribedSymbols.contains(symbol)) {
            subscribedSymbols.add(symbol);
        }
    }

    public static List<String> returnRandomSymbolList() {
        if (subscribedSymbols.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> copy = new ArrayList<>(subscribedSymbols);
        Collections.shuffle(copy, rand);

        int max = Math.min(copy.size(), 8);
        int count = 1 + rand.nextInt(max); // 1..max
        return copy.subList(0, count);
    }

    public static TradeSource start() {
        return new MockFinnhubClient();
    }
}


