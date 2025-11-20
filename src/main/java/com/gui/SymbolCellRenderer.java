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

    // Match SearchPanel colors
    private static final Color BG_DARK = new Color(30, 34, 45);
    private static final Color BG_SELECTED = new Color(45, 50, 65);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 125, 140);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color RED = new Color(239, 68, 68);
    private static final Color SEPARATOR = new Color(40, 44, 55);

    public SymbolCellRenderer() {
        setLayout(new BorderLayout());

        // Larger, bolder fonts for modern look
        symbolLabel.setFont(new Font("Arial", Font.BOLD, 18));
        symbolLabel.setForeground(TEXT_PRIMARY);

        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        nameLabel.setForeground(TEXT_SECONDARY);

        priceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        priceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        priceLabel.setForeground(TEXT_PRIMARY);

        changeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        changeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        changeLabel.setOpaque(false); // Ensure visibility

        // Left side - symbol and name stacked
        JPanel leftStack = new JPanel();
        leftStack.setLayout(new BoxLayout(leftStack, BoxLayout.Y_AXIS));
        leftStack.setOpaque(false);
        symbolLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftStack.add(symbolLabel);
        leftStack.add(Box.createVerticalStrut(4)); // Small gap between symbol and name
        leftStack.add(nameLabel);

        // Right side - price and change stacked
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 1/2));
        rightPanel.setOpaque(false);
        rightPanel.add(priceLabel);
        rightPanel.add(changeLabel);
        rightPanel.setPreferredSize(new Dimension(120, 70));

        add(leftStack, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends TradeItem> list, TradeItem value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            symbolLabel.setText(value.getSymbol());
            nameLabel.setText(value.getSymbol()); // Show ticker again as in the reference image

            // Ensure labels are visible
            symbolLabel.setVisible(true);
            nameLabel.setVisible(true);
            priceLabel.setVisible(true);
            changeLabel.setVisible(true);

            double px = value.getCurrentPrice();
            if (Double.isNaN(px) || px == 0) {
                priceLabel.setText("—");
                priceLabel.setForeground(TEXT_SECONDARY);
            } else {
                priceLabel.setText(java.lang.String.format(java.util.Locale.US, "$%.2f", px));
                priceLabel.setForeground(TEXT_PRIMARY);
            }

            double changePercent = value.getChangePercent();
            boolean up = changePercent > 0, down = changePercent < 0;
            java.lang.String arrow = up ? "▲" : (down ? "▼" : "•");
            java.lang.String changeText = Double.isNaN(changePercent) ? "—" : java.lang.String.format(java.util.Locale.US, "%s %+.2f%%", arrow, changePercent);
            changeLabel.setText(changeText);

            // color coding for change
            if (changePercent > 0) {
                changeLabel.setForeground(GREEN);
            } else if (changePercent < 0) {
                changeLabel.setForeground(RED);
            } else {
                changeLabel.setForeground(TEXT_SECONDARY);
            }
        }

        // Modern selection and background styling
        if (isSelected) {
            // Subtle highlight for selected item
            setBackground(BG_SELECTED);
        } else {
            // Uniform dark background
            setBackground(BG_DARK);
        }

        // Keep text colors consistent
        symbolLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setForeground(TEXT_SECONDARY);

        // More generous padding for modern spacious feel
        Border padding = BorderFactory.createEmptyBorder(12, 16, 12, 16);

        // Subtle separator line between items
        if (index > 0) {
            Border sep = BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARATOR);
            setBorder(BorderFactory.createCompoundBorder(sep, padding));
        } else {
            setBorder(padding);
        }

        setOpaque(true);
        return this;
    }
}