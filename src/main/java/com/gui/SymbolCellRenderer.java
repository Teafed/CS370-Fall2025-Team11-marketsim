package com.gui;

import com.market.TradeItem;
import javax.swing.*;
import java.awt.*;

// handles cell rendering in SymbolListPanel
public class SymbolCellRenderer extends JPanel implements ListCellRenderer<TradeItem> {
   private final JLabel symbolLabel;
   private final JLabel nameLabel;
   private final JLabel priceLabel;
   private final JLabel changeLabel;
   
   public SymbolCellRenderer() {
      setLayout(new BorderLayout());
      setBorder(GUIComponents.createBorder());
      
      symbolLabel = new JLabel();
      symbolLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
      
      nameLabel = new JLabel();
      nameLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
      nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);
      
      priceLabel = new JLabel();
      priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      priceLabel.setHorizontalAlignment(JLabel.RIGHT);
      
      changeLabel = new JLabel();
      changeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
      changeLabel.setHorizontalAlignment(JLabel.RIGHT);
      
      // layout
      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.add(symbolLabel, BorderLayout.NORTH);
      leftPanel.add(nameLabel, BorderLayout.SOUTH);
      leftPanel.setOpaque(false);
      
      JPanel rightPanel = new JPanel(new BorderLayout());
      rightPanel.add(priceLabel, BorderLayout.NORTH);
      rightPanel.add(changeLabel, BorderLayout.SOUTH);
      rightPanel.setPreferredSize(new Dimension(100, 40));
      rightPanel.setOpaque(false);
      
      add(leftPanel, BorderLayout.WEST);
      add(rightPanel, BorderLayout.EAST);
   }
   
   @Override
   public Component getListCellRendererComponent(
         JList<? extends TradeItem> list,
         TradeItem value,
         int index,
         boolean isSelected,
         boolean cellHasFocus) {
      
      if (value != null) {
         symbolLabel.setText(value.getSymbol());
         nameLabel.setText(value.getName());
         
         double price = value.getPrice();
         priceLabel.setText(String.format("$%.2f", price));
         
         double changePercent = value.getChangePercent();
         double change = value.getChange();
         String changeText = String.format("%+.2f (%+.2f%%)", change, changePercent);
         changeLabel.setText(changeText);
         
         // color coding for change
         if (changePercent > 0) {
            changeLabel.setForeground(GUIComponents.ACCENT_GREEN);
         } else if (changePercent < 0) {
            changeLabel.setForeground(GUIComponents.ACCENT_RED);
         } else {
            changeLabel.setForeground(GUIComponents.TEXT_SECONDARY);
         }
      }
      
      // selection styling
      if (isSelected) {
         setBackground(GUIComponents.ACCENT_BLUE);
         symbolLabel.setForeground(Color.WHITE);
         nameLabel.setForeground(Color.WHITE);
         priceLabel.setForeground(Color.WHITE);
      } else {
         // alternating row colors
         if (index % 2 == 0) {
            setBackground(GUIComponents.BG_DARK);
         } else {
            setBackground(GUIComponents.BG_MEDIUM);
         }
         symbolLabel.setForeground(GUIComponents.TEXT_PRIMARY);
         nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);
         priceLabel.setForeground(GUIComponents.TEXT_PRIMARY);
      }
      
      setOpaque(true);
      return this;
   }
}