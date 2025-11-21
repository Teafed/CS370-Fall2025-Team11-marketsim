package com.gui;

import com.models.ModelFacade;
import com.models.market.TradeItem;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

// handles cell rendering in SymbolListPanel
/**
 * Custom list cell renderer for displaying stock symbols.
 * Shows the symbol, company name, logo, current price, and percent change.
 * Handles asynchronous logo loading.
 */
public class SymbolCellRenderer extends JPanel implements ListCellRenderer<TradeItem> {
    private final JLabel logoLabel = new JLabel();
    private final JLabel symbolLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private final JLabel priceLabel = new JLabel();
    private final JLabel changeLabel = new JLabel();
    private final LogoCache logoCache;
    private final ModelFacade model;
    private final int iconSize = 40;

    /**
     * Constructs a new SymbolCellRenderer.
     *
     * @param cache The LogoCache instance.
     * @param model The ModelFacade instance.
     */
    public SymbolCellRenderer(LogoCache cache, ModelFacade model) {
        this.logoCache = cache;
        this.model = model;
        setLayout(new BorderLayout());
        setBorder(GUIComponents.createBorder());

        // Configure logo label
        logoLabel.setPreferredSize(new Dimension(iconSize, iconSize));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setVerticalAlignment(SwingConstants.CENTER);

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

        JPanel leftPanel = new JPanel(new BorderLayout(6, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(logoLabel, BorderLayout.WEST);
        leftPanel.add(leftStack, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(priceLabel, BorderLayout.NORTH);
        rightPanel.add(changeLabel, BorderLayout.SOUTH);
        rightPanel.setPreferredSize(new Dimension(100, 40));
        rightPanel.setOpaque(false);

        add(leftPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends TradeItem> list, TradeItem value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            String symbol = value.getSymbol();
            symbolLabel.setText(symbol);
            nameLabel.setText(value.getName());

            // Load company logo
            ImageIcon cached = logoCache.getIfCached(symbol);
            if (cached != null) {
                logoLabel.setIcon(cached);
            } else {
                logoLabel.setIcon(logoCache.getPlaceholder());
                logoCache.load(symbol, model::getLogoForSymbol, iconSize, iconSize, icon -> {
                    // Repaint the specific row once icon arrives
                    if (list != null && list.getModel().getSize() > index) {
                        Rectangle rect = list.getCellBounds(index, index);
                        if (rect != null)
                            list.repaint(rect);
                        else
                            list.repaint();
                    } else if (list != null) {
                        list.repaint();
                    }
                });
            }

            double px = value.getCurrentPrice();
            if (Double.isNaN(px) || px == 0) {
                priceLabel.setText("—");
                priceLabel.setForeground(GUIComponents.TEXT_TERTIARY);
            } else {
                priceLabel.setText(java.lang.String.format(java.util.Locale.US, "$%,.2f", px));
                priceLabel.setForeground(isSelected ? GUIComponents.TEXT_TERTIARY : GUIComponents.TEXT_PRIMARY);
            }

            double changePercent = value.getChangePercent();
            boolean up = changePercent > 0, down = changePercent < 0;
            java.lang.String arrow = up ? "▲" : (down ? "▼" : "•");
            java.lang.String changeText = Double.isNaN(changePercent) ? "—"
                    : java.lang.String.format(java.util.Locale.US, "%s %+.2f%%", arrow, changePercent);
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