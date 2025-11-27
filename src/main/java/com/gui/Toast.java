package com.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Small floating toast notification, rendered inside the app window near
 * a specific component (e.g. an input field). Fades out smoothly.
 */
public final class Toast {

    private static class ToastPanel extends JPanel {
        private float alpha = 1f;

        ToastPanel(String message) {
            setOpaque(false);
            setLayout(new BorderLayout());

            JLabel label = new JLabel(message);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            add(label, BorderLayout.CENTER);
        }

        void setAlpha(float a) {
            alpha = a;
            repaint();
        }

        float getAlpha() {
            return alpha;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension base = super.getPreferredSize();
            return new Dimension(base.width + 12, base.height + 8);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            g2.setColor(GUIComponents.BG_LIGHTER);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            g2.setColor(GUIComponents.BG_LIGHT);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private Toast() { }

    public static void show(Component relativeTo, String message) {
        if (relativeTo == null || message == null || message.isBlank()) return;

        JRootPane root = SwingUtilities.getRootPane(relativeTo);
        if (root == null) return;

        Component glass = root.getGlassPane();
        boolean useGlass = glass instanceof JComponent && glass.isVisible();

        JComponent target;
        if (useGlass) {
            target = (JComponent) glass;
        } else {
            target = root.getLayeredPane();
        }

        // Make sure we can position freely
        if (target.getLayout() != null) {
            target.setLayout(null);
        }

        ToastPanel toast = new ToastPanel(message);
        toast.setSize(toast.getPreferredSize());

        Point compOnScreen = relativeTo.getLocationOnScreen();
        Point compInTarget = new Point(compOnScreen);
        SwingUtilities.convertPointFromScreen(compInTarget, target);

        int x = compInTarget.x;
        int y = compInTarget.y - toast.getHeight() - 8; // try above

        if (y < 8) {
            // Put below if there's no room above
            y = compInTarget.y + relativeTo.getHeight() + 8;
        }

        x = Math.max(8, Math.min(x, target.getWidth() - toast.getWidth() - 8));

        toast.setLocation(x, y);

        if (useGlass) {
            target.add(toast);
            // Make sure it's on top of other glass children
            target.setComponentZOrder(toast, 0);
        } else if (target instanceof JLayeredPane lp) {
            lp.add(toast, JLayeredPane.DRAG_LAYER); // highest standard layer
        } else {
            target.add(toast);
        }

        target.revalidate();
        target.repaint();

        // Fade-out timer
        Timer fadeTimer = new Timer(40, null);
        fadeTimer.setInitialDelay(1800);
        fadeTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                float a = toast.getAlpha() - 0.08f;
                if (a <= 0f) {
                    fadeTimer.stop();
                    target.remove(toast);
                    target.revalidate();
                    target.repaint();
                } else {
                    toast.setAlpha(a);
                }
            }
        });
        fadeTimer.start();
    }
}
