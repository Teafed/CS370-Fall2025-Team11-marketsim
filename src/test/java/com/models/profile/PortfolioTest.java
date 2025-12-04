package com.models.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioTest {

    @Test
    void addSharesIncrementsPositions() {
        Portfolio portfolio = new Portfolio();

        assertTrue(portfolio.addShares("AAPL", 5));
        assertEquals(5, portfolio.getNumberOfShares("AAPL"));

        assertTrue(portfolio.addShares("AAPL", 3));
        assertEquals(8, portfolio.getNumberOfShares("AAPL"));
    }

    @Test
    void addSharesRejectsInvalidInput() {
        Portfolio portfolio = new Portfolio();

        assertFalse(portfolio.addShares(null, 1));
        assertFalse(portfolio.addShares("", 1));
        assertFalse(portfolio.addShares("MSFT", 0));
        assertFalse(portfolio.addShares("MSFT", -4));
        assertEquals(0, portfolio.getNumberOfShares("MSFT"));
    }

    @Test
    void removeSharesHandlesPartialAndFullRemoval() {
        Portfolio portfolio = new Portfolio();
        portfolio.addShares("TSLA", 10);

        assertTrue(portfolio.removeShares("TSLA", 4));
        assertEquals(6, portfolio.getNumberOfShares("TSLA"));
        assertTrue(portfolio.hasShare("TSLA"));

        assertTrue(portfolio.removeShares("TSLA", 6));
        assertEquals(0, portfolio.getNumberOfShares("TSLA"));
        assertFalse(portfolio.hasShare("TSLA"));
    }

    @Test
    void removeSharesRejectsInvalidRequests() {
        Portfolio portfolio = new Portfolio();
        portfolio.addShares("NVDA", 2);

        assertFalse(portfolio.removeShares("NVDA", 0));
        assertFalse(portfolio.removeShares("NVDA", -1));
        assertFalse(portfolio.removeShares("NVDA", 3));
        assertEquals(2, portfolio.getNumberOfShares("NVDA"));
    }
}
