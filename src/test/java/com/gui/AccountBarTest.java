package com.gui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class AccountBarTest {

    @Test
    public void enterKeyActivatesOnClick() {
        AccountBar bar = new AccountBar();
        AtomicBoolean invoked = new AtomicBoolean(false);
        bar.setOnClick(() -> invoked.set(true));

        // action map entry created by AccountBar should be 'activate'
        javax.swing.Action act = bar.getActionMap().get("activate");
        assertNotNull(act, "activate action should be present");

        act.actionPerformed(null);
        assertTrue(invoked.get(), "onClick should be invoked when activate action performed");
    }
}
