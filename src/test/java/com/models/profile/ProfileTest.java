package com.models.profile;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {

    @Test
    void noArgConstructorCreatesEmptyProfile() {
        Profile profile = new Profile();
        assertNull(profile.getActiveAccount());
        assertEquals(0, profile.getNumberOfAccounts());
    }

    @Test
    void constructorWithAccountsSetsActive() {
        ArrayList<Account> accounts = new ArrayList<>();
        Account first = new Account("First");
        Account second = new Account("Second");
        accounts.add(first);
        accounts.add(second);

        Profile profile = new Profile(accounts);
        assertEquals(first, profile.getActiveAccount());
        assertEquals(2, profile.getNumberOfAccounts());
    }

    @Test
    void addAndRemoveAccountsManageActiveSelection() {
        Profile profile = new Profile();
        Account first = new Account("First");
        Account second = new Account("Second");

        assertTrue(profile.addAccount(first));
        assertEquals(first, profile.getActiveAccount());
        assertTrue(profile.addAccount(second));
        assertEquals(2, profile.getNumberOfAccounts());

        assertTrue(profile.removeAccount(first));
        assertEquals(second, profile.getActiveAccount());

        assertTrue(profile.removeAccount(second));
        assertNull(profile.getActiveAccount());
    }

    @Test
    void setActiveAccountRequiresMembership() {
        Profile profile = new Profile();
        Account account = new Account("First");

        profile.addAccount(account);
        profile.setActiveAccount(account);
        assertEquals(account, profile.getActiveAccount());

        Account outsider = new Account("Other");
        assertThrows(IllegalArgumentException.class, () -> profile.setActiveAccount(outsider));
    }
}
