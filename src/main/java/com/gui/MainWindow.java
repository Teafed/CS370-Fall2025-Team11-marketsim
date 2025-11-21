// splits window into left and right panels. left pane is the list of symbols, right will have chart panel and profile panel

package com.gui;

import com.models.*;
import com.models.market.TradeItem;
import com.models.profile.Account;

import javax.swing.*;
import java.awt.*;

/**
 * The main application window.
 * Splits the window into a left panel (SymbolPanel) and a right panel
 * (ChartPanel/AccountPanel).
 * Listens for symbol and account selection events, as well as model updates.
 */
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
    private JLabel countdownLabel;
    private Timer countdownTimer;

    private static final String WINDOW_TITLE = "Marketsim";
    private static final String CARD_CHART = "chart";
    private static final String CARD_ACCOUNT = "account";
    private static final int LEFT_PANEL_WIDTH = 250;
    private static final int MIN_RIGHT_WIDTH = 300;

    /**
     * Constructs a new MainWindow.
     *
     * @param model The ModelFacade instance.
     */
    private final LogoCache logoCache;

    /**
     * Constructs a new MainWindow.
     *
     * @param model     The ModelFacade instance.
     * @param logoCache The LogoCache instance.
     */
    public MainWindow(ModelFacade model, LogoCache logoCache) {
        this.model = model;
        this.logoCache = logoCache;
        model.addListener(this);
        createWindow();
        setMarketOpen(model.isMarketOpen());
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
                usable.x + (usable.width - w) / 2,
                usable.y + (usable.height - h) / 2);
    }

    private void setupSplitPane() {
        JSplitPane splitPane = GUIComponents.createSplitPane(
                "horizontal", symbolPanel, rightContainer);

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

        symbolPanel = new SymbolPanel(model, logoCache);
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

        countdownLabel = new JLabel("");
        countdownLabel.setForeground(GUIComponents.TEXT_SECONDARY);
        countdownLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        countdownLabel.setVisible(false);

        topBar.add(statusPrefix);
        topBar.add(marketStatusLabel);
        topBar.add(countdownLabel);

        rightContainer.add(topBar, BorderLayout.NORTH);
        rightContainer.add(rightCards, BorderLayout.CENTER);
    }

    private void setupCloseHook() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopCountdown();
                try {
                    model.close();
                } catch (Exception ignored) {
                }
            }
        });
    }

    /**
     * Set the market open/closed indicator. true => OPEN (green), false => CLOSED
     * (red).
     *
     * @param open True if the market is open, false otherwise.
     */
    public void setMarketOpen(boolean open) {
        if (marketStatusLabel == null)
            return;
        if (open) {
            marketStatusLabel.setText("OPEN");
            marketStatusLabel.setBackground(GUIComponents.ACCENT_GREEN.darker());
            startCountdown(); // Show countdown to close
        } else {
            marketStatusLabel.setText("CLOSED");
            marketStatusLabel.setBackground(new Color(180, 0, 0));
            startCountdown(); // Show countdown to open
        }
    }

    // SymbolPanel listeners
    /**
     * Callback for symbol selection. Switches to the chart view and opens the
     * selected symbol.
     *
     * @param item The selected TradeItem.
     */
    @Override
    public void onSymbolSelected(TradeItem item) {
        cards.show(rightCards, CARD_CHART);
        chartPanel.openChart(item.getSymbol());
    }

    /**
     * Callback for account selection. Switches to the account view.
     *
     * @param account The selected Account.
     */
    @Override
    public void onAccountBarSelected(Account account) {
        cards.show(rightCards, CARD_ACCOUNT);
    }

    // ModelListener listeners
    /**
     * Callback for quote updates. Repaints the symbol and chart panels.
     */
    @Override
    public void onQuotesUpdated() {
        symbolPanel.repaint();
        chartPanel.repaint();
    }

    /**
     * Callback for account changes.
     *
     * @param snapshot The updated AccountDTO.
     */
    @Override
    public void onAccountChanged(AccountDTO snapshot) {
        // update right-side account panel, balances, etc
        // if AccountPanel exposes a method to refresh with latest model:
        // accountPanel.refresh(snapshot);
    }

    /**
     * Callback for watchlist changes. Updates the symbol panel.
     *
     * @param items The updated list of TradeItems.
     */
    @Override
    public void onWatchlistChanged(java.util.List<TradeItem> items) {
        symbolPanel.setSymbols(items);
    }

    /**
     * Callback for errors. Displays an error message dialog.
     *
     * @param message The error message.
     * @param t       The exception (optional).
     */
    @Override
    public void onError(java.lang.String message, Throwable t) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Starts the countdown timer for the next market opening.
     */
    private void startCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (countdownLabel == null)
            return;

        countdownLabel.setVisible(true);
        countdownTimer = new Timer(1000, e -> updateCountdown());
        countdownTimer.start();
        updateCountdown(); // Initial update
    }

    /**
     * Stops the countdown timer.
     */
    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
        if (countdownLabel != null) {
            countdownLabel.setVisible(false);
        }
    }

    /**
     * Updates the countdown display with time remaining until next market open or close.
     */
    private void updateCountdown() {
        if (countdownLabel == null)
            return;

        boolean isOpen = model.isMarketOpen();
        long millisRemaining = isOpen ? getMillisUntilClose() : getMillisUntilNextOpen();
        
        if (millisRemaining <= 0) {
            countdownLabel.setText(isOpen ? "Closing soon..." : "Opening soon...");
            return;
        }

        long hours = millisRemaining / (1000 * 60 * 60);
        long minutes = (millisRemaining / (1000 * 60)) % 60;
        long seconds = (millisRemaining / 1000) % 60;

        String action = isOpen ? "Closes" : "Opens";
        
        if (hours > 0) {
            countdownLabel.setText(String.format(" • %s in %dh %dm", action, hours, minutes));
        } else if (minutes > 0) {
            countdownLabel.setText(String.format(" • %s in %dm %ds", action, minutes, seconds));
        } else {
            countdownLabel.setText(String.format(" • %s in %ds", action, seconds));
        }
    }

    /**
     * Calculates milliseconds until the next market opening.
     * Market opens at 9:30 AM ET on weekdays.
     *
     * @return Milliseconds until next market open.
     */
    private long getMillisUntilNextOpen() {
        java.time.ZoneId etZone = java.time.ZoneId.of("America/New_York");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(etZone);
        
        // Market opens at 9:30 AM ET
        java.time.ZonedDateTime nextOpen = now
                .withHour(9)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);

        // If we're past 9:30 AM today, or it's a weekend, move to next weekday
        if (now.isAfter(nextOpen) || now.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || now.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            nextOpen = nextOpen.plusDays(1);
        }

        // Skip weekends
        while (nextOpen.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || nextOpen.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            nextOpen = nextOpen.plusDays(1);
        }

        return java.time.Duration.between(now, nextOpen).toMillis();
    }

    /**
     * Calculates milliseconds until the market closes.
     * Market closes at 4:00 PM ET on weekdays.
     *
     * @return Milliseconds until market close.
     */
    private long getMillisUntilClose() {
        java.time.ZoneId etZone = java.time.ZoneId.of("America/New_York");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(etZone);
        
        // Market closes at 4:00 PM ET
        java.time.ZonedDateTime closeTime = now
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // If we're past 4:00 PM today, return 0 (market should be closed)
        if (now.isAfter(closeTime)) {
            return 0;
        }

        return java.time.Duration.between(now, closeTime).toMillis();
    }
}
