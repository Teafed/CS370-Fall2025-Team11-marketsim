package com.market;

public abstract class TradeItem {
    private String name;
    private String symbol;
    private int price;

    public TradeItem(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
        price = 0;
    }

    public String getName() {
        return name;
    }
    public String getSymbol() {
        return symbol;
    }
    public int getCurrentPrice() {
        //updatePrice();
        return price;}

    public void updatePrice(int price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" + "name=" + name + ", symbol=" + symbol + ", price=" + price + '}';
    }
}
