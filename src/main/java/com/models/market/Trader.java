package com.models.market;

// Handles buying and selling of stocks
/**
 * Handles buying and selling of stocks.
 * (Currently commented out/inactive).
 */
public class Trader {

    // Constructor
    // Construct one for each account? or pass values to functions in here?
    /**
     * Constructs a new Trader.
     */
    public Trader() {
    }

    // Buy stock
    // public boolean buyStock(Account account, String stock, int quantityOfShares)
    // {
    // int stockPrice = broker.getStockPrice("stock"); // get stock price
    // int cost = stockPrice * quantityOfShares; // calculate total cost
    // if (account.getAccountTotalValue() > cost) { // check account has enough to
    // make purchase
    // account.addOwnedStock(stock); // add stock to account
    // account.removeValue(cost); // reduce account value
    // return true; // shares were bought
    // }
    // return false; // shares were unable to be purchased
    // }

    // Sell stock
    // public boolean sellStock(Account account, String stock, int quantityOfShares)
    // {
    // if (account.ownsStock(stock)) {
    // int stockPrice = broker.getStockPrice("stock");
    // int cost = stockPrice * quantityOfShares;
    // account.removeOwnedStock(stock);
    // account.removeValue(cost);
    // return true;
    // }
    // return false;
    // }
}
