package com.gui;

import com.models.ModelFacade;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SearchPanel extends ContentPanel {

    private final JTextField searchField;
    private final JButton searchButton;
    private final JPopupMenu suggestionPopup;
    private ModelFacade model;

    private static final Color BG_DARK = new Color(30, 34, 45);
    private static final Color SEARCH_BG = new Color(45, 50, 65);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(120, 125, 140);
    private static final Color BORDER_COLOR = new Color(60, 65, 80);

    public SearchPanel(ModelFacade model) {
        this.model = model;
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Create search panel with rounded look
        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.setBackground(SEARCH_BG);
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // Search icon label
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setForeground(TEXT_SECONDARY);
        searchIcon.setBorder(new EmptyBorder(0, 0, 0, 8));

        searchField = new JTextField();
        searchField.setBackground(SEARCH_BG);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(TEXT_PRIMARY);
        searchField.setFont(new Font("Arial", Font.PLAIN, 15));
        searchField.setBorder(null);
        searchField.setOpaque(false);

        // Placeholder text effect
        searchField.setText("Search stocks...");
        searchField.setForeground(TEXT_SECONDARY);

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals("Search stocks...")) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_PRIMARY);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(TEXT_SECONDARY);
                    searchField.setText("Search stocks...");
                }
            }
        });

        searchButton = new JButton();
        searchButton.setVisible(false); // Hidden, triggered by Enter key

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setBackground(SEARCH_BG);
        suggestionPopup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        searchContainer.add(searchIcon, BorderLayout.WEST);
        searchContainer.add(searchField, BorderLayout.CENTER);

        add(searchContainer, BorderLayout.CENTER);

        searchField.addActionListener(e -> performSearch());
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (!query.isEmpty() && !query.equals("Search stocks...")) {
            String[][] results = model.searchSymbol(query);
            showSuggestions(results);
        }
    }

    private void showSuggestions(String[][] symbolDescriptions) {
        suggestionPopup.removeAll();

        for (String[] pair : symbolDescriptions) {
            String symbol = pair[0];
            String description = pair[1];
            String display = symbol + " — " + description;

            JMenuItem item = new JMenuItem(display);
            item.setBackground(SEARCH_BG);
            item.setForeground(TEXT_PRIMARY);
            item.setFont(new Font("Arial", Font.PLAIN, 14));
            item.setBorder(new EmptyBorder(8, 12, 8, 12));

            // Hover effect
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    item.setBackground(new Color(55, 60, 75));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    item.setBackground(SEARCH_BG);
                }
            });

            item.addActionListener(e2 -> {
                suggestionPopup.setVisible(false);
                try {
                    model.addToWatchlist(symbol);
                    searchField.setText("");
                    searchField.setForeground(TEXT_SECONDARY);
                    searchField.setText("Search stocks...");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            suggestionPopup.add(item);
        }

        suggestionPopup.show(searchField, 0, searchField.getHeight() + 5);
    }
}