package com.gui;

import javax.swing.*;
import java.awt.*;

public class StartupPanel extends JPanel {

    private JTextField profileNameField;
    private JTextField balanceField;
    private JButton startButton;

    public StartupPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));

        // Title
        JLabel titleLabel = new JLabel("Marketsim", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 30)));

        // Profile Name
        add(new JLabel("Profile Name:"));
        profileNameField = new JTextField(20);
        add(profileNameField);
        add(Box.createRigidArea(new Dimension(0, 20)));

        // Beginning Balance
        add(new JLabel("Beginning Balance:"));
        balanceField = new JTextField("10000", 20);
        add(balanceField);
        add(Box.createRigidArea(new Dimension(0, 30)));

        // Start Button
        startButton = new JButton("Start Simulation");
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(startButton);

        // Placeholder for your startup logic
        startButton.addActionListener(e -> {
            String profileName = profileNameField.getText();
            String balanceText = balanceField.getText();

            // TODO: Validate input and start your app logic here
            System.out.println("Starting app with profile: " + profileName + ", balance: $" + balanceText);
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Marketsim Startup");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 400);
            frame.setLocationRelativeTo(null);
            frame.add(new StartupPanel());
            frame.setVisible(true);
        });
    }
}
