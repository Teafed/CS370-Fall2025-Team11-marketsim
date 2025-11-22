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
    private final DefaultListModel<TradeItem> watchlistModel = new DefaultListModel<>();
    private final JList<TradeItem> watchlistSymbols = new JList<>(watchlistModel);
    private final DefaultListModel<TradeItem> portfolioModel = new DefaultListModel<>();
    private final JList<TradeItem> portfolioSymbols = new JList<>(portfolioModel);
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

            GUIComponents.createList(watchlistModel);
            GUIComponents.createList(portfolioModel);

            watchlistSymbols.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            watchlistSymbols.setCellRenderer(new SymbolCellRenderer());
            watchlistSymbols.setFixedCellHeight(50);

            portfolioSymbols.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            portfolioSymbols.setCellRenderer(new SymbolCellRenderer());
            portfolioSymbols.setFixedCellHeight(50);

            JScrollPane scrollPane = GUIComponents.createScrollPane(watchlistSymbols);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            JPanel listWrapper = new JPanel(new BorderLayout());
            listWrapper.add(watchlistSymbols, BorderLayout.NORTH);
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

        public void setWatchlistSymbols(List<TradeItem> symbols) {
            watchlistModel.clear();
            symbols.forEach(watchlistModel::addElement);
            lastNotifiedSymbol = null;
            watchlistSymbols.revalidate();
            watchlistSymbols.repaint();
        }

        public void setPortfolioSymbols(List<TradeItem> symbols) {
            portfolioModel.clear();
            symbols.forEach(portfolioModel::addElement);
            lastNotifiedSymbol = null;
            portfolioSymbols.revalidate();
            portfolioSymbols.repaint();
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
            watchlistSymbols.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) { // only fire when selection is final
                    TradeItem selected = watchlistSymbols.getSelectedValue();
                    if (selected != null) {
                        java.lang.String sym = selected.getSymbol();
                        if (!sym.equals(lastNotifiedSymbol)) { // guard against reloading same
                            lastNotifiedSymbol = sym;
                            notifyListeners(selected);
                        }
                    }
                }
            });
            watchlistSymbols.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) showContextMenu(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) showContextMenu(e);
                }

                private void showContextMenu(MouseEvent e) {
                    int index = watchlistSymbols.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        watchlistSymbols.setSelectedIndex(index);
                        TradeItem item = watchlistModel.getElementAt(index);

                        JPopupMenu menu = new JPopupMenu();
                        JMenuItem removeItem = new JMenuItem("Remove from Watchlist");
                        removeItem.addActionListener(ev -> {
                            watchlistModel.removeElementAt(index);
                            watchlistSymbols.clearSelection();
                            lastNotifiedSymbol = null;
                            model.removeFromWatchlist(item);
                        });

                        menu.add(removeItem);
                        menu.show(watchlistSymbols, e.getX(), e.getY());
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

        public java.lang.String getSelectedSymbol() {
            TradeItem selected = watchlistSymbols.getSelectedValue();
            return selected != null ? selected.getSymbol() : null;
        }

        public void clearSelection() {
            watchlistSymbols.clearSelection();
            lastNotifiedSymbol = null;
        }

        public void selectFirst() {
            if (watchlistModel.getSize() > 0) {
                SwingUtilities.invokeLater(() -> {
                    watchlistSymbols.setSelectedIndex(0);
                    watchlistSymbols.ensureIndexIsVisible(0);
                });
            }
        }
    }