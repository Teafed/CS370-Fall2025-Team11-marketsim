package com.gui;

import javax.swing.*;
import java.awt.*;

public class PortfolioPanel extends ContentPanel {
    private JLabel placeholderLabel;

    public PortfolioPanel() {
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_LIGHTER);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        initializeComponents();
    }

    private void initializeComponents() {
        // Placeholder for future portfolio display
        placeholderLabel = new JLabel("Portfolio Holdings", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        placeholderLabel.setForeground(new Color(180, 180, 180));

        add(placeholderLabel, BorderLayout.CENTER);
    }

    // TODO: Add methods to display portfolio holdings
    // public void updatePortfolio(List<Holding> holdings) { ... }
}