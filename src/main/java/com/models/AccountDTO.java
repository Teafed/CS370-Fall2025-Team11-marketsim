package com.models;

/**
 * Data Transfer Object for Account information.
 * Used to pass account state (cash, positions) to listeners.
 */
public class AccountDTO {
    public final long accountId;
    public final double cash;
    public final java.util.Map<String, Integer> positions; // symbol, shares

    /**
     * Constructs a new AccountDTO.
     *
     * @param accountId The unique ID of the account.
     * @param cash      The current cash balance.
     * @param positions A map of symbol to quantity for current positions.
     */
    public AccountDTO(long accountId, double cash,
            java.util.Map<String, Integer> positions) {
        this.accountId = accountId;
        this.cash = cash;
        this.positions = positions;
    }
}