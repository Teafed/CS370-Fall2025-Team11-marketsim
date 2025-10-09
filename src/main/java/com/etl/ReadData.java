//Read data from data directory, selected file
//Access data access singular .csv
package com.etl;

import com.market.DatabaseManager;

import java.sql.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReadData {
    private final DatabaseManager db;

    public static class Row {
        public final long timestamp;
        public final double open, high, low, close;
        public final long volume;
        public Row(long t, double o, double h, double l, double c, long v) {
            this.timestamp = t; this.open=o; this.high=h; this.low=l; this.close=c; this.volume=v;
        }
    }

    /**
     * for drawing chart
     * @param symbol
     * @param startMillis
     * @param endMillis
     * @return
     * @throws SQLException
     */
    public List<Row> loadSeries(String symbol, long startMillis, long endMillis) throws SQLException {
        List<Row> out = new ArrayList<>();
        try (ResultSet rs = db.getPrices(symbol, startMillis, endMillis)) {
            while (rs.next()) {
                out.add(new Row(
                        rs.getLong("timestamp"),
                        rs.getDouble("open"),
                        rs.getDouble("high"),
                        rs.getDouble("low"),
                        rs.getDouble("close"),
                        rs.getLong("volume")
                ));
            }
        }
        return out;
    }

    /**
     * for symbol list
     * @return
     * @throws SQLException
     */
    public Set<String> listSymbols() throws SQLException {
        String sql = "SELECT DISTINCT symbol FROM prices ORDER BY symbol";
        Set<String> syms = new TreeSet<>();
        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) syms.add(rs.getString(1));
        }
        return syms;
    }

    // temporary, eventually remove dataDir string
    public ReadData(String dataDir, DatabaseManager db) throws IOException {
        this.db = db;
        loadData(dataDir);
    }

    // - - -
    // TODO: transition below code to use db
    // - - -

    private Map<String, List<String[]>> data = new HashMap<>();
    private Map<String, String[]> headers = new HashMap<>();

    private void loadData(String dataDir) throws IOException {
        Files.list(Paths.get(dataDir))
                .filter(path -> path.toString().endsWith(".csv"))
                .forEach(path -> {
                    try {
                        loadSingleFile(path.toFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    // Load a single CSV file
    public void loadSingleFile(File file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                String[] values = Arrays.stream(line.split(","))
                        .map(String::trim)
                        .toArray(String[]::new);
                if (firstLine) {
                    headers.put(file.getName(), values); // store header
                    firstLine = false;
                } else {
                    rows.add(values);
                }
            }
        }
        data.put(file.getName(), rows);
    }

    // Get rows of a specific file
    public List<String[]> getFileData(String fileName) {
        return data.get(fileName);
    }

    // Get column headers of a specific file
    public String[] getHeaders(String fileName) {
        return headers.get(fileName);
    }

    // Get all CSV file names loaded
    public Set<String> getFileNames() {
        return data.keySet();
    }

    // Convenience: get first CSV file (if you just want "singular access")
    public String getFirstFileName() {
        return data.keySet().stream().findFirst().orElse(null);
    }

    public List<String[]> getFirstFileData() {
        String file = getFirstFileName();
        return file != null ? getFileData(file) : Collections.emptyList();
    }

    public String[] getFirstFileHeaders() {
        String file = getFirstFileName();
        return file != null ? getHeaders(file) : null;
    }
}
