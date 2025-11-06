package com.gui;

import com.accountmanager.Account;
import com.etl.FinnhubClient;
import com.etl.FinnhubMarketStatus;
import com.etl.TradeSource;
import com.market.Database;
import com.market.Market;
import com.market.MarketListener;
import com.market.TradeItem;
import com.tools.MockFinnhubClient;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class StartupWindow extends ContentPanel {
    private JTextField profileNameField;
    private JTextField balanceField;

    // constructor for creating profile
    public StartupWindow(StartupListener startupListener) {
        createProfileUI(startupListener);
    }

    // constructor used for "Start with existing profile" mode
    public StartupWindow(Runnable onStartExisting) {
        createAccountSelectUI(onStartExisting);
    }

    private void createProfileUI(StartupListener startupListener) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        // Title
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

    private void createAccountSelectUI(Runnable onStartExisting) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JLabel titleLabel = new JLabel("Marketsim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        add(Box.createVerticalStrut(12));

        JLabel info = new JLabel("<html>An existing profile was found.<br/>" +
                "Account selection UI is coming later.<br/><br/>Click Start to launch.</html>");
        info.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(info);
        add(Box.createVerticalStrut(16));

        JButton startButton = new JButton("Start");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(startButton);

        startButton.addActionListener(e -> onStartExisting.run());
    }

    public static void getStartWindow(Database db, StartupListener startupListener) throws SQLException {
        Database.StartupState state = db.determineStartupState();

        final boolean firstRun;
        final long profileId;

        if (state == Database.StartupState.FIRST_RUN) {
            System.out.println("No profile detected");
            firstRun = true;
            profileId = -1;
        } else {
            profileId = db.getSingletonProfileId();
            System.out.println("Profile " + profileId + " loaded from db");
            firstRun = false;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Marketsim Startup");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 400);
            frame.setLocationRelativeTo(null);

            if (firstRun) {
                frame.add(new StartupWindow((profileName, balance) -> {
                    try {
                        long id = db.ensureSingletonProfile(profileName);
                        long accountId = db.getOrCreateAccount(profileName, "USD");
                        runMarketSim(db, profileName, balance);
                        frame.dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame,
                                "Failed to create profile/account:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }));
            } else {
                    frame.add(new StartupWindow(() -> {
                        try {
                            // call db.listAccounts(profileIdIfAny)
                            String placeholderName = "Placeholder";
                            runMarketSim(db, placeholderName, 10000.0); // balance ignored if account already exists
                            frame.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(frame, "Failed to launch Marketsim:\n" + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }));
            }
            frame.setVisible(true);
        });
    }

    public static void runMarketSim(Database db, String profileName, double balance) {
        try {
            // Initialize account
            Account account = new Account(profileName,balance);

            long profileId = db.getOrCreateProfile(profileName);
            long accountId = db.getOrCreateAccount(account.getName(), "USD");
            java.util.List<String> dbSymbols = db.loadWatchlistSymbols(accountId);

            if (dbSymbols.isEmpty()) {
                // add in-memory demo watchlist to database
                java.util.List<String> current = new java.util.ArrayList<>();
                for (com.market.TradeItem ti : account.getWatchList().getWatchlist()) {
                    current.add(ti.getSymbol());
                }
                db.saveWatchlistSymbols(accountId, "Default", current);
                System.out.println("[startup] Seeded DB watchlist from demo account (" + current.size() + " symbols)");
            } else {
                // use watchlist from database
                account.getWatchList().clearList();
                for (String sym : dbSymbols) {
                    // TODO: get the actual name of the symbol
                    account.getWatchList().addWatchlistItem(new com.market.TradeItem(sym, sym));
                }
                System.out.println("[startup] Loaded watchlist from DB (" + dbSymbols.size() + " symbols)");
            }


            // Check if market is open or closed
            System.out.println("Checking market status...");
            boolean marketHours = FinnhubMarketStatus.checkStatus();
            TradeSource client;
            if (marketHours) {
                try {
                    System.out.println("Market open, starting Finnhub...");
                    client = FinnhubClient.start();
                    System.out.println("Finnhub started...");
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Market closed, starting Mock Client...");
                client = MockFinnhubClient.start();
                System.out.println("Mock client started...");
            }

            // Initialize market
            Market market;
            try {
                market = new Market(client, db, account);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start Market", e);
            }
            // temporary no-op listener to safely subscribe in addFromWatchlist()
            // real gui listener attached after MainWindow is initialized
            market.setMarketListener(new MarketListener() {
                @Override public void onMarketUpdate() { }

                @Override
                public void loadSymbols(List<TradeItem> items) { }
            });
            market.addFromWatchlist(account.getWatchList());
            while (!market.isReady()) {
                System.out.println("Waiting for Market status...");
            }
            System.out.println("Market started...");

            // Initialize GUI Client
            SwingUtilities.invokeLater(() -> {
                MainWindow mw = new MainWindow(db, account, market);
                market.setMarketListener(mw.getSymbolListPanel());
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
