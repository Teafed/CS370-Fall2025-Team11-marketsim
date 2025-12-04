package com.models.market;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockTest {

    @Test
    void constructorUppercasesSymbol() {
        Stock stock = new Stock("aapl");
        assertEquals("AAPL", stock.getSymbol());
        assertEquals("AAPL", stock.getName());
    }

    @Test
    void updatePriceTracksChangeAgainstPrevClose() {
        Stock stock = new Stock("MSFT");
        stock.setPrevClose(100.0);
        stock.updatePrice(110.0);

        assertEquals(110.0, stock.getCurrentPrice());
        assertEquals(10.0, stock.getChange(), 1e-9);
        assertEquals(10.0, stock.getChangePercent(), 1e-9);
    }

    @Test
    void setValuesPopulatesPriceAndChange() {
        Stock stock = new Stock("TSLA");
        stock.setValues(new double[]{90.0, 95.0, 80.0});

        assertEquals(95.0, stock.getCurrentPrice());
        assertEquals(15.0, stock.getChange(), 1e-9);
        assertEquals(18.75, stock.getChangePercent(), 1e-9);
    }

    @Test
    void changePercentBecomesNaNWithoutPrevClose() {
        Stock stock = new Stock("AMZN");
        stock.updatePrice(120.0);
        assertTrue(Double.isNaN(stock.getChangePercent()));
    }

    @Test
    void changePercentResetsWhenPrevCloseZero() {
        Stock stock = new Stock("META");
        stock.setPrevClose(0.0);
        stock.updatePrice(50.0);
        assertTrue(Double.isNaN(stock.getChangePercent()));
    }

    @Test
    void updatePriceRejectsNegativeValues() {
        Stock stock = new Stock("NVDA");
        assertFalse(stock.updatePrice(-1.0));
        assertTrue(stock.updatePrice(0.0));
    }
}
