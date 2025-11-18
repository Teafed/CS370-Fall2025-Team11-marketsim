    // symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

    package com.gui;

    import com.models.*;
    import com.models.market.MarketListener;
    import com.models.market.TradeItem;
    import com.models.profile.Account;

    import javax.swing.*;
    import java.awt.*;
    import java.util.ArrayList;
    import java.util.List;

    public class SymbolPanel extends ContentPanel {
        private final DefaultListModel<TradeItem> symbolModel = new DefaultListModel<>();
        private final JList<TradeItem> symbolList = new JList<>(symbolModel);
        private final List<SymbolSelectionListener> symbolListener;
        private Account account;
        private AccountSelectionListener accountListener;
        private java.lang.String lastNotifiedSymbol = null;
        private AccountBar accountBar;

        // interface that listeners must implement
        public interface SymbolSelectionListener {
            void onSymbolSelected(TradeItem symbol);
        }
        public interface AccountSelectionListener {
            void onAccountBarSelected(Account account);
        }

        public SymbolPanel(ModelFacade model) {
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

        public void setSymbols(List<TradeItem> symbols) {
            symbolModel.clear();
            symbols.forEach(symbolModel::addElement);
            lastNotifiedSymbol = null;
            symbolList.revalidate();
            symbolList.repaint();
        }

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

        public java.lang.String getSelectedSymbol() {
            TradeItem selected = symbolList.getSelectedValue();
            return selected != null ? selected.getSymbol() : null;
        }

        public void clearSelection() {
            symbolList.clearSelection();
            lastNotifiedSymbol = null;
        }
    }