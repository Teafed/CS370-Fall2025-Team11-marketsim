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

    private final JLabel lblSymbol = new JLabel("-", SwingConstants.CENTER);
    private final JLabel lblPrice = new JLabel("-", SwingConstants.CENTER);
    private final JLabel lblCash = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblPos  = new JLabel("0", SwingConstants.LEFT);

    private final JTextField sharesField = new JTextField("0", 8);
    private final JLabel lblTotalPrice = new JLabel("$0.00", SwingConstants.RIGHT);

    private JButton buyButton;
    private JButton sellButton;

    private final DecimalFormat money = new DecimalFormat("$#,##0.00");
    private final DecimalFormat sharesFmt = new DecimalFormat("#,##0");

    private final Timer refreshTimer;

    // Match SearchPanel and SymbolCellRenderer colors
    private static final Color BG_DARK = new Color(30, 34, 45);
    private static final Color BG_MEDIUM = new Color(45, 50, 65);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 125, 140);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color RED = new Color(239, 68, 68);
    private static final Color BORDER_COLOR = new Color(60, 65, 80);

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
        setBackground(BG_DARK);

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
        lblSymbol.setForeground(TEXT_PRIMARY);

        lblPrice.setFont(new Font("Arial", Font.BOLD, 16));
        lblPrice.setForeground(TEXT_PRIMARY);

        panel.add(lblSymbol, BorderLayout.WEST);
        panel.add(lblPrice, BorderLayout.EAST);

        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        buyButton = createActionButton("Buy", GREEN);
        sellButton = createActionButton("Sell", RED);

        buyButton.addActionListener(e -> submit(true));
        sellButton.addActionListener(e -> submit(false));

        panel.add(buyButton);
        panel.add(sellButton);

        return panel;
    }

    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 36));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        button.setOpaque(true);

        return button;
    }

    private JPanel createSharesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel label = new JLabel("Number of Shares");
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        sharesField.setFont(new Font("Arial", Font.PLAIN, 13));
        sharesField.setBackground(BG_MEDIUM);
        sharesField.setForeground(TEXT_PRIMARY);
        sharesField.setCaretColor(TEXT_PRIMARY);
        sharesField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        sharesField.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(sharesField, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createTotalPricePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(BG_MEDIUM);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel label = new JLabel("Total Price");
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(TEXT_SECONDARY);

        lblTotalPrice.setFont(new Font("Arial", Font.BOLD, 14));
        lblTotalPrice.setForeground(TEXT_PRIMARY);

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

        // Position: we want quantity for selected symbol (if available in DTO)
        int qty = 0;
        try {
            var dto = model.getAccountDTO();
            var pos = dto.positions();
            if (sym != null && pos != null) {
                Integer q = pos.get(sym);
                if (q != null) qty = q;
            }
        } catch (Exception ignore) {}
        lblPos.setText(sharesFmt.format(qty));

        // Button enablement
        boolean haveSym = (sym != null && !sym.isBlank());
        boolean havePrice = !Double.isNaN(price) && price > 0.0;
        boolean validShares = shares > 0;
        buyButton.setEnabled(haveSym && havePrice && validShares);
        sellButton.setEnabled(haveSym && havePrice && validShares);
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