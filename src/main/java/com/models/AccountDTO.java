package com.models;

public class AccountDTO {
    public final long accountId;
    public final double cash;
    public final java.util.Map<String, Integer> positions; // symbol, shares

    public AccountDTO(long accountId, double cash,
                      java.util.Map<String,Integer> positions) {
        this.accountId = accountId; this.cash = cash; this.positions = positions;
    }
}