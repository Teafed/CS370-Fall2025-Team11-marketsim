package com.gui;

import com.models.ModelFacade;

import javax.swing.*;
import java.awt.*;

public class SearchPanel extends ContentPanel {

    private final JTextField searchField;
    private final JButton searchButton;
    private final JPopupMenu suggestionPopup;

    public SearchPanel(ModelFacade model) {
        setLayout(new BorderLayout());

        searchField = new JTextField();
        searchButton = new JButton("Search");
        suggestionPopup = new JPopupMenu();

        add(searchField, BorderLayout.CENTER);
        add(searchButton, BorderLayout.EAST);

        searchField.addActionListener(e -> searchButton.doClick());

        searchButton.addActionListener(e -> {
            String query = searchField.getText().trim();
            if (!query.isEmpty()) {
                String[][] results = model.searchSymbol(query);
                showSuggestions(results);
            }
        });
    }

    private void showSuggestions(String[][] symbolDescriptions) {
        suggestionPopup.removeAll();

        for (String[] pair : symbolDescriptions) {
            String symbol = pair[0];
            String description = pair[1];
            String display = symbol + " — " + description;

            JMenuItem item = new JMenuItem(display);
            item.addActionListener(e2 -> {
                searchField.setText(symbol);
                suggestionPopup.setVisible(false);
            });
            suggestionPopup.add(item);
        }

        suggestionPopup.show(searchField, 0, searchField.getHeight());
    }
}
