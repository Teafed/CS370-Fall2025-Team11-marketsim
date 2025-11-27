package com.gui.tabs;

import com.gui.GUIComponents;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

/**
 * Custom UI for tabbed panes to match the application's design.
 * Removes standard borders and adds a custom selection indicator.
 */
public class TabStripUI extends BasicTabbedPaneUI {
    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(6, 10, 6, 10);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(0, 0, 0, 0);
        focus = new Color(0, 0, 0, 0);
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
            Rectangle iconRect, Rectangle textRect, boolean isSelected) {
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
            boolean isSelected) {
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h,
            boolean isSelected) {
        if (isSelected) {
            g.setColor(GUIComponents.ACCENT_BLUE);
            g.fillRect(x, y + h - 2, w, 2);
        }
    }
}
