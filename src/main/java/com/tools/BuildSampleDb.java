package com.tools;

import com.market.DatabaseManager;
import java.time.*;
import java.util.*;

public class BuildSampleDb {

    public static void main(String[] args) throws Exception {
        String dbFile = "data/marketsim-sample.db";
        boolean force = Arrays.asList(args).contains("--force");

        try (DatabaseManager db = new DatabaseManager(dbFile)) {
            if (!force && isSeeded(db)) {
                System.out.println("[seed] Already seeded. Use --force to overwrite. DB: " + dbFile);
                System.out.println(db.listSymbols());
                return;
            }
            seedAll(db);
        }
        System.out.println("[seed] Done: " + dbFile);
    }

    private static boolean isSeeded(DatabaseManager db) throws Exception {
        return !db.listSymbols().isEmpty() && db.getLatestTimestamp("AAPL") > 0;
    }

    private static void seedAll(DatabaseManager db) throws Exception {
        List<String> symbols = List.of("AAPL", "MSFT", "SPY");
        int days = 120;

        for (String sym : symbols) {
            var rows = makeDailySeries(sym, days);
            db.insertPricesBatch(rows);
        }
        System.out.println("[seed] Inserted " + symbols.size() + " symbols × " + days + " days");
    }

    /** Make a simple, repeatable OHLCV series (daily close around 16:00 UTC). */
    private static java.util.List<DatabaseManager.PriceRow> makeDailySeries(String symbol, int days) {
        // deterministic RNG per symbol
        long seed = symbol.chars().asLongStream().reduce(0, (a, b) -> a * 131L + b);
        Random rng = new Random(seed);

        double base = switch (symbol) {
            case "AAPL" -> 180.0;
            case "MSFT" -> 400.0;
            case "SPY"  -> 500.0;
            default -> 100.0;
        };

        double drift = 0.0005;
        double vol   = 0.01;
        double close = base;

        var out = new ArrayList<DatabaseManager.PriceRow>(days);
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = end.minusDays(days - 1);

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            // sinusoid + noise + drift
            double wave = 0.01 * Math.sin(2 * Math.PI * (d.toEpochDay() % 30) / 30.0);
            double ret  = drift + wave + (rng.nextGaussian() * vol * 0.2);
            double newClose = Math.max(1.0, close * (1.0 + ret));
            double open = close * (1.0 + (rng.nextGaussian() * vol * 0.05));
            double high = Math.max(open, newClose) * (1.0 + Math.abs(rng.nextGaussian()) * 0.01);
            double low  = Math.min(open, newClose) * (1.0 - Math.abs(rng.nextGaussian()) * 0.01);
            long volume = (long) (5_000_000 + Math.abs(rng.nextGaussian()) * 2_000_000);

            long ts = d.atTime(16, 0).toInstant(ZoneOffset.UTC).toEpochMilli(); // 16:00 UTC
            out.add(new DatabaseManager.PriceRow(symbol, ts, open, high, low, newClose, volume));
            close = newClose;
        }
        return out;
    }
}
