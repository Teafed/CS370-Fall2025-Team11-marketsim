package com.tools;

import com.models.profile.Account;

public class BuildDemoAccount {

    public static Account buildDemoAccount() {
        Account account = new Account("Demo Account", 42069.0f);
        System.out.printf("[BuildDemoAccount]: Created \"" + account.getName() + "\" with balance $%.2f%n", account.getAvailableBalance());
        return account;
    }
}
