// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

package com.gui;

import com.models.*;
import com.models.market.TradeItem;
import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel displaying a list of stock symbols.
 * Allows selecting a symbol to view its chart and details.
 * Also contains the account bar and search panel.
 */
public class SymbolPanel extends ContentPanel {
    private final DefaultListModel<TradeItem> symbolModel = new DefaultListModel<>();
    private final JList<TradeItem> symbolList = new JList<>(symbolModel);
    private final List<SymbolSelectionListener> symbolListener;
    private Account account;
    private AccountSelectionListener accountListener;
    private String lastNotifiedSymbol = null;
    private AccountBar accountBar;
    private ModelFacade model;
    private LogoCache logoCache;

    // interface that listeners must implement
    /**
     * Listener interface for symbol selection events.
     */
    public interface SymbolSelectionListener {
        /**
         * Called when a symbol is selected.
         *
         * @param symbol The selected TradeItem.
         */
        void onSymbolSelected(TradeItem symbol);
    }

    /**
     * Listener interface for account bar selection events.
     */
    public interface AccountSelectionListener {
        /**
         * Called when the account bar is selected.
         *
         * @param account The selected Account.
         */
        void onAccountBarSelected(Account account);
    }

    /**
     * Constructs a new SymbolPanel.
     *
     * @param model The ModelFacade instance.
     */
    public SymbolPanel(ModelFacade model) {
        this.model = model;
        this.symbolListener = new ArrayList<>();
        this.logoCache = new LogoCache(40);
        initializeComponents();

        setupListeners();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_MEDIUM);
        setBorder(GUIComponents.createBorder());

        GUIComponents.createList(symbolModel);

        symbolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        symbolList.setCellRenderer(new SymbolCellRenderer(logoCache, model));
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

        SearchPanel searchBar = new SearchPanel(model);
        searchBar.setVisible(true);

        add(searchBar, BorderLayout.NORTH);
        add(accountBar, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates the list of symbols displayed in the panel.
     *
     * @param symbols The list of TradeItems to display.
     */
    public void setSymbols(List<TradeItem> symbols) {
        symbolModel.clear();
        symbols.forEach(symbolModel::addElement);
        lastNotifiedSymbol = null;
        symbolList.revalidate();
        symbolList.repaint();
    }

    /**
     * Sets the active account and listener for the account bar.
     *
     * @param account  The Account to display.
     * @param listener The listener for account bar clicks.
     */
    public void setAccount(Account account, AccountSelectionListener listener) {
        this.account = account;
        this.accountListener = listener;

        accountBar.setAccount(account);
        accountBar.setOnClick(() -> {
            if (this.accountListener != null) {
                this.accountListener.onAccountBarSelected(this.account);
            }
        });
        accountBar.setVisible(true);
        revalidate();
        repaint();
    }

    private void setupListeners() {
        symbolList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // only fire when selection is final
                TradeItem selected = symbolList.getSelectedValue();
                if (selected != null) {
                    java.lang.String sym = selected.getSymbol();
                    if (!sym.equals(lastNotifiedSymbol)) { // guard against reloading same
                        lastNotifiedSymbol = sym;
                        notifyListeners(selected);
                    }
                }
            }
        });
        symbolList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            private void showContextMenu(MouseEvent e) {
                int index = symbolList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    symbolList.setSelectedIndex(index);
                    TradeItem item = symbolModel.getElementAt(index);

                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem removeItem = new JMenuItem("Remove from Watchlist");
                    removeItem.addActionListener(ev -> {
                        symbolModel.removeElementAt(index);
                        symbolList.clearSelection();
                        lastNotifiedSymbol = null;
                        try {
                            model.removeFromWatchlist(item);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });

                    menu.add(removeItem);
                    menu.show(symbolList, e.getX(), e.getY());
                }
            }
        });
    }

    // methods for managing listeners
    /**
     * Adds a listener for symbol selection events.
     *
     * @param listener The listener to add.
     */
    public void addSymbolSelectionListener(SymbolSelectionListener listener) {
        symbolListener.add(listener);
    }

    /**
     * Removes a listener for symbol selection events.
     *
     * @param listener The listener to remove.
     */
    public void removeSymbolSelectionListener(SymbolSelectionListener listener) {
        symbolListener.remove(listener);
    }

    private void notifyListeners(TradeItem symbol) {
        for (SymbolSelectionListener listener : symbolListener) {
            listener.onSymbolSelected(symbol);
        }
    }

    /**
     * Gets the currently selected symbol.
     *
     * @return The symbol string, or null if none selected.
     */
    public String getSelectedSymbol() {
        TradeItem selected = symbolList.getSelectedValue();
        return selected != null ? selected.getSymbol() : null;
    }

    /**
     * Clears the current symbol selection.
     */
    public void clearSelection() {
        symbolList.clearSelection();
        lastNotifiedSymbol = null;
    }

    /**
     * Selects the first symbol in the list, if available.
     */
    public void selectFirst() {
        if (symbolModel.getSize() > 0) {
            SwingUtilities.invokeLater(() -> {
                symbolList.setSelectedIndex(0);
                symbolList.ensureIndexIsVisible(0);
            });
        }
    }
}