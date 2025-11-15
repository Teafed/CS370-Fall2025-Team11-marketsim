package com.gui;

import com.accountmanager.Account;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AccountCardTest {

    @Test
    public void activateActionInvokesOnPick() {
        Account acct = new Account("Test Account", 1000.0);
        AtomicBoolean picked = new AtomicBoolean(false);

        AccountCard card = AccountCard.forAccount(acct, a -> picked.set(true));

        javax.swing.Action act = card.getActionMap().get("activate");
        assertNotNull(act, "activate action should be present on AccountCard");

        act.actionPerformed(null);
        assertTrue(picked.get(), "AccountCard.activate should call onPick.accept(account)");
    }
}
