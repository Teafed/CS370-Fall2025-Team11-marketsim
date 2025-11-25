package com.gui.navigation;

import com.gui.GUIComponents;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom UI for scrollbars to match the application's dark theme.
 * Hides arrow buttons and provides a custom thumb design.
 */
public class ScrollBarUI extends BasicScrollBarUI {
   private boolean isThumbHover = false;
   private boolean isThumbPressed = false;

   @Override
   protected void configureScrollBarColors() {
      this.thumbColor = GUIComponents.BG_LIGHT;
      this.thumbHighlightColor = GUIComponents.BG_LIGHTER;
      this.thumbDarkShadowColor = GUIComponents.BG_LIGHT;
      this.trackColor = GUIComponents.BG_DARK;
      this.trackHighlightColor = GUIComponents.BG_MEDIUM;
   }

   // invisible buttons to remove arrow buttons
   private JButton createZeroButton() {
      JButton button = new JButton();
      button.setPreferredSize(new Dimension(0, 0));
      button.setMinimumSize(new Dimension(0, 0));
      button.setMaximumSize(new Dimension(0, 0));
      return button;
   }

   @Override
   protected JButton createDecreaseButton(int orientation) {
      return createZeroButton();
   }

   @Override
   protected JButton createIncreaseButton(int orientation) {
      return createZeroButton();
   }

   @Override
   protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
      g.setColor(trackColor);
      g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
   }

   @Override
   protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
      if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
         return;
      }

      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // very thin rounded thumb
      g2.setColor(thumbColor);
      g2.fillRoundRect(thumbBounds.x + 3, thumbBounds.y + 1,
            thumbBounds.width - 6, thumbBounds.height - 2, 3, 3);
      g2.dispose();
   }

   @Override
   protected void installListeners() {
      super.installListeners();

      // add mouse listeners for hover effects
      scrollbar.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            isThumbHover = true;
            scrollbar.repaint();
         }

         @Override
         public void mouseExited(MouseEvent e) {
            isThumbHover = false;
            scrollbar.repaint();
         }

         @Override
         public void mousePressed(MouseEvent e) {
            isThumbPressed = true;
            scrollbar.repaint();
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            isThumbPressed = false;
            scrollbar.repaint();
         }
      });
   }

   @Override
   protected Dimension getMinimumThumbSize() {
      return new Dimension(8, 30); // minimum thumb size
   }
}