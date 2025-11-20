package com.gui;

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

    private final JLabel lblSymbol = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblPrice = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblCash = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblPos = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblEst = new JLabel("-", SwingConstants.LEFT);

    private final JTextField sharesField = new JTextField("1", 8);
    private final JButton btnMinus = new JButton("–");
    private final JButton btnPlus = new JButton("+");
    private final JButton buyButton;
    private final JButton sellButton;

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

        setLayout(new GridBagLayout());
        setBackground(GUIComponents.BG_MEDIUM);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));

        buyButton = createActionButton("Buy", GUIComponents.ACCENT_GREEN);
        sellButton = createActionButton("Sell", GUIComponents.ACCENT_RED);

        buyButton.addActionListener(e -> submit(true));
        sellButton.addActionListener(e -> submit(false));

        btnMinus.addActionListener(e -> nudgeShares(-1));
        btnPlus.addActionListener(e -> nudgeShares(+1));
        stylizeStepper(btnMinus);
        stylizeStepper(btnPlus);

        // build UI
        GridBagConstraints gbc = baseGbc();

        // Row 0: Symbol & Price
        add(label("Symbol:"), gbc(0, 0, 1));
        add(value(lblSymbol), gbc(1, 0, 2));
        add(label("Price:"), gbc(3, 0, 1));
        add(value(lblPrice), gbc(4, 0, 1));

        // Row 1: Shares (+/-)
        add(label("Shares:"), gbc(0, 1, 1));
        JPanel sharesBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        sharesBox.setOpaque(false);
        sharesField.setColumns(8);
        sharesField.setHorizontalAlignment(SwingConstants.RIGHT);
        sharesBox.add(btnMinus);
        sharesBox.add(sharesField);
        sharesBox.add(btnPlus);
        add(sharesBox, gbc(1, 1, 2));

        // Row 2: Estimated cost/proceeds
        add(label("Estimated:"), gbc(0, 2, 1));
        add(value(lblEst), gbc(1, 2, 2));

        // Row 3: Cash / Position
        add(label("Cash:"), gbc(3, 1, 1));
        add(value(lblCash), gbc(4, 1, 1));
        add(label("Position:"), gbc(3, 2, 1));
        add(value(lblPos), gbc(4, 2, 1));

        // Row 4: Buttons
        gbc = baseGbc();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(buyButton, gbc);
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(sellButton, gbc);

        // Spacer
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), gbc);

        // Live refresh (price, est, cash, pos)
        refreshTimer = new Timer(1000, e -> refreshReadouts());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        // Also recompute when shares changes
        sharesField.getDocument().addDocumentListener((SimpleDocListener) this::refreshReadouts);

        // Do an initial refresh now
        SwingUtilities.invokeLater(this::refreshReadouts);
    }

    private void refreshReadouts() {
        String sym = symbol();
        lblSymbol.setText(sym == null ? "-" : sym);

        double price = (sym == null) ? Double.NaN : model.getPrice(sym);
        lblPrice.setText(Double.isNaN(price) ? "—" : money.format(price));

        int shares = parseShares();
        if (!Double.isNaN(price) && shares > 0) {
            double est = shares * price;
            lblEst.setText(money.format(est));
        } else {
            lblEst.setText("—");
        }

        try {
            double cash = model.getAccountDTO().cash;
            lblCash.setText(money.format(cash));
        } catch (Exception e) {
            lblCash.setText("—");
        }

        // Position: we want quantity for selected symbol (if available in DTO)
        int qty = 0;
        try {
            var dto = model.getAccountDTO();
            var pos = dto.positions;
            if (sym != null && pos != null) {
                Integer q = pos.get(sym);
                if (q != null)
                    qty = q;
            }
        } catch (Exception ignore) {
        }
        lblPos.setText(sharesFmt.format(qty));

        // Button enablement
        boolean haveSym = (sym != null && !sym.isBlank());
        boolean havePrice = !Double.isNaN(price) && price > 0.0;
        boolean validShares = shares > 0;
        buyButton.setEnabled(haveSym && havePrice && validShares);
        sellButton.setEnabled(haveSym && havePrice && validShares);
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(GUIComponents.TEXT_SECONDARY);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private JLabel value(JLabel l) {
        l.setForeground(GUIComponents.TEXT_PRIMARY);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return l;
    }

    private JButton createActionButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 40));
        button.setBackground(GUIComponents.BG_LIGHT);
        button.setForeground(GUIComponents.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(accentColor);
                button.setForeground(Color.WHITE);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(GUIComponents.BG_LIGHT);
                button.setForeground(GUIComponents.TEXT_PRIMARY);
            }
        });

        return button;
    }

    private void stylizeStepper(JButton b) {
        b.setMargin(new Insets(2, 8, 2, 8));
        b.setFocusPainted(false);
        b.setBackground(GUIComponents.BG_LIGHT);
        b.setForeground(GUIComponents.TEXT_PRIMARY);
    }

    private GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        return gbc;
    }

    private GridBagConstraints gbc(int x, int y, int w) {
        GridBagConstraints g = baseGbc();
        g.gridx = x;
        g.gridy = y;
        g.gridwidth = w;
        if (w > 1)
            g.weightx = 1.0;
        return g;
    }

    private void handleBuy() {
        submit(true);
    }

    private void handleSell() {
        submit(false);
    }

    private void nudgeShares(int delta) {
        int n = parseShares();
        n = Math.max(1, n + delta);
        sharesField.setText(Integer.toString(n));
        refreshReadouts();
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

        // price sanity (if NaN, we’ll block)
        double price = model.getPrice(symbol);
        if (Double.isNaN(price) || price <= 0) {
            msg("No available price for " + symbol + " yet.");
            return;
        }

        if (buy) {
            try {
                double cash = model.getAccountDTO().cash;
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

        sharesField.setText("1");
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