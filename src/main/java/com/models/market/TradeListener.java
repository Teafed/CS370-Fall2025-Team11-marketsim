package com.models.market;

public interface TradeListener {
   public void onTrade(TradeItem symbol, double price);
}
