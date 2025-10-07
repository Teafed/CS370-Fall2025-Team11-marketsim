// splits window into left and right panels. left pane is the list of symbols, right will have chart panel and profile panel

package com.gui;

import com.etl.SymbolData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MainWindow extends JFrame implements SymbolListPanel.SymbolSelectionListener {
    private JSplitPane splitPane;
    private SymbolListPanel symbolPanel;
    private ChartPanel chartPanel;

    private static final String WINDOW_TITLE = "Marketsim";
    private static final String DATA_FOLDER = "data";
    private static final int LEFT_PANEL_WIDTH = 250;
    private static final int MIN_LEFT_WIDTH = 150;
    private static final int MIN_RIGHT_WIDTH = 300;

    public MainWindow() {
        initializeWindow();
        createPanels();
        setupSplitPane();
        setupResizeListener();

        setVisible(true);
    }

    private void initializeWindow() {
        setTitle(WINDOW_TITLE);
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen

        getContentPane().setBackground(GUIComponents.BG_DARK);
    }

    private void setupSplitPane() {
        splitPane = GUIComponents.createSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, symbolPanel, chartPanel
        );

        splitPane.setDividerLocation(LEFT_PANEL_WIDTH);
        splitPane.setOneTouchExpandable(false);
        splitPane.setResizeWeight(0.0); // right panel gets all extra space
        splitPane.setContinuousLayout(true); // smooth resizing

        add(splitPane, BorderLayout.CENTER);
    }

    // create
    private void createPanels() {
        // data panel - contains list of symbols from csv data folder
        symbolPanel = new SymbolListPanel(DATA_FOLDER);
        symbolPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
        symbolPanel.setMinimumSize(new Dimension(MIN_LEFT_WIDTH, 0));
        symbolPanel.addSymbolSelectionListener(this);

        // right panel - will show selected symbol content
        chartPanel = new ChartPanel();
        chartPanel.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));
    }

    // ensure left panel has a minimum width
    private void setupResizeListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // maintain the left panel at constant width
                int currentDividerLocation = splitPane.getDividerLocation();
                if (currentDividerLocation != LEFT_PANEL_WIDTH) {
                    // only reset if user hasn't manually moved the divider
                    // might need adjusting idk
                    SwingUtilities.invokeLater(() -> {
                        if (splitPane.getDividerLocation() < MIN_LEFT_WIDTH) {
                            splitPane.setDividerLocation(MIN_LEFT_WIDTH);
                        } else if (getWidth() - splitPane.getDividerLocation() < MIN_RIGHT_WIDTH) {
                            splitPane.setDividerLocation(getWidth() - MIN_RIGHT_WIDTH);
                        }
                    });
                }
            }
        });
    }

    // implement the SymbolSelectionListener interface
    @Override
    public void onSymbolSelected(SymbolData symbol) {
        chartPanel.openChart(symbolPanel.getReader(), symbol.getSymbol());
    }
}