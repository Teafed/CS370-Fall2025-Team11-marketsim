package com.models.market;

import com.models.profile.Account;

/**
 * Manages trade execution and validation.
 * (Currently commented out/inactive).
 */
public class TradeManager {
    private static TradeManager instance;
    private Account account;

    private TradeManager() {
    }

    // public void processTrade(Order trade) {
    // int shares = trade.shares();
    // TradeItem tradeItem = trade.tradeItem();
    // double tradeValue = calculateTradeValue(shares, tradeItem);
    //
    // if (trade.side() == Order.side.BUY && account.hasSufficientFunds(tradeValue))
    // {
    // account.executeBuy(tradeItem, shares, tradeValue);
    // }
    // if (trade.side() == Order.side.SELL &&
    // account.getPortfolio().hasTradeItem(trade.tradeItem()) &&
    // account.hasSufficientFunds(tradeValue)) {
    // account.executeSell(tradeItem, shares, tradeValue);
    // }
    // else {
    // System.out.println("invalid trade");
    // }
    // }
    //
    // private double calculateTradeValue(int shares, TradeItem tradeItem) {
    // double price = tradeItem.getCurrentPrice();
    // return price * shares;
    // }
    //
    // public void setAccount(Account account) {
    // this.account = account;
    // }
    //
    // public static synchronized TradeManager getInstance() {
    // if (instance == null) {
    // instance = new TradeManager();
    // }
    // return instance;
    // }
}
