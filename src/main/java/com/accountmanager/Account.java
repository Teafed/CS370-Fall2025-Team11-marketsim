package com.accountmanager;

import com.market.TradeItem;

// One account
public class Account {

    private double totalValue = 0.0f; // Current total account value
    private double availableBalance; // the amount the user can currently trade
    private String name; // User defined name of account
    private Portfolio portfolio = new Portfolio();
    private Watchlist watchlist = new Watchlist();

    // constructors
    public Account() {
        setName("New Account");
    }

    public Account(String name) {
        setName(name);
    }

    // Set name
    public void setName(String name) { this.name = name; }

    // Get name
    public String getName() {
        return name;
    }

    // Get account value
    public double getTotalValue() {
        updateValue();
        return totalValue;
    }

    // get the available balance
    public double getAvailableBalance() {
        return this.availableBalance;
    }

    /**
     * Returns the portfolio of the account.
     * @return The portfolio of the account.
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * set the watchlist of the account
     * @param symbols TradeItem array of symbols
     */
    public void setWatchlist(TradeItem[] symbols) {
        for (TradeItem s : symbols) {
            this.watchlist.addWatchlistItem(s);
        }
    }

    /**
     * Returns the watchlist of the account.
     * @return The watchlist of the account.
     */
    public Watchlist getWatchList() {
        return watchlist;
    }

    // DEPOSIT WITHDRAW UPDATE

    // Add value to account
    public boolean depositFunds(double amount) {
        if (amount < 1) {
            return false;
        }
        this.availableBalance += amount;
        updateValue();
        return true;
    }

    // withdraw funds from account
    public boolean withdrawFunds(double amount) {
        if (amount < 1) {
            return false;
        }
        if (availableBalance >= amount) {
            availableBalance -= amount;
            updateValue();
            return true;
        }
        return false;
        // TODO else throw error
    }

    public void updateValue() {
        totalValue = availableBalance + portfolio.getPortfolioValue();
    }



}
