package com.etl;

import com.models.market.TradeListener;

public interface TradeSource {
    void setTradeListener(TradeListener listener);
    void subscribe(String symbol) throws Exception;
    void unsubscribe(String symbol) throws Exception;
}

