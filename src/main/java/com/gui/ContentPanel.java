// holds either ChartPanel or ProfilePanel. not implemented yet

package com.gui;

import javax.swing.*;
import java.awt.*;

/**
 * A base panel for content areas in the application.
 * Sets a default background color.
 */
public class ContentPanel extends JPanel {
   /**
    * Constructs a new ContentPanel with default styling.
    */
   public ContentPanel() {
      setOpaque(true);
      setBackground(GUIComponents.BG_DARKER);
   }
}