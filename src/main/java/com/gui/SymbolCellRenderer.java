package com.gui;

import com.models.market.TradeItem;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

// handles cell rendering in SymbolListPanel
public class SymbolCellRenderer extends JPanel implements ListCellRenderer<TradeItem> {
   private final JLabel symbolLabel = new JLabel();
   private final JLabel nameLabel   = new JLabel();
   private final JLabel priceLabel = new JLabel();
   private final JLabel changeLabel = new JLabel();
   
   public SymbolCellRenderer() {
       setLayout(new BorderLayout());
       setBorder(GUIComponents.createBorder());

       symbolLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
       symbolLabel.setForeground(GUIComponents.TEXT_PRIMARY);

       nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
       nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);

       priceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
       priceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
       priceLabel.setForeground(GUIComponents.TEXT_PRIMARY);

       changeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
       changeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

       // layout
       JPanel leftStack = new JPanel();
       leftStack.setLayout(new BoxLayout(leftStack, BoxLayout.Y_AXIS));
       leftStack.setOpaque(false);
       leftStack.add(symbolLabel);
       leftStack.add(nameLabel);

       JPanel rightPanel = new JPanel(new BorderLayout());
       rightPanel.add(priceLabel, BorderLayout.NORTH);
       rightPanel.add(changeLabel, BorderLayout.SOUTH);
       rightPanel.setPreferredSize(new Dimension(100, 40));
       rightPanel.setOpaque(false);

       add(leftStack, BorderLayout.CENTER);
       add(rightPanel, BorderLayout.EAST);
   }
   
   @Override
   public Component getListCellRendererComponent(
         JList<? extends TradeItem> list, TradeItem value, int index,
         boolean isSelected, boolean cellHasFocus) {
      
      if (value != null) {
         symbolLabel.setText(value.getSymbol());
          nameLabel.setText(value.getName());

          double px = value.getCurrentPrice();
          if (Double.isNaN(px) || px == 0) {
              priceLabel.setText("—");
              priceLabel.setForeground(GUIComponents.TEXT_TERTIARY);
          } else {
              priceLabel.setText(String.format(java.util.Locale.US, "$%,.2f", px));
              priceLabel.setForeground(isSelected ? GUIComponents.TEXT_TERTIARY : GUIComponents.TEXT_PRIMARY);
          }
         
         double changePercent = value.getChangePercent();
          boolean up = changePercent > 0, down = changePercent < 0;
          String arrow = up ? "▲" : (down ? "▼" : "•");
          String changeText = Double.isNaN(changePercent) ? "—" : String.format(java.util.Locale.US, "%s %+.2f%%", arrow, changePercent);
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
         priceLabel.setForeground(GUIComponents.TEXT_PRIMARY);
         nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);
      }

       Border padding = BorderFactory.createEmptyBorder(8, 12, 8, 12);
       if (!isSelected && index > 0) {
           Border sep = BorderFactory.createMatteBorder(1, 0, 0, 0, GUIComponents.BG_LIGHT);
           setBorder(BorderFactory.createCompoundBorder(sep, padding));
       } else {
           setBorder(padding);
       }

      setOpaque(true);
      return this;
   }
}