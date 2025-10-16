package com.market;

import java.util.ArrayList;

public class Stock extends TradeItem {
    private String symbol;      // the symbol the stock trades under
    private String name;        // the full name of the company
    // market cap?
    // options?


    public Stock(String name, String symbol) {
        super(name, symbol);
    }
}
