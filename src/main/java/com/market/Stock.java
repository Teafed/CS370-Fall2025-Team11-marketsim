package com.markets;

import java.util.ArrayList;

public class Stock extends TradeItem {
    private String symbol;      // the symbol the stock trades under
    private String name;        // the full name of the company
    private int price;          // the current price of the stock
    // market cap?
    // options?


    public Stock(String symbol, String name, int price) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
    }



}
