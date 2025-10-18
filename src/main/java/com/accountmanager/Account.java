package com.accountmanager;

// One account
public class Account {

    private int totalValue; // Current total account value stored in cents
    private int availableBalance; // the amount the user can currently trade
    private String name; // User defined name of account
    private Portfolio portfolio;
    private Watchlist watchList;

    // constructor
    public Account(String name) {
        this.totalValue = 0;
        this.name = name;
        this.portfolio = new Portfolio();
        this.watchList = new Watchlist();
    }

    // Set name
    public void setName(String name) {

        this.name = name;
    }

    // Get name
    public String getName() {
        return name;
    }

    // Get account value
    public int getTotalValue() {
        updateValue();
        return totalValue;
    }

    // get the available balance
    public int getAvailableBalance() {
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
     * Returns the watchlist of the account.
     * @return The watchlist of the account.
     */
    public Watchlist getWatchList() {
        return watchList;
    }

    // DEPOSIT WITHDRAW UPDATE

    // Add value to account
    public boolean depositFunds(int amount) {
        if (amount < 1) {
            return false;
        }
        this.availableBalance += amount;
        updateValue();
        return true;
    }

    // withdraw funds from account
    public boolean withdrawFunds(int amount) {
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
