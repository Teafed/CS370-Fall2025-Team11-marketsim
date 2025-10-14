package com.accountmanager;

import com.market.*;

import java.util.ArrayList;
import java.util.Map;

// One account
public class Account {

    private int accountTotalValue; // Current total account value stored in cents
    private int availableBalance; // the amount the user can currently trade
    private String accountName; // User defined name of account
    private Portfolio portfolio;
    private Watchlist watchList;

    // constructor
    public Account(String accountName) {
        this.accountTotalValue = 0;
        this.accountName = accountName;
        this.portfolio = new Portfolio();
        this.watchList = new Watchlist();
    }

    // Set name
    public void setAccountName(String accountName) {

        this.accountName = accountName;
    }

    // Get name
    public String getName() {
        return accountName;
    }

    // Get account value
    public int getAccountTotalValue() {
        updateAccountValue();
        return accountTotalValue;
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
        updateAccountValue();
        return true;
    }

    // withdraw funds from account
    public boolean withdrawFunds(int amount) {
        if (amount < 1) {
            return false;
        }
        if (availableBalance >= amount) {
            availableBalance -= amount;
            updateAccountValue();
            return true;
        }
        return false;
        // TODO else throw error
    }

    public void updateAccountValue() {
        accountTotalValue = availableBalance + portfolio.getPortfolioValue();
    }



}
