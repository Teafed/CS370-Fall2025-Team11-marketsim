package com.gui;

import com.accountmanager.Account;

import javax.swing.*;
import java.awt.*;

public class OrderPanel extends ContentPanel {
    private final JPanel header;
    private final JLabel indicator;
    private final JTabbedPane tabs;
    private boolean collapsed = false;
    private final PortfolioPanel portfolioPanel;

    public OrderPanel(Account account) {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(GUIComponents.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        indicator = new JLabel("ˇ", SwingConstants.CENTER);
        indicator.setForeground(GUIComponents.TEXT_SECONDARY);
        indicator.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        header.add(indicator, BorderLayout.CENTER);

        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { setCollapsed(!collapsed); }
        });

        tabs = new JTabbedPane() {
            @Override public void updateUI() {
                super.updateUI();
                setUI(new TabStripUI());
                setOpaque(false);
                setBackground(GUIComponents.BG_DARK);
                setForeground(GUIComponents.TEXT_SECONDARY);
                setBorder(null);
            }
        };
        tabs.updateUI();
        tabs.addTab("Trade", new TradingPanel());

        // Create a single PortfolioPanel instance for this OrderPanel
        portfolioPanel = new PortfolioPanel(account);
        tabs.addTab("Portfolio", portfolioPanel);

        // Replace the default tab component with a header that includes a close button
        int portfolioIndex = tabs.indexOfComponent(portfolioPanel);
        if (portfolioIndex >= 0) {
            JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            tabHeader.setOpaque(false);
            JLabel title = new JLabel("Portfolio");
            title.setForeground(GUIComponents.TEXT_PRIMARY);
            title.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            JButton closeBtn = new JButton("✕");
            closeBtn.setBorder(null);
            closeBtn.setOpaque(false);
            closeBtn.setContentAreaFilled(false);
            closeBtn.setForeground(GUIComponents.TEXT_SECONDARY);
            closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeBtn.setToolTipText("Close Portfolio");
            closeBtn.addActionListener(e -> {
                // When closing the portfolio tab, revert to the Trade tab
                tabs.setSelectedIndex(0);
                setCollapsed(true);
            });
            tabHeader.add(title);
            tabHeader.add(closeBtn);
            tabs.setTabComponentAt(portfolioIndex, tabHeader);
        }

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    void setCollapsed(boolean collapse) {
        this.collapsed = collapse;
        tabs.setVisible(!collapse);
        indicator.setText(collapse ? "ˆ" : "ˇ");
        revalidate();
        repaint();
    }
}