package com.gui;

import com.models.AccountDTO;
import com.models.ModelFacade;
import com.models.ModelListener;
import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * A redesigned panel displaying comprehensive account information.
 * Shows total account value, cash balance, portfolio holdings, and visualizations.
 */
public class AccountPanel extends ContentPanel implements ModelListener {
    private final ModelFacade model;

    private final JLabel totalAccountValueLabel = new JLabel();
    private final JLabel cashBalanceLabel = new JLabel();
    private final JLabel portfolioValueLabel = new JLabel();
    private final JButton btnDeposit = new JButton("Deposit Cash");
    private final JButton btnCustomize = new JButton("Customize");
    private final JButton btnSwitchAccount = new JButton("Switch Account");
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

        // Cash Balance and Portfolio Value side by side (with Total spanning across)
        JPanel balanceRow = new JPanel(new GridLayout(1, 2, 12, 0));
        balanceRow.setOpaque(false);
        balanceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel cashCard = createCard();
        cashCard.setLayout(new BorderLayout());

        JPanel cashContent = new JPanel();
        cashContent.setLayout(new BoxLayout(cashContent, BoxLayout.Y_AXIS));
        cashContent.setOpaque(false);

        JLabel totalTitle = new JLabel("Total Account Value");
        totalTitle.setForeground(GUIComponents.TEXT_SECONDARY);
        totalTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        totalTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalAccountValueLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        totalAccountValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        totalAccountValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        cashContent.add(totalTitle);
        cashContent.add(Box.createVerticalStrut(4));
        cashContent.add(totalAccountValueLabel);
        cashContent.add(Box.createVerticalStrut(12));

        JLabel cashTitle = new JLabel("💵 Cash Balance");
        cashTitle.setForeground(GUIComponents.TEXT_SECONDARY);
        cashTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cashTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        cashBalanceLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        cashBalanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        cashBalanceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        cashContent.add(cashTitle);
        cashContent.add(Box.createVerticalStrut(4));
        cashContent.add(cashBalanceLabel);

        cashCard.add(cashContent, BorderLayout.NORTH);

        JPanel portfolioCard = createCard();
        portfolioCard.setLayout(new BoxLayout(portfolioCard, BoxLayout.Y_AXIS));
        JLabel portfolioTitle = new JLabel("📊 Portfolio Value");
        portfolioTitle.setForeground(GUIComponents.TEXT_SECONDARY);
        portfolioTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        portfolioValueLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        portfolioValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        portfolioCard.add(portfolioTitle);
        portfolioCard.add(Box.createVerticalStrut(8));
        portfolioCard.add(portfolioValueLabel);

        balanceRow.add(cashCard);
        balanceRow.add(portfolioCard);

        // Action buttons - all in one row
        JPanel buttonRow = new JPanel(new GridLayout(1, 3, 12, 0));
        buttonRow.setOpaque(false);
        buttonRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        styleButton(btnDeposit);
        styleButton(btnCustomize);
        styleButton(btnSwitchAccount);

        btnDeposit.addActionListener(e -> onDepositCash());
        btnCustomize.addActionListener(e -> {
            try {
                onCustomize();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        btnSwitchAccount.addActionListener(e -> onSwitchAccount());

        buttonRow.add(btnDeposit);
        buttonRow.add(btnCustomize);
        buttonRow.add(btnSwitchAccount);

        // Portfolio Holdings section
        JPanel holdingsSection = createCard();
        holdingsSection.setLayout(new BorderLayout());
        holdingsSection.setPreferredSize(new Dimension(450, 300));

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
        goalSection.setPreferredSize(new Dimension(300, 250));

        JLabel goalTitle = new JLabel("Goal Progress");
        goalTitle.setForeground(GUIComponents.TEXT_PRIMARY);
        goalTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        goalTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        goalSection.add(goalTitle, BorderLayout.NORTH);
        goalSection.add(goalChart, BorderLayout.CENTER);

        JPanel pieSection = createCard();
        pieSection.setLayout(new BorderLayout());
        pieSection.setPreferredSize(new Dimension(300, 300));

        JLabel pieTitle = new JLabel("Asset Allocation");
        pieTitle.setForeground(GUIComponents.TEXT_PRIMARY);
        pieTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        pieTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        pieSection.add(pieTitle, BorderLayout.NORTH);
        pieSection.add(pieChart, BorderLayout.CENTER);

        rightPanel.add(goalSection);
        rightPanel.add(Box.createVerticalStrut(16));
        rightPanel.add(pieSection);

        // Add panels to main content
        mainContent.add(leftPanel, BorderLayout.CENTER);
        mainContent.add(rightPanel, BorderLayout.EAST);

        JScrollPane mainScroll = new JScrollPane(mainContent);
        mainScroll.setBorder(BorderFactory.createEmptyBorder());
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);

        add(mainScroll, BorderLayout.CENTER);

        // Initial data load
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

                // Draw rounded background
                g2.setColor(GUIComponents.BG_MEDIUM);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                // Draw subtle border
                g2.setColor(GUIComponents.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
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
        double portfolioValue = calculatePortfolioValue(dto);
        double totalValue = cash + portfolioValue;

        totalAccountValueLabel.setText(currency.format(totalValue));
        cashBalanceLabel.setText(currency.format(cash));
        portfolioValueLabel.setText(currency.format(portfolioValue));

        // Update holdings
        updateHoldings(dto);

        // Update charts
        goalChart.setValues(totalValue, goalAmount);
        pieChart.setValues(cash, portfolioValue);

        revalidate();
        repaint();
    }

    private double calculatePortfolioValue(AccountDTO dto) {
        if (dto == null || dto.positions() == null) return 0.0;

        double total = 0.0;
        for (Map.Entry<String, Integer> entry : dto.positions().entrySet()) {
            String symbol = entry.getKey();
            int shares = entry.getValue();
            if (shares > 0) {
                double price = model.getPrice(symbol);
                if (!Double.isNaN(price)) {
                    total += shares * price;
                }
            }
        }
        return total;
    }

    private void updateHoldings(AccountDTO dto) {
        holdingsPanel.removeAll();

        if (dto == null || dto.positions() == null || dto.positions().isEmpty()) {
            JLabel noData = new JLabel("No holdings to display");
            noData.setForeground(GUIComponents.TEXT_SECONDARY);
            noData.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            holdingsPanel.add(noData);
        } else {
            // Filter for positive positions only
            List<Map.Entry<String, Integer>> holdings = dto.positions().entrySet().stream()
                    .filter(e -> e.getValue() != null && e.getValue() > 0)
                    .sorted((a, b) -> b.getKey().compareTo(a.getKey())) // Sort by symbol
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
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

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

        JLabel priceLabel = new JLabel("Current: " + priceStr);
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

            JLabel valueLabel = new JLabel(NumberFormat.getCurrencyInstance().format(value));
            valueLabel.setForeground(GUIComponents.TEXT_PRIMARY);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            rightInfo.add(valueLabel);

            // Try to get trade history for gain/loss calculation
            try {
                List<ModelFacade.TradeRow> trades = model.getRecentTrades(100);
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
                    double gain = value - (shares * avgCost);
                    double gainPct = (avgCost > 0) ? (gain / (shares * avgCost) * 100) : 0;

                    Color gainColor = gain >= 0 ? new Color(76, 175, 80) : new Color(244, 67, 54);
                    JLabel gainLabel = new JLabel(String.format("%+.2f (%.2f%%)", gain, gainPct));
                    gainLabel.setForeground(gainColor);
                    gainLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    gainLabel.setHorizontalAlignment(SwingConstants.RIGHT);

                    rightInfo.add(gainLabel);
                }
            } catch (Exception ignored) {
                // If we can't calculate gain/loss, just show the value
            }
        } else {
            JLabel valueLabel = new JLabel("—");
            valueLabel.setForeground(GUIComponents.TEXT_SECONDARY);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            rightInfo.add(valueLabel);
        }

        row.add(leftInfo, BorderLayout.WEST);
        row.add(rightInfo, BorderLayout.EAST);

        return row;
    }

    private void onDepositCash() {
        JFormattedTextField amountField = new JFormattedTextField(NumberFormat.getNumberInstance());
        amountField.setValue(0.0);
        amountField.setColumns(15);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Amount to deposit:"));
        panel.add(amountField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Deposit Cash",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        double amount = 0.0;
        try {
            amountField.commitEdit();
            Object v = amountField.getValue();
            if (v instanceof Number n) amount = n.doubleValue();
        } catch (Exception ignored) { }

        if (amount <= 0) {
            JOptionPane.showMessageDialog(this, "Enter a positive amount.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (amount > 100000) {
            JOptionPane.showMessageDialog(this, "Maximum deposit is $100,000.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            model.deposit(amount, "Cash deposit");
            JOptionPane.showMessageDialog(this, "Deposit successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to deposit cash:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCustomize() throws SQLException {
        Account activeAccount = model.getActiveAccount();
        if (activeAccount == null) {
            JOptionPane.showMessageDialog(this, "No active account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField nameField = new JTextField(activeAccount.getName());
        JFormattedTextField goalField = new JFormattedTextField(NumberFormat.getNumberInstance());
        goalField.setValue(goalAmount);

        JPanel colorPanel = new JPanel();
        colorPanel.setPreferredSize(new Dimension(40, 25));
        colorPanel.setBackground(accountColor);
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JButton colorButton = new JButton("Choose Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose Account Color", accountColor);
            if (newColor != null) {
                accountColor = newColor;
                colorPanel.setBackground(newColor);
            }
        });

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0; panel.add(new JLabel("Account name:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        panel.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.fill = GridBagConstraints.NONE; gc.weightx = 0.0;
        panel.add(new JLabel("Goal amount:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        panel.add(goalField, gc);

        gc.gridx = 0; gc.gridy = 2; gc.fill = GridBagConstraints.NONE; gc.weightx = 0.0;
        panel.add(new JLabel("Account color:"), gc);
        gc.gridx = 1; gc.gridy = 2;
        JPanel colorRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        colorRow.add(colorPanel);
        colorRow.add(colorButton);
        panel.add(colorRow, gc);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Customize Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String newName = nameField.getText().trim();
        if (!newName.isBlank() && !newName.equals(activeAccount.getName())) {
            model.setAccountName(newName);
        }

        try {
            goalField.commitEdit();
            Object v = goalField.getValue();
            if (v instanceof Number n) {
                goalAmount = n.doubleValue();
                goalChart.setValues(calculateTotalValue(), goalAmount);
            }
        } catch (Exception ignored) { }

        repaint();
    }

    private void onSwitchAccount() {
        var accounts = model.listAccounts();
        if (accounts == null || accounts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No accounts available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultComboBoxModel<Account> comboModel = new DefaultComboBoxModel<>();
        for (Account a : accounts) comboModel.addElement(a);

        JComboBox<Account> combo = new JComboBox<>(comboModel);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Account a) setText(a.getName());
                return this;
            }
        });

        int result = JOptionPane.showConfirmDialog(
                this, combo, "Switch Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        Account selected = (Account) combo.getSelectedItem();
        if (selected == null) return;

        try {
            model.switchAccount(selected.getId());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to switch account:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
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
            setPreferredSize(new Dimension(280, 180));
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
        private double portfolio = 0;

        public PieChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(280, 220));
        }

        public void setValues(double cash, double portfolio) {
            this.cash = cash;
            this.portfolio = portfolio;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height) - 60;
            int x = (width - size) / 2;
            int y = 20;

            double total = cash + portfolio;
            if (total == 0) {
                g2.setColor(GUIComponents.TEXT_SECONDARY);
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                String msg = "No data to display";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, width / 2 - fm.stringWidth(msg) / 2, height / 2);
                return;
            }

            // Draw pie slices
            double cashAngle = (cash / total) * 360;
            double portfolioAngle = (portfolio / total) * 360;

            g2.setColor(new Color(76, 175, 80)); // Green for cash
            g2.fillArc(x, y, size, size, 90, (int) cashAngle);

            g2.setColor(accountColor); // Account color for portfolio
            g2.fillArc(x, y, size, size, 90 + (int) cashAngle, (int) portfolioAngle);

            // Draw legend
            int legendY = y + size + 30;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            g2.setColor(new Color(76, 175, 80));
            g2.fillRect(20, legendY, 12, 12);
            g2.setColor(GUIComponents.TEXT_PRIMARY);
            g2.drawString(String.format("Cash: %.1f%%", (cash / total) * 100), 40, legendY + 10);

            g2.setColor(accountColor);
            g2.fillRect(20, legendY + 20, 12, 12);
            g2.setColor(GUIComponents.TEXT_PRIMARY);
            g2.drawString(String.format("Portfolio: %.1f%%", (portfolio / total) * 100), 40, legendY + 30);
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