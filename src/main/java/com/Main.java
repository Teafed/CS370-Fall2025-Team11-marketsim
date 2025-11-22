package com;

import com.models.*;
import com.gui.*;

import java.sql.SQLException;

/**
 * The entry point for the Market Simulator application.
 * Initializes the database and launches the startup window.
 */
public class Main {
    /**
     * Main method.
     *
     * @param args Command line arguments.
     * @throws SQLException If the database cannot be initialized.
     */
    public static void main(String[] args) throws SQLException {
        String dbFile = "data/marketsim-sample.db";

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

    /**
     * Runs test cases (currently unused).
     */
    public static void runTestCase() {
        // Add new testing data

    }
}
