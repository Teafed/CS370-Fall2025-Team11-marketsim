package com.models.market;

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
    public void setCountry(String country) {
        this.country = country;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public void setIpo(String ipo) {
        this.ipo = ipo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public void setMarketCapitalization(String marketCapitalization) {
        this.marketCapitalization = marketCapitalization;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSharesOutstanding(String sharesOutstanding) {
        this.sharesOutstanding = sharesOutstanding;
    }

    public void setWeburl(String weburl) {
        this.weburl = weburl;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public String getExchange() {
        return exchange;
    }

    public String getIpo() {
        return ipo;
    }

    public String getLogo() {
        return logo;
    }

    public String getMarketCapitalization() {
        return marketCapitalization;
    }

    public String getName() {
        return name;
    }

    public String getSharesOutstanding() {
        return sharesOutstanding;
    }

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