package com.models;

import com.models.market.TradeItem;

public interface ModelListener {
    // market data
    default void onQuotesUpdated() { }
    // portfolio/account changes
    default void onAccountChanged(AccountDTO dto) { }
    // watchlist changes
    default void onWatchlistChanged(java.util.List<TradeItem> items) { }
    // errors
    default void onError(String message, Throwable t) { }
}
