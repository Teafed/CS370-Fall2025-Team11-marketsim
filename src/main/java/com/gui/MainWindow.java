// splits window into left and right panels. left pane is the list of symbols, right will have chart panel and profile panel

package com.gui;

import com.accountmanager.Account;
import com.accountmanager.AccountManager;
import com.market.Market;
import com.market.Stock;
import com.market.TradeItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MainWindow extends JFrame implements SymbolListPanel.SymbolSelectionListener {
    private JSplitPane splitPane;
    private SymbolListPanel symbolPanel;
    private ChartPanel chartPanel;
    private JPanel accountPanel;
    private JPanel tradePanel;
    private JTabbedPane rightTabPane;
    
    private Market market;
    private AccountManager accountManager;
    private Account currentAccount;

    private static final String WINDOW_TITLE = "Marketsim";
    private static final String DATA_FOLDER = "data";
    private static final int LEFT_PANEL_WIDTH = 250;
    private static final int MIN_LEFT_WIDTH = 150;
    private static final int MIN_RIGHT_WIDTH = 300;

    public MainWindow() {
        // Initialize market and account manager
        market = Market.getInstance();
        market.initialize();
        
        accountManager = new AccountManager();
        
        // Create a default account if none exists
        if (accountManager.getAllAccounts().isEmpty()) {
            currentAccount = accountManager.createAccount("Default User", 10000.0);
        } else {
            currentAccount = accountManager.getAllAccounts().get(0);
        }
        
        initializeWindow();
        createPanels();
        setupSplitPane();
        setupResizeListener();
        createMenuBar();

        setVisible(true);
    }

    private void initializeWindow() {
        setTitle(WINDOW_TITLE);
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen

        getContentPane().setBackground(GUIComponents.BG_DARK);
    }

    private void setupSplitPane() {
        splitPane = GUIComponents.createSplitPane(
                "horizontal", symbolPanel, rightTabPane
        );

        splitPane.setDividerLocation(LEFT_PANEL_WIDTH);
        splitPane.setOneTouchExpandable(false);
        splitPane.setResizeWeight(0.0); // right panel gets all extra space
        splitPane.setContinuousLayout(true); // smooth resizing

        add(splitPane, BorderLayout.CENTER);
    }

    // create
    private void createPanels() {
        // data panel - contains list of symbols from csv data folder
        symbolPanel = new SymbolListPanel(DATA_FOLDER);
        symbolPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
        symbolPanel.setMinimumSize(new Dimension(MIN_LEFT_WIDTH, 0));
        symbolPanel.addSymbolSelectionListener(this);
        
        // Update symbol panel with market stocks
        for (String symbol : market.getAvailableStocks()) {
            Stock stock = market.getStock(symbol);
            symbolPanel.addSymbol(stock);
        }

        // right panel - will show selected symbol content
        chartPanel = new ChartPanel();
        chartPanel.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));
        
        // Create account panel
        accountPanel = createAccountPanel();
        
        // Create trade panel
        tradePanel = createTradePanel();
        
        // Create tabbed pane for right side
        rightTabPane = new JTabbedPane();
        rightTabPane.addTab("Chart", chartPanel);
        rightTabPane.addTab("Account", accountPanel);
        rightTabPane.addTab("Trade", tradePanel);
    }
    
    private JPanel createAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GUIComponents.BG_DARK);
        
        // Account info at the top
        JPanel accountInfoPanel = new JPanel(new GridLayout(3, 2));
        accountInfoPanel.setBackground(GUIComponents.BG_DARK);
        accountInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel nameLabel = new JLabel("Account Name:");
        nameLabel.setForeground(Color.WHITE);
        JLabel nameValue = new JLabel(currentAccount.getName());
        nameValue.setForeground(Color.WHITE);
        
        JLabel balanceLabel = new JLabel("Available Balance:");
        balanceLabel.setForeground(Color.WHITE);
        JLabel balanceValue = new JLabel(String.format("$%.2f", currentAccount.getAvailableBalance()));
        balanceValue.setForeground(Color.GREEN);
        
        JLabel totalLabel = new JLabel("Total Value:");
        totalLabel.setForeground(Color.WHITE);
        JLabel totalValue = new JLabel(String.format("$%.2f", currentAccount.getAccountTotalValue()));
        totalValue.setForeground(Color.GREEN);
        
        accountInfoPanel.add(nameLabel);
        accountInfoPanel.add(nameValue);
        accountInfoPanel.add(balanceLabel);
        accountInfoPanel.add(balanceValue);
        accountInfoPanel.add(totalLabel);
        accountInfoPanel.add(totalValue);
        
        panel.add(accountInfoPanel, BorderLayout.NORTH);
        
        // Portfolio table in the center
        String[] columns = {"Symbol", "Quantity", "Avg Price", "Current Price", "Value", "P/L"};
        Object[][] data = new Object[currentAccount.getPortfolio().getHoldings().size()][6];
        
        int i = 0;
        for (String symbol : currentAccount.getPortfolio().getHoldings().keySet()) {
            Stock stock = market.getStock(symbol);
            int quantity = currentAccount.getPortfolio().getQuantity(symbol);
            double avgPrice = currentAccount.getPortfolio().getAveragePurchasePrice(symbol);
            double currentPrice = stock.getPrice();
            double value = currentPrice * quantity;
            double pl = currentAccount.getPortfolio().getProfitLoss(symbol);
            
            data[i][0] = symbol;
            data[i][1] = quantity;
            data[i][2] = String.format("$%.2f", avgPrice);
            data[i][3] = String.format("$%.2f", currentPrice);
            data[i][4] = String.format("$%.2f", value);
            data[i][5] = String.format("$%.2f", pl);
            
            i++;
        }
        
        JTable portfolioTable = new JTable(data, columns);
        JScrollPane scrollPane = new JScrollPane(portfolioTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Refresh button at the bottom
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> updateAccountPanel());
        panel.add(refreshButton, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateAccountPanel() {
        // Remove the old panel
        rightTabPane.remove(accountPanel);
        
        // Create a new one with updated data
        accountPanel = createAccountPanel();
        
        // Add it back
        rightTabPane.addTab("Account", accountPanel);
        rightTabPane.setSelectedComponent(accountPanel);
    }
    
    private JPanel createTradePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GUIComponents.BG_DARK);
        
        // Trade form
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBackground(GUIComponents.BG_DARK);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel symbolLabel = new JLabel("Symbol:");
        symbolLabel.setForeground(Color.WHITE);
        JComboBox<String> symbolCombo = new JComboBox<>();
        for (String symbol : market.getAvailableStocks()) {
            symbolCombo.addItem(symbol);
        }
        
        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setForeground(Color.WHITE);
        JTextField quantityField = new JTextField("1");
        
        JLabel priceLabel = new JLabel("Price:");
        priceLabel.setForeground(Color.WHITE);
        JTextField priceField = new JTextField();
        
        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setForeground(Color.WHITE);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Buy", "Sell"});
        
        // Update price field when symbol changes
        symbolCombo.addActionListener(e -> {
            String symbol = (String) symbolCombo.getSelectedItem();
            if (symbol != null) {
                Stock stock = market.getStock(symbol);
                if (stock != null) {
                    priceField.setText(String.format("%.2f", stock.getPrice()));
                }
            }
        });
        
        // Set initial price
        if (symbolCombo.getItemCount() > 0) {
            String symbol = (String) symbolCombo.getSelectedItem();
            if (symbol != null) {
                Stock stock = market.getStock(symbol);
                if (stock != null) {
                    priceField.setText(String.format("%.2f", stock.getPrice()));
                }
            }
        }
        
        formPanel.add(symbolLabel);
        formPanel.add(symbolCombo);
        formPanel.add(quantityLabel);
        formPanel.add(quantityField);
        formPanel.add(priceLabel);
        formPanel.add(priceField);
        formPanel.add(typeLabel);
        formPanel.add(typeCombo);
        
        // Execute button
        JButton executeButton = new JButton("Execute Trade");
        executeButton.addActionListener(e -> {
            try {
                String symbol = (String) symbolCombo.getSelectedItem();
                int quantity = Integer.parseInt(quantityField.getText());
                double price = Double.parseDouble(priceField.getText());
                boolean isBuy = typeCombo.getSelectedIndex() == 0;
                
                boolean success;
                if (isBuy) {
                    success = currentAccount.buyStock(symbol, quantity, price);
                } else {
                    success = currentAccount.sellStock(symbol, quantity, price);
                }
                
                if (success) {
                    JOptionPane.showMessageDialog(this, 
                            "Trade executed successfully!", 
                            "Trade Confirmation", 
                            JOptionPane.INFORMATION_MESSAGE);
                    updateAccountPanel();
                } else {
                    JOptionPane.showMessageDialog(this, 
                            "Trade failed. Please check your account balance or holdings.", 
                            "Trade Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                        "Invalid quantity or price. Please enter numeric values.", 
                        "Input Error", 
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        
        formPanel.add(new JLabel()); // Empty cell for spacing
        formPanel.add(executeButton);
        
        panel.add(formPanel, BorderLayout.NORTH);
        
        return panel;
    }

    // ensure left panel has a minimum width
    private void setupResizeListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // maintain the left panel at constant width
                int currentDividerLocation = splitPane.getDividerLocation();
                if (currentDividerLocation != LEFT_PANEL_WIDTH) {
                    // only reset if user hasn't manually moved the divider
                    // might need adjusting idk
                    SwingUtilities.invokeLater(() -> {
                        if (splitPane.getDividerLocation() < MIN_LEFT_WIDTH) {
                            splitPane.setDividerLocation(MIN_LEFT_WIDTH);
                        } else if (getWidth() - splitPane.getDividerLocation() < MIN_RIGHT_WIDTH) {
                            splitPane.setDividerLocation(getWidth() - MIN_RIGHT_WIDTH);
                        }
                    });
                }
            }
        });
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // Account menu
        JMenu accountMenu = new JMenu("Account");
        JMenuItem newAccountItem = new JMenuItem("New Account");
        newAccountItem.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter account name:");
            if (name != null && !name.trim().isEmpty()) {
                currentAccount = accountManager.createAccount(name, 10000.0);
                updateAccountPanel();
            }
        });
        
        JMenuItem switchAccountItem = new JMenuItem("Switch Account");
        switchAccountItem.addActionListener(e -> {
            // Create a list of account names
            String[] accountNames = accountManager.getAllAccounts().stream()
                    .map(Account::getName)
                    .toArray(String[]::new);
            
            if (accountNames.length == 0) {
                JOptionPane.showMessageDialog(this, "No accounts available.");
                return;
            }
            
            // Show selection dialog
            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Select an account:",
                    "Switch Account",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    accountNames,
                    accountNames[0]);
            
            if (selected != null) {
                // Find the account with this name
                for (Account account : accountManager.getAllAccounts()) {
                    if (account.getName().equals(selected)) {
                        currentAccount = account;
                        updateAccountPanel();
                        break;
                    }
                }
            }
        });
        
        accountMenu.add(newAccountItem);
        accountMenu.add(switchAccountItem);
        
        // Add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(accountMenu);
        
        setJMenuBar(menuBar);
    }

    // implement the SymbolSelectionListener interface
    @Override
    public void onSymbolSelected(TradeItem item) {
        chartPanel.openChart(symbolPanel.getReader(), item.getSymbol());
        
        // Switch to chart tab
        rightTabPane.setSelectedIndex(0);
    }
}
