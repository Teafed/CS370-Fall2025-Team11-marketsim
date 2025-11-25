package com.gui;

import com.models.Database;
import com.models.ModelFacade;
import com.models.profile.Profile;
import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.text.*;
import java.util.function.BiConsumer;

/**
 * Window for initial setup or account selection.
 * Allows creating a new profile or selecting an existing account.
 */
public class StartupWindow extends ContentPanel {
    private JTextField profileNameField;
    private JTextField nameField;
    private JTextField balanceField;
    private JButton continueButton;
    private JLabel errorLabel;

    private static final double MIN_BALANCE = 100.0;
    private static final double MAX_BALANCE = 100_000_000.0;
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    /**
     * Constructs a new StartupWindow for creating a profile.
     *
     * @param startupListener The listener for startup events.
     */
    public StartupWindow(StartupListener startupListener) {
        createProfileUI(startupListener);
    }

    private void createProfileUI(StartupListener startupListener) {
        setLayout(null);
        setBackground(new Color(20, 24, 32));
        setPreferredSize(new Dimension(700, 700));

        // Add component listener to re-center when resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                centerContent();
            }
        });

        // Logo panel with chart icon
        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Rounded square background with gradient
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(90, 110, 230),
                        getWidth(), getHeight(), new Color(110, 90, 230)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, 100, 100, 25, 25);

                // Draw upward trending chart line
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Draw upward trending zig-zag chart line
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // More dramatic zig-zag coordinates
                int[] xPoints = {20, 35, 50, 65, 80};
                int[] yPoints = {70, 45, 60, 40, 25};
                g2.drawPolyline(xPoints, yPoints, xPoints.length);

            }
        };
        logoPanel.setBounds(300, 80, 100, 100);
        logoPanel.setOpaque(false);
        logoPanel.setName("logo"); // For centering reference
        add(logoPanel);

        // MarketSim title
        JLabel titleLabel = new JLabel("<html><span style='color: white;'>Market</span><span style='color: #7B8CDE;'>Sim</span></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setBounds(220, 195, 400, 60);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setName("title"); // For centering reference
        add(titleLabel);

        // Form container
        JPanel formPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 35, 45));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
        };
        formPanel.setLayout(null);
        formPanel.setBounds(100, 300, 500, 360);
        formPanel.setOpaque(false);
        formPanel.setName("form"); // For centering reference
        add(formPanel);

        // "Your Name" label
        JLabel nameLabel = new JLabel("Your Name");
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameLabel.setForeground(new Color(160, 160, 170));
        nameLabel.setBounds(40, 40, 420, 25);
        formPanel.add(nameLabel);

        // Name input field
        nameField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 45, 58));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
            }
        };
        nameField.setBounds(40, 70, 420, 50);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        nameField.setForeground(new Color(160, 160, 170));
        nameField.setCaretColor(Color.WHITE);
        nameField.setBackground(new Color(40, 45, 58));
        nameField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        nameField.setOpaque(false);
        nameField.setText("Enter your name");

        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (nameField.getText().equals("Enter your name")) {
                    nameField.setText("");
                    nameField.setForeground(Color.WHITE);
                }
                clearError();
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (nameField.getText().isEmpty()) {
                    nameField.setText("Enter your name");
                    nameField.setForeground(new Color(160, 160, 170));
                }
            }
        });
        formPanel.add(nameField);

        // "Initial Balance" label
        JLabel balanceLabel = new JLabel("Initial Balance");
        balanceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        balanceLabel.setForeground(new Color(160, 160, 170));
        balanceLabel.setBounds(40, 140, 420, 25);
        formPanel.add(balanceLabel);

        // Balance input field with $ prefix
        balanceField = new JTextField("$ 10,000.00") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 45, 58));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
            }
        };
        balanceField.setBounds(40, 170, 420, 50);
        balanceField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        balanceField.setForeground(Color.WHITE);
        balanceField.setCaretColor(Color.WHITE);
        balanceField.setBackground(new Color(40, 45, 58));
        balanceField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        balanceField.setOpaque(false);

        // Handle balance field formatting
        balanceField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                clearError();
                // Remove formatting for editing
                String text = balanceField.getText().replace("$", "").replace(",", "").trim();
                balanceField.setText(text);
            }
            @Override
            public void focusLost(FocusEvent e) {
                // Reformat with $ and commas
                formatBalanceField();
            }
        });

        balanceField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                String current = balanceField.getText();

                // Allow digits, one decimal point, backspace, delete
                if (!Character.isDigit(c) && c != '.' && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                    return;
                }

                // Only allow one decimal point
                if (c == '.' && current.contains(".")) {
                    e.consume();
                }

                // Limit decimal places to 2
                if (current.contains(".")) {
                    String[] parts = current.split("\\.");
                    if (parts.length > 1 && parts[1].length() >= 2 &&
                            balanceField.getCaretPosition() > current.indexOf('.')) {
                        e.consume();
                    }
                }
            }
        });

        formPanel.add(balanceField);

        // Suggested range label
        JLabel suggestedLabel = new JLabel("Suggested: $10,000 - $100,000");
        suggestedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        suggestedLabel.setForeground(new Color(100, 100, 110));
        suggestedLabel.setBounds(40, 225, 420, 20);
        formPanel.add(suggestedLabel);

        // Error message label (hidden by default)
        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        errorLabel.setForeground(new Color(244, 67, 54));
        errorLabel.setBounds(40, 250, 420, 20);
        errorLabel.setVisible(false);
        formPanel.add(errorLabel);

        // Continue button with gradient
        continueButton = new JButton("Continue to Market") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(90, 110, 230),
                        getWidth(), 0, new Color(130, 90, 230)
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, x, y);
            }
        };
        continueButton.setBounds(40, 280, 420, 50);
        continueButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        continueButton.setForeground(Color.WHITE);
        continueButton.setBorder(null);
        continueButton.setContentAreaFilled(false);
        continueButton.setFocusPainted(false);
        continueButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        continueButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                continueButton.setFont(new Font("Segoe UI", Font.BOLD, 17));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                continueButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
            }
        });

        continueButton.addActionListener(e -> {
            if (validateAndSubmit(startupListener)) {
                // Validation passed, submission handled
            }
        });

        formPanel.add(continueButton);

        // Initial centering
        SwingUtilities.invokeLater(this::centerContent);
    }

    /**
     * Centers all content in the middle of the panel, regardless of size
     */
    private void centerContent() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        if (panelWidth == 0 || panelHeight == 0) return;

        // Find components
        Component logo = findComponentByName("logo");
        Component title = findComponentByName("title");
        Component form = findComponentByName("form");

        if (logo == null || title == null || form == null) return;

        // Calculate total content height
        int logoHeight = logo.getHeight();
        int titleHeight = title.getHeight();
        int formHeight = form.getHeight();
        int spacingLogoToTitle = 15;
        int spacingTitleToForm = 45;

        int totalHeight = logoHeight + spacingLogoToTitle + titleHeight + spacingTitleToForm + formHeight;

        // Calculate starting Y position to center vertically
        int startY = (panelHeight - totalHeight) / 2;

        // Position logo (centered horizontally)
        int logoX = (panelWidth - logo.getWidth()) / 2;
        logo.setBounds(logoX, startY, logo.getWidth(), logo.getHeight());

        // Position title (centered horizontally)
        int titleX = (panelWidth - title.getWidth()) / 2;
        int titleY = startY + logoHeight + spacingLogoToTitle;
        title.setBounds(titleX, titleY, title.getWidth(), title.getHeight());

        // Position form (centered horizontally)
        int formX = (panelWidth - form.getWidth()) / 2;
        int formY = titleY + titleHeight + spacingTitleToForm;
        form.setBounds(formX, formY, form.getWidth(), form.getHeight());

        revalidate();
        repaint();
    }

    /**
     * Helper to find component by name
     */
    private Component findComponentByName(String name) {
        for (Component comp : getComponents()) {
            if (name.equals(comp.getName())) {
                return comp;
            }
        }
        return null;
    }

    private void formatBalanceField() {
        try {
            String text = balanceField.getText().replace("$", "").replace(",", "").trim();
            if (!text.isEmpty()) {
                double value = Double.parseDouble(text);
                balanceField.setText("$ " + CURRENCY_FORMAT.format(value));
            }
        } catch (NumberFormatException e) {
            balanceField.setText("$ 10,000.00");
        }
    }

    private boolean validateAndSubmit(StartupListener listener) {
        // Validate name
        String name = nameField.getText().trim();
        if (name.isEmpty() || name.equals("Enter your name")) {
            showError("Please enter your name");
            nameField.requestFocus();
            return false;
        }

        // Validate balance
        try {
            String balanceText = balanceField.getText()
                    .replace("$", "")
                    .replace(",", "")
                    .trim();

            if (balanceText.isEmpty()) {
                showError("Please enter an initial balance");
                balanceField.requestFocus();
                return false;
            }

            double balance = Double.parseDouble(balanceText);

            if (balance < MIN_BALANCE) {
                showError(String.format("Minimum balance is $%.0f", MIN_BALANCE));
                balanceField.requestFocus();
                return false;
            }

            if (balance > MAX_BALANCE) {
                showError(String.format("Maximum balance is $%s", CURRENCY_FORMAT.format(MAX_BALANCE)));
                balanceField.requestFocus();
                return false;
            }

            // All validation passed
            listener.onStart(name, balance);
            return true;

        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
            balanceField.requestFocus();
            return false;
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
    }

    private static ContentPanel createAccountSelectPanel(
            List<Account> accounts,
            BiConsumer<Account, Boolean> onPick) {
        ContentPanel panel = new ContentPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        JLabel titleLabel = new JLabel("Marketsim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(GUIComponents.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(12));

        JLabel info = new JLabel("Select an account to start:");
        info.setForeground(GUIComponents.TEXT_SECONDARY);
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
        panel.add(Box.createVerticalStrut(12));

        JCheckBox alwaysUse = new JCheckBox("Always use this account on startup");
        alwaysUse.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(alwaysUse);
        panel.add(Box.createVerticalStrut(16));

        JButton startButton = new JButton("Start");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(startButton);

        startButton.addActionListener(e -> {
            Account selected = (Account) combo.getSelectedItem();
            if (selected != null)
                onPick.accept(selected, alwaysUse.isSelected());
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
        SwingUtilities.invokeLater(() -> {
            // splash screen
            JFrame frame = new JFrame();
            frame.setUndecorated(true);
            frame.setSize(500, 250);
            frame.setLocationRelativeTo(null);

            JPanel message = new JPanel(new BorderLayout());
            message.setBackground(new Color(30, 30, 30));
            JLabel label = new JLabel("Welcome to Marketsim :)", SwingConstants.CENTER);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("SansSerif", Font.BOLD, 20));
            message.add(label, BorderLayout.CENTER);
            frame.setContentPane(message);
            frame.setVisible(true);

            new SwingWorker<Void, Void>() {
                boolean firstRun = false;
                Profile profile;
                Account defaultAccount;
                Exception error;

                @Override
                protected Void doInBackground() throws SQLException {
                    try {
                        Database.StartupState state = db.determineStartupState();
                        this.firstRun = (state == Database.StartupState.FIRST_RUN);

                        if (!firstRun) {
                            long profileId = db.getSingletonProfileId();
                            profile = db.buildProfile(profileId);
                            Long defaultAccountId = db.getDefaultAccountId(profileId);
                            if (defaultAccountId != null) {
                                defaultAccount = profile.getAccounts().stream()
                                        .filter(a -> a.getId() == defaultAccountId)
                                        .findFirst().orElse(null);

                                if (defaultAccount == null) {
                                    db.clearDefaultAccount(profileId);
                                }
                            }
                        }

                    } catch (Exception e) {
                        error = e;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (error != null) {
                        error.printStackTrace();
                        JOptionPane.showMessageDialog(frame,
                                "Startup failed:\n" + error.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (!firstRun && defaultAccount != null) {
                        runApp(db, profile, defaultAccount);
                        frame.dispose();
                        return;
                    }

                    if (firstRun) {
                        frame.setContentPane(new StartupWindow((profileName, balance) -> {
                            try {
                                long profileId = db.ensureSingletonProfile(profileName);
                                long accountId = db.getOrCreateAccount("Default", "USD");
                                db.depositCash(accountId, balance, System.currentTimeMillis(), "Initial deposit");
                                Profile p = db.buildProfile(profileId);
                                runApp(db, p);
                                frame.dispose();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                JOptionPane.showMessageDialog(frame,
                                        "Failed to create profile/account:\n" + ex.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }));
                        frame.revalidate();
                        frame.repaint();
                    } else {
                        ContentPanel picker = createAccountSelectPanel(
                                profile.getAccounts(),
                                (selected, alwaysUse) -> {
                                    try {
                                        if (alwaysUse) {
                                            db.setDefaultAccountId(profile.getId(), selected.getId());
                                        }
                                        runApp(db, profile, selected);
                                        frame.dispose();
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                        JOptionPane.showMessageDialog(frame,
                                                "Failed to launch Marketsim:\n" + ex.getMessage(),
                                                "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                }
                        );
                        frame.setContentPane(picker);
                        frame.revalidate(); frame.repaint();
                    }
                }
            }.execute();
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