package com.models;

import com.models.market.TradeItem;
import com.models.profile.Account;
import com.models.profile.Profile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ModelFacadeWatchlistTest {

    @Test
    public void createAccountCreatesDefaultWatchlistAndPopulation() throws Exception {
        Path dbFile = Files.createTempFile("ms_test_db", ".sqlite");
        Database db = new Database(dbFile.toAbsolutePath().toString());

        // ensure singleton profile
        long profileId = db.ensureSingletonProfile("test");
        Profile p = db.buildProfile(profileId);

        ModelFacade mf = new ModelFacade(db, p);

        // create account
        mf.createAccount("acct1", 0.0);
        Account a = mf.getActiveAccount();
        assertNotNull(a);

        // after creation, db should have a Default watchlist for the account
        List<TradeItem> wl = db.loadWatchlistSymbols(a.getId());
        assertFalse(wl.isEmpty(), "DB should have persisted default watchlist");

        // switching to same account should populate market synchronously (no exceptions)
        mf.switchAccount(a.getId());

        // watchlist view (canonical market-backed items) should include all DB symbols
        List<TradeItem> view = mf.getWatchlistView();
        assertEquals(wl.size(), view.size());

        db.close();
        Files.deleteIfExists(dbFile);
    }
}
