package com.market;

import com.accountmanager.Account;


public record TradeRequest(Account account, TradeItem tradeItem, TradeType tradeType, int shares) {
    public enum TradeType {
        BUY,
        SELL
    }
}
