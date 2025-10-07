package com.markets;

public class Option extends TradeItem {

    public enum PositionType {
        CALL,
        PUT;
    }
    private final PositionType position;      // the position of the option
    private int cost;       // the cost of the option
    private final Stock stock;    // the stock this option belongs to

    public Option (Stock stock, PositionType position) {
        super(stock.getName(), stock.getSymbol());
        this.stock = stock;         // assign the stock this option belongs to
        this.position = position;   // assign the option a position
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public PositionType getPosition() {
        return position;
    }

    public Stock getStock() {
        return stock;
    }

}
