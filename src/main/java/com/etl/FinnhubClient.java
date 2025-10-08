package com.etl;

import jakarta.websocket.*;
import java.net.URI;
import java.sql.ResultSet;

import com.google.gson.*;
import com.market.DatabaseManager;

@ClientEndpoint
public class FinnhubClient {
    private static final String API_KEY = "d2sq3t1r01qkuv3hu930d2sq3t1r01qkuv3hu93g";
    private final DatabaseManager db;
    private final String symbol;

    public FinnhubClient(DatabaseManager db, String symbol) {
        this.db = db;
        this.symbol = symbol;
    }

    @OnOpen
    public void onOpen(Session session) throws Exception {
        String subscribeMsg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
        session.getBasicRemote().sendText(subscribeMsg);
        System.out.println("[Finnhub] Subscribed to " + symbol);
    }

    @OnMessage
    public void onMessage(String msg) {
        try {
            JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
            if (!obj.has("data")) return;

            for (JsonElement el : obj.getAsJsonArray("data")) {
                JsonObject trade = el.getAsJsonObject();
                double price = trade.get("p").getAsDouble();
                long timestamp = trade.get("t").getAsLong();
                long volume = trade.get("v").getAsLong();
                String s = trade.get("s").getAsString();

                // Store trade as OHLC candle with identical values (prototype only)
                db.insertPrice(s, timestamp, price, price, price, price, volume);
                System.out.printf("[DB] Inserted %s at %d: %.2f (vol=%d)%n", s, timestamp, price, volume);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[Finnhub] Connection closed: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        System.err.println("[Finnhub] Error: " + t.getMessage());
        t.printStackTrace();
    }

    public static void start(DatabaseManager db, String symbol) throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        URI uri = new URI("wss", "ws.finnhub.io", "/", "token=" + API_KEY, null);
        container.connectToServer(new FinnhubClient(db, symbol), uri);
    }
}