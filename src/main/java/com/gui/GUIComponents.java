package com.gui;

import com.gui.navigation.ScrollBarUI;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Utility class containing shared GUI components, colors, and factory methods.
 * Ensures consistent styling across the application.
 */
public class GUIComponents {
   // hi
   // color palette
   public static final Color BG_DARKER = new Color(25, 25, 25);
   public static final Color BG_DARK = new Color(30, 34, 45);
   public static final Color BG_MEDIUM = new Color(45, 49, 60);
   public static final Color BG_LIGHT = new Color(55, 59, 70);
   public static final Color BG_LIGHTER = new Color(65, 69, 80);
   public static final Color BG_SELECTED = new Color(45, 50, 65);
   public static final Color SEPARATOR = new Color(40, 44, 55);
   public static final Color SEARCH_BG = new Color(45, 50, 65);

   public static final Color TEXT_PRIMARY = new Color(240, 240, 245);
   public static final Color TEXT_SECONDARY = new Color(120, 125, 140);
   public static final Color TEXT_TERTIARY = new Color(140, 140, 140);

   public static final Color ACCENT_BLUE = new Color(64, 128, 255);
   public static final Color GREEN = new Color(34, 197, 94);
   public static final Color RED = new Color(239, 68, 68);
   public static final Color ACCENT_ORANGE = new Color(255, 152, 0);

   public static final Color BORDER_COLOR = new Color(60, 65, 80);
   public static final Color BORDER_FOCUS = new Color(100, 150, 255);

   /**
    * Creates a styled JScrollPane.
    *
    * @param view The component to be scrolled.
    * @return A configured JScrollPane.
    */
   public static JScrollPane createScrollPane(Component view) {
      JScrollPane scrollPane = new JScrollPane(view);

      scrollPane.getVerticalScrollBar().setUI(new ScrollBarUI());
      scrollPane.getHorizontalScrollBar().setUI(new ScrollBarUI());

      scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
      scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));

      scrollPane.setBackground(BG_DARK);
      scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
      scrollPane.getViewport().setBackground(BG_DARK);
      scrollPane.getViewport().setOpaque(true);

      scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createCorner());
      scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, createCorner());
      scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, createCorner());
      scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER, createCorner());

      return scrollPane;
   }

   /**
    * Creates a styled JSplitPane.
    *
    * @param orientation The split orientation ("horizontal" or "vertical").
    * @param left        The left/top component.
    * @param right       The right/bottom component.
    * @return A configured JSplitPane.
    */
   public static JSplitPane createSplitPane(String orientation, Component left, Component right) {
      JSplitPane splitPane = new JSplitPane(
            (Objects.equals(orientation, "horizontal") ? JSplitPane.HORIZONTAL_SPLIT
                  : JSplitPane.VERTICAL_SPLIT),
            left, right);

      splitPane.setUI(new SplitPaneUI());
      splitPane.setBackground(GUIComponents.BG_DARK);
      splitPane.setBorder(null);

      // divider properties
      splitPane.setDividerSize(8);
      splitPane.setContinuousLayout(true);
      splitPane.setOneTouchExpandable(false);

      return splitPane;
   }

   private static JPanel createCorner() {
      JPanel corner = new JPanel();
      corner.setBackground(BG_MEDIUM);
      return corner;
   }

   /**
    * Creates a styled JList.
    *
    * @param model The list model.
    * @param <T>   The type of elements in the list.
    * @return A configured JList.
    */
   public static <T> JList<T> createList(DefaultListModel<T> model) {
      JList<T> list = new JList<>(model);

      list.setBackground(BG_DARK);
      list.setForeground(TEXT_PRIMARY);
      list.setSelectionBackground(ACCENT_BLUE);
      list.setSelectionForeground(Color.WHITE);
      list.setFont(new Font("Segoe UI", Font.PLAIN, 13));

      list.setFocusable(true);
      list.setBorder(createBorder());

      list.setOpaque(true);
      list.setBackground(GUIComponents.BG_MEDIUM);

      return list;
   }

   /**
    * Creates a standard border for components.
    *
    * @return A Border object.
    */
   public static javax.swing.border.Border createBorder() {
      return BorderFactory.createEmptyBorder();
   }
}