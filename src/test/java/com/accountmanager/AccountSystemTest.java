package com.accountmanager;

import com.models.market.Stock;
import com.models.market.TradeItem;
import com.models.profile.Account;
import com.models.profile.Portfolio;
import com.models.profile.Profile;
import com.models.profile.Watchlist;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// This class provides comprehensive logic for testing the functions involved
// in creating and managing accounts, portfolios, and watchlists

public class AccountSystemTest {

    // TESTS FOR ACCOUNTS

    @Test
    void testAccount(){
        Account account1 = new Account("account1");

        // an Account has a name
        assertEquals("account1", account1.getName());

        // An Account has a beginning value of 0
        assertEquals(0,account1.getTotalValue());

        // An account has a beginning available balance of 0
        assertEquals(0, account1.getAvailableBalance());
    }

    @Test
    void testAccountDeposit() {
        Account account1 = new Account("account1");
        assertEquals(0, account1.getTotalValue());
        assertEquals(0, account1.getAvailableBalance());

        // An account can accept deposits
        assertTrue(account1.depositFunds(10));
        assertEquals(10, account1.getTotalValue());
        assertEquals(10, account1.getAvailableBalance());

        // An account will not accept a $0 deposit
        assertFalse(account1.depositFunds(0));

        // An account will not accept a negative deposit
        assertFalse(account1.depositFunds(-1));
    }

    @Test
    void testAccountWithdrawalStandard() {
        Account account1 = new Account("account1");
        account1.depositFunds(10);

        // An account allows a withdrawal
        assertTrue(account1.withdrawFunds(1));
        assertEquals(9, account1.getTotalValue());
        assertEquals(9, account1.getAvailableBalance());

        // An account does not allow withdrawing more than the available balance
        assertFalse(account1.withdrawFunds(10));

        // An account does not allow a $0 withdrawal
        assertFalse(account1.withdrawFunds(0));

        // An account does not allow a negative withdrawal
        assertFalse(account1.withdrawFunds(-1));

    }

    @Test
    void testAccountValueUpdatesWithWithdrawal(){
        // setup account, stock, and portfolio objects
        Account account1 = new Account("account1");
    Stock stock1 = new Stock("stock1", "s_init");
        Portfolio portfolio = account1.getPortfolio();

        // Deposit funds, assign price to stock
        account1.depositFunds(10);
        stock1.updatePrice(10);

        // Add stock to portfolio
        portfolio.addTradeItem(stock1,1);

        // An account can return its total value
        // (available balance + portfolio value)
        assertEquals(10, account1.getAvailableBalance());
        assertEquals(20, account1.getTotalValue());

        // An account allows withdrawals from its available balance
        assertTrue(account1.withdrawFunds(10));
        assertEquals(0, account1.getAvailableBalance());
        assertEquals(10, account1.getTotalValue());

        // An account will not allow withdrawals if its available balance is <1
        assertFalse(account1.withdrawFunds(10));
        assertEquals(10, account1.getTotalValue());
    }

    @Test
    void testAccountValueFunctions(){
        Account accountOne = new Account("accountOneName");
        // An account can have funds deposited
        accountOne.depositFunds(10);
        // Despoited funds are in available balance
        assertEquals(10, accountOne.getAvailableBalance());
        // the available balance updates the account total value
        assertEquals(10, accountOne.getTotalValue());
        // An account can have funds withdrawn
        accountOne.withdrawFunds(2);
        assertEquals(8, accountOne.getAvailableBalance());
        assertEquals(8, accountOne.getTotalValue());
        // Withdrawn request cannot exceed available funds
        assertFalse(accountOne.withdrawFunds(10));
        assertEquals(8, accountOne.getAvailableBalance());
    }

    @Test
    void testAccountManager() {
        Profile profile = new Profile();
        // Two test accounts are created
        Account accountOne =  new Account("accountOneName");
        Account accountTwo = new Account("accountTwoName");
        // An account can be added to the account manager
        assertTrue(profile.addAccount(accountOne));
        assertTrue(profile.addAccount(accountTwo));
        // An account manager knows how many accounts it holds
        assertEquals(2, profile.getNumberOfAccounts());


        Account accountThree = new Account("accountThreeName");
        Account accountFour = new Account("accountFourName");
        Account accountFive = new Account("accountFiveName");
        Account accountSix = new Account("accountSixName");
        profile.addAccount(accountThree);
        profile.addAccount(accountFour);
        profile.addAccount(accountFive);

        // An account manager will only allow 5 accounts
        assertFalse(profile.addAccount(accountSix));
        assertEquals(5, profile.getNumberOfAccounts());

        // An account manager can remove accounts
        assertTrue(profile.removeAccount(accountOne));
        assertTrue(profile.removeAccount(accountTwo));

        // An account manager returns false if it does not hold requested account
        assertFalse(profile.removeAccount(accountOne));


    }

    // TESTS FOR PORTFOLIO

    @Test
    void testPortfolio() {
        // A portfolio is created
        Portfolio portfolio = new Portfolio();

    Stock stock1 = new Stock("stock1", "s_init2");
        portfolio.addTradeItem(stock1, 1);

        // A portfolio can return true if it holds a stock
        assertTrue(portfolio.hasTradeItem(stock1));
    }

    @Test
    void testPortfolioAddsItems() {
        Portfolio portfolio = new Portfolio();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = new Stock("stock2", "s2");

        // A portfolio can add items
        assertTrue(portfolio.addTradeItem(stock1, 1));
        assertTrue(portfolio.hasTradeItem(stock1));

        // A portfolio will not add 0 items
        assertFalse(portfolio.addTradeItem(stock2, 0));

        // A portfolio will not add new negative items
        assertFalse(portfolio.addTradeItem(stock2, -1));

        // A portfolio will not add existing negative items
        assertFalse(portfolio.addTradeItem(stock1, -1));
    }

    @Test
    void testPortfolioRemovesItems() {
        Portfolio portfolio = new Portfolio();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = new Stock("stock2", "s2");
        Stock stock3 = null;
        portfolio.addTradeItem(stock1, 1);
        portfolio.addTradeItem(stock2, 1);

        // A portfolio can remove items
        assertTrue(portfolio.removeTradeItem(stock1, 1));
        assertFalse(portfolio.hasTradeItem(stock1));

        // A portfolio will not remove 0 items
        assertFalse(portfolio.removeTradeItem(stock1, 0));

        // A portfolio will not remove negative items
        assertFalse(portfolio.removeTradeItem(stock2, -1));

        // A portfolio will not remove a null item
        assertFalse(portfolio.removeTradeItem(stock3, 1));

        // A portfolio will not remove an item it does not have
        assertFalse(portfolio.removeTradeItem(stock1, 1));
    }

    @Test
    void testPortfolioListsTradeItems() {
        Portfolio portfolio = new Portfolio();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = new Stock("stock2", "s2");
        portfolio.addTradeItem(stock1, 1);
        portfolio.addTradeItem(stock2, 1);

        // A portfolio will return a list of its trade items
        List<TradeItem> list = portfolio.listTradeItems();
        assertEquals(2, list.size());
        assertTrue(portfolio.hasTradeItem(list.get(0)));
        assertTrue(portfolio.hasTradeItem(list.get(1)));
    }

    @Test
    void testPortfolioCalculatesValue() {
        Portfolio portfolio = new Portfolio();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = new Stock("stock2", "s2");
        stock1.updatePrice(20);
        stock2.updatePrice(20);
        portfolio.addTradeItem(stock1, 1);
        portfolio.addTradeItem(stock2, 1);

        // A portfolio can calculate its value
        assertEquals(40, portfolio.getPortfolioValue());

        // Add one more share of stock to portfolio
        portfolio.addTradeItem(stock1, 1);

        // Value is calculated with number of shares account for
        assertEquals(60, portfolio.getPortfolioValue());

        stock2.updatePrice(21);
        // Prices of portfolio are calculated based on new stock values
        assertEquals(61, portfolio.getPortfolioValue());

    }

    // TESTS FOR WATCHLIST

    @Test
    void testWatchlist() {
        Watchlist watchlist = new Watchlist();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = new Stock("stock2", "s2");

        // A watchlist can return its length
        assertEquals(0, watchlist.getWatchlistSize());
        watchlist.addWatchlistItem(stock1);
        assertEquals(1, watchlist.getWatchlistSize());
    }

    @Test
    void testWatchlistAddsItems() {
        Watchlist watchlist = new Watchlist();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = null;

        // A watchlist can add items to itself
        assertTrue(watchlist.addWatchlistItem(stock1));
        assertEquals(1, watchlist.getWatchlistSize());
        assertTrue(watchlist.hasTradeItem(stock1));

        // A watchlist will not add a null item
        assertFalse(watchlist.addWatchlistItem(stock2));
        assertFalse(watchlist.hasTradeItem(stock2));
        assertEquals(1, watchlist.getWatchlistSize());

        // A watchlist will not add an item it already has
        assertFalse(watchlist.addWatchlistItem(stock1));

        // Add stocks to max size of watchlist
        for (int i = 0; i < watchlist.getMaxSize()-1; i++) {
            Stock s = new Stock("stock" + i, "s" + i);
            watchlist.addWatchlistItem(s);
        }
        assertEquals(watchlist.getMaxSize(), watchlist.getWatchlistSize());

        Stock stock3 = new Stock("stock3", "s3");

        // A watchlist will not add items past its max size
        assertFalse(watchlist.addWatchlistItem(stock3));
    }

    @Test
    void testWatchlistRemovesItems() {
        Watchlist watchlist = new Watchlist();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = new Stock("stock2", "s2");
        Stock stock3 = null;
        watchlist.addWatchlistItem(stock1);
        watchlist.addWatchlistItem(stock2);

        // A Watchlist can remove items from itself
        assertTrue(watchlist.removeWatchlistItem(stock1));
        assertFalse(watchlist.hasTradeItem(stock1));

        // A watchlist will not remove an item it does not have
        assertFalse(watchlist.removeWatchlistItem(stock1));

        // A watchlist will not remove a null item
        assertFalse(watchlist.removeWatchlistItem(stock3));

        watchlist.addWatchlistItem(stock1);
        assertEquals(2, watchlist.getWatchlistSize());

        // A watchlist can be cleared of all items
        watchlist.clearList();
        assertEquals(0, watchlist.getWatchlistSize());
        assertFalse(watchlist.hasTradeItem(stock1));
        assertFalse(watchlist.hasTradeItem(stock2));

    }

    @Test
    void testWatchlistListsTradeItems() {
        Watchlist watchlist = new Watchlist();
        Stock stock1 = new Stock("stock1", "s1");
        Stock stock2 = new Stock("stock2", "s2");
        watchlist.addWatchlistItem(stock1);
        watchlist.addWatchlistItem(stock2);

        // A watchlist can return a list of trade items it contains
        List list = watchlist.getWatchlist();
        assertEquals(stock1, list.get(0));
        assertEquals(stock2, list.get(1));
    }
}