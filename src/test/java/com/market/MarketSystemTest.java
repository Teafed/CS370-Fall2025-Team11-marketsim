package com.market;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class MarketSystemTest {

    // TEST FOR TRADEITEM

    @Test
    void testTradeItem() {
        Stock stock = new Stock("stock1", "s1");

        // A Stock is a TradeItem
        assertInstanceOf(TradeItem.class, stock);

        // A trade item can return its current price
        assertEquals(0, stock.getCurrentPrice());

        // A trade item can return its name
        assertEquals("stock1",stock.getName());

        // A trade item can return its symbol
        assertEquals("s1", stock.getSymbol());

    }

    @Test
    void testTradeItemUpdatesPrice() {
        Stock stock = new Stock("stock1", "s1");
        assertEquals(0, stock.getCurrentPrice());

        // A trade item can update its current price
        stock.updatePrice(10);
        assertEquals(10, stock.getCurrentPrice());

        // A trade item will not update to a negative price
        assertFalse(stock.updatePrice(-1));
    }




    // TEST FOR MARKET

    @Test
    void testMarket() {

    }

    @Test
    void testTradeRequest() {

    }

}
