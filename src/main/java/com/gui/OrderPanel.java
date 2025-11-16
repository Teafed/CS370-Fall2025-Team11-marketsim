package com.gui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class OrderPanel extends ContentPanel {
    private final JPanel header;
    private final JLabel indicator;
    private final JTabbedPane tabs;
    private boolean collapsed = false;
    private final Consumer<Boolean> onCollapseChanged;

    OrderPanel() { this(null); }

    OrderPanel(Consumer<Boolean> onCollapseChanged) {
        this.onCollapseChanged = onCollapseChanged != null ? onCollapseChanged : (c) -> { };
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
        tabs.addTab("Order", new OrderTab());
        tabs.addTab("Order History", new OrderHistoryTab());

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    void setCollapsed(boolean collapse) {
        this.collapsed = collapse;
        tabs.setVisible(!collapse);
        indicator.setText(collapse ? "ˆ" : "ˇ");
        revalidate();
        repaint();
        onCollapseChanged.accept(this.collapsed);
    }

    int getHeaderHeight() {
        return header.getPreferredSize().height + getInsets().top + getInsets().bottom;
    }
}