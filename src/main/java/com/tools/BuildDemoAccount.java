package com.tools;

import com.accountmanager.Account;
import com.market.TradeItem;

public class BuildDemoAccount {

    public static Account buildDemoAccount() {
        TradeItem[] initialSymbols = {
                new TradeItem("Apple", "AAPL"),
                new TradeItem("MSFT", "Microsoft"),
                new TradeItem("GOOGL","Alphabet"),
                new TradeItem("NVDA", "NVIDIA"),
                new TradeItem("AMZN", "Amazon"),
                new TradeItem("META", "Meta Platforms"),
                new TradeItem("TSLA", "Tesla"),
                new TradeItem("AVGO", "Broadcom"),
                new TradeItem("TSM", "Taiwan Semiconductor Manufacturing Company"),
                new TradeItem("BRK.B", "Berkshire Hathaway")
        };

        Account account = new Account("Demo Account");
        account.depositFunds(42069.0f);
        System.out.printf("[BuildDemoAccount]: Created \"" + account.getName() + "\" with balance $%.2f%n", account.getAvailableBalance());
        return account;
    }
}
