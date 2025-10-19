package com.market;

/**
 * Interface for entities that can trade in the market
 */
public interface Trader {
    /**
     * Notify the trader that a trade has been executed
     * @param request The original trade request
     * @param executionPrice The price at which the trade was executed
     */
    void notifyTradeExecuted(TradeRequest request, double executionPrice);
}