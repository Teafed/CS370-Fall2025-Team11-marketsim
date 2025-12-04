package com.models;

import com.models.market.Stock;
import com.models.market.TradeItem;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class MarketSystemTest {

    // TEST FOR TRADEITEM

    @Test
    void stockExposesTradeItemProperties() {
        Stock stock = new Stock("AAPL");
        assertInstanceOf(TradeItem.class, stock);
        assertEquals("AAPL", stock.getSymbol());
        assertEquals("AAPL", stock.getName());
        assertEquals(0.0, stock.getCurrentPrice());
    }

    @Test
    void stockUpdatesPriceAndRejectsNegatives() {
        Stock stock = new Stock("MSFT");
        assertEquals(0.0, stock.getCurrentPrice());

        assertTrue(stock.updatePrice(150.0));
        assertEquals(150.0, stock.getCurrentPrice());

        assertFalse(stock.updatePrice(-1.0));
    }

}
