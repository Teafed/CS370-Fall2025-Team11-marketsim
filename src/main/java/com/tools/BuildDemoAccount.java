// src/main/java/com/tools/MockAccountFactory.java
package com.tools;

import com.accountmanager.Account;

public class BuildMockAccount {
    private Account account;

    public Account createMockAccount() {
        this.account = new Account("Demo Account");
        account.depositFunds(42069.0f);
        return account;
    }
}
