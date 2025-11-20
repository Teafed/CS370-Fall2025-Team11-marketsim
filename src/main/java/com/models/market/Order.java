package com.models.market;

import com.models.profile.Account;

/**
 * Represents a stock order (buy or sell).
 *
 * @param account   The account placing the order.
 * @param tradeItem The stock being traded.
 * @param side      The side of the order (BUY or SELL).
 * @param shares    The number of shares.
 * @param price     The price per share.
 * @param ts        The timestamp of the order.
 */
public record Order(Account account, TradeItem tradeItem, Order.side side, int shares, double price, long ts) {
    public enum side {
        BUY,
        SELL
    }

    /**
     * Validates the order parameters.
     *
     * @throws IllegalArgumentException If shares are less than or equal to 0.
     */
    public Order {
        if (shares <= 0)
            throw new IllegalArgumentException("Shares must be 1 or greater");
    }
}
