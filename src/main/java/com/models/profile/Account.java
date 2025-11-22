package com.models.profile;

import com.models.market.TradeItem;

import java.util.List;
import java.util.Objects;

/**
 * Represents a user account.
 * Holds the account ID, name, cash balance, portfolio, and watchlist.
 */
public class Account {
    private long id; // identifier used in database
    private String name; // User defined name of account

    private double cash; // the amount the user can currently trade
    private Portfolio portfolio = new Portfolio();
    private Watchlist watchlist = new Watchlist();

    // constructors
    /**
     * Constructs a new Account with a default name.
     */
    public Account() {
        this("New Account");
    }

    /**
     * Constructs a new Account with the specified name.
     *
     * @param name The name of the account.
     */
    public Account(String name) {
        setName(name);
    }

    /**
     * Constructs a new Account with the specified ID and name.
     *
     * @param id   The account ID.
     * @param name The name of the account.
     */
    public Account(long id, String name) {
        this.id = id;
        setName(name);
    }

    /**
     * Constructs a new Account with the specified name and initial cash balance.
     * Also initializes the watchlist with default items.
     *
     * @param name The name of the account.
     * @param cash The initial cash balance.
     */
    public Account(String name, double cash) {
        setName(name);
        this.cash = cash;
    }

    // name
    /**
     * Sets the account name.
     *
     * @param name The new name.
     */
    public void setName(java.lang.String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Gets the account name.
     *
     * @return The account name.
     */
    public java.lang.String getName() {
        return name;
    }

    // id
    private void setId(long id) {
        this.id = id;
    }

    /**
     * Gets the account ID.
     *
     * @return The account ID.
     */
    public long getId() {
        return id;
    }

    // cash
    /**
     * Sets the cash balance.
     *
     * @param balance The new cash balance.
     */
    public void setCash(double balance) {
        this.cash = balance;
    }

    /**
     * Gets the current cash balance.
     *
     * @return The cash balance.
     */
    public double getCash() {
        return this.cash;
    }

    // portfolio/watchlist
    /**
     * Gets the account's portfolio.
     *
     * @return The Portfolio object.
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * Gets the list of items in the watchlist.
     *
     * @return A list of TradeItems.
     */
    public List<TradeItem> getWatchlistItems() {
        return this.watchlist.getWatchlist();
    }

    /**
     * Gets the account's watchlist.
     *
     * @return The Watchlist object.
     */
    public Watchlist getWatchlist() {
        return watchlist;
    }

    // Add value to account
    /**
     * Deposits funds into the account.
     *
     * @param amount The amount to deposit.
     * @return True if successful, false if amount is invalid.
     */
    public boolean depositFunds(double amount) {
        if (amount <= 0) {
            return false;
        }
        this.cash += amount;
        return true;
    }

    // withdraw funds from account
    /**
     * Withdraws funds from the account.
     *
     * @param amount The amount to withdraw.
     * @return True if successful, false if invalid amount or insufficient funds.
     */
    public boolean reduceBalance(double amount) {
        if (amount <= 0) {
            return false;
        }
        if (cash >= amount) {
            cash -= amount;
            return true;
        }
        return false;
    }

    // public void executeBuy(TradeItem tradeItem, int shares, double value) {
    // reduceBalance(value);
    // portfolio.addTradeItem(tradeItem, shares);
    // }
    //
    // public void executeSell(TradeItem tradeItem, int shares, double value) {
    // portfolio.removeTradeItem(tradeItem, shares);
    // depositFunds(value);
    // }
}
