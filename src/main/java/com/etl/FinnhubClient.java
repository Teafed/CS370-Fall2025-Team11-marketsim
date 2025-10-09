package com.etl;

import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;
import com.market.DatabaseManager;

@ClientEndpoint
public class FinnhubClient {
    private final DatabaseManager db;
    private final String symbol;
    private Session session;
    private final CountDownLatch received = new CountDownLatch(1); // or N

    public FinnhubClient(DatabaseManager db, String symbol) {
        this.db = db;
        this.symbol = symbol;
    }

    @OnOpen
    public void onOpen(Session s) throws IOException {
        this.session = s;
        // subscribe to trades for this.symbol (double-check Finnhub’s expected payload)
        String msg = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
        s.getBasicRemote().sendText(msg);
    }

    @OnMessage
    public void onMessage(String msg) {
        parseAndStore(msg, db);
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

    public static FinnhubClient start(DatabaseManager db, String symbol) throws Exception {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = System.getenv("FINNHUB_API_KEY");
        if (k == null || k.isBlank()) k = dotenv.get("FINNHUB_API_KEY");

        FinnhubClient client = new FinnhubClient(db, symbol);
        WebSocketContainer c = ContainerProvider.getWebSocketContainer();
        URI uri = new URI("wss", "ws.finnhub.io", "/", "token=" + k, null);
        c.connectToServer(client, uri);
        return client;
    }

    public void stop() {
        try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignore) {}
    }

    // helper functions

    /**
     * Finnhub message parsing logic
     * @param msg
     * @param db
     */
    static void parseAndStore(String msg, DatabaseManager db) {
        JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
        if (!obj.has("data")) return;

        for (JsonElement el : obj.getAsJsonArray("data")) {
            JsonObject trade = el.getAsJsonObject();
            double price = trade.get("p").getAsDouble();
            long timestamp = trade.get("t").getAsLong();
            long volume = trade.get("v").getAsLong();
            String s = trade.get("s").getAsString();
            try {
                db.insertPrice(s, timestamp, price, price, price, price, volume);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}