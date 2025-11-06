package com.market;

public class TradeItem {
    private final String name;
    private final String symbol;
    private double price;
    private double changePercent;
    private double change;
    private double open;
    private double prevClose = Double.NaN; // for calculating % change

    public TradeItem(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
        price = 0;
    }

    public TradeItem(String name, String symbol, double price, double changePercent) {
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

    public double getCurrentPrice() {
        //updatePrice();
        return price;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public double getChange() {
        return change;
    }

    public boolean updatePrice(double price) {
        if (price < 0) {
            return false;
        }
        this.price = price;
        this.change = price-open;
        this.changePercent = change/open * 100;

        return true;
    }

    public void setPrevClose(double prevClose) {
        this.prevClose = prevClose;
        if (!Double.isNaN(price) && prevClose > 0.0) {
            this.change = price - prevClose;
            this.changePercent = (change / prevClose) * 100.0;
        }
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public void setChange(double change, double changePercent) {
        this.change = change;
        this.changePercent = changePercent;
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" + "name=" + name + ", symbol=" + symbol + ", price=" + price + '}';
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeItem ti)) return false;
        return symbol != null && symbol.equalsIgnoreCase(ti.getSymbol());
    }
    @Override public int hashCode() { return symbol == null ? 0 : symbol.toUpperCase().hashCode(); }

}
