package com.gui;

import com.models.AccountDTO;
import com.models.ModelFacade;
import com.models.ModelListener;
import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.text.NumberFormat;

/**
 * A panel displaying detailed information about a specific account.
 * Shows the account name and current balance.
 */
public class AccountPanel extends ContentPanel implements ModelListener {
    private final ModelFacade model;

    private final JLabel profileLabel = new JLabel();
    private final JLabel accountLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();

    public AccountPanel(ModelFacade model) {
        this.model = model;
        model.addListener(this);

        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_DARK);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        headerBar.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnNew = new JButton("New Account");
        JButton btnSwitch = new JButton("Switch Account");
        btnNew.addActionListener(e -> onCreateAccount());
        btnSwitch.addActionListener(e -> onSwitchAccount());

        actions.add(btnNew);
        actions.add(btnSwitch);
        headerBar.add(actions, BorderLayout.EAST);

        add(headerBar, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Account");
        title.setForeground(GUIComponents.TEXT_PRIMARY);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        styleLabel(profileLabel, true);
        styleLabel(accountLabel, false);
        styleMoney(totalLabel);

        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(profileLabel);
        content.add(Box.createVerticalStrut(2));
        content.add(accountLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(totalLabel);
        content.add(Box.createVerticalStrut(16));

        JScrollPane scroller = new JScrollPane(content);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        add(scroller, BorderLayout.CENTER);

        try {
            onAccountChanged(model.getAccountDTO());
        } catch (Exception ignore) {
            refreshLabels(null);
        }
    }

    private static void styleLabel(JLabel l, boolean primary) {
        l.setForeground(primary ? GUIComponents.TEXT_PRIMARY : GUIComponents.TEXT_SECONDARY);
        l.setFont(new Font("Segoe UI", primary ? Font.BOLD : Font.PLAIN, primary ? 14 : 13));
    }

    private static void styleMoney(JLabel l) {
        l.setForeground(GUIComponents.TEXT_SECONDARY);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void refreshLabels(AccountDTO dto) {
        String owner = model.getProfileName();
        String acctName = model.getActiveAccountName();

        profileLabel.setText("Profile: " + (owner == null ? "—" : owner));
        accountLabel.setText("Active account: " + (acctName == null ? "—" : acctName));

        if (dto != null) {
            NumberFormat currency = NumberFormat.getCurrencyInstance();
            totalLabel.setText("Cash: " + currency.format(dto.cash()));
        } else {
            totalLabel.setText("Cash: —");
        }

        revalidate();
        repaint();
    }

    private void onCreateAccount() {
        JTextField nameField = new JTextField();
        JFormattedTextField depositField = new JFormattedTextField(NumberFormat.getNumberInstance());
        depositField.setValue(0.0);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0; gc.gridy = 0; panel.add(new JLabel("Account name:"), gc);
        gc.gridx = 1; gc.gridy = 0; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        panel.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.fill = GridBagConstraints.NONE; gc.weightx = 0.0;
        panel.add(new JLabel("Initial deposit:"), gc);
        gc.gridx = 1; gc.gridy = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        panel.add(depositField, gc);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Create Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        double deposit = 0.0;
        try {
            depositField.commitEdit();
            Object v = depositField.getValue();
            if (v instanceof Number n) deposit = n.doubleValue();
        } catch (Exception ignored) { }

        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter an account name.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (deposit < 0) {
            JOptionPane.showMessageDialog(this, "Initial deposit cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            model.createAccount(name, deposit);
            JOptionPane.showMessageDialog(this, "Account created and set active.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to create account:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSwitchAccount() {
        var accounts = model.listAccounts();
        if (accounts == null || accounts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No accounts available. Create one first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        DefaultComboBoxModel<Account> comboBoxModel = new DefaultComboBoxModel<>();
        for (Account a : accounts) comboBoxModel.addElement(a);

        JComboBox<Account> combo = new JComboBox<>(comboBoxModel);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Account a) setText(a.getName());
                return this;
            }
        });

        int result = JOptionPane.showConfirmDialog(
                this, combo, "Switch Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        Account selected = (Account) combo.getSelectedItem();
        if (selected == null) return;

        try {
            model.switchAccount(selected.getId());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to switch account:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // listeners
    @Override public void onAccountChanged(AccountDTO snapshot) { refreshLabels(snapshot); }
    @Override public void onQuotesUpdated() { /* ignore */ }
    @Override public void onWatchlistChanged(java.util.List<com.models.market.TradeItem> items) { /* ignore */ }
    @Override public void onError(String message, Throwable t) { /* optional */ }

    @Override public void addNotify() {
        super.addNotify();
        model.addListener(this);
    }
    @Override public void removeNotify() {
        model.removeListener(this);
        super.removeNotify();
    }
}
