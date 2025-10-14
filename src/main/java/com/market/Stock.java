package com.market;

public class Stock extends TradeItem {

    public Stock(String symbol, String name) {
        super(name, symbol);
    }

    public Stock(String name, String symbol, int price, double changePercent) {
        super(name, symbol, price, changePercent);
    }
}
