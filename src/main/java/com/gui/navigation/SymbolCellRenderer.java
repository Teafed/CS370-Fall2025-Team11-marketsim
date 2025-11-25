package com.gui.navigation;

import com.gui.GUIComponents;
import com.gui.LogoCache;
import com.models.ModelFacade;
import com.models.market.TradeItem;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

// handles cell rendering in SymbolListPanel
public class SymbolCellRenderer extends JPanel implements ListCellRenderer<TradeItem> {
    private final JLabel logoLabel = new JLabel();
    private final JLabel symbolLabel = new JLabel();
    private final JLabel nameLabel   = new JLabel();
    private final JLabel priceLabel = new JLabel();
    private final JLabel changeLabel = new JLabel();
    private final LogoCache logoCache;
    private final ModelFacade model;
    private final int iconSize = 40;
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    private static final long REQUEST_DELAY_MS = 300; // 300ms between logo fetch requests

    public SymbolCellRenderer(ModelFacade model, LogoCache logoCache) {
        this.model = model;
        this.logoCache = logoCache;
        setLayout(new BorderLayout());
        setBorder(GUIComponents.createBorder());

        symbolLabel.setFont(new Font("Arial", Font.BOLD, 18));
        symbolLabel.setForeground(GUIComponents.TEXT_PRIMARY);

        nameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);

        priceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        priceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        priceLabel.setForeground(GUIComponents.TEXT_PRIMARY);

        changeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        changeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        changeLabel.setOpaque(false);

        // Left side - symbol and name stacked
        JPanel leftStack = new JPanel();
        leftStack.setLayout(new BoxLayout(leftStack, BoxLayout.Y_AXIS));
        leftStack.setOpaque(false);
        symbolLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftStack.add(symbolLabel);
        leftStack.add(Box.createVerticalStrut(4));
        leftStack.add(nameLabel);

        // Right side - price and change stacked
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 1/2));
        rightPanel.setOpaque(false);
        rightPanel.add(priceLabel);
        rightPanel.add(changeLabel);
        rightPanel.setPreferredSize(new Dimension(120, 50));

        add(leftStack, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends TradeItem> list, TradeItem value, int index,
            boolean isSelected, boolean cellHasFocus) {

        if (value != null) {
            TradeItem ti = (TradeItem) value;
            String symbol = ti.getSymbol();
            symbolLabel.setText(symbol);
            nameLabel.setText(ti.getName());

            // Load company logo with staggered timing to avoid rate limits
            ImageIcon cached = logoCache.getIfCached(symbol);
            if (cached != null) {
                logoLabel.setIcon(cached);
            } else {
                logoLabel.setIcon(logoCache.getPlaceholder());

                // Stagger logo fetch requests with a delay based on the index
                int requestIndex = requestCounter.getAndIncrement();
                long delay = requestIndex * REQUEST_DELAY_MS;

                Timer timer = new Timer((int) delay, e -> {
                    // Fetch logo URL only when needed
                    String logoUrl = null;
                    try {
                        logoUrl = model != null ? model.getLogoForSymbol(symbol) : null;
                    } catch (Exception ignored) {
                    }

                    // Start async load (callback runs on EDT)
                    if (logoUrl != null && !logoUrl.isBlank()) {
                        logoCache.load(symbol, logoUrl, iconSize, iconSize, icon -> {
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
                });
                timer.setRepeats(false);
                timer.start();
            }
            symbolLabel.setText(value.getSymbol());
            nameLabel.setText(value.getName()); // Show ticker again as in the reference image

            // Ensure labels are visible
            symbolLabel.setVisible(true);
            nameLabel.setVisible(true);
            priceLabel.setVisible(true);
            changeLabel.setVisible(true);

            double px = value.getCurrentPrice();
            if (Double.isNaN(px) || px == 0) {
                priceLabel.setText("—");
                priceLabel.setForeground(GUIComponents.TEXT_SECONDARY);
            } else {
                priceLabel.setText(java.lang.String.format(java.util.Locale.US, "$%.2f", px));
                priceLabel.setForeground(GUIComponents.TEXT_PRIMARY);
            }

            double changePercent = value.getChangePercent();
            boolean up = changePercent > 0, down = changePercent < 0;
            java.lang.String arrow = up ? "▲" : (down ? "▼" : "•");
            java.lang.String changeText = Double.isNaN(changePercent) ? "—" : java.lang.String.format(java.util.Locale.US, "%s %+.2f%%", arrow, changePercent);
            changeLabel.setText(changeText);

            // color coding for change
            if (changePercent > 0) {
                changeLabel.setForeground(GUIComponents.GREEN);
            } else if (changePercent < 0) {
                changeLabel.setForeground(GUIComponents.RED);
            } else {
                changeLabel.setForeground(GUIComponents.TEXT_SECONDARY);
            }
        }

        // selection styling
        if (isSelected) {
            setBackground(GUIComponents.BG_SELECTED);
        } else {
            // alternating row colors
            if (index % 2 == 0) {
                setBackground(GUIComponents.BG_DARK);
            } else {
                setBackground(GUIComponents.BG_DARK);
            }
            symbolLabel.setForeground(GUIComponents.TEXT_PRIMARY);
            priceLabel.setForeground(GUIComponents.TEXT_PRIMARY);
            nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        }

        symbolLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        nameLabel.setForeground(GUIComponents.TEXT_SECONDARY);

        Border padding = BorderFactory.createEmptyBorder(12, 16, 12, 16);
        if (index > 0) {
            Border sep = BorderFactory.createMatteBorder(1, 0, 0, 0, GUIComponents.SEPARATOR);
            setBorder(BorderFactory.createCompoundBorder(sep, padding));
        } else {
            setBorder(padding);
        }

        setOpaque(true);
        return this;
    }
}