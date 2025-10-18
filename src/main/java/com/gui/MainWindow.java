// splits window into left and right panels. left pane is the list of symbols, right will have chart panel and profile panel

package com.gui;

import com.market.TradeItem;
import com.market.DatabaseManager;
import com.accountmanager.Account;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame
        implements SymbolListPanel.SymbolSelectionListener,
                   SymbolListPanel.AccountSelectionListener {
    private SymbolListPanel symbolPanel;
    private ChartPanel chartPanel;
    private AccountPanel accountPanel;
    private DatabaseManager db;
    private JPanel rightCards;
    private CardLayout cards;

    private static final String WINDOW_TITLE = "Marketsim";
    private static final String CARD_CHART = "chart";
    private static final String CARD_ACCOUNT = "account";
    private static final int LEFT_PANEL_WIDTH = 250;
    private static final int MIN_LEFT_WIDTH = 150;
    private static final int MIN_RIGHT_WIDTH = 300;
    private static String defaultDbPath() { return "data/marketsim-sample.db"; }

    public MainWindow() {
        this(defaultDbPath());
    }

    public MainWindow(String dbFile) {
        System.out.println("Launching Marketsim (" + dbFile + ")");
        initializeWindow();
        initDatabase(dbFile);
        createPanels();
        setupSplitPane();
        setupCloseHook();

        setVisible(true);
    }

    private void initializeWindow() {
        setTitle(WINDOW_TITLE);
        fitToScreen(1200, 800); // make sure it doesn't go offscreen
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen
        getContentPane().setBackground(GUIComponents.BG_DARK);
        setLayout(new BorderLayout());
    }

    private void initDatabase(String dbFile) {
        try {
            db = new DatabaseManager(dbFile);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to open database: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }
    }

    private void fitToScreen(int prefW, int prefH) {
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds(); // excludes taskbar/dock

        int w = Math.min(prefW, usable.width);
        int h = Math.min(prefH, usable.height);

        setSize(w, h);
        setLocation(
                usable.x + (usable.width  - w) / 2,
                usable.y + (usable.height - h) / 2
        );
    }

    private void setupSplitPane() {
        JSplitPane splitPane = GUIComponents.createSplitPane(
                "horizontal", symbolPanel, rightCards
        );

        splitPane.setDividerLocation(LEFT_PANEL_WIDTH);
        splitPane.setOneTouchExpandable(false);
        splitPane.setResizeWeight(0.0); // right panel gets all extra space
        splitPane.setContinuousLayout(true); // smooth resizing
        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(LEFT_PANEL_WIDTH));

        add(splitPane, BorderLayout.CENTER);
    }

    // create
    private void createPanels() {
        /*
        // data panel - contains list of symbols from csv data folder
        symbolPanel = new SymbolListPanel(db);
        symbolPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
        symbolPanel.setMinimumSize(new Dimension(MIN_LEFT_WIDTH, 0));
        symbolPanel.addSymbolSelectionListener(this);

        // right panel - will show selected symbol content
        chartPanel = new ChartPanel();
        chartPanel.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));
         */
        chartPanel = new ChartPanel();
        cards = new CardLayout();
        rightCards = new JPanel(cards);
        rightCards.setBackground(GUIComponents.BG_DARK);
        rightCards.add(chartPanel, CARD_CHART);

        Account demo = com.tools.BuildDemoAccount.buildDemoAccount();
        accountPanel = new AccountPanel(demo);
        rightCards.add(accountPanel, CARD_ACCOUNT);

        symbolPanel = new SymbolListPanel(db);
        symbolPanel.addSymbolSelectionListener(this);
        symbolPanel.setAccount(demo, this);

        cards.show(rightCards, CARD_CHART);
        rightCards.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));

    }

    private void setupCloseHook() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                try { if (db != null) db.close(); } catch (Exception ignored) {}
            }
        });
    }

    public SymbolListPanel getSymbolListPanel() {
        return symbolPanel;
    }

    // implement the SymbolSelectionListener interface
    @Override
    public void onSymbolSelected(TradeItem item) {
        cards.show(rightCards, CARD_CHART);
        chartPanel.openChart(db, item.getSymbol());
    }

    @Override
    public void onAccountSelected(Account account) {
        cards.show(rightCards, CARD_ACCOUNT);
    }
}
