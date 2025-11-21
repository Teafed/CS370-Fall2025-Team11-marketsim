package com.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom UI for split panes to match the application's dark theme.
 * Provides a custom divider with hover effects and grip lines.
 */
public class SplitPaneUI extends BasicSplitPaneUI {

   @Override
   public BasicSplitPaneDivider createDefaultDivider() {
      return new SplitPaneDivider(this);
   }

   /**
    * Custom divider for the split pane.
    * Handles painting and mouse events for hover effects.
    */
   public static class SplitPaneDivider extends BasicSplitPaneDivider {
      private boolean isHovered = false;
      private boolean isPressed = false;

      /**
       * Constructs a new SplitPaneDivider.
       *
       * @param ui The parent SplitPaneUI.
       */
      public SplitPaneDivider(BasicSplitPaneUI ui) {
         super(ui);
         setBackground(GUIComponents.BG_MEDIUM);
         setBorder(null);

         // add hover effects
         addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               isHovered = true;
               repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               isHovered = false;
               repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
               isPressed = true;
               repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
               isPressed = false;
               repaint();
            }
         });
      }

      @Override
      public void paint(Graphics g) {
         Graphics2D g2 = (Graphics2D) g.create();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

         // base color based on state
         Color baseColor;
         if (isPressed) {
            baseColor = GUIComponents.BG_LIGHT.brighter();
         } else if (isHovered) {
            baseColor = GUIComponents.BG_LIGHT;
         } else {
            baseColor = GUIComponents.BG_MEDIUM;
         }

         // fill background
         g2.setColor(baseColor);
         g2.fillRect(0, 0, getWidth(), getHeight());

         // add subtle grip pattern
         drawGripPattern(g2);

         g2.dispose();
      }

      /**
       * Draws a grip pattern on the divider.
       *
       * @param g2 The Graphics2D context.
       */
      private void drawGripPattern(Graphics2D g2) {
         g2.setColor(GUIComponents.BORDER_COLOR);
         int orientation = splitPaneUI.getSplitPane().getOrientation();

         if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            // vertical divider - horizontal grip lines
            int centerY = getHeight() / 2;
            int centerX = getWidth() / 2;

            for (int i = -8; i <= 8; i += 4) {
               g2.fillRoundRect(centerX - 15, centerY + i, 30, 2, 1, 1);
            }
         } else {
            // horizontal divider - vertical grip lines
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;

            for (int i = -8; i <= 8; i += 4) {
               g2.fillRoundRect(centerX + i, centerY - 15, 2, 30, 1, 1);
            }
         }
      }
   }
}