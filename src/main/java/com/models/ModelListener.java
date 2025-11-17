package com.models;

public interface ModelListener {
    // market data
    default void onQuotesUpdated() { }
    // portfolio/account changes
    default void onAccountChanged(AccountDTO dto) { }
    // watchlist changes
    default void onWatchlistChanged(java.util.List<com.models.market.TradeItem> items) { }
    // errors
    default void onError(String message, Throwable t) { }
}
