package com.gui.navigation;

import com.gui.GUIComponents;
import com.models.market.TradeItem;

import javax.swing.*;
import java.awt.*;

public class CellRenderer implements ListCellRenderer<SymbolListEntry> {
    private final SymbolCellRenderer symbolRenderer;
    private final JLabel headerLabel = new JLabel();

    public CellRenderer(SymbolCellRenderer symbolRenderer) {
        this.symbolRenderer = symbolRenderer;
        headerLabel.setOpaque(true);
        headerLabel.setFont(new Font("Arial Unicode MS", Font.BOLD, 10));
        headerLabel.setBackground(GUIComponents.BG_LIGHT);
        headerLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends SymbolListEntry> list, SymbolListEntry value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (value instanceof SectionHeader header) {
            String arrow = header.isCollapsed() ? "▶ " : "▼ ";
            headerLabel.setText(arrow + header.getTitle());
            headerLabel.setForeground(GUIComponents.TEXT_PRIMARY);
            headerLabel.setPreferredSize(new Dimension(100, 25));
            if (isSelected) {
                headerLabel.setBackground(GUIComponents.ACCENT_BLUE);
                headerLabel.setForeground(Color.WHITE);
            } else {
                headerLabel.setBackground(GUIComponents.BG_LIGHT);
            }
            return headerLabel;
        } else if (value instanceof TradeItem item) {
            return symbolRenderer.getListCellRendererComponent(
                    (JList<? extends TradeItem>) list, item, index, isSelected, cellHasFocus);
        } else if (value == null) {
            return new JLabel("");
        }
        return new JLabel(value.toString());
    }
}