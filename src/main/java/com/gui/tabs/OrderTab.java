package com.gui.tabs;

import com.gui.ContentPanel;
import com.gui.GUIComponents;
import com.models.ModelFacade;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.function.Supplier;

/**
 * A tab for placing buy/sell orders.
 * Displays current symbol info, price, and allows entering share quantity.
 */
public class OrderTab extends ContentPanel {
    ModelFacade model;
    Supplier<String> getSelectedSymbol;

    private final JLabel lblSymbol = new JLabel("-", SwingConstants.CENTER);
    private final JLabel lblPrice = new JLabel("-", SwingConstants.CENTER);
    private final JLabel lblCash = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblPos  = new JLabel("0", SwingConstants.LEFT);

    private final JTextField sharesField = new JTextField("0", 8);
    private final JLabel lblTotalPrice = new JLabel("$0.00", SwingConstants.RIGHT);

    private JButton buyButton;
    private JButton sellButton;
    private final Color buyColor  = GUIComponents.GREEN;
    private final Color sellColor = GUIComponents.RED;

    private JButton btn25;
    private JButton btn50;
    private JButton btnBuyMax;
    private JButton btnSellMax;

    private final DecimalFormat money = new DecimalFormat("$#,##0.00");
    private final DecimalFormat sharesFmt = new DecimalFormat("#,##0");

    private final Timer refreshTimer;

    /**
     * Constructs a new OrderTab.
     *
     * @param model             The ModelFacade instance.
     * @param getSelectedSymbol A supplier for the currently selected symbol.
     */
    public OrderTab(ModelFacade model, Supplier<String> getSelectedSymbol) {
        this.model = model;
        this.getSelectedSymbol = getSelectedSymbol;

        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_DARK);

        // Scrollable content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Symbol and Price display at top
        JPanel symbolPricePanel = createSymbolPricePanel();
        contentPanel.add(symbolPricePanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Number of shares input
        JPanel sharesPanel = createSharesPanel();
        contentPanel.add(sharesPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Total price display
        JPanel totalPanel = createTotalPricePanel();
        contentPanel.add(totalPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Buy and Sell buttons side by side
        JPanel buttonsPanel = createButtonsPanel();
        contentPanel.add(buttonsPanel);

        // Wrap in scroll pane for resizing
        JScrollPane scrollPane = GUIComponents.createScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        // Live refresh
        refreshTimer = new Timer(1000, e -> refreshReadouts());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        sharesField.getDocument().addDocumentListener(new SimpleDocListener() {
            @Override
            public void changed() {
                refreshReadouts();
            }
        });

        SwingUtilities.invokeLater(this::refreshReadouts);
    }

    private JPanel createSymbolPricePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        lblSymbol.setFont(new Font("Arial", Font.BOLD, 16));
        lblSymbol.setForeground(GUIComponents.TEXT_PRIMARY);

        lblPrice.setFont(new Font("Arial", Font.BOLD, 16));
        lblPrice.setForeground(GUIComponents.TEXT_PRIMARY);

        panel.add(lblSymbol, BorderLayout.WEST);
        panel.add(lblPrice, BorderLayout.EAST);

        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        buyButton = createActionButton("Buy", buyColor);
        sellButton = createActionButton("Sell", sellColor);

        buyButton.addActionListener(e -> submit(true));
        sellButton.addActionListener(e -> submit(false));

        panel.add(buyButton);
        panel.add(sellButton);

        return panel;
    }

    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 36));
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        button.setBackground(color);
        button.setForeground(Color.WHITE);

        return button;
    }

    private void updateButtonState(JButton button, boolean enabled, Color base) {
        button.setEnabled(enabled);

        if (enabled) {
            button.setBackground(base);
            button.setForeground(Color.WHITE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(base.darker(), 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
        } else {
            // Desaturate by mixing with panel background
            Color bg = GUIComponents.BG_MEDIUM;
            Color disabledBg = new Color(
                    (base.getRed()   + bg.getRed())   / 2,
                    (base.getGreen() + bg.getGreen()) / 2,
                    (base.getBlue()  + bg.getBlue())  / 2
            );
            button.setBackground(disabledBg);
            button.setForeground(GUIComponents.TEXT_SECONDARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
        }
    }

    private JPanel createSharesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Label
        JLabel label = new JLabel("Number of Shares");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(GUIComponents.TEXT_SECONDARY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        panel.add(label, BorderLayout.NORTH);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);

        // Shares field
        sharesField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sharesField.setBackground(GUIComponents.BG_MEDIUM);
        sharesField.setForeground(GUIComponents.TEXT_PRIMARY);
        sharesField.setCaretColor(GUIComponents.TEXT_PRIMARY);
        sharesField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        sharesField.setMaximumSize(new Dimension(120, 36));

        // Clear "0" on focus
        sharesField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("0".equals(sharesField.getText().trim())) {
                    sharesField.setText("");
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (sharesField.getText().trim().isEmpty()) {
                    sharesField.setText("0");
                }
            }
        });

        row.add(sharesField);
        row.add(Box.createHorizontalStrut(8));

        btn25 = createSmallSharesButton("25%", () -> applyFraction(0.25));
        btn50 = createSmallSharesButton("50%", () -> applyFraction(0.50));
        btnBuyMax = createSmallSharesButton("Buy Max", this::setBuyMax);
        btnSellMax = createSmallSharesButton("Sell Max", this::setSellMax);
        row.add(btn25);
        row.add(Box.createHorizontalStrut(4));
        row.add(btn50);
        row.add(Box.createHorizontalStrut(4));
        row.add(btnBuyMax);
        row.add(Box.createHorizontalStrut(4));
        row.add(btnSellMax);

        panel.add(row, BorderLayout.CENTER);

        return panel;
    }

    private JButton createSmallSharesButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setBackground(GUIComponents.BG_MEDIUM);
        b.setForeground(GUIComponents.TEXT_PRIMARY);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        b.addActionListener(e -> action.run());
        return b;
    }

    private void updateSmallButtonState(JButton b, boolean enabled) {
        b.setEnabled(enabled);

        if (enabled) {
            b.setBackground(GUIComponents.BG_MEDIUM);
            b.setForeground(GUIComponents.TEXT_PRIMARY);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        } else {
            // Desaturate manually
            Color bg = GUIComponents.BG_DARK;  // darker background
            b.setBackground(bg);
            b.setForeground(GUIComponents.TEXT_SECONDARY);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR.darker(), 1),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        }
    }


    private int getCurrentPosition(String sym) {
        if (sym == null) return 0;
        try {
            var dto = model.getAccountDTO();
            var pos = dto.positions();
            if (pos != null) {
                Integer q = pos.get(sym);
                return q == null ? 0 : q;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private double getCurrentCash() {
        try {
            return model.getAccountDTO().cash();
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private void applyFraction(double fraction) {
        String sym = symbol();
        if (sym == null || sym.isBlank()) return;

        double price = model.getPrice(sym);
        if (Double.isNaN(price) || price <= 0.0) return;

        int position = getCurrentPosition(sym);
        int base;

        if (position > 0) {
            // If we own shares, 25/50% of current position
            base = position;
        } else {
            // Otherwise, 25/50% of max affordable shares
            double cash = getCurrentCash();
            base = (int) Math.floor(cash / price);
        }

        int shares = (int) Math.floor(base * fraction);
        if (shares < 0) shares = 0;

        sharesField.setText(String.valueOf(shares));
        refreshReadouts();
    }

    private void setBuyMax() {
        String sym = symbol();
        if (sym == null || sym.isBlank()) return;

        double price = model.getPrice(sym);
        if (Double.isNaN(price) || price <= 0.0) return;

        double cash = getCurrentCash();
        int max = (int) Math.floor(cash / price);
        if (max < 0) max = 0;

        sharesField.setText(String.valueOf(max));
        refreshReadouts();
    }

    private void setSellMax() {
        String sym = symbol();
        if (sym == null || sym.isBlank()) return;

        int position = getCurrentPosition(sym);
        if (position <= 0) return;

        sharesField.setText(String.valueOf(position));
        refreshReadouts();
    }


    private JPanel createTotalPricePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(GUIComponents.BG_MEDIUM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel label = new JLabel("Total Price");
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(GUIComponents.TEXT_SECONDARY);

        lblTotalPrice.setFont(new Font("Arial", Font.BOLD, 14));
        lblTotalPrice.setForeground(GUIComponents.TEXT_PRIMARY);

        panel.add(label, BorderLayout.WEST);
        panel.add(lblTotalPrice, BorderLayout.EAST);

        return panel;
    }

    private void refreshReadouts() {
        String sym = symbol();
        lblSymbol.setText(sym == null ? "-" : sym);

        double price = (sym == null) ? Double.NaN : model.getPrice(sym);
        lblPrice.setText(Double.isNaN(price) ? "—" : money.format(price));

        int shares = parseShares();
        if (!Double.isNaN(price) && shares > 0) {
            double total = shares * price;
            lblTotalPrice.setText(money.format(total));
        } else {
            lblTotalPrice.setText("$0.00");
        }

        int qty = 0;
        try {
            var dto = model.getAccountDTO();
            var pos = dto.positions();
            if (sym != null && pos != null) {
                Integer q = pos.get(sym);
                if (q != null) qty = q;
            }
        } catch (Exception ignore) { }
        lblPos.setText(sharesFmt.format(qty));

        double cash = getCurrentCash();
        boolean haveSym = (sym != null && !sym.isBlank());
        boolean havePrice = !Double.isNaN(price) && price > 0.0;
        boolean validShares = shares > 0;

        updateButtonState(buyButton, haveSym && havePrice && validShares, buyColor);
        updateButtonState(sellButton, haveSym && havePrice && validShares, sellColor);

        int affordable = (havePrice ? (int) Math.floor(cash / price) : 0);
        int baseForFractions = (qty > 0 ? qty : affordable);

        boolean canUseFractions = haveSym && havePrice && baseForFractions > 0;
        boolean canBuyMax = haveSym && havePrice && affordable > 0;
        boolean hasPosition = haveSym && qty > 0;

        if (btn25 != null) updateSmallButtonState(btn25, canUseFractions);
        if (btn50 != null) updateSmallButtonState(btn50, canUseFractions);
        if (btnBuyMax != null) updateSmallButtonState(btnBuyMax, canBuyMax);
        if (btnSellMax != null) updateSmallButtonState(btnSellMax, hasPosition);
    }

    private int parseShares() {
        try {
            int n = Integer.parseInt(sharesField.getText().trim());
            return Math.max(0, n);
        } catch (Exception ex) {
            return 0;
        }
    }

    private String symbol() {
        try {
            return getSelectedSymbol.get();
        } catch (Exception ignore) {
            return null;
        }
    }

    private void submit(boolean buy) {
        String symbol = symbol();
        if (symbol == null || symbol.isBlank()) {
            msg("Select a symbol first.");
            return;
        }
        int shares = parseShares();
        if (shares <= 0) {
            msg("Enter a positive whole number of shares.");
            return;
        }

        double price = model.getPrice(symbol);
        if (Double.isNaN(price) || price <= 0) {
            msg("No available price for " + symbol + " yet.");
            return;
        }

        if (buy) {
            try {
                double cash = model.getAccountDTO().cash();
                double need = shares * price;
                if (cash + 1e-6 < need) {
                    msg("Insufficient cash. Need " + money.format(need) + " but have " + money.format(cash));
                    return;
                }
            } catch (Exception ex) {
                msg("Failed to check cash: " + ex.getMessage());
                return;
            }
        }

        model.executeTrade(symbol, buy, shares, System.currentTimeMillis());

        sharesField.setText("0");
        refreshReadouts();
    }

    private void msg(String text) {
        JOptionPane.showMessageDialog(this, text, "Order", JOptionPane.WARNING_MESSAGE);
    }

    @FunctionalInterface
    interface SimpleDocListener extends javax.swing.event.DocumentListener {
        void changed();

        @Override
        default void insertUpdate(javax.swing.event.DocumentEvent e) {
            changed();
        }

        @Override
        default void removeUpdate(javax.swing.event.DocumentEvent e) {
            changed();
        }

        @Override
        default void changedUpdate(javax.swing.event.DocumentEvent e) {
            changed();
        }
    }
}