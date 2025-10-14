package com.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class GUIComponents {
   // color palette
   public static final Color BG_DARKER = new Color(25, 25, 25);
   public static final Color BG_DARK = new Color(35, 35, 35);
   public static final Color BG_MEDIUM = new Color(45, 45, 45);
   public static final Color BG_LIGHT = new Color(55, 55, 55);
   public static final Color BG_LIGHTER = new Color(65, 65, 65);
   
   public static final Color TEXT_PRIMARY = new Color(220, 220, 220);
   public static final Color TEXT_SECONDARY = new Color(180, 180, 180);
   public static final Color TEXT_TERTIARY = new Color(140, 140, 140);
   
   public static final Color ACCENT_BLUE = new Color(64, 128, 255);
   public static final Color ACCENT_GREEN = new Color(76, 175, 80);
   public static final Color ACCENT_RED = new Color(244, 67, 54);
   public static final Color ACCENT_ORANGE = new Color(255, 152, 0);
   
   public static final Color BORDER_COLOR = new Color(70, 70, 70);
   public static final Color BORDER_FOCUS = new Color(100, 150, 255);
   
   public static JScrollPane createScrollPane(Component view) {
      JScrollPane scrollPane = new JScrollPane(view);
      
      scrollPane.getVerticalScrollBar().setUI(new ScrollBarUI());
      scrollPane.getHorizontalScrollBar().setUI(new ScrollBarUI());
      
      scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
      scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
      
      scrollPane.setBackground(BG_DARK);
      scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
      scrollPane.getViewport().setBackground(BG_DARK);
      
      scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createCorner());
      scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, createCorner());
      scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, createCorner());
      scrollPane.setCorner(JScrollPane.LOWER_LEFT_CORNER, createCorner());
      
      return scrollPane;
   }

   public static JSplitPane createSplitPane(String orientation, Component left, Component right) {
      JSplitPane splitPane = new JSplitPane(
              (Objects.equals(orientation, "horizontal") ?JSplitPane.HORIZONTAL_SPLIT
                      : JSplitPane.VERTICAL_SPLIT), left, right);

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

   public static <T> JList<T> createList(DefaultListModel<T> model) {
      JList<T> list = new JList<>(model);
      
      list.setBackground(BG_DARK);
      list.setForeground(TEXT_PRIMARY);
      list.setSelectionBackground(ACCENT_BLUE);
      list.setSelectionForeground(Color.WHITE);
      list.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      
      list.setFocusable(true);
      list.setBorder(createBorder());
      
      return list;
   }

   public static javax.swing.border.Border createBorder() {
      return BorderFactory.createEmptyBorder();
   }
}