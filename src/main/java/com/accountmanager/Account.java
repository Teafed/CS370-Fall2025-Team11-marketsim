package com.accountmanager;

import com.market.*;

/**
 * Represents a user account in the market simulation
 */
public class Account implements Trader {
    private int id;
    private double accountTotalValue; // Current total account value
    private double availableBalance; // the amount the user can currently trade
    private String accountName; // User defined name of account
    private Portfolio portfolio;
    private Watchlist watchlist;
    private final Market market;

    /**
     * Create a new account with the given name and initial balance
     * @param accountName User defined name of account
     * @param initialBalance Initial account balance
     */
    public Account(String accountName, double initialBalance) {
        this.accountName = accountName;
        this.availableBalance = initialBalance;
        this.accountTotalValue = initialBalance;
        this.portfolio = new Portfolio();
        this.watchlist = new Watchlist();
        this.market = Market.getInstance();
    }

    /**
     * Create a new account with the given name and zero balance
     * @param accountName User defined name of account
     */
    public Account(String accountName) {
        this(accountName, 0.0);
    }
    
    /**
     * Create an account from database
     * @param id Account ID from database
     * @param accountName User defined name of account
     * @param balance Current available balance
     */
    public Account(int id, String accountName, double balance) {
        this.id = id;
        this.accountName = accountName;
        this.availableBalance = balance;
        this.portfolio = new Portfolio();
        this.watchlist = new Watchlist();
        this.market = Market.getInstance();
        updateAccountValue();
    }

    /**
     * Set account name
     * @param accountName New account name
     */
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    /**
     * Get account name
     * @return Account name
     */
    public String getName() {
        return accountName;
    }
    
    /**
     * Get account ID
     * @return Account ID
     */
    public int getId() {
        return id;
    }
    
    /**
     * Set account ID
     * @param id Account ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get total account value (cash + investments)
     * @return Total account value
     */
    public double getAccountTotalValue() {
        updateAccountValue();
        return accountTotalValue;
    }

    /**
     * Get available cash balance
     * @return Available balance
     */
    public double getAvailableBalance() {
        return this.availableBalance;
    }

    /**
     * Returns the portfolio of the account
     * @return The portfolio of the account
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * Returns the watchlist of the account
     * @return The watchlist of the account
     */
    public Watchlist getWatchlist() {
        return watchlist;
    }
    
    /**
     * Update the total account value based on current stock prices
     */
    public void updateAccountValue() {
        double portfolioValue = portfolio.getPortfolioValue();
        accountTotalValue = availableBalance + portfolioValue;
    }
    
    /**
     * Buy a stock
     * @param symbol Stock symbol
     * @param quantity Quantity to buy
     * @param price Maximum price willing to pay
     * @return True if order was placed successfully
     */
    public boolean buyStock(String symbol, int quantity, double price) {
        // Check if we have enough funds
        double totalCost = price * quantity;
        if (totalCost > availableBalance) {
            return false;
        }
        
        // Reserve the funds
        availableBalance -= totalCost;
        
        // Create and place the order
        TradeRequest request = new TradeRequest(id, symbol, quantity, price, true, this);
        market.getTradeManager().placeBuyOrder(request);
        
        // If not executed immediately, the funds remain reserved
        // If executed, the trade manager will update the portfolio
        return true;
    }
    
    /**
     * Sell a stock
     * @param symbol Stock symbol
     * @param quantity Quantity to sell
     * @param price Minimum price willing to accept
     * @return True if order was placed successfully
     */
    public boolean sellStock(String symbol, int quantity, double price) {
        // Check if we have enough shares
        if (!portfolio.hasEnoughShares(symbol, quantity)) {
            return false;
        }
        
        // Create and place the order
        TradeRequest request = new TradeRequest(id, symbol, quantity, price, false, this);
        market.getTradeManager().placeSellOrder(request);
        
        // If executed, the trade manager will update the portfolio and balance
        return true;
    }
    
    /**
     * Add a stock to the watchlist
     * @param symbol Stock symbol
     */
    public void addToWatchlist(String symbol) {
        Stock stock = market.getStock(symbol);
        if (stock != null) {
            watchlist.addStock(stock);
        }
    }
    
    /**
     * Remove a stock from the watchlist
     * @param symbol Stock symbol
     */
    public void removeFromWatchlist(String symbol) {
        watchlist.removeStock(symbol);
    }
    
    /**
     * Notification when a trade is executed
     */
    @Override
    public void notifyTradeExecuted(TradeRequest request, double executionPrice) {
        if (request.isBuyOrder()) {
            // Buy order executed - add to portfolio
            Stock stock = market.getStock(request.getSymbol());
            portfolio.addStock(stock, request.getQuantity(), executionPrice);
        } else {
            // Sell order executed - remove from portfolio and add funds
            portfolio.removeStock(request.getSymbol(), request.getQuantity());
            availableBalance += request.getQuantity() * executionPrice;
        }
        
        // Update account value
        updateAccountValue();
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

}
