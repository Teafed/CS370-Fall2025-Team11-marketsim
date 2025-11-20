package com.etl.finnhub;

import com.etl.TradeSource;
import com.models.market.TradeListener;
import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import com.google.gson.*;
import com.models.Database;

/**
 * WebSocket client for connecting to Finnhub's real-time trade stream.
 */
@ClientEndpoint
public class WebSocketClient implements TradeSource {
    private Session session;
    private final CountDownLatch received = new CountDownLatch(1); // or N
    private TradeListener tradeListener;
    private String apiKey;

    /**
     * Constructs a new WebSocketClient.
     *
     * @param apiKey The Finnhub API key.
     */
    public WebSocketClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Called when the WebSocket connection is opened.
     *
     * @param s The WebSocket session.
     * @throws IOException If an I/O error occurs.
     */
    @OnOpen
    public void onOpen(Session s) throws IOException {
        this.session = s;
    }

    /**
     * Called when a message is received from the WebSocket.
     *
     * @param msg The message received.
     */
    @OnMessage
    public void onMessage(String msg) {
        parseAndNotify(msg);
        received.countDown(); // signals “we got something”
    }

    /**
     * Called when the WebSocket connection is closed.
     *
     * @param session The WebSocket session.
     * @param reason  The reason for closure.
     */
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[Finnhub] Connection closed: " + reason);
    }

    /**
     * Called when an error occurs.
     *
     * @param session The WebSocket session.
     * @param t       The throwable error.
     */
    @OnError
    public void onError(Session session, Throwable t) {
        System.err.println("[Finnhub] Error: " + t.getMessage());
        t.printStackTrace();
    }

    /**
     * Starts a new WebSocket connection.
     *
     * @param apiKey The Finnhub API key.
     * @return A connected TradeSource instance.
     * @throws Exception If an error occurs during connection.
     */
    public static TradeSource start(String apiKey) throws Exception {
        WebSocketClient client = new WebSocketClient(apiKey);
        WebSocketContainer c = ContainerProvider.getWebSocketContainer();
        URI uri = new URI("wss", "ws.finnhub.io", "/", "token=" + apiKey, null);
        c.connectToServer(client, uri);
        return client;
    }

    /**
     * Stops the WebSocket connection.
     */
    public void stop() {
        try {
            if (session != null && session.isOpen())
                session.close();
        } catch (Exception ignore) {
        }
    }

    // helper functions

    /**
     * Waits until the first message is received.
     *
     * @param timeout The maximum time to wait.
     * @return True if a message was received, false if the timeout elapsed.
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public boolean awaitFirstMessage(java.time.Duration timeout) throws InterruptedException {
        return received.await(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Parses a Finnhub message and stores it in the database (currently disabled).
     *
     * @param msg The message string.
     * @param db  The database instance.
     */
    static void parseAndStore(String msg, Database db) {
        JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
        if (!obj.has("data"))
            return;

        for (JsonElement el : obj.getAsJsonArray("data")) {
            JsonObject trade = el.getAsJsonObject();
            double price = trade.get("p").getAsDouble();
            long timestamp = trade.get("t").getAsLong();
            long volume = trade.get("v").getAsLong();
            String s = trade.get("s").getAsString();

            try {
                // Insert as a daily candle (compatibility overload) so tests can query by
                // timestamp
                // db.insertCandle(s, timestamp, price, price, price, price, volume);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Subscribes to real-time updates for a specific stock symbol.
     *
     * @param symbol The symbol of the stock to subscribe to.
     * @throws Exception If an error occurs during subscription.
     */
    public void subscribe(String symbol) throws Exception {
        if (session != null && session.isOpen()) {
            String msg = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
            session.getAsyncRemote().sendText(msg);
        }
    }

    /**
     * Sets the listener to receive trade updates.
     *
     * @param tradeListener The TradeListener.
     */
    public void setTradeListener(TradeListener tradeListener) {
        this.tradeListener = tradeListener;
    }

    /**
     * Parses a message and notifies the listener of trade events.
     *
     * @param msg The message string.
     */
    public void parseAndNotify(String msg) {
        JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
        if (!obj.has("data"))
            return;

        for (JsonElement el : obj.getAsJsonArray("data")) {
            // System.out.println("JSON: ");
            JsonObject trade = el.getAsJsonObject();
            double price = trade.get("p").getAsDouble();
            String symbol = trade.get("s").getAsString();
            tradeListener.onTrade(symbol, price);
        }
    }
}