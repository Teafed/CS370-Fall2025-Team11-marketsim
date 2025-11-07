// splits window into left and right panels. left pane is the list of symbols, right will have chart panel and profile panel

package com.gui;

import com.market.*;
import com.accountmanager.Account;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame
        implements SymbolListPanel.SymbolSelectionListener,
                   SymbolListPanel.AccountSelectionListener {
    private final DatabaseManager db;
    private final Account account;
    private final Market market;

    private SymbolListPanel symbolPanel;
    private ChartPanel chartPanel;
    private JPanel rightCards;
    private CardLayout cards;

    private static final String WINDOW_TITLE = "Marketsim";
    private static final String CARD_CHART = "chart";
    private static final String CARD_ACCOUNT = "account";
    private static final int LEFT_PANEL_WIDTH = 250;
    private static final int MIN_RIGHT_WIDTH = 300;

    public MainWindow(DatabaseManager db, Account account, Market market) {
        this.db = db;
        this.account = account;
        this.market = market;
        System.out.println("Launching Marketsim");
        createWindow();
    }

    private void createWindow() {
        setTitle(WINDOW_TITLE);
        fitToScreen(1200, 800); // make sure it doesn't go offscreen
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // center on screen
        getContentPane().setBackground(GUIComponents.BG_DARK);
        setLayout(new BorderLayout());

        createPanels();
        setupSplitPane();
        setupCloseHook();

        setVisible(true);
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
        // initialize left symbol list panel
        symbolPanel = new SymbolListPanel(db);

        chartPanel = new ChartPanel(account);
        cards = new CardLayout();
        rightCards = new JPanel(cards);
        rightCards.setBackground(GUIComponents.BG_DARK);
        rightCards.add(chartPanel, CARD_CHART);

        AccountPanel accountPanel = new AccountPanel(account);
        rightCards.add(accountPanel, CARD_ACCOUNT);

        OrderPanel orderPanel = new OrderPanel(account);
        // ...existing code...
        symbolPanel.addSymbolSelectionListener(this);
        symbolPanel.setAccount(account, this);

        cards.show(rightCards, CARD_CHART);
        rightCards.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));

        // Add the orderPanel to the main window, likely in the south or east
        add(orderPanel, BorderLayout.SOUTH);
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
