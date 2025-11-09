package com.gui;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import com.accountmanager.Portfolio;
import com.market.TradeItem;

public class PortfolioPanel extends ContentPanel {
    private JTable holdingsTable;
    private DefaultTableModel tableModel;

    public PortfolioPanel() {
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_LIGHTER);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        initializeComponents();
    }

    private void initializeComponents() {
        // Table to display portfolio holdings
        String[] columnNames = {"Symbol", "Name", "Shares", "Price", "Total"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

    holdingsTable = new JTable(tableModel);
    holdingsTable.setFillsViewportHeight(true);
    holdingsTable.setRowHeight(24);
    holdingsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    holdingsTable.setGridColor(GUIComponents.BORDER_COLOR);

    // Colors to match the dark theme
    holdingsTable.setBackground(GUIComponents.BG_DARK);
    holdingsTable.setForeground(GUIComponents.TEXT_PRIMARY);
    holdingsTable.setSelectionBackground(GUIComponents.ACCENT_BLUE);
    holdingsTable.setSelectionForeground(Color.WHITE);
    holdingsTable.setShowGrid(true);

    // Header styling
    javax.swing.table.JTableHeader header = holdingsTable.getTableHeader();
    header.setBackground(GUIComponents.BG_MEDIUM);
    header.setForeground(GUIComponents.TEXT_PRIMARY);
    header.setFont(new Font("Segoe UI", Font.BOLD, 13));

    // Right-align numeric columns (Shares, Price, Total)
    javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
    holdingsTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
    holdingsTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
    holdingsTable.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

    JScrollPane scrollPane = GUIComponents.createScrollPane(holdingsTable);
    add(scrollPane, BorderLayout.CENTER);
    }

    // TODO: Add methods to display portfolio holdings
    /**
     * Update the portfolio table with data from a Portfolio object.
     * This will clear existing rows and repopulate the table.
     */
    public void updatePortfolio(Portfolio portfolio) {
        // clear existing rows
        tableModel.setRowCount(0);
        if (portfolio == null) return;

        List<TradeItem> items = portfolio.listTradeItems();
        for (TradeItem item : items) {
            int shares = portfolio.getNumberOfShares(item);
            double price = item.getCurrentPrice();
            double total = price * shares;
            String symbol = item.getSymbol();
            String name = item.getName();

            Object[] row = {symbol, name, shares, String.format("$%.2f", price), String.format("$%.2f", total)};
            tableModel.addRow(row);
        }
    }
}