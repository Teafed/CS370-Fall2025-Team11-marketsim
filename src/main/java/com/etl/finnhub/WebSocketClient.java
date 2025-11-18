package com.etl.finnhub;

import com.etl.TradeSource;
import com.models.market.TradeListener;
import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import com.google.gson.*;
import com.models.Database;

@ClientEndpoint
public class WebSocketClient implements TradeSource {
    private Session session;
    private final CountDownLatch received = new CountDownLatch(1); // or N
    private TradeListener tradeListener;
    private String apiKey;

    public WebSocketClient(String apiKey) {
        this.apiKey = apiKey;
    }

    @OnOpen
    public void onOpen(Session s) throws IOException {
        this.session = s;
    }

    @OnMessage
    public void onMessage(String msg) {
        parseAndNotify(msg);
        received.countDown(); // signals “we got something”
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

    public static TradeSource start(String apiKey) throws Exception {
        WebSocketClient client = new WebSocketClient(apiKey);
        WebSocketContainer c = ContainerProvider.getWebSocketContainer();
        URI uri = new URI("wss", "ws.finnhub.io", "/", "token=" + apiKey, null);
        c.connectToServer(client, uri);
        return client;
    }



    public void stop() {
        try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignore) {}
    }

    // helper functions

    /**
     * wait until first message
     * @param timeout
     * @return CountDownLatch received
     * @throws InterruptedException
     */
    public boolean awaitFirstMessage(java.time.Duration timeout) throws InterruptedException {
        return received.await(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Finnhub message parsing logic
     * @param msg
     * @param db
     */
    static void parseAndStore(String msg, Database db) {
        JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
        if (!obj.has("data")) return;

        for (JsonElement el : obj.getAsJsonArray("data")) {
            JsonObject trade = el.getAsJsonObject();
            double price = trade.get("p").getAsDouble();
            long timestamp = trade.get("t").getAsLong();
            long volume = trade.get("v").getAsLong();
            String s = trade.get("s").getAsString();

            try {
                // Insert as a daily candle (compatibility overload) so tests can query by timestamp
                // db.insertCandle(s, timestamp, price, price, price, price, volume);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * To subscribe to specific stocks
     * @param symbol The symbol of the stock you want to add
     */
    public void subscribe(String symbol) throws Exception {
        if (session != null && session.isOpen()) {
            String msg = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
            session.getAsyncRemote().sendText(msg);
        }
    }

    /**
     * To listen for trades to update stocks
     */
    public void setTradeListener(TradeListener tradeListener) {
        this.tradeListener = tradeListener;
    }

    public void parseAndNotify(String msg) {
        JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
        if (!obj.has("data")) return;

        for (JsonElement el : obj.getAsJsonArray("data")) {
//            System.out.println("JSON: ");
            JsonObject trade = el.getAsJsonObject();
            double price = trade.get("p").getAsDouble();
            String symbol = trade.get("s").getAsString();
            tradeListener.onTrade(symbol, price);
        }
    }
}