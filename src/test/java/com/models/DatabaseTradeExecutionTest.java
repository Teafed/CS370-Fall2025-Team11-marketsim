package com.models;

import com.models.market.Order;
import com.models.profile.Account;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTradeExecutionTest {

    private TestContext setupAccount(double initialDeposit) throws Exception {
        Database db = new Database(":memory:");
        try {
            db.ensureSingletonProfile("JUnit Profile");
            long accountId = db.getOrCreateAccount("JUnit Account", "USD");
            long seedTs = 1_000_000L;
            db.depositCash(accountId, initialDeposit, seedTs, "Initial deposit");
            Account account = new Account(accountId, "JUnit Account");
            account.setCash(initialDeposit);
            return new TestContext(db, account, accountId, initialDeposit);
        } catch (Exception ex) {
            db.close();
            throw ex;
        }
    }

    private record TestContext(Database db, Account account, long accountId, double initialCash) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            db.close();
        }
    }

    @Test
    void buyOrderReducesCashAndAddsPosition() throws Exception {
        try (var ctx = setupAccount(10_000.0)) {
            double price = 250.0;
            int shares = 10;
            long ts = 2_000_000L;

            Order order = new Order(ctx.account(), "AAPL", Order.side.BUY, shares, price, ts);
            long tradeId = ctx.db().recordOrder(order);
            assertTrue(tradeId > 0, "Trade ID should be generated");

            double expectedCash = ctx.initialCash() - (shares * price);
            assertEquals(expectedCash, ctx.db().getAccountCash(ctx.accountId()), 1e-6, "Cash should decrease by trade cost");

            Map<String, Integer> positions = ctx.db().getPositions(ctx.accountId());
            assertTrue(positions.containsKey("AAPL"), "Positions should include purchased symbol");
            assertEquals(shares, positions.get("AAPL"));

            List<ModelFacade.TradeRow> trades = ctx.db().listRecentTrades(ctx.accountId(), 5);
            assertEquals(1, trades.size(), "Exactly one trade should exist");
            ModelFacade.TradeRow row = trades.get(0);
            assertEquals("BUY", row.side());
            assertEquals("AAPL", row.symbol());
            assertEquals(shares, row.quantity());
            assertEquals(price, row.price(), 1e-6);
        }
    }

    @Test
    void sellOrderIncreasesCashAndClearsPositionWhenFullySold() throws Exception {
        try (var ctx = setupAccount(10_000.0)) {
            int shares = 10;
            double buyPrice = 100.0;
            long buyTs = 3_000_000L;

            Order buyOrder = new Order(ctx.account(), "MSFT", Order.side.BUY, shares, buyPrice, buyTs);
            ctx.db().recordOrder(buyOrder);
            ctx.account().setCash(ctx.db().getAccountCash(ctx.accountId()));

            double cashAfterBuy = ctx.db().getAccountCash(ctx.accountId());

            double sellPrice = 120.0;
            long sellTs = buyTs + 1_000L;
            Order sellOrder = new Order(ctx.account(), "MSFT", Order.side.SELL, shares, sellPrice, sellTs);
            ctx.db().recordOrder(sellOrder);
            ctx.account().setCash(ctx.db().getAccountCash(ctx.accountId()));

            double cashAfterSell = ctx.db().getAccountCash(ctx.accountId());
            double expectedCash = cashAfterBuy + (shares * sellPrice);
            assertEquals(expectedCash, cashAfterSell, 1e-6, "Sell proceeds should increase cash");

            Map<String, Integer> positions = ctx.db().getPositions(ctx.accountId());
            assertFalse(positions.containsKey("MSFT"), "Positions should remove symbol after full sell");

            List<ModelFacade.TradeRow> trades = ctx.db().listRecentTrades(ctx.accountId(), 10);
            assertEquals(2, trades.size(), "Two trades should be recorded");
            assertEquals("SELL", trades.get(0).side(), "Most recent trade should be sell");
            assertEquals("BUY", trades.get(1).side(), "Earlier trade should be buy");
        }
    }
}
