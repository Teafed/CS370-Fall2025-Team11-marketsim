package com.models.market;

import com.models.profile.Account;

public record Order(Account account, TradeItem tradeItem, Order.side side, int shares, double price, long ts) {
    public enum side {
        BUY,
        SELL
    }
    public Order {
        if (shares <= 0) throw new IllegalArgumentException("Shares must be 1 or greater");
    }
}
