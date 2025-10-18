package com.etl;

import com.market.TradeListener;

public interface TradeSource {
    void setTradeListener(TradeListener listener);
    void subscribe(String symbol) throws Exception;
    void start();
}

