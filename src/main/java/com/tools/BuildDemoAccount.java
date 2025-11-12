package com.tools;

import com.accountmanager.Account;
import com.accountmanager.Watchlist;
import com.market.TradeItem;

public class BuildDemoAccount {

    public static Account buildDemoAccount() {
        Account account = new Account("Demo Account", 42069.0f);
        System.out.printf("[BuildDemoAccount]: Created \"" + account.getName() + "\" with balance $%.2f%n", account.getAvailableBalance());
        return account;
    }
}
