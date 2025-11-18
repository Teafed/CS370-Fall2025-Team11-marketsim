package com.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

import com.models.AccountDTO;
import com.models.ModelFacade;
import com.models.ModelListener;
import com.models.market.TradeItem;

public class OrderHistoryTab extends ContentPanel implements ModelListener {
    private final ModelFacade model;
    private JTable holdingsTable;
    private DefaultTableModel tableModel;
    private final SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public OrderHistoryTab(ModelFacade model) {
        this.model = model;
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_LIGHTER);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        initializeComponents();
        model.addListener(this);
        refresh();
    }

    private void initializeComponents() {
        // Table to display portfolio holdings
        java.lang.String[] columnNames = {"Time", "Price", "Shares", "P/L"};
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

    public void refresh() {
        tableModel.setRowCount(0);
        try {
            List<ModelFacade.TradeRow> rows = model.getRecentTrades(50);
            for (var r : rows) {
                double cashDelta = ("BUY".equals(r.side()) ? -1.0 : 1.0) * (r.quantity() * r.price());
                Object[] line = {
                        tsFmt.format(new java.util.Date(r.timestamp())),
                        r.side(),
                        r.symbol(),
                        r.quantity(),
                        String.format("$%.2f", r.price()),
                        String.format("%+.2f", cashDelta),
                        r.posAfter() // qty after trade (filled in by DB or façade)
                };
                tableModel.addRow(line);
            }
        } catch (Exception e) {
            // optionally show a toast
        }
    }


    // ModelListener
    @Override public void onAccountChanged(AccountDTO snapshot) { refresh(); }
    @Override public void onQuotesUpdated() { /* not needed */ }
    @Override public void onWatchlistChanged(java.util.List<com.models.market.TradeItem> items) { /* not needed */ }
    @Override public void onError(String message, Throwable t) { /* optional */ }
}