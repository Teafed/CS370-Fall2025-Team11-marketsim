// includes chart area and orders panel

package com.gui;

import com.market.DatabaseManager;

import java.awt.geom.Path2D;
import java.sql.Date;
import java.sql.ResultSet;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.Map;
import java.util.TreeMap;

public class ChartPanel extends ContentPanel {
    // --- data access & state ---
    private DatabaseManager dbRef; // set on first openChart call
    private String symbol;

    // --- child components ---
    private final ChartCanvas canvas;
    private final TimeframeBar timeframeBar;
    private final OrderPanel orderPanel;

    public ChartPanel() {
        super();
        this.symbol = null;setLayout(new BorderLayout(0, 10));
        setOpaque(true);
        setBackground(GUIComponents.BG_DARK); // container color

        // chart canvas
        this.canvas = new ChartCanvas();
        add(canvas, BorderLayout.CENTER);

        // chart options + order panel
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));

        this.timeframeBar = new TimeframeBar();
        south.add(timeframeBar);

        this.orderPanel = new OrderPanel();
        south.add(orderPanel);
        add(south, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(600, 300));
    }

    /**
     * load data for a symbol from database and prep it for painting
     * @param db
     * @param symbol
     */
    public void openChart(DatabaseManager db, String symbol) {
        this.dbRef = db;
        this.symbol = symbol;
        timeframeBar.setCurrentSymbol(symbol);

        if (!timeframeBar.loadSelectedIfAny(dbRef)) {
            try {
                long latest = db.getLatestTimestamp(symbol);
                if (latest == 0L) { canvas.clear(symbol); return; }
                long ninetyDays = 90L * 24 * 60 * 60 * 1000L;
                long start = Math.max(0, latest - ninetyDays);
                long end = latest;
                openChart(db, symbol, start, end, 400);
            } catch (Exception e) {
                canvas.clear(symbol);
            }
        }
    }

    /**
     * load data for a symbol from database and prep it for painting
     * overloaded to specify time frame
     *
     * @param db        DatabaseManager
     * @param symbol    e.g. "AAPL"
     * @param startMs   inclusive epoch millis
     * @param endMs     inclusive epoch millis
     * @param maxPoints cap plotted points to keep the line smooth (e.g. 200)
     */
    public void openChart(DatabaseManager db, String symbol, long startMs, long endMs, int maxPoints) {
        this.dbRef = db;
        this.symbol = symbol;
        canvas.loadFromDb(db, symbol, startMs, endMs, maxPoints);
    }

    // ===========
    // ChartCanvas
    // ===========
    private static final class ChartCanvas extends JPanel {
        private long[] times;
        private double[] prices;
        private String symbol;

        private double minPrice = Double.MAX_VALUE;
        private double maxPrice = Double.MIN_VALUE;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = Long.MIN_VALUE;

        ChartCanvas() {
            setPreferredSize(new Dimension(800, 400));
            setBackground(GUIComponents.BG_LIGHTER);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(20, 20, 20, 60)
            ));
        }

        void clear(String symbol) {
            this.symbol = symbol;
            this.times = null;
            this.prices = null;
            repaint();
        }

        void loadFromDb(DatabaseManager db, String symbol, long startMs, long endMs, int maxPoints) {
            this.symbol = symbol;
            try (ResultSet rs = db.getPrices(symbol, startMs, endMs)) {
                TreeMap<Long, Double> sorted = new TreeMap<>();
                while (rs.next()) {
                    long t = rs.getLong("timestamp");
                    double c = rs.getDouble("close");
                    sorted.put(t, c);
                }

                if (sorted.isEmpty()) { times = null; prices = null; repaint(); return; }

                int dataSize = sorted.size();
                int step = Math.max(1, dataSize / Math.max(1, maxPoints));
                int finalSize = (dataSize + step - 1) / step;

                times = new long[finalSize];
                prices = new double[finalSize];

                minTime = Long.MAX_VALUE; maxTime = Long.MIN_VALUE;
                minPrice = Double.MAX_VALUE; maxPrice = Double.MIN_VALUE;

                int i = 0, k = 0;
                for (Map.Entry<Long, Double> e : sorted.entrySet()) {
                    if (k++ % step != 0) continue;
                    if (i >= finalSize) break;
                    long t = e.getKey();
                    double p = e.getValue();
                    times[i] = t; prices[i] = p;
                    if (t < minTime) minTime = t; if (t > maxTime) maxTime = t;
                    if (p < minPrice) minPrice = p; if (p > maxPrice) maxPrice = p;
                    i++;
                }
            } catch (Exception ex) {
                times = null; prices = null;
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (times == null || times.length < 2) {
                g.setColor(new Color(150, 150, 150));
                g.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g.getFontMetrics();
                String msg = (symbol == null) ? "Select a stock to view chart" : ("No data available for " + symbol);
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isPositive = prices[prices.length - 1] >= prices[0];
            Color lineColor = isPositive ? GUIComponents.ACCENT_GREEN : GUIComponents.ACCENT_RED;
            Color gradientStart = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 100);
            Color gradientEnd   = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 0);
            Color labelColor = new Color(120, 120, 120);
            Font labelFont = new Font("Segoe UI", Font.PLAIN, 10);

            int w = getWidth();
            int h = getHeight();
            Insets in = getInsets();
            int drawWidth = w - in.left - in.right;
            int drawHeight = h - in.top - in.bottom;
            if (maxTime == minTime || maxPrice == minPrice) return;

            int n = times.length;
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];
            for (int i = 0; i < n; i++) {
                xPoints[i] = in.left + (int) ((times[i] - minTime) * drawWidth / (double) (maxTime - minTime));
                yPoints[i] = h - in.bottom - (int) ((prices[i] - minPrice) * drawHeight / (maxPrice - minPrice));
            }

            Path2D.Double path = new Path2D.Double();
            path.moveTo(xPoints[0], h - in.bottom);
            for (int i = 0; i < n; i++) path.lineTo(xPoints[i], yPoints[i]);
            path.lineTo(xPoints[n - 1], h - in.bottom);
            path.closePath();

            GradientPaint gp = new GradientPaint(0, in.top, gradientStart, 0, h - in.bottom, gradientEnd);
            g2.setPaint(gp);
            g2.fill(path);

            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2f));
            g2.drawPolyline(xPoints, yPoints, n);

            g2.setColor(labelColor);
            g2.setFont(labelFont);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(String.format("%.0f", maxPrice), w - in.right + 5, in.top + fm.getAscent());
            g2.drawString(String.format("%.0f", minPrice), w - in.right + 5, h - in.bottom);

            SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, HH:mm");
            String startLabel = timeFormat.format(new Date(minTime));
            String endLabel = timeFormat.format(new Date(maxTime));
            g2.drawString(startLabel, in.left, h - in.bottom + fm.getAscent() + 5);
            g2.drawString(endLabel, w - in.right - fm.stringWidth(endLabel), h - in.bottom + fm.getAscent() + 5);
        }
    }

    // ============
    // TimeframeBar
    // ============
    private final class TimeframeBar extends JPanel {
        private final ButtonGroup group = new ButtonGroup();
        private final JToggleButton btn1D = createBtn("1D");
        private final JToggleButton btn1W = createBtn("1W");
        private final JToggleButton btn1M = createBtn("1M");
        private String currentSymbol;

        private static final long ONE_DAY   = 24L * 60 * 60 * 1000;
        private static final long ONE_WEEK  = 7L * ONE_DAY;
        private static final long ONE_MONTH = 30L * ONE_DAY;

        TimeframeBar() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
            setBackground(GUIComponents.BG_DARK);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));

            group.add(btn1D); group.add(btn1W); group.add(btn1M);
            add(btn1D); add(btn1W); add(btn1M);
            btn1W.setSelected(true); // sensible default
        }

        private JToggleButton createBtn(String text) {
            JToggleButton b = new JToggleButton(text);
            b.setFont(new Font("Segoe UI", Font.BOLD, 12));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setPreferredSize(new Dimension(65, 28));
            b.setBackground(GUIComponents.BG_MEDIUM);
            b.setForeground(GUIComponents.TEXT_SECONDARY);
            b.setBorder(BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1));

            b.addActionListener(e -> applySelection());
            b.addChangeListener(e -> {
                if (b.isSelected()) {
                    b.setBackground(GUIComponents.ACCENT_BLUE);
                    b.setForeground(Color.WHITE);
                    b.setBorder(BorderFactory.createLineBorder(GUIComponents.ACCENT_BLUE, 2));
                } else {
                    b.setBackground(GUIComponents.BG_MEDIUM);
                    b.setForeground(GUIComponents.TEXT_SECONDARY);
                    b.setBorder(BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1));
                }
            });
            return b;
        }

        void setCurrentSymbol(String symbol) { this.currentSymbol = symbol; }

        /**
         * returns true if it reloaded using a selected button
         * @param db
         * @return
         */
        boolean loadSelectedIfAny(DatabaseManager db) {
            if (db == null || currentSymbol == null) return false;
            if (!btn1D.isSelected() && !btn1W.isSelected() && !btn1M.isSelected()) return false;
            applySelection();
            return true;
        }

        void applySelection() {
            if (dbRef == null || currentSymbol == null) return;
            try {
                long now = System.currentTimeMillis();
                long start = now - ONE_WEEK; // default
                if (btn1D.isSelected()) start = now - ONE_DAY;
                else if (btn1M.isSelected()) start = now - ONE_MONTH;
                openChart(dbRef, currentSymbol, start, now, 400);
            } catch (Exception ignored) {}
        }
    }

    // ==========
    // OrderPanel
    // ==========
    private static final class OrderPanel extends JPanel {
        private final JPanel header;
        private final JLabel title;
        private final JButton toggle;
        private final JTabbedPane tabs;
        private boolean collapsed = false;

        OrderPanel() {
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(GUIComponents.BG_DARK);
            setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

            header = new JPanel(new BorderLayout());
            header.setOpaque(true);
            header.setBackground(GUIComponents.BG_MEDIUM);
            header.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));

            title = new JLabel("Orders & Portfolio");
            title.setForeground(GUIComponents.TEXT_PRIMARY);
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));

            toggle = new JButton("▾");
            toggle.setFocusPainted(false);
            toggle.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggle.addActionListener(e -> setCollapsed(!collapsed));

            header.add(title, BorderLayout.WEST);
            header.add(toggle, BorderLayout.EAST);

            tabs = new JTabbedPane();
            tabs.setBackground(GUIComponents.BG_DARK);
            tabs.setForeground(GUIComponents.TEXT_PRIMARY);
            tabs.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GUIComponents.BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            tabs.addTab("Trade", new TradingPanel());
            tabs.addTab("Portfolio", new PortfolioPanel());

            add(header, BorderLayout.NORTH);
            add(tabs, BorderLayout.CENTER);
        }

        void setCollapsed(boolean collapse) {
            this.collapsed = collapse;
            tabs.setVisible(!collapse);
            toggle.setText(collapse ? "▸" : "▾");
            revalidate();
        }
    }
}