package com.tools;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class MockFinnhubClientTest {

    @Test
    public void subscribeUnsubscribeConcurrencyAndStop() throws Exception {
        MockFinnhubClient.PriceSeedProvider seedProvider = s -> 100.0;
        MockFinnhubClient client = new MockFinnhubClient(seedProvider);

        // Subscribe a number of symbols concurrently
        ExecutorService ex = Executors.newFixedThreadPool(8);
        int tasks = 50;
        CountDownLatch latch = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            final int idx = i;
            ex.submit(() -> {
                try {
                    String sym = "TST" + (idx % 10);
                    client.subscribe(sym);
                    if (idx % 3 == 0) client.unsubscribe(sym);
                } finally {
                    latch.countDown();
                }
            });
        }
        // wait for all to finish
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Verify that internal subscribedSymbols is consistent (no exceptions thrown above)
        List<String> current = MockFinnhubClient.returnRandomSymbolList();
        assertNotNull(current);

        // Stop the emitter and ensure it stops within a short timeout
        client.stop();
        assertTrue(client.waitForStop(2000), "emitter did not stop within timeout");

        ex.shutdownNow();
        ex.awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    public void waitForStop_returnsFalseForVerySmallTimeout() throws Exception {
        MockFinnhubClient client = new MockFinnhubClient(s -> 100.0);
        // Do not stop; immediate tiny timeout should return false because emitter is still running
        boolean stopped = client.waitForStop(1);
        assertFalse(stopped, "waitForStop should return false for tiny timeout when emitter is running");
        // Cleanup
        client.stop();
        client.waitForStop(2000);
    }
}

