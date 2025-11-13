package com.market;

import java.time.LocalDate;
import com.accountmanager.Account;

public record Order(Account account, TradeItem tradeItem, Order.side side, int shares, double price, LocalDate time) {
    public enum side {
        BUY,
        SELL
    }
    public Order {
        if (shares <= 0) throw new IllegalArgumentException("Shares must be 1 or greater");
    }

    public void calculateTotal() {

    }
}
