package com.gui;

import com.models.ModelFacade;
import com.models.market.TradeItem;
import com.gui.LogoCache;

import javax.swing.*;
import java.awt.*;


public class SymbolListRenderer extends JPanel implements ListCellRenderer<TradeItem> {
    private final JLabel iconLabel = new JLabel();
    private final JLabel textLabel = new JLabel();
    private final LogoCache logoCache;
    private final ModelFacade model;
    private final int iconSize;

    public SymbolListRenderer(LogoCache cache, ModelFacade model, int iconSize) {
        this.logoCache = cache;
        this.model = model;
        this.iconSize = iconSize;
        setLayout(new BorderLayout(6,0));
        iconLabel.setPreferredSize(new Dimension(iconSize, iconSize));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(iconLabel, BorderLayout.WEST);
        add(textLabel, BorderLayout.CENTER);
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends TradeItem> list, TradeItem value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        String symbol = value != null ? value.getSymbol() : "";
        String name = (value != null && value.getName() != null) ? value.getName() : symbol;
        textLabel.setText(name + " (" + symbol + ")");

        // try cached icon first
        String logoStr = null;
        try {
            logoStr = model != null ? model.getLogoForSymbol(symbol) : null;
        } catch (Exception ignored) { }

        ImageIcon cached = logoCache.getIfCached(logoStr != null ? logoStr : symbol);
        if (cached != null) {
            iconLabel.setIcon(cached);
        } else {
            iconLabel.setIcon(logoCache.getPlaceholder());
            // start async load (callback runs on EDT)
            logoCache.load(symbol, logoStr, iconSize, iconSize, icon -> {
                // repaint the specific row once icon arrives
                if (list != null && list.getModel().getSize() > index) {
                    Rectangle rect = list.getCellBounds(index, index);
                    if (rect != null) list.repaint(rect);
                    else list.repaint();
                } else if (list != null) {
                    list.repaint();
                }
            });
        }

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}