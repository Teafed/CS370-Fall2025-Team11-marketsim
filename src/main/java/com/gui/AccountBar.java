package com.gui;

import com.models.profile.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;

public class AccountBar extends JPanel {
    private final JLabel avatar = new JLabel();
    private final JLabel name = new JLabel();
    private final JLabel total = new JLabel();
    private Runnable onClick = null;
    private Account account;

    public AccountBar() {
        super(new BorderLayout(10, 0));
        setOpaque(true);
        setBackground(GUIComponents.BG_MEDIUM);
        setBorder(new EmptyBorder(10, 12, 10, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        name.setForeground(GUIComponents.TEXT_PRIMARY);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        total.setForeground(GUIComponents.TEXT_SECONDARY);
        total.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.add(name);
        texts.add(total);

        add(avatar, BorderLayout.WEST);
        add(texts, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (onClick != null) onClick.run();
            }
            @Override public void mouseEntered(MouseEvent e) {
                setBackground(GUIComponents.BG_LIGHT);
            }
            @Override public void mouseExited(MouseEvent e) {
                setBackground(GUIComponents.BG_MEDIUM);
            }
        });
    }

    public void setAccount(Account account) {
        this.account = account;
        name.setText(account.getName());
        total.setText(NumberFormat.getCurrencyInstance().format(account.getCash()));
        avatar.setIcon(createAvatarIcon(account.getName()));
        revalidate();
        repaint();
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    private static Icon createAvatarIcon(String displayName) {
        final int size = 28;
        final String initials = safeInitials(displayName);
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GUIComponents.ACCENT_BLUE);
                g2.fillOval(x, y, size, size);
                g2.setColor(Color.WHITE);
                Font f = new Font("Segoe UI", Font.BOLD, 12);
                g2.setFont(f);
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(initials)) / 2;
                int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    private static String safeInitials(String name) {
        if (name == null || name.isBlank()) return "A";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase();
        String second = parts.length > 1 ? parts[1].substring(0, 1).toUpperCase() : "";
        return first + second;
    }
}
