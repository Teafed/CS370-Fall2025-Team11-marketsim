package com.market;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages trade execution and order matching in the market
 */
public class TradeManager {
    private final Market market;
    private final ConcurrentMap<String, List<TradeRequest>> buyOrders;
    private final ConcurrentMap<String, List<TradeRequest>> sellOrders;
    
    public TradeManager(Market market) {
        this.market = market;
        this.buyOrders = new ConcurrentHashMap<>();
        this.sellOrders = new ConcurrentHashMap<>();
    }
    
    /**
     * Place a buy order in the market
     * @param request The trade request
     * @return True if the order was executed, false if it was queued
     */
    public boolean placeBuyOrder(TradeRequest request) {
        Stock stock = market.getStock(request.getSymbol());
        if (stock == null) {
            return false;
        }
        
        // Check if there are matching sell orders
        List<TradeRequest> matchingSellOrders = sellOrders.getOrDefault(request.getSymbol(), new ArrayList<>());
        for (TradeRequest sellOrder : matchingSellOrders) {
            if (sellOrder.getPrice() <= request.getPrice() && sellOrder.getQuantity() == request.getQuantity()) {
                // Execute the trade
                executeTrade(request, sellOrder);
                matchingSellOrders.remove(sellOrder);
                return true;
            }
        }
        
        // No matching sell order, queue the buy order
        List<TradeRequest> orders = buyOrders.getOrDefault(request.getSymbol(), new ArrayList<>());
        orders.add(request);
        buyOrders.put(request.getSymbol(), orders);
        return false;
    }
    
    /**
     * Place a sell order in the market
     * @param request The trade request
     * @return True if the order was executed, false if it was queued
     */
    public boolean placeSellOrder(TradeRequest request) {
        Stock stock = market.getStock(request.getSymbol());
        if (stock == null) {
            return false;
        }
        
        // Check if there are matching buy orders
        List<TradeRequest> matchingBuyOrders = buyOrders.getOrDefault(request.getSymbol(), new ArrayList<>());
        for (TradeRequest buyOrder : matchingBuyOrders) {
            if (buyOrder.getPrice() >= request.getPrice() && buyOrder.getQuantity() == request.getQuantity()) {
                // Execute the trade
                executeTrade(buyOrder, request);
                matchingBuyOrders.remove(buyOrder);
                return true;
            }
        }
        
        // No matching buy order, queue the sell order
        List<TradeRequest> orders = sellOrders.getOrDefault(request.getSymbol(), new ArrayList<>());
        orders.add(request);
        sellOrders.put(request.getSymbol(), orders);
        return false;
    }
    
    /**
     * Execute a trade between a buy and sell order
     * @param buyOrder The buy order
     * @param sellOrder The sell order
     */
    private void executeTrade(TradeRequest buyOrder, TradeRequest sellOrder) {
        // Use the sell price for the trade
        double tradePrice = sellOrder.getPrice();
        int quantity = buyOrder.getQuantity();
        String symbol = buyOrder.getSymbol();
        
        // Update buyer's account
        int buyerId = buyOrder.getAccountId();
        double buyerBalance = market.getDatabaseManager().getAccountBalance(buyerId);
        double totalCost = tradePrice * quantity;
        market.getDatabaseManager().updateAccountBalance(buyerId, buyerBalance - totalCost);
        market.getDatabaseManager().addToPortfolio(buyerId, symbol, quantity, tradePrice);
        market.getDatabaseManager().recordTrade(buyerId, symbol, quantity, tradePrice, "BUY");
        
        // Update seller's account
        int sellerId = sellOrder.getAccountId();
        double sellerBalance = market.getDatabaseManager().getAccountBalance(sellerId);
        market.getDatabaseManager().updateAccountBalance(sellerId, sellerBalance + totalCost);
        market.getDatabaseManager().recordTrade(sellerId, symbol, quantity, tradePrice, "SELL");
        
        // Notify the traders
        buyOrder.getTrader().notifyTradeExecuted(buyOrder, tradePrice);
        sellOrder.getTrader().notifyTradeExecuted(sellOrder, tradePrice);
    }
    
    /**
     * Cancel a trade request
     * @param request The trade request to cancel
     * @return True if the request was cancelled, false if it was not found
     */
    public boolean cancelOrder(TradeRequest request) {
        if (request.isBuyOrder()) {
            List<TradeRequest> orders = buyOrders.getOrDefault(request.getSymbol(), new ArrayList<>());
            return orders.remove(request);
        } else {
            List<TradeRequest> orders = sellOrders.getOrDefault(request.getSymbol(), new ArrayList<>());
            return orders.remove(request);
        }
    }
    
    /**
     * Get all buy orders for a symbol
     * @param symbol The stock symbol
     * @return List of buy orders
     */
    public List<TradeRequest> getBuyOrders(String symbol) {
        return buyOrders.getOrDefault(symbol, new ArrayList<>());
    }
    
    /**
     * Get all sell orders for a symbol
     * @param symbol The stock symbol
     * @return List of sell orders
     */
    public List<TradeRequest> getSellOrders(String symbol) {
        return sellOrders.getOrDefault(symbol, new ArrayList<>());
    }
}
