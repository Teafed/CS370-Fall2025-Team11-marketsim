package com.models.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// This class holds all accounts for the user and provides methods for adding, deleting, and returning each account.
public class Profile {
    private final ArrayList<Account> accounts = new ArrayList<>(); // list of accounts
    private Account activeAccount;
    private int maxNumberOfAccounts = 5;    // maximum number of accounts
    private String owner;

    // Constructor
    public Profile(ArrayList<Account> accounts) {
        if (accounts != null) {
            this.accounts.addAll(accounts);
            if (!this.accounts.isEmpty()) {
                this.activeAccount = this.accounts.get(0);
            }
        }
    }

    // Purpose: To add an account to the manager. Returns true if successful, returns false if not.
    public boolean addAccount(Account account) {
        Objects.requireNonNull(account, "account");
        if (accounts.size() >= maxNumberOfAccounts) {
            System.out.println("[Profile] Max number of accounts allowed is " + maxNumberOfAccounts);
            return false;
        }
        accounts.add(account);
        if (activeAccount == null) activeAccount = account; // default first
        return true;
    }

    // Remove an account
    public boolean removeAccount(Account account) {
        if (!accounts.remove(account)) {
            System.out.println("Account does not exist");
            return false;
        }
        if (account == activeAccount) {
            activeAccount = accounts.isEmpty() ? null : accounts.get(0);
        }
        return true;
    }

    // Get account by name
    public Account getAccount(String accountName) {
        for (Account account : accounts) {
            if (account.getName().equals(accountName)) {
                return account;
            }
        }
        System.out.println("[Profile] Account does not exist");
        return null;
    }

    public List<Account> getAccounts() { return accounts; }

    public Account getFirstAccount() {
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    public Account getActiveAccount() { return activeAccount; }

    public void setActiveAccount(Account account) {
        if (account != null && !accounts.contains(account)) {
            System.out.println(getNumberOfAccounts());
            throw new IllegalArgumentException("[Profile] Account does not exist");
        }
        this.activeAccount = account;
    }

    // Get all names of accounts
    public List<String> getAccountNames() {
        return accounts.stream().map(Account::getName).toList();
    }

    public int getNumberOfAccounts() {
        return accounts.size();
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
