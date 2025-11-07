package com.accountmanager;

import com.market.TradeItem;

/**
 * Represents a position in a portfolio: a TradeItem, quantity, and an average cost basis.
 * This is intentionally small and works alongside the existing Portfolio implementation
 * so we don't duplicate or remove existing behaviour.
 */
public class Position {
    private final TradeItem item;
    private int quantity;
    private double averageCost; // per-share cost basis

    public Position(TradeItem item, int quantity, double averageCost) {
        this.item = item;
        this.quantity = Math.max(0, quantity);
        this.averageCost = averageCost;
    }

    public TradeItem getItem() { return item; }

    public int getQuantity() { return quantity; }

    public double getAverageCost() { return averageCost; }

    /**
     * Add shares to this position and update the average cost.
     * @param n number of shares to add (must be > 0)
     * @param pricePaid per-share price paid for the added shares
     */
    public void addShares(int n, double pricePaid) {
        if (n <= 0) return;
        double totalExistingCost = this.averageCost * this.quantity;
        double totalNewCost = pricePaid * n;
        this.quantity += n;
        this.averageCost = (totalExistingCost + totalNewCost) / this.quantity;
    }

    /**
     * Remove shares from this position.
     * If the removal empties the position, quantity becomes zero but average cost is retained.
     * @param n number of shares to remove
     */
    public void removeShares(int n) {
        if (n <= 0) return;
        this.quantity = Math.max(0, this.quantity - n);
    }

    @Override
    public String toString() {
        return "Position{" + item.getSymbol() + ": qty=" + quantity + ", avgCost=" + averageCost + '}';
    }
}
