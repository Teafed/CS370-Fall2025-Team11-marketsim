package com.gui;

import com.models.ModelFacade;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class OrderTab extends ContentPanel {
    ModelFacade model;
    Supplier<String> getSelectedSymbol;

    private JButton buyButton;
    private JButton sellButton;
    private JLabel numberOfSharesLabel;
    private JTextField numberOfSharesField;
    private JLabel priceLabel;
    private JTextField priceField;

    public OrderTab(ModelFacade model, Supplier<String> getSelectedSymbol) {
        this.model = model;
        this.getSelectedSymbol = getSelectedSymbol;
        setLayout(new GridBagLayout());
        setBackground(GUIComponents.BG_MEDIUM);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        buyButton = createActionButton("Buy", GUIComponents.ACCENT_GREEN);
        sellButton = createActionButton("Sell", GUIComponents.ACCENT_RED);

        numberOfSharesLabel = new JLabel("Number of Shares: ");
        numberOfSharesLabel.setForeground(Color.WHITE);
        numberOfSharesField = new JTextField(20);

        priceLabel = new JLabel("Price: ");
        priceLabel.setForeground(Color.WHITE);
        priceField = new JTextField(20);

        // placeholder action listeners
        buyButton.addActionListener(e -> handleBuy());
        sellButton.addActionListener(e -> handleSell());
    }

    private JButton createActionButton(String text, Color accentColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 45));

        // Default state
        button.setBackground(GUIComponents.BG_LIGHT);
        button.setForeground(GUIComponents.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(accentColor);
                button.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 2),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(GUIComponents.BG_LIGHT);
                button.setForeground(GUIComponents.TEXT_PRIMARY);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }
        });

        return button;
    }

    private void layoutComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        add(buyButton, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        add(numberOfSharesLabel, gbc);
        gbc.gridx = 2; gbc.gridy = 0;
        add(numberOfSharesField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(sellButton, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        add(priceLabel, gbc);
        gbc.gridx = 2; gbc.gridy = 1;
        add(priceField, gbc);

        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(Box.createVerticalGlue(), gbc);
    }

    private void handleBuy() {
        submit(true);
    }

    private void handleSell() {
        submit(false);
    }

    private void submit(boolean buy) {
        String symbol = getSelectedSymbol.get();
        if (symbol == null || symbol.isBlank()) {
            JOptionPane.showMessageDialog(this, "No symbol selected.", "Order", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int shares;
        try {
            shares = Integer.parseInt(numberOfSharesField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid share count.", "Order", JOptionPane.WARNING_MESSAGE);
            return;
        }
        model.executeTrade(symbol, buy, shares, System.currentTimeMillis());
    }
}