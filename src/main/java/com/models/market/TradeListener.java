package com.models.market;

/**
 * Listener interface for real-time trade events.
 */
public interface TradeListener {
   /**
    * Called when a trade occurs for a subscribed symbol.
    *
    * @param symbol The stock symbol.
    * @param price  The trade price.
    */
   public void onTrade(String symbol, double price);
}
