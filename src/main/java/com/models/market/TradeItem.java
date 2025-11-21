package com.models.market;

/**
 * Represents a stock or tradeable item in the market.
 * Holds current price, change, and other market data.
 */
public class TradeItem {
    private String name;
    private final String symbol;
    private double price;
    private double changePercent;
    private double change;
    private double open;
    private double prevClose = Double.NaN; // for calculating % change
    private CompanyProfile companyProfile;

    public TradeItem(String symbol) {
        this.symbol = symbol;
        this.companyProfile = ;
    }

    /**
     * Constructs a new TradeItem.
     *
     * @param name   The name of the company.
     * @param symbol The stock symbol.
     */
    public TradeItem(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
        price = 0;
    }

    public TradeItem(String name, String symbol, double price, double changePercent) {
    /**
     * Constructs a new TradeItem with initial price data.
     *
     * @param name          The name of the company.
     * @param symbol        The stock symbol.
     * @param price         The current price.
     * @param changePercent The percentage change.
     */
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

    /**
     * Gets the current price.
     *
     * @return The current price.
     */
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

    /**
     * Updates the price and recalculates change metrics.
     *
     * @param price The new price.
     * @return True if updated successfully, false if price is invalid.
     */
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

    /**
     * Sets the previous close price and recalculates change metrics if current
     * price is valid.
     *
     * @param prevClose The previous close price.
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TradeItem ti))
            return false;
        return symbol != null && symbol.equalsIgnoreCase(ti.getSymbol());
    }

    @Override
    public int hashCode() {
        return symbol == null ? 0 : symbol.toUpperCase().hashCode();
    }

    /**
     * Sets a user-friendly name for known symbols if the current name is unknown.
     *
     * @param ti The TradeItem to update.
     * @return The updated TradeItem.
     */
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
