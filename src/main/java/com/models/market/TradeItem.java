package com.models.market;

/**
 * Represents a stock or tradeable item in the market.
 * Holds current price, change, and other market data.
 */
public class TradeItem {
    private String name; // DEPRECATED
    private final String symbol;
    private double price;
    private double changePercent;
    private double change;
    private double open;
    private double prevClose = Double.NaN; // for calculating % change
    private CompanyProfile companyProfile;

    public TradeItem(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol required");
        }
        this.symbol = symbol.trim().toUpperCase();
    }

    public String getName() {
        if (companyProfile != null && companyProfile.getName() != null && !companyProfile.getName().isBlank()) {
            return companyProfile.getName();
        }
        System.out.println("No company name found for " + this.symbol);
        return symbol;
    }

    public String getSymbol() { return symbol; }

    public CompanyProfile getCompanyProfile() { return companyProfile; }
    public void setCompanyProfile(CompanyProfile cp) { this.companyProfile = cp; }

    /**
     * Gets the current price.
     *
     * @return The current price.
     */
    public double getCurrentPrice() { return price; }
    public double getChangePercent() { return changePercent; }
    public void setValues(double[] openCurrent) {
        this.open = openCurrent[0];
        this.price = openCurrent[1];
        this.prevClose = openCurrent[2];
        calculateChange();
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
        if (!Double.isNaN(prevClose) && prevClose > 0) {
            this.change = price - prevClose;
            this.changePercent = change / prevClose * 100;
        } else {
            this.change = Double.NaN;
            this.changePercent = Double.NaN;
        }
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
     * Local fallback for company name if live client cannot retrieve it
     *
     * @param ti The TradeItem to update.
     * @return The updated TradeItem.
     */
    public TradeItem setNameLookup(TradeItem ti) {
        if (ti.companyProfile == null)  { ti.companyProfile = new CompanyProfile(); }
        if (ti.getSymbol() == null) { ti.companyProfile.setName("Unknown Symbol"); return ti; }
        switch (ti.getSymbol()) {
            case "AAPL":  ti.companyProfile.setName("Apple"); break;
            case "MSFT":  ti.companyProfile.setName("Microsoft"); break;
            case "GOOGL": ti.companyProfile.setName("Alphabet"); break;
            case "NVDA":  ti.companyProfile.setName("NVIDIA"); break;
            case "AMZN":  ti.companyProfile.setName("Amazon"); break;
            case "META":  ti.companyProfile.setName("Meta Platforms"); break;
            case "TSLA":  ti.companyProfile.setName("Tesla"); break;
            case "AVGO":  ti.companyProfile.setName("Broadcom"); break;
            case "TSM":   ti.companyProfile.setName("Taiwan Semiconductor Manufacturing Company"); break;
            case "BRK.B": ti.companyProfile.setName("Berkshire Hathaway"); break;
            default:      ti.companyProfile.setName("Unknown Name");
        }
        return ti;
    }
}
