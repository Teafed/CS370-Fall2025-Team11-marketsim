package com.models.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTest {
    @BeforeEach
    void resetProfileSingleton() throws Exception {
        Field instance = Profile.class.getDeclaredField("profile");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    void noArgConstructorCreatesEmptyProfile() {
        Profile profile = Profile.initProfile(null);
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

        Profile profile = Profile.initProfile(accounts);
        assertEquals(first, profile.getActiveAccount());
        assertEquals(2, profile.getNumberOfAccounts());
    }

    @Test
    void addAndRemoveAccountsManageActiveSelection() {
        Profile profile = Profile.initProfile(null);
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
        Profile profile = Profile.initProfile(null);
        Account account = new Account("First");

        profile.addAccount(account);
        profile.setActiveAccount(account);
        assertEquals(account, profile.getActiveAccount());

        Account outsider = new Account("Other");
        assertThrows(IllegalArgumentException.class, () -> profile.setActiveAccount(outsider));
    }
}
