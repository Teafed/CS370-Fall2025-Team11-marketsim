package com.gui.navigation;

import com.gui.ContentPanel;
import com.gui.GUIComponents;
import com.models.ModelFacade;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A panel for searching stock symbols.
 * Provides a text field and a popup with search suggestions.
 */
public class SearchPanel extends ContentPanel {

    private final JTextField searchField;
    private final JButton searchButton;
    private final JPopupMenu suggestionPopup;
    private ModelFacade model;


    /**
     * Constructs a new SearchPanel.
     *
     * @param model The ModelFacade instance.
     */
    public SearchPanel(ModelFacade model) {
        this.model = model;
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_DARK);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Create search panel with rounded look
        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.setBackground(GUIComponents.SEARCH_BG);
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // Search icon label
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setForeground(GUIComponents.TEXT_SECONDARY);
        searchIcon.setBorder(new EmptyBorder(0, 0, 0, 8));

        searchField = new JTextField();
        searchField.setBackground(GUIComponents.SEARCH_BG);
        searchField.setForeground(GUIComponents.TEXT_PRIMARY);
        searchField.setCaretColor(GUIComponents.TEXT_PRIMARY);
        searchField.setFont(new Font("Arial", Font.PLAIN, 15));
        searchField.setBorder(null);
        searchField.setOpaque(false);

        // Placeholder text effect
        searchField.setText("Search stocks...");
        searchField.setForeground(GUIComponents.TEXT_SECONDARY);

        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (searchField.getText().equals("Search stocks...")) {
                    searchField.setText("");
                    searchField.setForeground(GUIComponents.TEXT_PRIMARY);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(GUIComponents.TEXT_SECONDARY);
                    searchField.setText("Search stocks...");
                }
            }
        });

        searchButton = new JButton();
        searchButton.setVisible(false); // Hidden, triggered by Enter key

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setBackground(GUIComponents.SEARCH_BG);
        suggestionPopup.setBorder(BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1));

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
            item.setBackground(GUIComponents.SEARCH_BG);
            item.setForeground(GUIComponents.TEXT_PRIMARY);
            item.setFont(new Font("Arial", Font.PLAIN, 14));
            item.setBorder(new EmptyBorder(8, 12, 8, 12));

            // Hover effect
            item.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    item.setBackground(new Color(55, 60, 75));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    item.setBackground(GUIComponents.SEARCH_BG);
                }
            });

            item.addActionListener(e2 -> {
                suggestionPopup.setVisible(false);
                try {
                    model.addToWatchlist(symbol);
                    searchField.setText("");
                    searchField.setForeground(GUIComponents.TEXT_SECONDARY);
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