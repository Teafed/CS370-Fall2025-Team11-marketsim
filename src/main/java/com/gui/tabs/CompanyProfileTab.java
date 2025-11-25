package com.gui.tabs;

import com.gui.ContentPanel;
import com.gui.GUIComponents;
import com.models.ModelFacade;
import com.models.market.CompanyProfile;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * A tab for displaying company information.
 * Shows IPO date, logo, market cap, name, and website.
 */
public class CompanyProfileTab extends ContentPanel {
    private final JLabel lblName = new JLabel("-", SwingConstants.CENTER);
    private final JLabel lblIPO = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblMarketCap = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblWebsite = new JLabel("-", SwingConstants.LEFT);
    private final JLabel lblLogo = new JLabel();
    private ModelFacade model;
    private Supplier<String> selectedSymbol;
    private Timer refreshTimer;

    private final NumberFormat moneyFmt = NumberFormat.getCurrencyInstance(Locale.US);
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy");


    public CompanyProfileTab(ModelFacade model, Supplier<String> selectedSymbol) {
        this.selectedSymbol = selectedSymbol;
        this.model = model;

        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_DARK);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Company name
        lblName.setFont(new Font("Arial", Font.BOLD, 18));
        lblName.setForeground(GUIComponents.TEXT_PRIMARY);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblName);
        contentPanel.add(Box.createVerticalStrut(10));

        // Logo
        lblLogo.setHorizontalAlignment(SwingConstants.CENTER);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(lblLogo);
        contentPanel.add(Box.createVerticalStrut(10));

        // IPO date
        contentPanel.add(createInfoRow("IPO Date:", lblIPO));
        contentPanel.add(Box.createVerticalStrut(6));

        // Market cap
        contentPanel.add(createInfoRow("Market Cap:", lblMarketCap));
        contentPanel.add(Box.createVerticalStrut(6));

        // Website
        contentPanel.add(createInfoRow("Website:", lblWebsite));
        contentPanel.add(Box.createVerticalStrut(6));

        JScrollPane scrollPane = GUIComponents.createScrollPane(contentPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        refreshTimer = new Timer(1000, e -> refreshReadouts());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        SwingUtilities.invokeLater(this::refreshReadouts);


    }

    private JPanel createInfoRow(String labelText, JLabel valueLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(GUIComponents.TEXT_SECONDARY);

        valueLabel.setFont(new Font("Arial", Font.BOLD, 13));
        valueLabel.setForeground(GUIComponents.TEXT_PRIMARY);

        panel.add(label, BorderLayout.WEST);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }


    private void refreshReadouts() {
        String sym = null;
        try {
            sym = selectedSymbol.get();
        } catch (Exception ignore) {}

        if (sym == null || sym.isBlank()) {
            lblName.setText("-");
            lblIPO.setText("-");
            lblMarketCap.setText("-");
            lblWebsite.setText("-");
            lblLogo.setIcon(null);
            return;
        }

        CompanyProfile profile = model.getCompanyProfile(sym);
        if (profile == null) {
            lblName.setText("Loading...");
            lblIPO.setText("-");
            lblMarketCap.setText("-");
            lblWebsite.setText("-");
            lblLogo.setIcon(null);
            return;
        }

        lblName.setText(profile.getName());
        lblWebsite.setText(profile.getWeburl());

        // setup ipo date
        String ipo = profile.getIpo();
        lblIPO.setText(ipo);
        if (!ipo.equals("unknown")) {
            LocalDate ipoDate = LocalDate.parse(ipo);
            String formatted = ipoDate.format(fmt);
            lblIPO.setText(formatted);
        }


        // setup market cap
        String mkcap = profile.getMarketCap();
        lblMarketCap.setText(mkcap);
        if (!mkcap.equals("unknown")) {
            double marketCapMillions =  Double.parseDouble(profile.getMarketCap());
            double marketCapDollars = marketCapMillions * 1_000_000;
            lblMarketCap.setText(moneyFmt.format(marketCapDollars));
        }



        try {
            ImageIcon icon = new ImageIcon(new java.net.URL(profile.getLogo()));
            Image scaled = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            lblLogo.setIcon(null);
        }
    }

    private String getSymbol() {
        try {
            return selectedSymbol.get();
        }
        catch (Exception ignore) {
            return null;
        }
    }
}