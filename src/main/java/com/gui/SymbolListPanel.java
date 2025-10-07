// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel
package com.gui;

import com.etl.ReadData;
import com.etl.SymbolData;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class SymbolListPanel extends JPanel {
    private DefaultListModel<SymbolData> symbolModel;
    private JList<SymbolData> symbolList;
    private List<SymbolSelectionListener> listeners;
    private String dataFolderPath;
    private ReadData reader;

    public interface SymbolSelectionListener {
        void onSymbolSelected(SymbolData symbol);
    }

    public SymbolListPanel(String dataFolderPath) {
        this.dataFolderPath = dataFolderPath;
        this.listeners = new ArrayList<>();
        try {
            reader = new ReadData(dataFolderPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initializeComponents();
        loadSymbols();
        setupListeners();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_MEDIUM);
        setBorder(GUIComponents.createBorder());

        symbolModel = new DefaultListModel<>();
        symbolList = GUIComponents.createList(symbolModel);
        symbolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        symbolList.setCellRenderer(new SymbolCellRenderer());
        symbolList.setFixedCellHeight(50);

        JScrollPane scrollPane = GUIComponents.createScrollPane(symbolList);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadSymbols() {
        symbolModel.clear();
        File dataFolder = new File(dataFolderPath);
        if (!dataFolder.exists() || !dataFolder.isDirectory()) return;

        File[] csvFiles = dataFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (csvFiles == null) return;

        Arrays.sort(csvFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        for (File file : csvFiles) {
            String fileName = file.getName();
            String symbol = fileName.substring(0, fileName.lastIndexOf('.'));
            List<String[]> rows = reader.getFileData(fileName);
            if (rows == null || rows.size() < 3) continue; // need header + 2 data rows

            try {
                String[] lastRow = rows.get(rows.size() - 1);
                String[] prevRow = rows.get(rows.size() - 2);

                if (lastRow.length < 3 || prevRow.length < 3) continue;

                double lastPrice = Double.parseDouble(lastRow[2].trim()); // price column
                double prevPrice = Double.parseDouble(prevRow[2].trim());
                double change = lastPrice - prevPrice;
                double changePercent = (change / prevPrice) * 100.0;

                symbolModel.addElement(new SymbolData(symbol, lastPrice, change, changePercent));
            } catch (NumberFormatException ex) {
                System.err.println("Skipping file " + fileName + " due to bad numeric data.");
            }
        }
    }

    private void setupListeners() {
        symbolList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                SymbolData selectedSymbol = symbolList.getSelectedValue();
                if (selectedSymbol != null) notifyListeners(selectedSymbol);
            }
        });
    }

    public void addSymbolSelectionListener(SymbolSelectionListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(SymbolData symbol) {
        for (SymbolSelectionListener listener : listeners) listener.onSymbolSelected(symbol);
    }

    /** Helper: return the first symbol name (for initializing chart) */
    public String getFirstSymbolName() {
        return symbolModel.isEmpty() ? null : symbolModel.get(0).getSymbol();
    }

    /** Helper: return the underlying ReadData instance */
    public ReadData getReader() {
        return reader;
    }
}
