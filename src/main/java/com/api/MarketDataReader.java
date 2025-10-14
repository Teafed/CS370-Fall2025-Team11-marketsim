// com.api.MarketDataReader
package com.api;

import java.util.List;

public interface MarketDataReader {
    List<String> listSymbols() throws Exception;

    // [close_last, close_prev]
    double[] latestAndPrevClose(String symbol) throws Exception;

    // for chart display
    record Candle(long timestampMs, double open, double high, double low, double close, long volume) { }
    List<Candle> getCandles(String symbol, long startMs, long endMs) throws Exception;

    // listener for live updates
    interface PriceListener { void onPrice(String symbol, long tsMs, double price, long volume); }
    AutoCloseable subscribe(String symbol, PriceListener l);
}