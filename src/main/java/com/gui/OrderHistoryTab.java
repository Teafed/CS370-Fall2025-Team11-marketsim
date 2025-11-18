package com.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.List;
import java.util.ArrayList;

import com.models.market.Market;
import com.models.market.TradeItem;
import com.models.profile.Account;
import com.models.profile.Portfolio;

public class OrderHistoryTab extends ContentPanel {
    private JTable holdingsTable;
    private DefaultTableModel tableModel;
    private final Market market;
    private final Account account;
    private List<TradeItem> items = new ArrayList<>();

    /**
     * No-arg constructor to allow UI components to instantiate a placeholder panel
     * when the Market/Account are not yet available (e.g. during initial layout).
     * Delegates to the main constructor with nulls.
     */
    public OrderHistoryTab() {
        this(null, null);
    }

    public OrderHistoryTab(Market market, Account account) {
        this.market = market;
        this.account = account;
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
        java.lang.String[] columnNames = {"Time", "Price", "Shares", "ROI"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        holdingsTable = new JTable(tableModel);
        holdingsTable.setFillsViewportHeight(true);
        holdingsTable.setRowHeight(24);
        holdingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        holdingsTable.setGridColor(GUIComponents.BORDER_COLOR);

        // Colors to match the dark theme
        holdingsTable.setBackground(GUIComponents.BG_DARK);
        holdingsTable.setForeground(GUIComponents.TEXT_PRIMARY);
        holdingsTable.setSelectionBackground(GUIComponents.ACCENT_BLUE);
        holdingsTable.setSelectionForeground(Color.WHITE);
        holdingsTable.setShowGrid(true);

        // Header styling
        JTableHeader header = holdingsTable.getTableHeader();
        header.setBackground(GUIComponents.BG_MEDIUM);
        header.setForeground(GUIComponents.TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Right-align numeric columns (Shares, Price, Total)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        holdingsTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        holdingsTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = GUIComponents.createScrollPane(holdingsTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Update the portfolio table with data from a Portfolio object.
     * This will clear existing rows and repopulate the table.
     */
    public void updateTable() {
        // clear existing rows
        Portfolio p = account.getPortfolio();
        tableModel.setRowCount(0);
        if (p == null) return;
        items = p.listTradeItems();
        for (TradeItem item : items) {
            int shares = p.getNumberOfShares(item);
            java.lang.String symbol = item.getSymbol();
            java.lang.String name = item.getName();
            double price = Double.NaN;
            // Prefer market's live price if available
            if (market != null) {
                double pr = market.getPrice(symbol);
                if (!Double.isNaN(pr)) price = pr;
            }
            if (Double.isNaN(price)) price = 0.0; // fallback
            double total = price * shares;

            Object[] row = {symbol, name, shares, java.lang.String.format("$%.2f", price), java.lang.String.format("$%.2f", total)};
            tableModel.addRow(row);
        }
    }
}