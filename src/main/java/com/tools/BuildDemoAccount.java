package com.tools;

import com.accountmanager.Account;

public class BuildDemoAccount {

// might try to integrate into database here
//    public static void main(String[] args) {
//        Account account = new Account("Demo Account");
//        account.depositFunds(42069.0f);
//        System.out.printf("[BuildDemoAccount]: Created \"" + account.getName() + "\" with balance $%.2f%n", account.getAvailableBalance());
//    }

    public static Account buildDemoAccount() {
        Account account = new Account("Demo Account");
        account.depositFunds(42069.0f);
        System.out.printf("[BuildDemoAccount]: Created \"" + account.getName() + "\" with balance $%.2f%n", account.getAvailableBalance());
        return account;
    }
}
