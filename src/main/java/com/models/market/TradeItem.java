package com.models.market;

public class TradeItem {
    private String name;
    private final java.lang.String symbol;
    private double price;
    private double changePercent;
    private double change;
    private double open;
    private double prevClose = Double.NaN; // for calculating % change

    public TradeItem(java.lang.String name, java.lang.String symbol) {
        this.name = name;
        this.symbol = symbol;
        price = 0;
    }

    public TradeItem(java.lang.String name, java.lang.String symbol, double price, double changePercent) {
        this.name = name;
        this.symbol = symbol;
        this.price = price;
        this.changePercent = changePercent;
    }

    public java.lang.String getName() {
        return name;
    }

    public void setName(java.lang.String n) { this.name = n; }

    public java.lang.String getSymbol() {
        return symbol;
    }

    public double getCurrentPrice() {
        //updatePrice();
        return price;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setValues(double[] openCurrent) {
        this.open = openCurrent[0];
        this.price = openCurrent[1];
        this.prevClose = openCurrent[2];
        calculateChange();
    }

    public double getChange() {
        return change;
    }

    public boolean updatePrice(double price) {
        if (price < 0) {
            return false;
        }
        this.price = price;
        calculateChange();

        return true;
    }

    private void calculateChange() {
        this.change = price-prevClose;
        this.changePercent = change/prevClose * 100;
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
    public java.lang.String toString() {
        return this.getClass().getSimpleName() + "{" + "name=" + name + ", symbol=" + symbol + ", price=" + price + '}';
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeItem ti)) return false;
        return symbol != null && symbol.equalsIgnoreCase(ti.getSymbol());
    }
    @Override public int hashCode() { return symbol == null ? 0 : symbol.toUpperCase().hashCode(); }

    // predefined symbol names
    public TradeItem setNameLookup(TradeItem ti) {
        if (ti.getSymbol() == null) { ti.setName("Unknown Symbol"); return ti; }
        switch (ti.getSymbol()) {
            case "AAPL":  ti.setName("Apple"); break;
            case "MSFT":  ti.setName("Microsoft"); break;
            case "GOOGL": ti.setName("Alphabet"); break;
            case "NVDA":  ti.setName("NVIDIA"); break;
            case "AMZN":  ti.setName("Amazon"); break;
            case "META":  ti.setName("Meta Platforms"); break;
            case "TSLA":  ti.setName("Tesla"); break;
            case "AVGO":  ti.setName("Broadcom"); break;
            case "TSM":   ti.setName("Taiwan Semiconductor Manufacturing Company"); break;
            case "BRK.B": ti.setName("Berkshire Hathaway"); break;
            default:      ti.setName("Unknown Name");
        }
        return ti;
    }
}
