package com.models.market;

/**
 * Represents a stock trade item.
 */
public class Stock extends TradeItem {

    /**
     * Constructs a new Stock with name and symbol.
     *
     * @param name   The name of the stock.
     * @param symbol The stock symbol.
     */
    public Stock(String name, String symbol) {
        super(name, symbol);
    }

    /**
     * Constructs a new Stock with full details.
     *
     * @param name          The name of the stock.
     * @param symbol        The stock symbol.
     * @param price         The current price.
     * @param changePercent The percent change.
     */
    public Stock(String name, String symbol, double price, double changePercent) {
        super(name, symbol, price, changePercent);
    }
}
