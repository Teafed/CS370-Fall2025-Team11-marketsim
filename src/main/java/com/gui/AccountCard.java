// file: com/gui/AccountCard.java
package com.gui;

import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class AccountCard extends ContentPanel {
    public enum Mode { ACCOUNT, ADD }

    private final Mode mode;
    private final Account account;                // only when mode == ACCOUNT
    private final Consumer<Account> onPick;       // only when mode == ACCOUNT
    private final Runnable onAdd;                 // only when mode == ADD

    private boolean hover;

    // constructor for an account tile
    public static AccountCard forAccount(Account account, Consumer<Account> onPick) {
        return new AccountCard(Mode.ACCOUNT, account, onPick, null);
    }

    // constructor for the "+ add account" tile
    public static AccountCard forAdd(Runnable onAdd) {
        return new AccountCard(Mode.ADD, null, null, onAdd);
    }

    private AccountCard(Mode mode, Account account, Consumer<Account> onPick, Runnable onAdd) {
        this.mode = mode;
        this.account = account;
        this.onPick = onPick;
        this.onAdd  = onAdd;

        setPreferredSize(new Dimension(140, 170));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
        setFocusable(true);
        setToolTipText(mode == Mode.ACCOUNT && account != null ? account.getName() : null);

        // mouse + keyboard activation
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activate(); }
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
        };
        addMouseListener(ma);

        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "activate");
        getActionMap().put("activate", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { activate(); }
        });
    }

    private void activate() {
        if (mode == Mode.ACCOUNT && onPick != null && account != null) onPick.accept(account);
        else if (mode == Mode.ADD && onAdd != null) onAdd.run();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            // card background
            g2.setColor(new Color(245, 246, 248));
            g2.fillRoundRect(6, 6, w - 12, h - 12, 16, 16);

            // hover/focus ring
            if (hover || isFocusOwner()) {
                g2.setColor(new Color(0, 0, 0, 35));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(6, 6, w - 12, h - 12, 16, 16);
            }

            // avatar circle
            int circleSize = 84;
            int cx = w / 2 - circleSize / 2;
            int cy = 18;
            g2.setColor(new Color(220, 224, 230));
            g2.fillOval(cx, cy, circleSize, circleSize);
            g2.setColor(new Color(200, 204, 210));
            g2.drawOval(cx, cy, circleSize, circleSize);

            if (mode == Mode.ACCOUNT) {
                // letter
                String letter = (account == null || account.getName() == null || account.getName().isBlank())
                        ? "?" : account.getName().trim().substring(0, 1).toUpperCase();
                g2.setFont(getFont().deriveFont(Font.BOLD, 36f));
                FontMetrics fm = g2.getFontMetrics();
                int tx = w / 2 - fm.stringWidth(letter) / 2;
                int ty = cy + circleSize / 2 + fm.getAscent() / 3;
                g2.setColor(new Color(70, 74, 79));
                g2.drawString(letter, tx, ty);

                // name
                String name = account != null && account.getName() != null ? account.getName() : "";
                drawCenteredText(g2, name, w, 18 + circleSize + 28);
            } else {
                // plus icon
                int ccx = w / 2;
                int ccy = cy + circleSize / 2;
                g2.setColor(new Color(120, 124, 129));
                int bar = 28;
                g2.fillRoundRect(ccx - bar / 2, ccy - 3, bar, 6, 6, 6);
                g2.fillRoundRect(ccx - 3, ccy - bar / 2, 6, bar, 6, 6);

                drawCenteredText(g2, "Add account", w, 18 + circleSize + 28);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawCenteredText(Graphics2D g2, String text, int width, int baselineY) {
        g2.setColor(new Color(40, 44, 49));
        g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        FontMetrics fm = g2.getFontMetrics();
        String clipped = clip(text, fm, width - 20);
        int x = width / 2 - fm.stringWidth(clipped) / 2;
        g2.drawString(clipped, x, baselineY);
    }

    private static String clip(String s, FontMetrics fm, int maxWidth) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= maxWidth) return s;
        String ell = "…";
        int w = fm.stringWidth(ell);
        for (int i = 0; i < s.length(); i++) {
            String sub = s.substring(0, i);
            if (fm.stringWidth(sub) + w > maxWidth) {
                return s.substring(0, Math.max(0, i - 1)) + ell;
            }
        }
        return s;
    }
}