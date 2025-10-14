package com.market;

// has a list of TradeItems (stocks, etc)

public class Exchange {
    private final String name;

    public Exchange(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
