package com.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class TabStripUI extends BasicTabbedPaneUI {
    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(6, 10, 6, 10);
        selectedTabPadInsets = new Insets(0,0,0,0);
        contentBorderInsets = new Insets(0,0,0,0);
        tabAreaInsets = new Insets(0, 0, 0, 0);
        focus = new Color(0,0,0,0);
    }

    @Override protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) { }
    @Override protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) { }
    @Override protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) { }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        if (isSelected) {
            g.setColor(GUIComponents.ACCENT_BLUE);
            g.fillRect(x, y + h - 2, w, 2);
        }
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        g.setFont(font);
        g.setColor(isSelected ? Color.WHITE : GUIComponents.TEXT_SECONDARY);


// Draw label text
        int x = textRect.x;
        int y = textRect.y + metrics.getAscent();
        g.drawString(title, x, y);


// Optional mnemonic underline (no SwingUtilities2 dependency)
        int mnemIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
        if (mnemIndex >= 0 && mnemIndex < title.length()) {
            int underlineX = x + metrics.stringWidth(title.substring(0, mnemIndex));
            int underlineW = Math.max(1, metrics.charWidth(title.charAt(mnemIndex)));
            int underlineY = y + 1;
            g.fillRect(underlineX, underlineY, underlineW, 1);
        }
    }
}
