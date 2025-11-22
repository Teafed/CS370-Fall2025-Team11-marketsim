package com.gui;

import com.models.Database;
import com.models.ModelFacade;
import com.models.profile.Account;
import com.models.profile.Profile;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Window for initial setup or account selection.
 * Allows creating a new profile or selecting an existing account.
 */
public class StartupWindow extends ContentPanel {
    private JTextField profileNameField;
    private JTextField balanceField;
    public static final boolean USE_ACCOUNT_PICKER = true; // settable from account select?

    // constructor for creating profile
    /**
     * Constructs a new StartupWindow for creating a profile.
     *
     * @param startupListener The listener for startup events.
     */
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

            // TODO Add error checking and response

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
            if (selected != null)
                onPick.accept(selected);
        });

        return panel;
    }

    /**
     * Entry point for the GUI. Determines the startup state and shows the
     * appropriate window.
     *
     * @param db The Database instance.
     * @throws SQLException If a database error occurs.
     */
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
                     List<Account> accounts = profile.getAccounts();

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

    /**
     * Launches the main application window.
     *
     * @param db      The Database instance.
     * @param profile The user Profile.
     */
    public static void runApp(Database db, Profile profile) {
        runApp(db, profile, profile.getFirstAccount());
    }

    /**
     * Launches the main application window with a specific account.
     *
     * @param db      The Database instance.
     * @param profile The user Profile.
     * @param account The active Account.
     */
    public static void runApp(Database db, Profile profile, Account account) {
        try {
            ModelFacade model = new ModelFacade(db, profile);
            model.setActiveAccount(account);
            SwingUtilities.invokeLater(() -> new MainWindow(model));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to start: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
