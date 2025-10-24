// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

package com.gui;

import com.market.*;
import com.accountmanager.Account;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SymbolListPanel extends ContentPanel implements MarketListener {
    private final DefaultListModel<TradeItem> symbolModel = new DefaultListModel<>();
    private final JList<TradeItem> symbolList = new JList<>(symbolModel);
    private final List<SymbolSelectionListener> symbolListener;
    private final DatabaseManager db;
    private Account account;
    private AccountSelectionListener accountListener;
    private AccountBar accountBar;

    @Override
    public void onMarketUpdate() {
        repaint();
    }

    // interface that listeners must implement
    public interface SymbolSelectionListener {
        void onSymbolSelected(TradeItem symbol);
    }
    public interface AccountSelectionListener {
        void onAccountSelected(Account account);
    }

    public SymbolListPanel(DatabaseManager db) {
        this.db = db;
        this.symbolListener = new ArrayList<>();
        initializeComponents();

        setupListeners();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_MEDIUM);
        setBorder(GUIComponents.createBorder());

        GUIComponents.createList(symbolModel);

        symbolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        symbolList.setCellRenderer(new SymbolCellRenderer());
        symbolList.setFixedCellHeight(50);

        JScrollPane scrollPane = GUIComponents.createScrollPane(symbolList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.add(symbolList, BorderLayout.NORTH);
        listWrapper.setOpaque(true);
        listWrapper.setBackground(GUIComponents.BG_MEDIUM);

        scrollPane.setViewportView(listWrapper);

        accountBar = new AccountBar();
        accountBar.setVisible(false); // setAccount() will make this visible

        add(accountBar, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadSymbols(List<TradeItem> symbols) {
        symbols.forEach(s -> symbolModel.addElement(s));
    }

    public void setAccount(Account account, AccountSelectionListener listener) {
        this.account = account;
        this.accountListener = listener;

        accountBar.setAccount(account);
        accountBar.setOnClick(() -> {
            if (this.accountListener != null) {
                this.accountListener.onAccountSelected(this.account);
            }
        });
        accountBar.setVisible(true);
        revalidate();
        repaint();
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
        symbolListener.add(listener);
    }

    public void removeSymbolSelectionListener(SymbolSelectionListener listener) {
        symbolListener.remove(listener);
    }

    private void notifyListeners(TradeItem symbol) {
        for (SymbolSelectionListener listener : symbolListener) {
            listener.onSymbolSelected(symbol);
        }
    }

    // utility methods
    public void refreshSymbols() {
        repaint();
    }

    public String getSelectedSymbol() {
        TradeItem selected = symbolList.getSelectedValue();
        return selected != null ? selected.getSymbol() : null;
    }

    public void clearSelection() {
        symbolList.clearSelection();
    }
}