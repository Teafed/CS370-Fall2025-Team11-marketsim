// com.api.MarketDataWriter
package com.api;
import java.util.List;

public interface MarketDataWriter {
    record PriceRow(String symbol, long tsMs, double open, double high, double low, double close, long volume) {}

    // DatabaseManager.insertPricesBatch() and .insertPrice() will use this
    void upsertBars(List<PriceRow> rows) throws Exception;
    void upsertTrade(String symbol, long tsMs, double price, long volume) throws Exception;
}
