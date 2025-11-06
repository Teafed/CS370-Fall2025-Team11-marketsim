package com;

import com.market.*;
import com.gui.*;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        String dbFile = "data/marketsim-sample.db";

        // Initialize Database
        Database db;
        try {
            db = new Database(dbFile);
        } catch (Exception e) {
            throw new SQLException("Failed to open database: " + dbFile, e);
        }

        StartupWindow.getStartWindow(db, (profileName, balance)-> {
            System.out.println("Profile: " + profileName + " Balance: " + balance);
            StartupWindow.runMarketSim(db, profileName, balance);
        });

        // runTestCase();
    }

    public static void runTestCase() {
        // Add new testing data

    }
}
