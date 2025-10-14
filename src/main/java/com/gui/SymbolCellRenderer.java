package com.gui;

import com.market.TradeItem;
import javax.swing.*;
import java.awt.*;

// handles cell rendering in SymbolListPanel
public class SymbolCellRenderer extends JPanel implements ListCellRenderer<TradeItem> {
   private final JLabel symbolLabel;
   private final JLabel priceLabel;
   private final JLabel changeLabel;
   
   public SymbolCellRenderer() {
      setLayout(new BorderLayout());
      setBorder(GUIComponents.createBorder());
      
      symbolLabel = new JLabel();
      symbolLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
      
      priceLabel = new JLabel();
      priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
      priceLabel.setHorizontalAlignment(JLabel.RIGHT);
      
      changeLabel = new JLabel();
      changeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
      changeLabel.setHorizontalAlignment(JLabel.RIGHT);
      
      // layout
      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.add(symbolLabel, BorderLayout.WEST);
      leftPanel.setOpaque(false);
      
      JPanel rightPanel = new JPanel(new BorderLayout());
      rightPanel.add(priceLabel, BorderLayout.NORTH);
      rightPanel.add(changeLabel, BorderLayout.SOUTH);
      rightPanel.setPreferredSize(new Dimension(80, 40));
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
         float price = (value.getCurrentPrice() * 100f) / 100;
         priceLabel.setText(String.format("$%.2f", price));
         
         double changePercent = value.getChangePercent();
         double change = value.getChange();
         String changeText = String.format("%+.2f %+.2f%%", change, changePercent);
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
         priceLabel.setForeground(Color.WHITE);
      } else {
         // alternating row colors
         if (index % 2 == 0) {
            setBackground(GUIComponents.BG_DARK);
         } else {
            setBackground(GUIComponents.BG_MEDIUM);
         }
         symbolLabel.setForeground(GUIComponents.TEXT_PRIMARY);
         priceLabel.setForeground(GUIComponents.TEXT_PRIMARY);
      }
      
      setOpaque(true);
      return this;
   }
}