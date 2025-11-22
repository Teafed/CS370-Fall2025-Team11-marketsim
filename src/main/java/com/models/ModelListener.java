package com.models;

import com.models.market.TradeItem;

/**
 * Listener interface for model updates.
 * Implementations can override specific methods to handle market data, account
 * changes, or errors.
 */
public interface ModelListener {
    /**
     * Called when market quotes have been updated.
     */
    default void onQuotesUpdated() {
    }

    /**
     * Called when the account state (cash, positions) has changed.
     *
     * @param dto The updated AccountDTO.
     */
    default void onAccountChanged(AccountDTO dto) {
    }

    /**
     * Called when the watchlist has changed.
     *
     * @param items The updated list of TradeItems in the watchlist.
     */
    default void onWatchlistChanged(java.util.List<TradeItem> items) {
    }

    /**
     * Called when an error occurs in the model.
     *
     * @param message The error message.
     * @param t       The throwable associated with the error.
     */
    default void onError(String message, Throwable t) {
    }
}
