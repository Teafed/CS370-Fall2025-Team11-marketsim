package com.market;

import java.sql.Connection;
import java.sql.*;
import java.io.File;

public class DatabaseManager {
    private Connection conn;
    private static final String DB_FILE = "marketsim.db";

    public DatabaseManager() {
        // Default constructor will initialize database in the current directory
    }

    public DatabaseManager(String dbFile) {
        initializeDatabase(dbFile);
    }

    public void initializeDatabase() {
        initializeDatabase(DB_FILE);
    }

    public void initializeDatabase(String dbFile) {
        try {
            // Ensure the database directory exists
            File dbDirectory = new File(dbFile).getParentFile();
            if (dbDirectory != null && !dbDirectory.exists()) {
                dbDirectory.mkdirs();
            }
            
            String url = "jdbc:sqlite:" + dbFile + "?busy_timeout=5000"; // 5s
            this.conn = DriverManager.getConnection(url);
            createSchema();
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    private void createSchema() throws SQLException {
        // Create prices table
        String pricesSql = "CREATE TABLE IF NOT EXISTS prices ("
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "    symbol TEXT NOT NULL,"
                + "    timestamp INTEGER NOT NULL," // store as epoch millis
                + "    open REAL,"
                + "    high REAL,"
                + "    low REAL,"
                + "    close REAL,"
                + "    volume INTEGER,"
                + "    UNIQUE(symbol, timestamp)"   // prevent duplicates
                + ");";
        conn.createStatement().execute(pricesSql);
        
        // Create accounts table
        String accountsSql = "CREATE TABLE IF NOT EXISTS accounts ("
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "    username TEXT UNIQUE NOT NULL,"
                + "    balance REAL DEFAULT 10000.0"
                + ");";
        conn.createStatement().execute(accountsSql);
        
        // Create portfolio table
        String portfolioSql = "CREATE TABLE IF NOT EXISTS portfolio ("
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "    account_id INTEGER NOT NULL,"
                + "    symbol TEXT NOT NULL,"
                + "    quantity INTEGER NOT NULL,"
                + "    purchase_price REAL NOT NULL,"
                + "    purchase_date INTEGER NOT NULL,"
                + "    FOREIGN KEY (account_id) REFERENCES accounts(id),"
                + "    UNIQUE(account_id, symbol)"
                + ");";
        conn.createStatement().execute(portfolioSql);
        
        // Create watchlist table
        String watchlistSql = "CREATE TABLE IF NOT EXISTS watchlist ("
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "    account_id INTEGER NOT NULL,"
                + "    symbol TEXT NOT NULL,"
                + "    FOREIGN KEY (account_id) REFERENCES accounts(id),"
                + "    UNIQUE(account_id, symbol)"
                + ");";
        conn.createStatement().execute(watchlistSql);
        
        // Create trade history table
        String tradeHistorySql = "CREATE TABLE IF NOT EXISTS trade_history ("
                + "    id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "    account_id INTEGER NOT NULL,"
                + "    symbol TEXT NOT NULL,"
                + "    quantity INTEGER NOT NULL,"
                + "    price REAL NOT NULL,"
                + "    trade_type TEXT NOT NULL," // BUY or SELL
                + "    timestamp INTEGER NOT NULL,"
                + "    FOREIGN KEY (account_id) REFERENCES accounts(id)"
                + ");";
        conn.createStatement().execute(tradeHistorySql);
    }

    public void insertPrice(String symbol, long timestamp,
                            double open, double high, double low,
                            double close, long volume) throws SQLException {
        String sql = "INSERT OR REPLACE INTO prices(symbol, timestamp, open, high, low, close, volume) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ps.setLong(2, timestamp);
            ps.setDouble(3, open);
            ps.setDouble(4, high);
            ps.setDouble(5, low);
            ps.setDouble(6, close);
            ps.setLong(7, volume);
            ps.executeUpdate();
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public ResultSet getPrices(String symbol, long start, long end) throws SQLException {
        String sql = "SELECT * FROM prices WHERE symbol=? AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, symbol);
        ps.setLong(2, start);
        ps.setLong(3, end);
        return ps.executeQuery();
    }

    public long getLatestTimestamp(String symbol) throws SQLException {
        String sql = "SELECT MAX(timestamp) FROM prices WHERE symbol=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }
    
    public Double getLastPrice(String symbol) {
        String sql = "SELECT close FROM prices WHERE symbol=? ORDER BY timestamp DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("close") : null;
        } catch (SQLException e) {
            System.err.println("Error getting last price: " + e.getMessage());
            return null;
        }
    }
    
    // Account management methods
    public int createAccount(String username) {
        String sql = "INSERT INTO accounts (username) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error creating account: " + e.getMessage());
        }
        return -1;
    }
    
    public double getAccountBalance(int accountId) {
        String sql = "SELECT balance FROM accounts WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            System.err.println("Error getting account balance: " + e.getMessage());
        }
        return 0.0;
    }
    
    public boolean updateAccountBalance(int accountId, double newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating account balance: " + e.getMessage());
            return false;
        }
    }
    
    // Portfolio management methods
    public boolean addToPortfolio(int accountId, String symbol, int quantity, double purchasePrice) {
        long timestamp = System.currentTimeMillis();
        String sql = "INSERT OR REPLACE INTO portfolio (account_id, symbol, quantity, purchase_price, purchase_date) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, symbol);
            ps.setInt(3, quantity);
            ps.setDouble(4, purchasePrice);
            ps.setLong(5, timestamp);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding to portfolio: " + e.getMessage());
            return false;
        }
    }
    
    public ResultSet getPortfolio(int accountId) {
        String sql = "SELECT * FROM portfolio WHERE account_id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, accountId);
            return ps.executeQuery();
        } catch (SQLException e) {
            System.err.println("Error getting portfolio: " + e.getMessage());
            return null;
        }
    }
    
    // Trade history methods
    public boolean recordTrade(int accountId, String symbol, int quantity, double price, String tradeType) {
        String sql = "INSERT INTO trade_history (account_id, symbol, quantity, price, trade_type, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setString(2, symbol);
            ps.setInt(3, quantity);
            ps.setDouble(4, price);
            ps.setString(5, tradeType);
            ps.setLong(6, System.currentTimeMillis());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error recording trade: " + e.getMessage());
            return false;
        }
    }
}

