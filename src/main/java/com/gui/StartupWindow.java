package com.gui;

import com.etl.finnhub.ClientFacade;
import com.models.Database;
import com.models.ModelFacade;
import com.models.market.Market;
import com.models.market.MarketListener;
import com.models.market.TradeItem;
import com.models.profile.Account;
import com.models.profile.Profile;
import com.models.profile.Watchlist;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

public class StartupWindow extends ContentPanel {
    private JTextField profileNameField;
    private JTextField balanceField;
    public static final boolean USE_ACCOUNT_PICKER = false; // settable from account select?

    // constructor for creating profile
    public StartupWindow(StartupListener startupListener) {
        createProfileUI(startupListener);
    }

    private void createProfileUI(StartupListener startupListener) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JLabel titleLabel = new JLabel("Marketsim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);

        // Profile Name
        add(new JLabel("Profile Name:"));
        profileNameField = new JTextField(1);
        add(profileNameField);

        // Beginning Balance
        add(new JLabel("Beginning Balance:"));
        balanceField = new JTextField("10000");
        add(balanceField);


        // Start Button
        JButton startButton = new JButton("Start Marketsim");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(startButton);

        startButton.addActionListener(e -> {
            String profileName = profileNameField.getText();
            String balanceText = balanceField.getText();

            //TODO Add error checking and response

            double balance = Double.parseDouble(balanceText);
            startupListener.onStart(profileName, balance);
        });
    }

    private static ContentPanel createAccountSelectPanel(List<Account> accounts, Consumer<Account> onPick) {
        ContentPanel panel = new ContentPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JLabel titleLabel = new JLabel("Marketsim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(12));

        JLabel info = new JLabel("Select an account to start:");
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(info);
        panel.add(Box.createVerticalStrut(16));

        JComboBox<Account> combo = new JComboBox<>(accounts.toArray(new Account[0]));
        combo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel lbl = new JLabel(value == null ? "" : value.getName());
            if (isSelected) {
                lbl.setOpaque(true);
                lbl.setBackground(list.getSelectionBackground());
                lbl.setForeground(list.getSelectionForeground());
            }
            return lbl;
        });
        panel.add(combo);
        panel.add(Box.createVerticalStrut(16));

        JButton startButton = new JButton("Start");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(startButton);

        startButton.addActionListener(e -> {
            Account selected = (Account) combo.getSelectedItem();
            if (selected != null) onPick.accept(selected);
        });

        return panel;
    }

    public static void getStartWindow(Database db) throws SQLException {
        Database.StartupState state = db.determineStartupState();
        final boolean firstRun = (state == Database.StartupState.FIRST_RUN);

        if (!firstRun && !USE_ACCOUNT_PICKER) {
            long profileId = db.getSingletonProfileId();
            Profile profile = db.buildProfile(profileId);
            runApp(db, profile);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Marketsim Startup");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 400);
            frame.setLocationRelativeTo(null);

            if (firstRun) {
                frame.add(new StartupWindow((profileName, balance) -> {
                    try {
                        long profileId = db.ensureSingletonProfile(profileName);
                        long accountId = db.getOrCreateAccount(profileName, "USD");
                        db.depositCash(accountId, balance, System.currentTimeMillis(), "Initial deposit");
                        Profile profile = db.buildProfile(profileId);

                        runApp(db, profile);
                        frame.dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame,
                                "Failed to create profile/account:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }));
            } else {
                 // show accounts and start with the chosen one
                 try {
                     long profileId = db.getSingletonProfileId();
                     Profile profile = db.buildProfile(profileId);
                     List<Account> accounts = db.listAccounts(profileId);

                     frame.getContentPane().removeAll();
                     frame.add(createAccountSelectPanel(accounts, selected -> {
                         try {
                             runApp(db, profile, selected);
                             frame.dispose();
                         } catch (Exception ex) {
                             ex.printStackTrace();
                             JOptionPane.showMessageDialog(frame,
                                     "Failed to launch Marketsim:\n" + ex.getMessage(),
                                     "Error", JOptionPane.ERROR_MESSAGE);
                         }
                     }));
                     frame.revalidate();
                     frame.repaint();
                 } catch (Exception ex) {
                     ex.printStackTrace();
                     JOptionPane.showMessageDialog(frame,
                             "Failed to load accounts:\n" + ex.getMessage(),
                             "Error", JOptionPane.ERROR_MESSAGE);
                 }
             }

            frame.setVisible(true);
        });
    }

    public static void runApp(Database db, Profile profile) {
        runApp(db, profile, profile.getFirstAccount());
    }
    public static void runApp(Database db, Profile profile, Account account) {
        try {
            profile.setActiveAccount(account);
            ModelFacade model = new ModelFacade(db, profile);
            SwingUtilities.invokeLater(() -> new MainWindow(model));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to start: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
