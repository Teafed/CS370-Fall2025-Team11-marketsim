package com.models.market;

import java.util.List;

public interface MarketListener {
    public void onMarketUpdate();
    public void loadSymbols(List<TradeItem> items);
}
