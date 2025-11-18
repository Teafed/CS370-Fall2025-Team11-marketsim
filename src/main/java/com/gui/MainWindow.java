// splits window into left and right panels. left pane is the list of symbols, right will have chart panel and profile panel

package com.gui;

import com.models.*;
import com.models.market.TradeItem;
import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame
        implements SymbolPanel.SymbolSelectionListener,
                   SymbolPanel.AccountSelectionListener,
                   ModelListener {

    private final ModelFacade model;

    private SymbolPanel symbolPanel;
    private ChartPanel chartPanel;
    private JPanel rightCards;
    private CardLayout cards;
    private JPanel rightContainer; // wrapper to hold top bar + cards
    private JLabel marketStatusLabel;

    private static final String WINDOW_TITLE = "Marketsim";
    private static final String CARD_CHART = "chart";
    private static final String CARD_ACCOUNT = "account";
    private static final int LEFT_PANEL_WIDTH = 250;
    private static final int MIN_RIGHT_WIDTH = 300;

    public MainWindow(ModelFacade model) {
        this.model = model;
        model.addListener(this);
        createWindow();
        setMarketOpen(model.isMarketOpen());
    }

    private void createWindow() {
        setTitle(WINDOW_TITLE);
        fitToScreen(1200, 800); // make sure it doesn't go offscreen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
            "horizontal", symbolPanel, rightContainer
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
        chartPanel = new ChartPanel(model);
        cards = new CardLayout();
        rightCards = new JPanel(cards);
        rightCards.setBackground(GUIComponents.BG_DARK);
        rightCards.add(chartPanel, CARD_CHART);

        AccountPanel accountPanel = new AccountPanel(model.getActiveAccount());
        rightCards.add(accountPanel, CARD_ACCOUNT);

        symbolPanel = new SymbolPanel(model);
        symbolPanel.addSymbolSelectionListener(this);
        symbolPanel.setAccount(model.getActiveAccount(), this);
        symbolPanel.setSymbols(model.getWatchlist());
        symbolPanel.selectFirst();

        cards.show(rightCards, CARD_CHART);
        rightCards.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));

        // Create a wrapper panel to hold a top bar (market status) and the card area.
        rightContainer = new JPanel(new BorderLayout());
        rightContainer.setBackground(GUIComponents.BG_DARK);

        // Top bar (align right) for small status widgets
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        topBar.setBackground(GUIComponents.BG_DARK);

        JLabel statusPrefix = new JLabel("Market Status:");
        statusPrefix.setForeground(GUIComponents.TEXT_SECONDARY);
        statusPrefix.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        marketStatusLabel = new JLabel("CLOSED");
        marketStatusLabel.setOpaque(true);
        marketStatusLabel.setBackground(new Color(180, 0, 0));
        marketStatusLabel.setForeground(Color.WHITE);
        marketStatusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        marketStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        topBar.add(statusPrefix);
        topBar.add(marketStatusLabel);

        rightContainer.add(topBar, BorderLayout.NORTH);
        rightContainer.add(rightCards, BorderLayout.CENTER);
    }

    private void setupCloseHook() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                try { model.close(); } catch (Exception ignored) { }
            }
        });
    }

    /**
     * Set the market open/closed indicator. true => OPEN (green), false => CLOSED (red).
     */
    public void setMarketOpen(boolean open) {
        if (marketStatusLabel == null) return;
        if (open) {
            marketStatusLabel.setText("OPEN");
            marketStatusLabel.setBackground(GUIComponents.ACCENT_GREEN.darker());
        } else {
            marketStatusLabel.setText("CLOSED");
            marketStatusLabel.setBackground(new Color(180, 0, 0));
        }
    }

    // SymbolPanel listeners
    @Override
    public void onSymbolSelected(TradeItem item) {
        cards.show(rightCards, CARD_CHART);
        chartPanel.openChart(item.getSymbol());
    }
    @Override
    public void onAccountBarSelected(Account account) {
        cards.show(rightCards, CARD_ACCOUNT);
    }

    // ModelListener listeners
    @Override public void onQuotesUpdated() {
        symbolPanel.repaint();
        chartPanel.repaint();
    }
    @Override public void onAccountChanged(AccountDTO snapshot) {
        // update right-side account panel, balances, etc
        // if AccountPanel exposes a method to refresh with latest model:
        // accountPanel.refresh(snapshot);
    }
    @Override public void onWatchlistChanged(java.util.List<TradeItem> items) {
        symbolPanel.setSymbols(items);
    }
    @Override public void onError(java.lang.String message, Throwable t) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
