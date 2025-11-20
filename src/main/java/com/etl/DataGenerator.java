package com.etl;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generates sample stock data for testing and development purposes
 */
public class DataGenerator {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * Generate sample data for all default stocks
     * 
     * @param dataDir Directory where data files will be stored
     */
    public static void generateAllStockData(String dataDir) {
        // Create data directory if it doesn't exist
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Generate data for default stocks
        String[] symbols = { "AAPL", "MSFT", "GOOGL", "AMZN", "META" };
        for (String symbol : symbols) {
            generateStockData(dataDir, symbol);
        }

        System.out.println("Sample data generated successfully in: " + dataDir);
    }

    /**
     * Generate sample data for a specific stock
     * 
     * @param dataDir Directory where data files will be stored
     * @param symbol  Stock symbol
     */
    public static void generateStockData(String dataDir, String symbol) {
        String filePath = new File(dataDir, symbol + ".csv").getPath();

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Write headers - match the format expected by ChartPanel
            writer.println("id,timestamp,price,volume,open,close,high,low");

            // Generate 3 days of data with 5-minute intervals
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 30);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // Start with a base price based on the symbol
            double basePrice = getBasePrice(symbol);

            // Generate data for 3 days
            for (int day = 0; day < 3; day++) {
                // Set to previous day
                calendar.add(Calendar.DATE, -1);

                // Trading hours: 9:30 AM to 4:00 PM
                calendar.set(Calendar.HOUR_OF_DAY, 9);
                calendar.set(Calendar.MINUTE, 30);

                double previousClose = basePrice;

                // Generate data points for one day (5-minute intervals)
                for (int i = 0; i < 78; i++) { // 78 intervals of 5 minutes in 6.5 hours
                    String timestamp = DATE_FORMAT.format(calendar.getTime());
                    int id = day * 100 + i;

                    // Generate realistic price movements
                    double change = previousClose * (Math.random() * 0.02 - 0.01); // -1% to +1%
                    double open = previousClose;
                    double close = Math.max(0.01, open + change);
                    double high = Math.max(open, close) * (1 + Math.random() * 0.005);
                    double low = Math.min(open, close) * (1 - Math.random() * 0.005);
                    int volume = (int) (Math.random() * 99000) + 1000; // 1,000 to 100,000

                    // Write data point - match the format expected by ChartPanel
                    writer.printf("%d,%s,%.2f,%d,%.2f,%.2f,%.2f,%.2f%n",
                            id, timestamp, close, volume, open, close, high, low);

                    // Move to next interval
                    calendar.add(Calendar.MINUTE, 5);
                    previousClose = close;
                }

                // Update base price for next day
                basePrice = previousClose;
            }
        } catch (IOException e) {
            System.err.println("Error generating data for " + symbol + ": " + e.getMessage());
        }
    }

    /**
     * Get a realistic base price for each stock symbol.
     *
     * @param symbol The stock symbol.
     * @return The base price.
     */
    private static double getBasePrice(String symbol) {
        switch (symbol) {
            case "AAPL":
                return 175.50;
            case "MSFT":
                return 325.75;
            case "GOOGL":
                return 140.25;
            case "AMZN":
                return 130.50;
            case "META":
                return 450.25;
            default:
                return 100.00;
        }
    }
}
