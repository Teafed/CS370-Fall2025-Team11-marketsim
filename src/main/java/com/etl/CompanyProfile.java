package com.etl;

/**
 * Represents the company profile data returned by the Finnhub API.
 */
public class CompanyProfile {
    private String country;
    private String currency;
    private String exchange;
    private String ipo;
    private String logo;
    private String marketCapitalization;
    private String name;
    private String sharesOutstanding;
    private String weburl;

    /**
     * Default constructor.
     */
    public CompanyProfile() {
    }

    // Setters
    /**
     * Sets the country.
     *
     * @param country The country.
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Sets the currency.
     *
     * @param currency The currency.
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Sets the exchange.
     *
     * @param exchange The exchange.
     */
    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    /**
     * Sets the IPO date.
     *
     * @param ipo The IPO date.
     */
    public void setIpo(String ipo) {
        this.ipo = ipo;
    }

    /**
     * Sets the logo URL.
     *
     * @param logo The logo URL.
     */
    public void setLogo(String logo) {
        this.logo = logo;
    }

    /**
     * Sets the market capitalization.
     *
     * @param marketCapitalization The market capitalization.
     */
    public void setMarketCapitalization(String marketCapitalization) {
        this.marketCapitalization = marketCapitalization;
    }

    /**
     * Sets the company name.
     *
     * @param name The company name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the shares outstanding.
     *
     * @param sharesOutstanding The shares outstanding.
     */
    public void setSharesOutstanding(String sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    /**
     * Sets the web URL.
     *
     * @param weburl The web URL.
     */
    public void setWeburl(String weburl) {
        this.weburl = weburl;
    }

    /**
     * Gets the country.
     *
     * @return The country.
     */
    public String getCountry() {
        return country;
    }

    /**
     * Gets the currency.
     *
     * @return The currency.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Gets the exchange.
     *
     * @return The exchange.
     */
    public String getExchange() {
        return exchange;
    }

    /**
     * Gets the IPO date.
     *
     * @return The IPO date.
     */
    public String getIpo() {
        return ipo;
    }

    /**
     * Gets the logo URL.
     *
     * @return The logo URL.
     */
    public String getLogo() {
        return logo;
    }

    /**
     * Gets the market capitalization.
     *
     * @return The market capitalization.
     */
    public String getMarketCapitalization() {
        return marketCapitalization;
    }

    /**
     * Gets the company name.
     *
     * @return The company name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the shares outstanding.
     *
     * @return The shares outstanding.
     */
    public String getSharesOutstanding() {
        return sharesOutstanding;
    }

    /**
     * Gets the web URL.
     *
     * @return The web URL.
     */
    public String getWeburl() {
        return weburl;
    }

    @Override
    public String toString() {
        return "CompanyProfile {" +
                "\n  name='" + name + '\'' +
                ",\n  country='" + country + '\'' +
                ",\n  currency='" + currency + '\'' +
                ",\n  exchange='" + exchange + '\'' +
                ",\n  ipo='" + ipo + '\'' +
                ",\n  logo='" + logo + '\'' +
                ",\n  marketCapitalization='" + marketCapitalization + '\'' +
                ",\n  sharesOutstanding='" + sharesOutstanding + '\'' +
                ",\n  weburl='" + weburl + '\'' +
                "\n}";
    }
}