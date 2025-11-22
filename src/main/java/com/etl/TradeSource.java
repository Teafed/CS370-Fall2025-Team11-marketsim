package com.etl;

import com.models.market.TradeListener;

/**
 * Interface for a source of real-time trade data.
 */
public interface TradeSource {
    /**
     * Sets the listener to receive trade updates.
     *
     * @param listener The TradeListener.
     */
    void setTradeListener(TradeListener listener);

    /**
     * Subscribes to trade updates for a specific symbol.
     *
     * @param symbol The symbol to subscribe to.
     * @throws Exception If an error occurs during subscription.
     */
    void subscribe(String symbol) throws Exception;
    void unsubscribe(String symbol) throws Exception;
}
