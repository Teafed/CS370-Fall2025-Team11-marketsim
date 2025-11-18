package com.models.market;

// has a list of TradeItems (stocks, etc)

public class Exchange {
    private final TradeItem name;

    public Exchange(TradeItem name) {
        this.name = name;
    }

    public TradeItem getName() {
        return name;
    }
}
