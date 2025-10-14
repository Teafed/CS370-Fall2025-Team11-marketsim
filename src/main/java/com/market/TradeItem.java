package com.market;

public abstract class TradeItem {
    private String name;
    private String symbol;
    private int price;
    private double changePercent;
    private double change;

    public TradeItem(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
        price = 0;
    }

    public TradeItem(String name, String symbol, int price, double changePercent) {
        this.name = name;
        this.symbol = symbol;
        this.price = price;
        this.changePercent = changePercent;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getCurrentPrice() {
        //updatePrice();
        return price;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public double getChange() {
        return change;
    }

    public void updatePrice(int price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" + "name=" + name + ", symbol=" + symbol + ", price=" + price + '}';
    }
}
