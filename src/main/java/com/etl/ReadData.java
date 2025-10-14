//Read data from data directory, selected file
//Access data access singular .csv
package com.etl;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReadData {
    private Map<String, List<String[]>> data = new HashMap<>();
    private Map<String, String[]> headers = new HashMap<>();

    // Constructor: load all CSV files in the directory
    public ReadData(String dataDir) throws IOException {
        loadData(dataDir);
    }

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
