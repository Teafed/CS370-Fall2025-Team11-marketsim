package com.models;

import java.util.Map;

/**
 * Data Transfer Object for Account information.
 * Used to pass account state (cash, positions) to listeners.
 *
 * @param positions symbol, shares
 */
public record AccountDTO(long accountId, double cash, Map<String, Integer> positions) {
    /**
     * Constructs a new AccountDTO.
     *
     * @param accountId The unique ID of the account.
     * @param cash      The current cash balance.
     * @param positions A map of symbol to quantity for current positions.
     */
    public AccountDTO { }
}