package com.models.profile;

import com.models.market.TradeItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void depositAndWithdrawAffectCash() {
        Account account = new Account("Demo", 500.0);

        assertEquals(500.0, account.getCash());

        assertTrue(account.depositFunds(250.0));
        assertEquals(750.0, account.getCash());

        assertFalse(account.depositFunds(0.0));
        assertFalse(account.depositFunds(-5.0));

        assertTrue(account.reduceBalance(200.0));
        assertEquals(550.0, account.getCash());

        assertFalse(account.reduceBalance(1000.0));
        assertFalse(account.reduceBalance(0.0));
        assertFalse(account.reduceBalance(-3.0));
        assertEquals(550.0, account.getCash());
    }

    @Test
    void goalAmountClampedToZeroOrMore() {
        Account account = new Account("GoalTester");
        assertEquals(100_000.0, account.getGoal()); // default

        account.setGoal(250_000.0);
        assertEquals(250_000.0, account.getGoal());

        account.setGoal(-500.0);
        assertEquals(0.0, account.getGoal());
    }

    @Test
    void watchlistExposesUnderlyingItems() {
        Account account = new Account("Watcher");
        TradeItem ti = new TradeItem("XOM");

        assertTrue(account.getWatchlist().addWatchlistItem(ti));
        assertTrue(account.getWatchlistItems().contains(ti));
    }
}
