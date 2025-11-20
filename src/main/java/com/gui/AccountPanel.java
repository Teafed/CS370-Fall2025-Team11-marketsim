package com.gui;

import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;

/**
 * A panel displaying detailed information about a specific account.
 * Shows the account name and current balance.
 */
public class AccountPanel extends ContentPanel {
    private final Account account;

    /**
     * Constructs a new AccountPanel.
     *
     * @param account The Account to display.
     */
    public AccountPanel(Account account) {
        this.account = account;

        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_DARK);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel("Account");
        header.setForeground(GUIComponents.TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel name = new JLabel(account.getName());
        name.setForeground(GUIComponents.TEXT_PRIMARY);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        NumberFormat balance = NumberFormat.getCurrencyInstance();
        JLabel total = new JLabel(balance.format(account.getCash()));
        total.setForeground(GUIComponents.TEXT_SECONDARY);
        total.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        content.add(header);
        content.add(Box.createVerticalStrut(8));
        content.add(name);
        content.add(Box.createVerticalStrut(2));
        content.add(total);
        content.add(Box.createVerticalStrut(16));

        // future sections can stack here; keep scrollable now
        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);

        setOpaque(true);
        setBackground(GUIComponents.BG_DARK);

        add(scroller, BorderLayout.CENTER);
    }
}
