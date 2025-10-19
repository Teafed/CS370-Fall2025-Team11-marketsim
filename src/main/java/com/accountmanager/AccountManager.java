package com.accountmanager;

import com.market.DatabaseManager;
import com.market.Market;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all user accounts in the system
 */
public class AccountManager {
    private final Map<Integer, Account> accounts;
    private final DatabaseManager dbManager;
    
    public AccountManager() {
        this.accounts = new HashMap<>();
        this.dbManager = Market.getInstance().getDatabaseManager();
        loadAccounts();
    }
    
    /**
     * Load all accounts from the database
     */
    private void loadAccounts() {
        try {
            String sql = "SELECT * FROM accounts";
            ResultSet rs = dbManager.getConnection().createStatement().executeQuery(sql);
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                double balance = rs.getDouble("balance");
                
                Account account = new Account(id, username, balance);
                accounts.put(id, account);
                
                // Load portfolio for this account
                loadPortfolio(account);
                
                // Load watchlist for this account
                loadWatchlist(account);
            }
        } catch (SQLException e) {
            System.err.println("Error loading accounts: " + e.getMessage());
        }
    }
    
    /**
     * Load portfolio for an account
     */
    private void loadPortfolio(Account account) {
        try {
            ResultSet rs = dbManager.getPortfolio(account.getId());
            Market market = Market.getInstance();
            
            while (rs != null && rs.next()) {
                String symbol = rs.getString("symbol");
                int quantity = rs.getInt("quantity");
                double purchasePrice = rs.getDouble("purchase_price");
                
                // Get the stock from the market
                com.market.Stock stock = market.getStock(symbol);
                if (stock != null) {
                    account.getPortfolio().addStock(stock, quantity, purchasePrice);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading portfolio: " + e.getMessage());
        }
    }
    
    /**
     * Load watchlist for an account
     */
    private void loadWatchlist(Account account) {
        try {
            String sql = "SELECT * FROM watchlist WHERE account_id = ?";
            java.sql.PreparedStatement ps = dbManager.getConnection().prepareStatement(sql);
            ps.setInt(1, account.getId());
            ResultSet rs = ps.executeQuery();
            
            Market market = Market.getInstance();
            while (rs.next()) {
                String symbol = rs.getString("symbol");
                
                // Get the stock from the market
                com.market.Stock stock = market.getStock(symbol);
                if (stock != null) {
                    account.getWatchlist().addStock(stock);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading watchlist: " + e.getMessage());
        }
    }
    
    /**
     * Create a new account
     * @param username Username for the account
     * @param initialBalance Initial balance
     * @return The created account, or null if creation failed
     */
    public Account createAccount(String username, double initialBalance) {
        // Create account in database
        int id = dbManager.createAccount(username);
        if (id == -1) {
            return null;
        }
        
        // Update balance if different from default
        if (initialBalance != 10000.0) {
            dbManager.updateAccountBalance(id, initialBalance);
        }
        
        // Create account object
        Account account = new Account(id, username, initialBalance);
        accounts.put(id, account);
        
        return account;
    }
    
    /**
     * Get an account by ID
     * @param id Account ID
     * @return The account, or null if not found
     */
    public Account getAccount(int id) {
        return accounts.get(id);
    }
    
    /**
     * Get all accounts
     * @return List of all accounts
     */
    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }
    
    /**
     * Delete an account
     * @param id Account ID
     * @return True if deleted, false if not found
     */
    public boolean deleteAccount(int id) {
        if (!accounts.containsKey(id)) {
            return false;
        }
        
        try {
            // Delete from database
            String sql = "DELETE FROM accounts WHERE id = ?";
            java.sql.PreparedStatement ps = dbManager.getConnection().prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            
            // Remove from memory
            accounts.remove(id);
            return true;
        } catch (SQLException e) {
            System.err.println("Error deleting account: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add a new account
     * @param account The account to add
     * @return True if successful
     */
    public boolean addAccount(Account account) {
        try {
            // Insert into database
            String sql = "INSERT INTO accounts (username, balance) VALUES (?, ?)";
            java.sql.PreparedStatement ps = dbManager.getConnection().prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, account.getName());
            ps.setDouble(2, account.getAvailableBalance());
            ps.executeUpdate();
            
            // Get generated id
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                account.setId(id);
                accounts.put(id, account);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error adding account: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the number of accounts
     * @return Number of accounts
     */
    public int getNumberOfAccounts() {
        return accounts.size();
    }

    /**
     * Remove an account
     * @param account The account to remove
     * @return True if successful
     */
    public boolean removeAccount(Account account) {
        return deleteAccount(account.getId());
    }
}
