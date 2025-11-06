package com.gui.startup;

import javax.swing.*;
import java.awt.*;

public class StartupPanel extends JPanel {

    private JTextField profileNameField;
    private JTextField balanceField;
    private JButton startButton;

    public StartupPanel(StartupListener startupListener) {

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
        startButton = new JButton("Start Simulation");
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




    public static void getStartWindow(StartupListener startupListener) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Marketsim Startup");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 400);
            frame.setLocationRelativeTo(null);
            frame.add(new StartupPanel(startupListener));
            frame.setVisible(true);
        });
    }
}
