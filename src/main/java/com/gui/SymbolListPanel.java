// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

package com.gui;

import com.etl.ReadData;
import com.market.Market;
import com.market.TradeItem;
import com.market.Stock;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SymbolListPanel extends JPanel {
    private DefaultListModel<TradeItem> symbolModel;
    private JList<TradeItem> symbolList;
    private final List<SymbolSelectionListener> listeners;
    private final String dataFolderPath;
    private ReadData reader;
    private Timer refreshTimer;
    private JButton addSymbolButton;
    private JButton refreshButton;

    // interface that listeners must implement
    public interface SymbolSelectionListener {
        void onSymbolSelected(TradeItem symbol);
    }

    public SymbolListPanel(String dataFolderPath) {
        this.dataFolderPath = dataFolderPath;
        this.listeners = new ArrayList<>();
        try {
            reader = new ReadData(dataFolderPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeComponents();
        loadSymbols();
        setupListeners();
        startAutoRefresh();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_MEDIUM);
        setBorder(GUIComponents.createBorder());

        symbolModel = new DefaultListModel<>();
        symbolList = GUIComponents.createList(symbolModel);

        symbolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        symbolList.setCellRenderer(new SymbolCellRenderer());
        symbolList.setFixedCellHeight(50);

        JScrollPane scrollPane = GUIComponents.createScrollPane(symbolList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
        
        // Add control panel at the bottom
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        controlPanel.setBackground(GUIComponents.BG_MEDIUM);
        
        addSymbolButton = new JButton("Add Symbol");
        addSymbolButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddSymbolDialog();
            }
        });
        
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSymbols();
            }
        });
        
        controlPanel.add(addSymbolButton);
        controlPanel.add(refreshButton);
        
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void loadSymbols() {
        symbolModel.clear();
        File dataFolder = new File(dataFolderPath);
        if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            System.out.println("data directory dne");
            return;
        }

        File[] csvFiles = dataFolder.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null || csvFiles.length == 0) {
            System.out.println("data files dne");
            return;
        }

        System.out.println("loading symbols from: " + dataFolderPath);
        System.out.println("found " + csvFiles.length + " csv files");
        for (File f : csvFiles) {
            System.out.println("   " + f.getName());
        }

        // sort and add symbols
        Arrays.sort(csvFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // Load symbols from Market instead of creating temporary ones
        Market market = Market.getInstance();
        for (File file : csvFiles) {
            String fileName = file.getName();
            String symbol = fileName.substring(0, fileName.lastIndexOf('.'));
            
            Stock stock = market.getStock(symbol);
            if (stock != null) {
                addSymbolIfNotExists(stock);
            }
        }
    }
    
    /**
     * Add a symbol to the list if it doesn't already exist
     */
    public void addSymbol(TradeItem item) {
        addSymbolIfNotExists(item);
    }
    
    private void addSymbolIfNotExists(TradeItem item) {
        // Check if symbol already exists in the list
        for (int i = 0; i < symbolModel.size(); i++) {
            if (symbolModel.getElementAt(i).getSymbol().equals(item.getSymbol())) {
                return; // Symbol already exists
            }
        }
        symbolModel.addElement(item);
    }
    
    private void showAddSymbolDialog() {
        String symbol = JOptionPane.showInputDialog(
                this, 
                "Enter stock symbol to add:", 
                "Add Symbol", 
                JOptionPane.PLAIN_MESSAGE);
        
        if (symbol != null && !symbol.trim().isEmpty()) {
            symbol = symbol.trim().toUpperCase();
            Market market = Market.getInstance();
            
            // Check if the symbol already exists in the market
            Stock stock = market.getStock(symbol);
            
            if (stock == null) {
                // If not, add it to the market
                stock = new Stock(symbol, symbol);
                market.addStock(symbol, stock);
                // startDataCollection requires a DatabaseManager parameter
                stock.startDataCollection(market.getDatabaseManager());
            }
            
            // Add to the symbol list
            addSymbol(stock);
        }
    }

    private void setupListeners() {
        symbolList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // only fire when selection is final
                TradeItem selectedSymbol = symbolList.getSelectedValue();
                if (selectedSymbol != null) {
                    notifyListeners(selectedSymbol);
                }
            }
        });
    }
    
    private void startAutoRefresh() {
        // Refresh the list every 5 seconds to update prices
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    // Store the current selection
                    TradeItem selectedItem = symbolList.getSelectedValue();
                    
                    // Update the list
                    updateSymbolData();
                    
                    // Restore selection if possible
                    if (selectedItem != null) {
                        for (int i = 0; i < symbolModel.size(); i++) {
                            if (symbolModel.getElementAt(i).getSymbol().equals(selectedItem.getSymbol())) {
                                symbolList.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                });
            }
        }, 5000, 5000);
    }
    
    private void updateSymbolData() {
        // This method updates the data for all symbols without clearing the model
        Market market = Market.getInstance();
        
        for (int i = 0; i < symbolModel.size(); i++) {
            TradeItem item = symbolModel.getElementAt(i);
            Stock stock = market.getStock(item.getSymbol());
            
            if (stock != null) {
                // The model will be updated automatically when the stock price changes
                symbolModel.set(i, stock);
            }
        }
        
        // Force the list to repaint
        symbolList.repaint();
    }

    // methods for managing listeners
    public void addSymbolSelectionListener(SymbolSelectionListener listener) {
        listeners.add(listener);
    }

    public void removeSymbolSelectionListener(SymbolSelectionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(TradeItem symbol) {
        for (SymbolSelectionListener listener : listeners) {
            listener.onSymbolSelected(symbol);
        }
    }

    // utility methods
    public void refreshSymbols() {
        updateSymbolData();
    }

    public String getSelectedSymbol() {
        TradeItem selected = symbolList.getSelectedValue();
        return selected != null ? selected.getSymbol() : null;
    }

    public void clearSelection() {
        symbolList.clearSelection();
    }

    /** Helper: return the underlying ReadData instance */
    public ReadData getReader() {
        return reader;
    }
    
    /**
     * Clean up resources when the panel is no longer needed
     */
    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}