package com.accountmanager;

import java.util.ArrayList;

// This class holds all accounts for the user and provides methods for adding, deleting, and returning each account.
public class AccountManager {

    private ArrayList<Account> accounts;    // list of accounts
    private int numberOfAccounts;       // number of active accounts
    private int maxNumberOfAccounts = 5;    // maximum number of accounts
    private String owner;

    // Constructor
    public AccountManager() {
        accounts = new ArrayList<Account>();
        numberOfAccounts = 0;
    }

    // Purpose: To add an account to the manager. Returns true if successful, returns false if not.
    public boolean addAccount(Account account) {
        if (numberOfAccounts < maxNumberOfAccounts) {
            accounts.add(account);
            numberOfAccounts++;
            return true;
        }
        else {
            System.out.println("Maximum number of accounts allowed is " + maxNumberOfAccounts);
            return false;
        }
    }

    // Remove an account
    public boolean removeAccount(Account account) {
        if (accounts.contains(account)) {
            accounts.remove(account);
            numberOfAccounts--;
            return true;
        }
        else {
            System.out.println("Account does not exist");
            return false;
        }
    }

    // Get account by name
    public Account getAccount(String accountName) {
        for (Account account : accounts) {
            if (account.getName().equals(accountName)) {
                return account;
            }
        }
        System.out.println("Account does not exist");
        return null;
    }

    // Get all names of accounts
    public String[] getAccountNames() {
        String[] names = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            names[i] = accounts.get(i).getName();
        }
        return names;
    }

    public int getNumberOfAccounts() {
        return numberOfAccounts;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

}
