package com;

import com.models.*;
import com.gui.*;

import java.sql.SQLException;

/**
 * The entry point for the MarketSim application.
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

        try {
            Database db = new Database(dbFile);
            StartupWindow.getStartWindow(db);
        } catch (Exception e) {
            throw new SQLException("Failed to open database: " + dbFile, e);
        }

    }
}
