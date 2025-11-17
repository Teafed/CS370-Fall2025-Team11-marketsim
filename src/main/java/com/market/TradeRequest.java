package com.market;

import com.accountmanager.Account;


public record TradeRequest(TradeItem tradeItem, TradeType type, int shares) {
    public enum TradeType {
        BUY,
        SELL
    }
}
