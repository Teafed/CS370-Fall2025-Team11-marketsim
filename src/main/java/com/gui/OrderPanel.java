package com.gui;

import javax.swing.*;
import java.awt.*;

public class OrderPanel extends ContentPanel {
    private final JPanel header;
    private final JLabel indicator;
    private final JTabbedPane tabs;
    private boolean collapsed = false;

    OrderPanel() {
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
        tabs.addTab("Portfolio", new PortfolioPanel());

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