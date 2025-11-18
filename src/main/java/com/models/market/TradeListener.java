package com.models.market;

public interface TradeListener {
   public void onTrade(String symbol, double price);
}
