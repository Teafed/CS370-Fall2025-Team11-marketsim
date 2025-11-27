package com.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Utility for showing a centered, in-window modal dialog over a darkened overlay
 * using the hosting frame's glass pane.
 */
public final class OverlayDialog {

    private OverlayDialog() { }

    public static void show(Component parent, JComponent content) {
        if (parent == null || content == null) return;

        JRootPane root = SwingUtilities.getRootPane(parent);
        if (root == null) return;

        JComponent glass = (JComponent) root.getGlassPane();
        glass.setVisible(true);
        glass.setOpaque(false);
        glass.setLayout(new BorderLayout());

        JPanel overlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 170)); // alpha controls how dark everything gets
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        overlay.setOpaque(false);

        overlay.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(overlay, e.getPoint(), content);
                if (!content.contains(p)) {
                    close(parent);
                }
            }
        });

        // Prevent clicks inside content from bubbling to overlay
        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                e.consume();
            }
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.NONE;
        overlay.add(content, gc);

        glass.removeAll();
        glass.add(overlay, BorderLayout.CENTER);
        glass.revalidate();
        glass.repaint();
    }

    public static void close(Component parent) {
        if (parent == null) return;
        JRootPane root = SwingUtilities.getRootPane(parent);
        if (root == null) return;
        JComponent glass = (JComponent) root.getGlassPane();
        glass.setVisible(false);
        glass.removeAll();
        glass.revalidate();
        glass.repaint();
    }
}
