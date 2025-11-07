package com.gui;

import com.accountmanager.Account;
import com.accountmanager.Portfolio;
import com.accountmanager.Position;
import com.etl.MarketPriceFetcher;
import com.market.TradeItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
// ...existing code...
public class PortfolioPanel extends ContentPanel {

    private Account account; // Assume this is passed in or set
    private JTable portfolioTable;
    private DefaultTableModel tableModel;
    private final MarketPriceFetcher priceFetcher = new MarketPriceFetcher();

    public PortfolioPanel(Account account) {
        this.account = account;
        setLayout(new BorderLayout());
    // Use the same dark background as other panels for a consistent theme
    setBackground(GUIComponents.BG_DARK);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        initializeTable();
        updatePortfolioDisplay(); // Initial display
    }

    private void initializeTable() {
        // Define table columns (added Avg Cost column)
        String[] columnNames = {"Symbol", "Quantity", "Avg Cost", "Current Price", "Total Value"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        portfolioTable = new JTable(tableModel);

        // Table style to match dark theme
        portfolioTable.setBackground(GUIComponents.BG_MEDIUM);
        portfolioTable.setForeground(GUIComponents.TEXT_PRIMARY);
        portfolioTable.setShowGrid(false);
        portfolioTable.setRowHeight(28);
        portfolioTable.setFillsViewportHeight(true);

        if (portfolioTable.getTableHeader() != null) {
            portfolioTable.getTableHeader().setBackground(GUIComponents.BG_DARK);
            portfolioTable.getTableHeader().setForeground(GUIComponents.TEXT_PRIMARY);
        }

        // Add table to a themed scroll pane
        JScrollPane scrollPane = GUIComponents.createScrollPane(portfolioTable);
        scrollPane.getViewport().setBackground(GUIComponents.BG_DARK);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updatePortfolioDisplay() {
        // Clear existing data
        tableModel.setRowCount(0);

        if (account == null) {
            // Handle case where account is not set
            return;
        }

        Portfolio portfolio = account.getPortfolio();
        List<TradeItem> holdings = portfolio.listTradeItems();

        // For each holding, fetch price asynchronously to avoid blocking the EDT.
        for (TradeItem item : holdings) {
            String symbol = item.getSymbol();
            int quantity = portfolio.getNumberOfShares(item);
            Position pos = portfolio.getPosition(item);
            Double avgCost = pos == null ? null : pos.getAverageCost();

            // Use cached or live price fetcher (async)
            priceFetcher.fetchCurrentPriceAsync(symbol).thenAccept(price -> {
                double currentPrice = price != null ? price : item.getCurrentPrice();
                double totalValue = currentPrice * quantity;

                // Ensure UI updates happen on the Event Dispatch Thread
                javax.swing.SwingUtilities.invokeLater(() -> {
                    tableModel.addRow(new Object[]{
                            symbol,
                            quantity,
                            avgCost == null ? "N/A" : String.format("%.2f", avgCost),
                            String.format("%.2f", currentPrice),
                            String.format("%.2f", totalValue)
                    });
                });
            });
        }

        // You might also want to display total portfolio value and cash balance
        // double totalPortfolioValue = account.getTotalValue(); // This includes cash
        // double cashBalance = account.getAvailableBalance();
        // Add these to a separate panel or labels if desired.
    }
}