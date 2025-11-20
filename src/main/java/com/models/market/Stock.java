package com.models.market;

public class Stock extends TradeItem {

    public Stock(String name, String symbol) {
        super(name, symbol);
    }

    public Stock(java.lang.String name, java.lang.String symbol, double price, double changePercent) {
        super(name, symbol, price, changePercent);
    }
}
