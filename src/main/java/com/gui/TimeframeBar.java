package com.gui;

import com.market.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class TimeframeBar extends ContentPanel {
    public interface Listener { void onTimeframeChanged(long startMs, long endMs); }

    private final Listener listener;
    private final ButtonGroup group = new ButtonGroup();
    private final JToggleButton btn1D = makeToggle("1D");
    private final JToggleButton btn1W = makeToggle("1W");
    private final JToggleButton btn1M = makeToggle("1M");

    private static final long ONE_DAY   = 24L * 60 * 60 * 1000;
    private static final long ONE_WEEK  = 7L * ONE_DAY;
    private static final long ONE_MONTH = 30L * ONE_DAY;

    TimeframeBar(Listener listener) {
        this.listener = listener;
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        group.add(btn1D); group.add(btn1W); group.add(btn1M);
        add(btn1D); add(btn1W); add(btn1M);
        btn1W.setSelected(true); // default to week

        ActionListener handler = e -> applySelection();
        btn1D.addActionListener(handler);
        btn1W.addActionListener(handler);
        btn1M.addActionListener(handler);
    }

    /**
     * Returns true if a selection exists and notifies listener
     * @return selection
     */
    public boolean fireCurrentSelection() {
        if (!btn1D.isSelected() && !btn1W.isSelected() && !btn1M.isSelected()) return false;
        applySelection();
        return true;
    }

    private void applySelection() {
        long now = System.currentTimeMillis();
        long start = now - ONE_WEEK; // default
        if (btn1D.isSelected()) start = now - ONE_DAY;
        else if (btn1M.isSelected()) start = now - ONE_MONTH;
        listener.onTimeframeChanged(start, now);
    }

    private JToggleButton makeToggle(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(GUIComponents.TEXT_SECONDARY);
        b.addChangeListener(e -> {
            if (b.isSelected()) {
                b.setForeground(Color.WHITE);
                b.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GUIComponents.ACCENT_BLUE));
            } else {
                b.setForeground(GUIComponents.TEXT_SECONDARY);
                b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
            }
        });
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}