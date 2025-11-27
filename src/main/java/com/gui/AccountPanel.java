package com.gui;

import com.models.AccountDTO;
import com.models.ModelFacade;
import com.models.ModelListener;
import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.Map;

import static com.gui.GUIComponents.makeCheckIcon;

/**
 * A redesigned panel displaying comprehensive account information.
 * Shows total account value, cash balance, portfolio holdings, and visualizations.
 */
public class AccountPanel extends ContentPanel implements ModelListener {
    private final ModelFacade model;

    private final JLabel totalAccountValueLabel = new JLabel();
    private final JLabel cashBalanceLabel = new JLabel();
    private final JLabel portfolioValueLabel = new JLabel();

    private final JLabel profileLabel = new JLabel();
    private final JLabel accountLabel = new JLabel();

    private final JButton btnDeposit = new JButton("Deposit / Withdraw");
    private final JButton btnSettings = new JButton("Account Settings");
    private final JButton btnSwitchAccount = new JButton("Switch Account");
    private final JButton btnNewAccount = new JButton("New Account");
    private final JPanel holdingsPanel = new JPanel();
    private final GoalChartPanel goalChart;
    private final PieChartPanel pieChart;

    private Color accountColor = new Color(100, 149, 237); // Default cornflower blue
    private double goalAmount = 100000.0; // Default goal

    public AccountPanel(ModelFacade model) {
        this.model = model;
        model.addListener(this);

        goalChart = new GoalChartPanel();
        pieChart = new PieChartPanel();

        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_DARK);

        // Main content area
        JPanel mainContent = new JPanel(new BorderLayout(16, 16));
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Left panel - account info and holdings
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        // Cash Balance and Portfolio Value side by side
        JPanel balanceRow = new JPanel(new GridLayout(1, 2, 12, 0));
        balanceRow.setOpaque(false);
        balanceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JPanel cashCard = createCard();
        cashCard.setLayout(new BorderLayout());

        JPanel cashContent = new JPanel();
        cashContent.setLayout(new BoxLayout(cashContent, BoxLayout.Y_AXIS));
        cashContent.setOpaque(false);

        // Profile & account labels here (above cash)
        profileLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        profileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        profileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        accountLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        accountLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        accountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel cashTitle = new JLabel("\uD83D\uDCB0 Cash Balance");
        cashTitle.setForeground(GUIComponents.TEXT_SECONDARY);
        cashTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cashTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        cashBalanceLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        cashBalanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        cashBalanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        cashContent.add(profileLabel);
        cashContent.add(Box.createVerticalStrut(2));
        cashContent.add(accountLabel);
        cashContent.add(Box.createVerticalStrut(8));
        cashContent.add(cashTitle);
        cashContent.add(Box.createVerticalStrut(6));
        cashContent.add(cashBalanceLabel);

        cashCard.add(cashContent, BorderLayout.NORTH);

        JPanel portfolioCard = createCard();
        portfolioCard.setLayout(new BoxLayout(portfolioCard, BoxLayout.Y_AXIS));

        JLabel totalTitle = new JLabel("Total Account Value");
        totalTitle.setForeground(GUIComponents.TEXT_SECONDARY);
        totalTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        totalTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalAccountValueLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        totalAccountValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        totalAccountValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel portfolioTitle = new JLabel("📊 Portfolio Value");
        portfolioTitle.setForeground(GUIComponents.TEXT_SECONDARY);
        portfolioTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        portfolioTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        portfolioValueLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        portfolioValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        portfolioValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        portfolioCard.add(totalTitle);
        portfolioCard.add(Box.createVerticalStrut(4));
        portfolioCard.add(totalAccountValueLabel);
        portfolioCard.add(Box.createVerticalStrut(12));
        portfolioCard.add(portfolioTitle);
        portfolioCard.add(Box.createVerticalStrut(4));
        portfolioCard.add(portfolioValueLabel);

        balanceRow.add(cashCard);
        balanceRow.add(portfolioCard);

        // ---- Action buttons ----
        JPanel buttonRow = new JPanel(new GridLayout(1, 4, 12, 0));
        buttonRow.setOpaque(false);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        styleButton(btnDeposit);
        styleButton(btnSettings);
        styleButton(btnSwitchAccount);
        styleButton(btnNewAccount);

        btnDeposit.addActionListener(e -> onDepositCash());
        btnSettings.addActionListener(e -> {
            try {
                onSettings();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        btnSwitchAccount.addActionListener(e -> onSwitchAccount());
        btnNewAccount.addActionListener(e -> onNewAccount());

        buttonRow.add(btnDeposit);
        buttonRow.add(btnSettings);
        buttonRow.add(btnSwitchAccount);
        buttonRow.add(btnNewAccount);

        // Portfolio Holdings section
        JPanel holdingsSection = createCard();
        holdingsSection.setLayout(new BorderLayout());

        JLabel holdingsTitle = new JLabel("Portfolio Holdings");
        holdingsTitle.setForeground(GUIComponents.TEXT_PRIMARY);
        holdingsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        holdingsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        holdingsPanel.setLayout(new BoxLayout(holdingsPanel, BoxLayout.Y_AXIS));
        holdingsPanel.setOpaque(false);

        JScrollPane holdingsScroll = new JScrollPane(holdingsPanel);
        holdingsScroll.setBorder(BorderFactory.createEmptyBorder());
        holdingsScroll.setOpaque(false);
        holdingsScroll.getViewport().setOpaque(false);
        holdingsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        holdingsSection.add(holdingsTitle, BorderLayout.NORTH);
        holdingsSection.add(holdingsScroll, BorderLayout.CENTER);

        // Add all to left panel
        leftPanel.add(balanceRow);
        leftPanel.add(Box.createVerticalStrut(12));
        leftPanel.add(buttonRow);
        leftPanel.add(Box.createVerticalStrut(16));
        leftPanel.add(holdingsSection);

        // Right panel - charts
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);

        JPanel goalSection = createCard();
        goalSection.setLayout(new BorderLayout());
        JLabel goalTitle = new JLabel("Goal Progress");
        goalTitle.setForeground(GUIComponents.TEXT_PRIMARY);
        goalTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        goalTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        goalSection.add(goalTitle, BorderLayout.NORTH);
        goalSection.add(goalChart, BorderLayout.CENTER);

        JPanel pieSection = createCard();
        pieSection.setLayout(new BorderLayout());
        pieSection.setPreferredSize(new Dimension(0, 260));
        JLabel pieTitle = new JLabel("Asset Allocation");
        pieTitle.setForeground(GUIComponents.TEXT_PRIMARY);
        pieTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pieTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        pieSection.add(pieTitle, BorderLayout.NORTH);
        pieSection.add(pieChart, BorderLayout.CENTER);

        rightPanel.add(goalSection);
        rightPanel.add(Box.createVerticalStrut(16));
        rightPanel.add(pieSection);

        // Add panels to main content (left + right)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(leftPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(16, 0)));
        centerPanel.add(rightPanel);

        mainContent.add(centerPanel, BorderLayout.CENTER);

        JScrollPane mainScroll = new JScrollPane(mainContent);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(mainScroll, BorderLayout.CENTER);

        try {
            onAccountChanged(model.getAccountDTO());
        } catch (Exception ignore) {
            refreshDisplay(null);
        }
    }

    private JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(GUIComponents.BG_MEDIUM);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(GUIComponents.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return card;
    }

    private JPanel createDialogCard(String title, JComponent body, JButton primaryButton, JButton cancelButton) {
        JPanel card = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension base = super.getPreferredSize();
                // cap width
                int maxWidth = 420;
                return new Dimension(Math.min(base.width, maxWidth), base.height);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(GUIComponents.BG_LIGHTER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                g2.setColor(GUIComponents.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);

        if (cancelButton != null) {
            styleButton(cancelButton);
            footer.add(cancelButton);
        }
        if (primaryButton != null) {
            styleButton(primaryButton);
            footer.add(primaryButton);
        }

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    // overload for more buttons
    private JPanel createDialogCard(String title, JComponent body, java.util.List<JButton> actionButtons) {
        JPanel card = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension base = super.getPreferredSize();
                int maxWidth = 420;
                return new Dimension(Math.min(base.width, maxWidth), base.height);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(GUIComponents.BG_LIGHTER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                g2.setColor(GUIComponents.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLabel, BorderLayout.WEST);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);

        if (actionButtons != null) {
            for (JButton b : actionButtons) {
                if (b != null) {
                    styleButton(b);
                    footer.add(b);
                }
            }
        }

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        return card;
    }

    private void styleButton(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setForeground(GUIComponents.TEXT_PRIMARY);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JButton button = (JButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                if (button.getModel().isPressed()) {
                    g2.setColor(GUIComponents.BG_DARKER);
                } else if (button.getModel().isRollover()) {
                    g2.setColor(GUIComponents.BG_LIGHT);
                } else {
                    g2.setColor(GUIComponents.BG_MEDIUM);
                }
                g2.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), 8, 8);

                // Border
                g2.setColor(GUIComponents.BORDER_COLOR);
                g2.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, 8, 8);

                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void refreshDisplay(AccountDTO dto) {
        NumberFormat currency = NumberFormat.getCurrencyInstance();

        double cash = dto != null ? dto.cash() : 0.0;

        Map<String, Double> holdingsValues = computeHoldingsValues(dto);
        double portfolioValue = holdingsValues.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalValue = cash + portfolioValue;

        totalAccountValueLabel.setText(currency.format(totalValue));
        cashBalanceLabel.setText(currency.format(cash));
        portfolioValueLabel.setText(currency.format(portfolioValue));

        // Account / profile label
        String profileName = model.getProfileName();
        String accountName = model.getActiveAccountName();

        accountLabel.setText((accountName == null || accountName.isBlank() ? "—" : accountName));

        // Update holdings list
        updateHoldings(dto);

        // Update charts
        goalChart.setValues(totalValue, goalAmount);
        pieChart.setValues(cash, holdingsValues);

        revalidate();
        repaint();
    }

    private Map<String, Double> computeHoldingsValues(AccountDTO dto) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (dto == null || dto.positions() == null || dto.positions().isEmpty()) {
            return out;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(dto.positions().entrySet());
        entries.removeIf(e -> e.getValue() == null || e.getValue() <= 0);

        // Sort by descending value
        entries.sort((a, b) -> {
            double pa = model.getPrice(a.getKey());
            double pb = model.getPrice(b.getKey());
            double va = Double.isNaN(pa) ? 0 : pa * a.getValue();
            double vb = Double.isNaN(pb) ? 0 : pb * b.getValue();
            return Double.compare(vb, va);
        });

        for (Map.Entry<String, Integer> e : entries) {
            String symbol = e.getKey();
            int shares = e.getValue();
            double price = model.getPrice(symbol);
            if (Double.isNaN(price) || price <= 0 || shares <= 0) continue;
            out.put(symbol, price * shares);
        }

        return out;
    }

    private void updateHoldings(AccountDTO dto) {
        holdingsPanel.removeAll();

        if (dto == null || dto.positions() == null || dto.positions().isEmpty()) {
            JLabel noData = new JLabel("No holdings to display");
            noData.setForeground(GUIComponents.TEXT_SECONDARY);
            noData.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            holdingsPanel.add(noData);
        } else {
            List<Map.Entry<String, Integer>> holdings = dto.positions().entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0)
                    .sorted((a, b) -> b.getKey().compareTo(a.getKey()))
                    .toList();

            if (holdings.isEmpty()) {
                JLabel noData = new JLabel("No holdings to display");
                noData.setForeground(GUIComponents.TEXT_SECONDARY);
                noData.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                holdingsPanel.add(noData);
            } else {
                for (Map.Entry<String, Integer> entry : holdings) {
                    JPanel row = createHoldingRow(entry.getKey(), entry.getValue());
                    holdingsPanel.add(row);
                    holdingsPanel.add(Box.createVerticalStrut(8));
                }
            }
        }

        holdingsPanel.revalidate();
        holdingsPanel.repaint();
    }

    private JPanel createHoldingRow(String symbol, int shares) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JPanel leftInfo = new JPanel();
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.setOpaque(false);

        JLabel symbolLabel = new JLabel(symbol);
        symbolLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        symbolLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        double currentPrice = model.getPrice(symbol);
        String priceStr = Double.isNaN(currentPrice) ? "—" : String.format("$%.2f", currentPrice);

        JLabel sharesLabel = new JLabel(String.format("%d shares", shares));
        sharesLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        sharesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel priceLabel = new JLabel("Current price: " + priceStr);
        priceLabel.setForeground(new Color(150, 150, 150));
        priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        leftInfo.add(symbolLabel);
        leftInfo.add(sharesLabel);
        leftInfo.add(priceLabel);

        JPanel rightInfo = new JPanel();
        rightInfo.setLayout(new BoxLayout(rightInfo, BoxLayout.Y_AXIS));
        rightInfo.setOpaque(false);

        if (!Double.isNaN(currentPrice)) {
            double value = shares * currentPrice;

            JLabel valueLabel = new JLabel("Value: " + NumberFormat.getCurrencyInstance().format(value));
            valueLabel.setForeground(GUIComponents.TEXT_PRIMARY);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            valueLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            rightInfo.add(valueLabel);

            // Try to get trade history for gain/loss calculation (approximation)
            try {
                List<ModelFacade.TradeRow> trades = model.getRecentTrades(Integer.MAX_VALUE);
                double totalCost = 0;
                int totalShares = 0;

                for (ModelFacade.TradeRow trade : trades) {
                    if (trade.symbol().equals(symbol) && trade.side().equals("BUY")) {
                        totalCost += trade.quantity() * trade.price();
                        totalShares += trade.quantity();
                    }
                }

                if (totalShares > 0) {
                    double avgCost = totalCost / totalShares;
                    double costBasisForCurrent = shares * avgCost;
                    double gain = value - costBasisForCurrent;
                    double gainPct = (costBasisForCurrent > 0) ? (gain / costBasisForCurrent * 100) : 0;

                    String colorHex = gain >= 0 ? "#4CAF50" : "#F44336";
                    String text = String.format(
                            "<html>Unrealized P/L: <span style='color:%s'>%+.2f (%.2f%%)</span></html>",
                            colorHex, gain, gainPct
                    );

                    JLabel gainLabel = new JLabel(text);
                    gainLabel.setForeground(GUIComponents.TEXT_SECONDARY); // base text color
                    gainLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    gainLabel.setHorizontalAlignment(SwingConstants.RIGHT);
                    gainLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

                    rightInfo.add(gainLabel);
                }
            } catch (Exception ignored) {
                // Just show value if we can't compute P/L
            }
        } else {
            JLabel valueLabel = new JLabel("Value: —");
            valueLabel.setForeground(GUIComponents.TEXT_SECONDARY);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            valueLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            rightInfo.add(valueLabel);
        }

        row.add(leftInfo, BorderLayout.WEST);
        row.add(rightInfo, BorderLayout.EAST);

        return row;
    }

    private void onDepositCash() {
        JFormattedTextField amountField = new JFormattedTextField(NumberFormat.getNumberInstance());
        amountField.setValue(0.0);
        amountField.setColumns(12);
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        amountField.setForeground(GUIComponents.TEXT_PRIMARY);
        amountField.setBackground(GUIComponents.BG_MEDIUM);
        amountField.setCaretColor(GUIComponents.TEXT_PRIMARY);
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));


        amountField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if ("0".equals(amountField.getText().trim())) {
                    amountField.setText("");
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (amountField.getText().trim().isEmpty()) {
                    amountField.setText("0");
                }
            }
        });

        JLabel label = new JLabel("Amount");
        label.setForeground(GUIComponents.TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(label);
        body.add(Box.createVerticalStrut(6));
        body.add(amountField);

        JButton depositBtn = new JButton("Deposit");
        JButton withdrawBtn = new JButton("Withdraw");
        JButton cancel = new JButton("Cancel");

        Runnable parseAndValidate = () -> {
            // no-op
        };

        depositBtn.addActionListener(e -> {
            double amount = 0.0;
            try {
                amountField.commitEdit();
                Object v = amountField.getValue();
                if (v instanceof Number n) amount = n.doubleValue();
            } catch (Exception ignored) { }

            if (amount <= 0) {
                Toast.show(amountField, "Enter a positive amount.");
                return;
            }
            if (amount > 100_000) {
                Toast.show(amountField, "Maximum deposit is $100,000.");
                return;
            }

            try {
                model.deposit(amount, "Cash deposit");
                OverlayDialog.close(AccountPanel.this);
                Toast.show(btnDeposit, "Deposit successful!");
            } catch (Exception ex) {
                Toast.show(btnDeposit, "Failed to deposit: " + ex.getMessage());
            }
        });

        withdrawBtn.addActionListener(e -> {
            double amount = 0.0;
            try {
                amountField.commitEdit();
                Object v = amountField.getValue();
                if (v instanceof Number n) amount = n.doubleValue();
            } catch (Exception ignored) { }

            if (amount <= 0) {
                Toast.show(amountField, "Enter a positive amount.");
                return;
            }

            try {
                model.withdraw(amount, "Cash withdrawal");
                OverlayDialog.close(AccountPanel.this);
                Toast.show(btnDeposit, "Withdrawal successful!");
            } catch (Exception ex) {
                Toast.show(btnDeposit, "Failed to withdraw: " + ex.getMessage());
            }
        });

        cancel.addActionListener(e -> OverlayDialog.close(AccountPanel.this));

        java.util.List<JButton> actions = java.util.Arrays.asList(depositBtn, withdrawBtn, cancel);
        JPanel card = createDialogCard("Adjust Cash Balance", body, actions);
        OverlayDialog.show(this, card);
    }

    private void onNewAccount() {
        JTextField nameField = new JTextField("New Account");
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nameField.setForeground(GUIComponents.TEXT_PRIMARY);
        nameField.setBackground(GUIComponents.BG_DARK);
        nameField.setCaretColor(GUIComponents.TEXT_PRIMARY);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JFormattedTextField balanceField = new JFormattedTextField(NumberFormat.getNumberInstance());
        balanceField.setValue(10_000.0);
        balanceField.setColumns(12);
        balanceField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        balanceField.setForeground(GUIComponents.TEXT_PRIMARY);
        balanceField.setBackground(GUIComponents.BG_DARK);
        balanceField.setCaretColor(GUIComponents.TEXT_PRIMARY);
        balanceField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel nameLabel = new JLabel("Account name");
        nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel balanceLabel = new JLabel("Initial deposit");
        balanceLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        balanceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 4, 6, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        gc.gridx = 0; gc.gridy = 0;
        body.add(nameLabel, gc);
        gc.gridy = 1;
        body.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 2;
        body.add(balanceLabel, gc);
        gc.gridy = 3;
        body.add(balanceField, gc);

        JButton create = new JButton("Create");
        JButton cancel = new JButton("Cancel");

        create.addActionListener(e -> {
            String name = nameField.getText().trim();
            double amount = 0.0;

            try {
                balanceField.commitEdit();
                Object v = balanceField.getValue();
                if (v instanceof Number n) amount = n.doubleValue();
            } catch (Exception ignored) { }

            if (name.isBlank()) {
                Toast.show(nameField, "Account name is required.");
                return;
            }
            if (amount < 0) {
                Toast.show(balanceField, "Initial deposit cannot be negative.");
                return;
            }

            try {
                model.createAccount(name, amount);
                OverlayDialog.close(AccountPanel.this);
                Toast.show(btnNewAccount, "Account created and set active.");
            } catch (Exception ex) {
                Toast.show(btnNewAccount, "Failed to create account: " + ex.getMessage());
            }
        });

        cancel.addActionListener(e -> OverlayDialog.close(AccountPanel.this));

        JPanel card = createDialogCard("New Account", body, create, cancel);
        OverlayDialog.show(this, card);
    }

    private void onSettings() throws SQLException {
        Account activeAccount = model.getActiveAccount();
        if (activeAccount == null) {
            Toast.show(btnSettings, "No active account.");
            return;
        }
        boolean isDefault = model.isDefaultAccount(activeAccount);

        JTextField nameField = new JTextField(activeAccount.getName());
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nameField.setForeground(GUIComponents.TEXT_PRIMARY);
        nameField.setBackground(GUIComponents.BG_DARK);
        nameField.setCaretColor(GUIComponents.TEXT_PRIMARY);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JFormattedTextField goalField = new JFormattedTextField(NumberFormat.getNumberInstance());
        goalField.setValue(goalAmount);
        goalField.setColumns(12);
        goalField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        goalField.setForeground(GUIComponents.TEXT_PRIMARY);
        goalField.setBackground(GUIComponents.BG_DARK);
        goalField.setCaretColor(GUIComponents.TEXT_PRIMARY);
        goalField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JPanel colorPanel = new JPanel();
        colorPanel.setPreferredSize(new Dimension(40, 24));
        colorPanel.setBackground(accountColor);
        colorPanel.setBorder(BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR));
        colorPanel.setOpaque(true);

        JButton colorButton = new JButton("Choose Color");
        styleButton(colorButton);
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Account Color", accountColor);
            if (newColor != null) {
                accountColor = newColor;
                colorPanel.setBackground(newColor);
                repaint();
            }
        });

        JCheckBox defaultCheck = new JCheckBox("Set as default account");

        defaultCheck.setSelected(isDefault);
        defaultCheck.setOpaque(false);
        defaultCheck.setForeground(GUIComponents.TEXT_SECONDARY);
        defaultCheck.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        defaultCheck.setFocusPainted(false);
        defaultCheck.setIcon(makeCheckIcon(false));
        defaultCheck.setSelectedIcon(makeCheckIcon(true));

        JLabel nameLabel = new JLabel("Account name");
        nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel goalLabel = new JLabel("Goal amount");
        goalLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        goalLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel colorLabel = new JLabel("Account color");
        colorLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        colorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        colorRow.setOpaque(false);
        colorRow.add(colorPanel);
        colorRow.add(colorButton);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 4, 6, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        gc.gridx = 0; gc.gridy = 0;
        body.add(nameLabel, gc);
        gc.gridy = 1;
        body.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 2;
        body.add(goalLabel, gc);
        gc.gridy = 3;
        body.add(goalField, gc);

        gc.gridx = 0; gc.gridy = 4;
        body.add(colorLabel, gc);
        gc.gridy = 5;
        body.add(colorRow, gc);

        gc.gridx = 0; gc.gridy = 6;
        gc.fill = GridBagConstraints.NONE;
        body.add(defaultCheck, gc);

        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");

        JButton delete = new JButton("Delete Account");
        delete.setForeground(GUIComponents.RED);
        delete.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        delete.setContentAreaFilled(false);
        delete.setFocusPainted(false);
        delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        final boolean[] confirming = { false };

        delete.addActionListener(e -> {
            if (!confirming[0]) {
                confirming[0] = true;
                delete.setText("Are you sure?");
                return;
            }

            try {
                model.deleteAccount(activeAccount);
                OverlayDialog.close(AccountPanel.this);
                Toast.show(btnSettings, "Account deleted.");
            } catch (Exception ex) {
                Toast.show(btnSettings, "Failed to delete account: " + ex.getMessage());
            }
        });

        save.addActionListener(e -> {
            String newName = nameField.getText().trim();
            if (newName.isBlank()) {
                Toast.show(nameField, "Account name is required.");
                return;
            }

            if (!newName.equals(activeAccount.getName())) {
                try {
                    model.setAccountName(newName);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

            try {
                goalField.commitEdit();
                Object v = goalField.getValue();
                if (v instanceof Number n) {
                    goalAmount = n.doubleValue();
                    goalChart.setValues(calculateTotalValue(), goalAmount);
                }
            } catch (Exception ignored) { }

            if (defaultCheck.isSelected() && !isDefault) {
                try {
                    model.setDefaultAccount(activeAccount);
                } catch (Exception ex) {
                    Toast.show(defaultCheck, "Failed to set default account: " + ex.getMessage());
                }
            }

            OverlayDialog.close(AccountPanel.this);
            repaint();
        });

        cancel.addActionListener(e -> OverlayDialog.close(AccountPanel.this));

        java.util.List<JButton> actions = java.util.Arrays.asList(delete, cancel, save);
        JPanel card = createDialogCard("Account Settings", body, actions);
        OverlayDialog.show(this, card);
    }

    private void onSwitchAccount() {
        var accounts = model.listAccounts();
        if (accounts == null || accounts.isEmpty()) {
            Toast.show(btnSwitchAccount, "No accounts available.");
            return;
        }

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        for (Account a : accounts) {
            JPanel card = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Color bg = getBackground();
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                    g2.setColor(GUIComponents.BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setBackground(GUIComponents.BG_MEDIUM);
            card.setLayout(new BorderLayout());
            card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel nameLabel = new JLabel(a.getName());
            nameLabel.setForeground(GUIComponents.TEXT_PRIMARY);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

            card.add(nameLabel, BorderLayout.CENTER);

            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    card.setBackground(GUIComponents.BG_LIGHT);
                    card.repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    card.setBackground(GUIComponents.BG_MEDIUM);
                    card.repaint();
                }

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    try {
                        model.switchAccount(a.getId());
                        OverlayDialog.close(AccountPanel.this);
                        Toast.show(btnSwitchAccount, "Switched to " + a.getName());
                    } catch (Exception ex) {
                        Toast.show(btnSwitchAccount, "Failed to switch: " + ex.getMessage());
                    }
                }
            });

            list.add(card);
            list.add(Box.createVerticalStrut(8));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(340, Math.min(250, accounts.size() * 60)));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(scroll, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> OverlayDialog.close(AccountPanel.this));

        JPanel card = createDialogCard("Switch Account", body, close, null);
        OverlayDialog.show(this, card);
    }

    private double calculateTotalValue() {
        try {
            return model.getAccountTotalValue();
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Chart panels
    private class GoalChartPanel extends JPanel {
        private double current = 0;
        private double goal = 100000;

        public GoalChartPanel() {
            setOpaque(false);
        }

        public void setValues(double current, double goal) {
            this.current = current;
            this.goal = goal;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int barHeight = 40;
            int barY = height / 2 - barHeight / 2;

            // Background bar
            g2.setColor(new Color(50, 54, 62));
            g2.fillRoundRect(20, barY, width - 40, barHeight, 20, 20);

            // Progress bar
            double progress = goal > 0 ? Math.min(current / goal, 1.0) : 0;
            int progressWidth = (int) ((width - 40) * progress);
            g2.setColor(accountColor);
            g2.fillRoundRect(20, barY, progressWidth, barHeight, 20, 20);

            // Text
            g2.setColor(GUIComponents.TEXT_PRIMARY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            String progressText = String.format("%.1f%%", progress * 100);
            FontMetrics fm = g2.getFontMetrics();
            int textX = width / 2 - fm.stringWidth(progressText) / 2;
            g2.drawString(progressText, textX, barY + barHeight / 2 + 5);

            // Labels
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(GUIComponents.TEXT_SECONDARY);
            NumberFormat currency = NumberFormat.getCurrencyInstance();
            g2.drawString(currency.format(current), 20, barY - 10);
            String goalText = "Goal: " + currency.format(goal);
            int goalWidth = g2.getFontMetrics().stringWidth(goalText);
            g2.drawString(goalText, width - 20 - goalWidth, barY - 10);
        }
    }

    private class PieChartPanel extends JPanel {
        private double cash = 0;
        private Map<String, Double> holdings = Collections.emptyMap(); // symbol -> value

        public PieChartPanel() {
            setOpaque(false);
        }

        public void setValues(double cash, Map<String, Double> holdings) {
            this.cash = cash;
            this.holdings = (holdings == null) ? Collections.emptyMap() : new LinkedHashMap<>(holdings);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            double portfolioTotal = holdings.values().stream().mapToDouble(Double::doubleValue).sum();
            double total = cash + portfolioTotal;

            if (total <= 0) {
                g2.setColor(GUIComponents.TEXT_SECONDARY);
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                String msg = "No data to display";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, width / 2 - fm.stringWidth(msg) / 2, height / 2);
                return;
            }

            // Reserve vertical space at bottom for legend
            int legendHeight = 60;
            int pieAreaHeight = height - legendHeight - 20;
            int size = Math.min(width - 60, pieAreaHeight);
            size = Math.max(80, size);

            int x = (width - size) / 2;
            int y = 10;

            // --- Draw pie ---

            double startAngle = 90.0;

            // Cash slice
            double cashAngle = (cash > 0 ? (cash / total) * 360.0 : 0.0);
            if (cashAngle > 0.0) {
                g2.setColor(new Color(76, 175, 80)); // Green for cash
                g2.fillArc(x, y, size, size, (int) startAngle, (int) cashAngle);
                startAngle += cashAngle;
            }

            // Color palette for holdings
            Color[] palette = new Color[]{
                    accountColor,
                    new Color(244, 67, 54),
                    new Color(255, 193, 7),
                    new Color(33, 150, 243),
                    new Color(156, 39, 176),
                    new Color(0, 150, 136)
            };

            int i = 0;
            for (Map.Entry<String, Double> e : holdings.entrySet()) {
                double v = e.getValue();
                if (v <= 0) continue;
                double angle = (v / total) * 360.0;
                if (angle <= 0) continue;

                Color c = palette[i % palette.length];
                i++;
                g2.setColor(c);
                g2.fillArc(x, y, size, size, (int) startAngle, (int) angle);
                startAngle += angle;
            }

            // --- Legend UNDER the pie ---

            int legendY = y + size + 12;
            int legendX = 20;
            int rowHeight = 18;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            int row = 0;

            // Cash legend
            if (cash > 0) {
                double pct = (cash / total) * 100.0;
                g2.setColor(new Color(76, 175, 80));
                g2.fillRect(legendX, legendY + row * rowHeight, 12, 12);
                g2.setColor(GUIComponents.TEXT_PRIMARY);
                g2.drawString(String.format("Cash: %.1f%%", pct),
                        legendX + 20,
                        legendY + row * rowHeight + 10);
                row++;
            }

            // Holdings legend (one row per stock)
            i = 0;
            for (Map.Entry<String, Double> e : holdings.entrySet()) {
                double v = e.getValue();
                if (v <= 0) continue;
                double pct = (v / total) * 100.0;

                Color c = palette[i % palette.length];
                i++;

                g2.setColor(c);
                g2.fillRect(legendX, legendY + row * rowHeight, 12, 12);
                g2.setColor(GUIComponents.TEXT_PRIMARY);
                g2.drawString(
                        String.format("%s: %.1f%%", e.getKey(), pct),
                        legendX + 20,
                        legendY + row * rowHeight + 10
                );
                row++;
            }
        }
    }

    // ModelListener implementation
    @Override public void onAccountChanged(AccountDTO snapshot) { refreshDisplay(snapshot); }
    @Override public void onQuotesUpdated() {
        try {
            refreshDisplay(model.getAccountDTO());
        } catch (SQLException ignored) { }
    }
    @Override public void onError(String message, Throwable t) { }

    @Override public void addNotify() {
        super.addNotify();
        model.addListener(this);
    }
    @Override public void removeNotify() {
        model.removeListener(this);
        super.removeNotify();
    }
}
