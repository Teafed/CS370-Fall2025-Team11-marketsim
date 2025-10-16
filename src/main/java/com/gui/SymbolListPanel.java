// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

package com.gui;

import com.market.DatabaseManager;
import com.market.TradeItem;
import com.market.Stock;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SymbolListPanel extends ContentPanel {
    private DefaultListModel<TradeItem> symbolModel;
    private JList<TradeItem> symbolList;
    private final List<SymbolSelectionListener> listeners;
    private final DatabaseManager db;

    // interface that listeners must implement
    public interface SymbolSelectionListener {
        void onSymbolSelected(TradeItem symbol);
    }

    public SymbolListPanel(DatabaseManager db) {
        this.db = db;
        this.listeners = new ArrayList<>();
        initializeComponents();
        loadSymbolsFromDb();   // NEW
        setupListeners();
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
    }

    /**
     * load symbols from database
     */
    private void loadSymbolsFromDb() {
        symbolModel.clear();
        try {
            for (String sym : db.listSymbols()) {
                double[] lp = db.latestAndPrevClose(sym); // [last, prev]
                int last = (int)lp[0], prev = (int)lp[1];
                double pct = (Double.isNaN(last) || Double.isNaN(prev) || prev == 0)
                        ? 0.0
                        : (last - prev) / prev * 100.0;

                symbolModel.addElement(new Stock(sym, sym));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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
        loadSymbolsFromDb();
    }

    public String getSelectedSymbol() {
        TradeItem selected = symbolList.getSelectedValue();
        return selected != null ? selected.getSymbol() : null;
    }

    public void clearSelection() {
        symbolList.clearSelection();
    }
}