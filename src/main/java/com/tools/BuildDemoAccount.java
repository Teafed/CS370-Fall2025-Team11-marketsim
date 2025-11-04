package com.tools;

import com.accountmanager.Account;
import com.accountmanager.Watchlist;
import com.market.TradeItem;

public class BuildDemoAccount {

    public static Account buildDemoAccount() {
        TradeItem[] initialSymbols = {
                new TradeItem("Apple", "AAPL"),
                new TradeItem("Microsoft", "MSFT"),
                new TradeItem("Alphabet", "GOOGL"),
                new TradeItem("NVIDIA", "NVDA"),
                new TradeItem("Amazon", "AMZN"),
                new TradeItem("Meta Platforms", "META"),
                new TradeItem("Tesla", "TSLA"),
                new TradeItem("Broadcom", "AVGO"),
                new TradeItem("Taiwan Semiconductor Manufacturing Company", "TSM"),
                new TradeItem("Berkshire Hathaway", "BRK.B")
        };

        Watchlist wl = new Watchlist();
        for (TradeItem item : initialSymbols) {
            wl.addWatchlistItem(item);
        }

        Account account = new Account("Demo Account");
        account.depositFunds(42069.0f);
        account.setWatchlist(initialSymbols);
        System.out.printf("[BuildDemoAccount]: Created \"" + account.getName() + "\" with balance $%.2f%n", account.getAvailableBalance());
        return account;
    }
}
