package com.gui;

import com.market.DatabaseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.*;

public class TimeframeBar extends ContentPanel {
    public interface Listener {
        void onTimeframeChanged(long startMs, long endMs, int multiplier, String timespanToken);
    }

    private final Listener listener;
    private final ButtonGroup group = new ButtonGroup();
    private final JToggleButton btn1D = makeToggle("1D");
    private final JToggleButton btn1W = makeToggle("1W");
    private final JToggleButton btn1M = makeToggle("1M");
    private final JToggleButton btn3M  = makeToggle("3M");
    private final JToggleButton btn6M  = makeToggle("6M");
    private final JToggleButton btn1Y  = makeToggle("1Y");
    private final JToggleButton btnYTD = makeToggle("YTD");

    TimeframeBar(Listener listener) {
        this.listener = listener;
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        group.add(btn1D); group.add(btn1W); group.add(btn1M); group.add(btn3M);
        group.add(btn6M); group.add(btn1Y); group.add(btnYTD);

        add(btn1D); add(btn1W); add(btn1M); add(btn3M);
        add(btn6M); add(btn1Y); add(btnYTD);

        btn1W.setSelected(true); // default to week

        ActionListener handler = e -> applySelection();
        btn1D.addActionListener(handler);
        btn1W.addActionListener(handler);
        btn1M.addActionListener(handler);
        btn3M.addActionListener(handler);
        btn6M.addActionListener(handler);
        btn1Y.addActionListener(handler);
        btnYTD.addActionListener(handler);
    }

    /**
     * Returns true if a selection exists and notifies listener
     * @return selection
     */
    public boolean fireCurrentSelection() {
        if (!btn1D.isSelected() && !btn1W.isSelected() && !btn1M.isSelected()
                && !btn3M.isSelected() && !btn6M.isSelected()
                && !btn1Y.isSelected() && !btnYTD.isSelected()) return false;
        applySelection();
        return true;
    }

    private void applySelection() {
        Instant now = Instant.now();
        ZoneId utc = ZoneOffset.UTC;
        long endMs = now.toEpochMilli();

        LocalDate todayUtc = now.atZone(utc).toLocalDate();
        LocalDate startDate;
        int multiplier;
        String timespan;

        if (btn1D.isSelected()) {
            startDate = todayUtc.minusDays(1);
            multiplier = 1; timespan = "minute";
        } else if (btn1W.isSelected()) {
            startDate = todayUtc.minusWeeks(1);
            multiplier = 1; timespan = "hour";
        } else if (btn1M.isSelected()) {
            startDate = todayUtc.minusMonths(1);
            multiplier = 1; timespan = "day";
        } else if (btn3M.isSelected()) {
            startDate = todayUtc.minusMonths(3);
            multiplier = 1; timespan = "day";
        } else if (btn6M.isSelected()) {
            startDate = todayUtc.minusMonths(6);
            multiplier = 1; timespan = "day";
        } else if (btn1Y.isSelected()) {
            startDate = todayUtc.minusYears(1);
            multiplier = 1; timespan = "day";
        } else { // YTD
            startDate = LocalDate.of(todayUtc.getYear(), 1, 1);
            multiplier = 1; timespan = "day";
        }

        long startMs = startDate.atStartOfDay(utc).toInstant().toEpochMilli();
        if (endMs < startMs) endMs = startMs;

        listener.onTimeframeChanged(startMs, endMs, multiplier, timespan);
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