package com.market;

public class Stock extends TradeItem {

    public Stock(String name, String symbol) {
        super(name, symbol);
    }

    public Stock(String name, String symbol, double price, double changePercent) {
        super(name, symbol, price, changePercent);
    }
}
