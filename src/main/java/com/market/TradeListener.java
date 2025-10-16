package com.market;

public interface TradeListener {
   public void onTrade(String symbol, double price, long timestamp, long volume);
}
