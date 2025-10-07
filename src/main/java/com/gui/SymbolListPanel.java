// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel
package com.gui;

import com.etl.ReadData;
import com.market.SymbolData;

import com.market.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SymbolListPanel extends JPanel {
   private DefaultListModel<TradeItem> symbolModel;
   private JList<TradeItem> symbolList;
   private final List<SymbolSelectionListener> listeners;
   private final String dataFolderPath;
    private DefaultListModel<TradeItem> symbolModel;
    private JList<TradeItem> symbolList;
    private List<SymbolSelectionListener> listeners;
    private String dataFolderPath;
    private ReadData reader;

   // interface that listeners must implement
   public interface SymbolSelectionListener {
       void onSymbolSelected(TradeItem symbol);
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
      if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            System.out.println("data directory dne");
         return;
      }
      
      File[] csvFiles = dataFolder.listFiles((dir, name) -> 
         name.toLowerCase().endsWith(".csv"));
      if (csvFiles == null || csvFiles.length == 0) {
            System.out.println("data files dne");
         return;
      }
      
      System.out.println("loading symbols from: " + dataFolderPath);
      System.out.println("found " + csvFiles.length + " csv files");
      for (File f : csvFiles) {
         System.out.println("   " + f.getName());
      }

       // sort and add symbols
      Arrays.sort(csvFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

      // TEMPORARY! please remove and replace with data   
      java.util.Random random = new java.util.Random();
      for (File file : csvFiles) {
         String fileName = file.getName();
         String symbol = fileName.substring(0, fileName.lastIndexOf('.'));
         
         double basePrice = 50 + random.nextDouble() * 200;
         double change = (random.nextDouble() - 0.5) * 10;
         double changePercent = (change / basePrice) * 100;
         
         symbolModel.addElement(new Stock(symbol, symbol));
      }
   }

   private void setupListeners() {
      symbolList.addListSelectionListener(e -> {
         if (!e.getValueIsAdjusting()) { // only fire when selection is final
            TradeItem selectedSymbol = symbolList.getSelectedValue();
            if (selectedSymbol != null) {
               notifyListeners(selectedSymbol);
            }
        });
    }

    public void addSymbolSelectionListener(SymbolSelectionListener listener) {
        listeners.add(listener);
    }

   public void removeSymbolSelectionListener(SymbolSelectionListener listener) {
      listeners.remove(listener);
   }

   private void notifyListeners(TradeItem symbol) {
      for (SymbolSelectionListener listener : listeners) {
         listener.onSymbolSelected(symbol);
      }
   }

    /** Helper: return the first symbol name (for initializing chart) */
    public String getFirstSymbolName() {
        return symbolModel.isEmpty() ? null : symbolModel.get(0).getSymbol();
    }

   public String getSelectedSymbol() {
      TradeItem selected = symbolList.getSelectedValue();
      return selected != null ? selected.getSymbol() : null;
   }

   public void clearSelection() {
      symbolList.clearSelection();
   }
    /** Helper: return the underlying ReadData instance */
    public ReadData getReader() {
        return reader;
    }
}
