// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

package com.gui.navigation;

import com.gui.*;
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
    private final DefaultListModel<SymbolListEntry> listModel = new DefaultListModel<>();
    private final JList<SymbolListEntry> listSymbols = new JList<>(listModel);
    private final SectionHeader watchlistHeader = new SectionHeader("Watchlist");
    private final SectionHeader portfolioHeader = new SectionHeader("Portfolio");
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
        void onSymbolSelected(TradeItem symbol);
    }

    /**
     * Listener interface for account bar selection events.
     */
    public interface AccountSelectionListener {
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

        GUIComponents.createList(listModel);

        listSymbols.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listSymbols.setCellRenderer(new CellRenderer(new SymbolCellRenderer(model, logoCache)));
        listSymbols.setFixedCellHeight(-1);


        // Headings
        JLabel watchlistHeading = new JLabel("Watchlist");
        watchlistHeading.setFont(watchlistHeading.getFont().deriveFont(Font.BOLD, 14f));
        watchlistHeading.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel portfolioHeading = new JLabel("Portfolio");
        portfolioHeading.setFont(portfolioHeading.getFont().deriveFont(Font.BOLD, 14f));
        portfolioHeading.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


        JScrollPane scrollPane = GUIComponents.createScrollPane(listSymbols);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.add(listSymbols, BorderLayout.NORTH);
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

    public void buildList(List<TradeItem> watchlist, List<TradeItem> portfolio) {
        listModel.clear();

        // only show portfolio header if it has items
        if (portfolio != null && !portfolio.isEmpty()) {
            listModel.addElement(portfolioHeader);
            if (!portfolioHeader.isCollapsed()) {
                portfolio.forEach(listModel::addElement);
            }
        }
        listModel.addElement(watchlistHeader);
        if (!watchlistHeader.isCollapsed()) {
            for (TradeItem ti : watchlist) {
                if (!portfolio.contains(ti)) {
                    listModel.addElement(ti);
                }
            }
        }
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
        listSymbols.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SymbolListEntry selected = listSymbols.getSelectedValue();
                if (selected instanceof TradeItem item) {
                    String sym = item.getSymbol();
                    if (!sym.equals(lastNotifiedSymbol)) {
                        lastNotifiedSymbol = sym;
                        notifyListeners(item);
                    }
                }
            }
        });

        listSymbols.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = listSymbols.locationToIndex(e.getPoint());
                if (index >= 0) {
                    SymbolListEntry entry = listModel.getElementAt(index);
                    if (entry instanceof SectionHeader header) {
                        header.toggleCollapsed();
                        buildList(model.getWatchlist(), model.getPortfolioItems());
                    }
                }
            }
        });


        listSymbols.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showContextMenu(e);
            }

            private void showContextMenu(MouseEvent e) {
                int index = listSymbols.locationToIndex(e.getPoint());
                if (index >= 0) {
                    listSymbols.setSelectedIndex(index);
                    SymbolListEntry entry = listModel.getElementAt(index);

                    // Only allow context menu for watchlist TradeItems
                    if (entry instanceof TradeItem item) {
                        int portfolioHeaderIndex = listModel.indexOf(portfolioHeader);
                        int watchlistHeaderIndex = listModel.indexOf(watchlistHeader);

                        boolean inWatchlistSection = index > watchlistHeaderIndex;

                        if (inWatchlistSection) {
                            JPopupMenu menu = new JPopupMenu();
                            JMenuItem removeItem = new JMenuItem("Remove from Watchlist");
                            removeItem.addActionListener(ev -> {
                                listSymbols.clearSelection();
                                lastNotifiedSymbol = null;
                                try {
                                    model.removeFromWatchlist(item);
                                } catch (Exception ex) {
                                    throw new RuntimeException(ex);
                                }
                                buildList(model.getWatchlist(), model.getPortfolioItems());
                            });
                            menu.add(removeItem);
                            menu.show(listSymbols, e.getX(), e.getY());
                        }
                    }
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

    public String getSelectedSymbol() {
        TradeItem selected = (TradeItem) listSymbols.getSelectedValue();
        return selected != null ? selected.getSymbol() : null;
    }

    public void clearSelection() {
        listSymbols.clearSelection();
        lastNotifiedSymbol = null;
    }

    public void selectFirst() {
        if (listModel.getSize() > 0) {
            SwingUtilities.invokeLater(() -> {
                listSymbols.setSelectedIndex(1);
                listSymbols.ensureIndexIsVisible(1);
            });
        }
    }
}