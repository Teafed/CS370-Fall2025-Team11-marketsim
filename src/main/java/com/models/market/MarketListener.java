package com.models.market;

import java.util.List;

/**
 * Listener interface for market events.
 */
public interface MarketListener {
    /**
     * Called when market data (prices) has been updated.
     */
    public void onMarketUpdate();

    /**
     * Called when the list of active symbols in the market changes.
     *
     * @param items The updated list of TradeItems.
     */
    public void loadSymbols(List<TradeItem> items);
}
