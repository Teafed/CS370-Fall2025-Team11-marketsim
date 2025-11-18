package com.models.profile;

import com.models.market.TradeItem;

import java.util.List;
import java.util.Objects;

// One account
public class Account {
    private long id; // identifier used in database
    private java.lang.String name; // User defined name of account

    private double cash; // the amount the user can currently trade
    private Portfolio portfolio = new Portfolio();
    private Watchlist watchlist = new Watchlist();

    // constructors
    public Account() {
        this("New Account");
    }
    public Account(java.lang.String name) {
        setName(name);
    }
    public Account(long id, java.lang.String name) {
        this.id = id;
        setName(name);
    }

    public Account(java.lang.String name, double cash) {
        setName(name);
        this.cash = cash;
        for (TradeItem ti : Watchlist.getDefaultWatchlist()) {
            this.watchlist.addWatchlistItem(ti);
        }
    }

    // name
    public void setName(java.lang.String name) { this.name = Objects.requireNonNull(name, "name"); }
    public java.lang.String getName() {
        return name;
    }

    // id
    private void setId(long id) { this.id = id; }
    public long getId() {
        return id;
    }

    // cash
    public void setCash(double balance) {
        this.cash = balance;
    }
    public double getCash() {
        return this.cash;
    }
    public double getTotalValue() {
        return cash + portfolio.getPortfolioValue();
    }

    // portfolio/watchlist
    public Portfolio getPortfolio() {
        return portfolio;
    }
    public List<TradeItem> getWatchlistItems() {
        return this.watchlist.getWatchlist();
    }
    public Watchlist getWatchlist() {
        return watchlist;
    }

    // Add value to account
    public boolean depositFunds(double amount) {
        if (amount <= 0) {
            return false;
        }
        this.cash += amount;
        return true;
    }

    // withdraw funds from account
    public boolean withdrawFunds(double amount) {
        if (amount <= 0) {
            return false;
        }
        if (cash >= amount) {
            cash -= amount;
            return true;
        }
        return false;
    }
}
