package com.models.market;

// has a list of TradeItems (stocks, etc)

/**
 * Represents a market exchange containing trade items.
 */
public class Exchange {
    private final TradeItem name;

    /**
     * Constructs a new Exchange.
     *
     * @param name The name of the exchange (as a TradeItem).
     */
    public Exchange(TradeItem name) {
        this.name = name;
    }

    /**
     * Gets the name of the exchange.
     *
     * @return The exchange name as a TradeItem.
     */
    public TradeItem getName() {
        return name;
    }
}
