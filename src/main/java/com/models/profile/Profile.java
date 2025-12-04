package com.models.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// This class holds all accounts for the user and provides methods for adding, deleting, and returning each account.
/**
 * Represents a user profile, which can contain multiple accounts.
 * Manages the list of accounts and the active account.
 */
public class Profile {
    private final ArrayList<Account> accounts = new ArrayList<>(); // list of accounts
    private Account activeAccount;
    private int maxNumberOfAccounts = 5; // maximum number of accounts
    private long id; // database identifier
    private String owner;

    // Constructor
    /**
     * Constructs an empty Profile with no accounts.
     */
    public Profile() {
    }

    /**
     * Constructs a new Profile with a list of accounts.
     * Sets the first account as active by default.
     *
     * @param accounts The list of accounts.
     */
    public Profile(ArrayList<Account> accounts) {
        if (accounts != null) {
            this.accounts.addAll(accounts);
            if (!this.accounts.isEmpty()) {
                this.activeAccount = this.accounts.get(0);
            }
        }
    }

    public Profile(java.util.Collection<Account> accounts) {
        if (accounts != null) {
            this.accounts.addAll(accounts);
            if (!this.accounts.isEmpty()) {
                this.activeAccount = this.accounts.get(0);
            }
        }
    }

    // Purpose: To add an account to the manager. Returns true if successful,
    // returns false if not.
    /**
     * Adds an account to the profile.
     *
     * @param account The account to add.
     * @return True if successful, false if the maximum number of accounts is
     *         reached.
     */
    public boolean addAccount(Account account) {
        Objects.requireNonNull(account, "account");
        if (accounts.size() >= maxNumberOfAccounts) {
            System.out.println("[Profile] Max number of accounts allowed is " + maxNumberOfAccounts);
            return false;
        }
        accounts.add(account);
        if (activeAccount == null)
            activeAccount = account; // default first
        return true;
    }

    // Remove an account
    /**
     * Removes an account from the profile.
     *
     * @param account The account to remove.
     * @return True if successful, false if the account does not exist.
     */
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

    /**
     * Gets an account by name.
     *
     * @param accountName The name of the account.
     * @return The Account object, or null if not found.
     */
    public Account getAccount(String accountName) {
        for (Account account : accounts) {
            if (account.getName().equals(accountName)) {
                return account;
            }
        }
        System.out.println("[Profile] Account does not exist");
        return null;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    /**
     * Gets the first account in the list.
     *
     * @return The first Account, or null if the list is empty.
     */
    public Account getFirstAccount() {
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    /**
     * Gets the currently active account.
     *
     * @return The active Account.
     */
    public Account getActiveAccount() {
        return activeAccount;
    }

    /**
     * Sets the active account.
     *
     * @param account The account to set as active.
     * @throws IllegalArgumentException If the account does not belong to this
     *                                  profile.
     */
    public void setActiveAccount(Account account) {
        if (account != null && !accounts.contains(account)) {
            System.out.println(getNumberOfAccounts());
            throw new IllegalArgumentException("[Profile] Account does not exist");
        }
        this.activeAccount = account;
    }

    // Get all names of accounts
    /**
     * Gets a list of all account names.
     *
     * @return A list of account names.
     */
    public List<String> getAccountNames() {
        return accounts.stream().map(Account::getName).toList();
    }

    /**
     * Gets the number of accounts in the profile.
     *
     * @return The number of accounts.
     */
    public int getNumberOfAccounts() {
        return accounts.size();
    }

    /**
     * Sets the owner name of the profile.
     *
     * @param owner The owner's name.
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }
    /**
     * Gets the name of the owner of the profile.
     *
     * @return The owner's name.
     */
    public String getOwner() { return this.owner; }

    public void setId(long profileId) { this.id = profileId; }

    public long getId() { return this.id; }
}
