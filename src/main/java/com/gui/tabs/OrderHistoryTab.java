package com.gui.tabs;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Supplier;

import com.gui.ContentPanel;
import com.gui.GUIComponents;
import com.models.AccountDTO;
import com.models.ModelFacade;
import com.models.ModelListener;

/**
 * A tab displaying the history of orders/trades.
 * Shows a table with time, side, quantity, price, cash change, and position.
 */
public class OrderHistoryTab extends ContentPanel implements ModelListener {
    private final ModelFacade model;
    private final Supplier<String> selectedSymbol; // may be null → no filter
    private JTable holdingsTable;
    private DefaultTableModel tableModel;

    private final SimpleDateFormat tsFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final DecimalFormat money = new DecimalFormat("$#,##0.00");

    /**
     * Constructs a new OrderHistoryTab.
     *
     * @param model          The ModelFacade instance.
     * @param selectedSymbol A supplier for the currently selected symbol (for
     *                       filtering).
     */
    public OrderHistoryTab(ModelFacade model, Supplier<String> selectedSymbol) {
        this.model = model;
        this.selectedSymbol = selectedSymbol;

        setLayout(new BorderLayout());
        setBackground(GUIComponents.BG_LIGHTER);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        initializeComponents();
        model.addListener(this);
        refresh();
    }

    private void initializeComponents() {
        // Table to display portfolio holdings
        String[] cols = { "Time", "Side", "Qty", "Price", "Cash Δ", "Pos" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        holdingsTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);

                c.setForeground(GUIComponents.TEXT_PRIMARY);
                c.setBackground((row % 2 == 0)
                        ? GUIComponents.BG_DARK
                        : GUIComponents.BG_MEDIUM);

                if (c instanceof JComponent jc) {
                    jc.setBorder(null);
                }

                return c;
            }

            @Override
            public void setRowSelectionInterval(int index0, int index1) {
                super.setRowSelectionInterval(index0, index1);
                repaint();
            }
        };

        holdingsTable.setShowGrid(false);
        holdingsTable.setIntercellSpacing(new Dimension(0, 0));
        holdingsTable.setRowHeight(24);
        holdingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        holdingsTable.setSelectionBackground(GUIComponents.ACCENT_BLUE);
        holdingsTable.setSelectionForeground(Color.WHITE);
        holdingsTable.setBackground(GUIComponents.BG_DARK);
        holdingsTable.setForeground(GUIComponents.TEXT_PRIMARY);
        holdingsTable.setFocusable(false);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        holdingsTable.getColumnModel().getColumn(2).setCellRenderer(right); // Qty
        holdingsTable.getColumnModel().getColumn(3).setCellRenderer(right); // Price
        holdingsTable.getColumnModel().getColumn(4).setCellRenderer(right); // Cash Δ
        holdingsTable.getColumnModel().getColumn(5).setCellRenderer(right); // Pos

        holdingsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(value);
                setHorizontalAlignment(SwingConstants.CENTER);
                String v = value == null ? "" : value.toString();
                if ("BUY".equals(v)) {
                    setForeground(GUIComponents.GREEN);
                } else if ("SELL".equals(v)) {
                    setForeground(GUIComponents.RED);
                } else {
                    setForeground(GUIComponents.TEXT_PRIMARY);
                }
            }
        });

        // widths
        holdingsTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Time
        holdingsTable.getColumnModel().getColumn(1).setPreferredWidth(60); // Side
        holdingsTable.getColumnModel().getColumn(2).setPreferredWidth(60); // Qty
        holdingsTable.getColumnModel().getColumn(3).setPreferredWidth(90); // Price
        holdingsTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Cash Δ
        holdingsTable.getColumnModel().getColumn(5).setPreferredWidth(60); // Pos

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(tableModel);
        holdingsTable.setRowSorter(sorter);
        holdingsTable.setBorder(BorderFactory.createEmptyBorder());
        sorter.toggleSortOrder(0); // default sort by time

        JTableHeader header = holdingsTable.getTableHeader();
        header.setBackground(GUIComponents.BG_MEDIUM);
        header.setForeground(GUIComponents.TEXT_PRIMARY);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBorder(BorderFactory.createEmptyBorder());

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        table, value, false, false, row, column);

                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                lbl.setOpaque(true);
                lbl.setBackground(GUIComponents.BG_MEDIUM);
                lbl.setForeground(GUIComponents.TEXT_PRIMARY);
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                String text = value == null ? "" : value.toString();
                lbl.setIcon(null);
                RowSorter<? extends TableModel> rs = table.getRowSorter();
                if (rs != null) {
                    List<? extends RowSorter.SortKey> keys = rs.getSortKeys();
                    if (!keys.isEmpty()) {
                        RowSorter.SortKey key = keys.get(0);
                        int modelCol = table.convertColumnIndexToModel(column);
                        if (key.getColumn() == modelCol) {
                            SortOrder order = key.getSortOrder();
                            if (order == SortOrder.ASCENDING) {
                                text += " ▲";
                            } else if (order == SortOrder.DESCENDING) {
                                text += " ▼";
                            }
                        }
                    }
                }

                lbl.setText(text);
                return lbl;
            }
        });

        JScrollPane scrollPane = GUIComponents.createScrollPane(holdingsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(GUIComponents.BG_DARK);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(true);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Refreshes the order history table with the latest data.
     * Filters by the selected symbol if applicable.
     */
    public void refresh() {
        tableModel.setRowCount(0);
        try {
            String symFilter = (selectedSymbol == null ? null : selectedSymbol.get());
            if (symFilter != null)
                symFilter = symFilter.trim().toUpperCase();

            List<ModelFacade.TradeRow> rows = model.getRecentTrades(100);
            for (var r : rows) {
                if (symFilter != null && !symFilter.equals(r.symbol()))
                    continue;

                double cashDelta = ("BUY".equals(r.side()) ? -1.0 : 1.0) * (r.quantity() * r.price());
                Object[] line = {
                        tsFmt.format(new java.util.Date(r.timestamp())),
                        r.side(),
                        r.quantity(),
                        money.format(r.price()),
                        (cashDelta >= 0 ? "+" : "") + new DecimalFormat("#,##0.00").format(cashDelta),
                        r.posAfter()
                };
                tableModel.addRow(line);

                if (tableModel.getRowCount() == 0) {
                    tableModel.addRow(new Object[] { "—", "—", "—", "—", "—", "—" });
                }

                @SuppressWarnings("unchecked")
                TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) holdingsTable.getRowSorter();
                sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
            }
        } catch (Exception e) {
            // optionally show a toast
        }
    }

    // ModelListener
    @Override
    public void onAccountChanged(AccountDTO snapshot) {
        refresh();
    }

    @Override
    public void onQuotesUpdated() {
        /* not needed */ }

    @Override
    public void onError(String message, Throwable t) {
        /* optional */ }
}