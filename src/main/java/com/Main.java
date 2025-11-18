package com;

import com.models.*;
import com.gui.*;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        String dbFile = "data/marketsim-sample2.db";

        // Initialize Database
        Database db;
        try {
            db = new Database(dbFile);
        } catch (Exception e) {
            throw new SQLException("Failed to open database: " + dbFile, e);
        }

        StartupWindow.getStartWindow(db);

        // runTestCase();
    }

    public static void runTestCase() {
        // Add new testing data

    }
}
