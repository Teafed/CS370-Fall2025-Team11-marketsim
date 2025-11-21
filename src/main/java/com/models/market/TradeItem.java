package com.models.market;

/**
 * Represents a stock or tradeable item in the market.
 * Holds current price, change, and other market data.
 */
public class TradeItem {
    private java.lang.String name;
    private final java.lang.String symbol;
    private double price;
    private double changePercent;
    private double change;
    private double open;
    private double prevClose = Double.NaN; // for calculating % change

    /**
     * Constructs a new TradeItem.
     *
     * @param name   The name of the company.
     * @param symbol The stock symbol.
     */
    public TradeItem(java.lang.String name, java.lang.String symbol) {
        this.name = name;
        this.symbol = symbol;
        price = 0;
    }

    /**
     * Constructs a new TradeItem with initial price data.
     *
     * @param name          The name of the company.
     * @param symbol        The stock symbol.
     * @param price         The current price.
     * @param changePercent The percentage change.
     */
    public TradeItem(java.lang.String name, java.lang.String symbol, double price, double changePercent) {
        this.name = name;
        this.symbol = symbol;
        this.price = price;
        this.changePercent = changePercent;
    }

    /**
     * Gets the company name.
     *
     * @return The company name.
     */
    public java.lang.String getName() {
        return name;
    }

    /**
     * Sets the company name.
     *
     * @param n The new name.
     */
    public void setName(java.lang.String n) {
        this.name = n;
    }

    /**
     * Gets the stock symbol.
     *
     * @return The stock symbol.
     */
    public java.lang.String getSymbol() {
        return symbol;
    }

    /**
     * Gets the current price.
     *
     * @return The current price.
     */
    public double getCurrentPrice() {
        // updatePrice();
        return price;
    }

    /**
     * Gets the percentage change.
     *
     * @return The percentage change.
     */
    public double getChangePercent() {
        return changePercent;
    }

    /**
     * Gets the price change amount.
     *
     * @return The price change.
     */
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
        this.change = price - open;
        this.changePercent = change / open * 100;

        return true;
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

    /**
     * Sets the opening price.
     *
     * @param open The opening price.
     */
    public void setOpen(double open) {
        this.open = open;
    }

    /**
     * Sets the change amount and percentage.
     *
     * @param change        The price change.
     * @param changePercent The percentage change.
     */
    public void setChange(double change, double changePercent) {
        this.change = change;
        this.changePercent = changePercent;
    }

    @Override
    public java.lang.String toString() {
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
        if (ti.getSymbol() == null) {
            ti.setName("Unknown Symbol");
            return ti;
        }
        switch (ti.getSymbol()) {
            case "AAPL":
                ti.setName("Apple");
                break;
            case "MSFT":
                ti.setName("Microsoft");
                break;
            case "GOOGL":
                ti.setName("Alphabet");
                break;
            case "NVDA":
                ti.setName("NVIDIA");
                break;
            case "AMZN":
                ti.setName("Amazon");
                break;
            case "META":
                ti.setName("Meta Platforms");
                break;
            case "TSLA":
                ti.setName("Tesla");
                break;
            case "AVGO":
                ti.setName("Broadcom");
                break;
            case "TSM":
                ti.setName("Taiwan Semiconductor Manufacturing Company");
                break;
            case "BRK.B":
                ti.setName("Berkshire Hathaway");
                break;
            default:
                ti.setName("Unknown Name");
        }
        return ti;
    }
}
