package com.models.market;

import java.util.List;

/**
 * Represents the company profile data returned by the Finnhub API.
 */
public class CompanyProfile {
    private String country;
    private String currency;
    private String exchange;
    private String ipo;
    private String logo;
    private String marketCap;
    private String name;
    private String sharesOutstanding;
    private String weburl;

    /**
     * Default constructor.
     */
    public CompanyProfile(String country, String currency, String exchange,
                          String ipo, String logo, String marketCap, String name,
                          String sharesOutstanding, String weburl) {
        this.setCountry(country);
        this.setCurrency(currency);
        this.setExchange(exchange);
        this.setIpo(ipo);
        this.setLogo(logo);
        this.setMarketCap(marketCap);
        this.setName(name);
        this.setSharesOutstanding(sharesOutstanding);
        this.setWeburl(weburl);
    }

    public CompanyProfile() { }

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

    public void setMarketCap(String marketCap) {
        this.marketCap = marketCap;
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

    public String getMarketCap() {
        return marketCap;
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
                ",\n  marketCap='" + marketCap + '\'' +
                ",\n  sharesOutstanding='" + sharesOutstanding + '\'' +
                ",\n  weburl='" + weburl + '\'' +
                "\n}";
    }
}