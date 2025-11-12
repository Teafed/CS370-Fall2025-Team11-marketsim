package com.market;

public class TradeItem {
    private String name;
    private final String symbol;
    private double price;
    private double changePercent;
    private double change;

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

    public void setName(String n) { this.name = n; }

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
        if (prevClose > 0.0) {
            this.change = price - prevClose;
            this.changePercent = (change / prevClose) * 100.0;
        } else {
            // No baseline yet → leave change values indeterminate
            this.change = Double.NaN;
            this.changePercent = Double.NaN;
        }
        return true;
    }

    public void setPrevClose(double prevClose) {
        this.prevClose = prevClose;
        if (!Double.isNaN(price) && prevClose > 0.0) {
            this.change = price - prevClose;
            this.changePercent = (change / prevClose) * 100.0;
        }
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
