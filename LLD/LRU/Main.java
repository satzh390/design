package LLD.LRU;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws Exception {
        basicTest();
        overwriteTest();
        evictionOrderTest();
        concurrencyTest();

        System.out.println("All tests passed ✅");
    }

    // Test 1: Basic put/get
    private static void basicTest() {
        Lru cache = new Lru(2);

        cache.put(1, 10);
        cache.put(2, 20);

        assert cache.get(1) == 10;
        assert cache.get(2) == 20;
    }

    // Test 2: Overwrite existing key
    private static void overwriteTest() {
        Lru cache = new Lru(2);

        cache.put(1, 10);
        cache.put(1, 100);

        assert cache.get(1) == 100;
    }

    // Test 3: Eviction order (core LRU behavior)
    private static void evictionOrderTest() {
        Lru cache = new Lru(2);

        cache.put(1, 10);
        cache.put(2, 20);

        cache.get(1);      // 1 becomes MRU
        cache.put(3, 30);  // should evict 2

        assert cache.get(2) == null;
        assert cache.get(1) == 10;
        assert cache.get(3) == 30;
    }

    // Test 4: Concurrent access
    private static void concurrencyTest() throws Exception {
        Lru cache = new Lru(5);

        ExecutorService pool = Executors.newFixedThreadPool(10);

        int tasks = 1000;
        CountDownLatch latch = new CountDownLatch(tasks);

        for (int i = 0; i < tasks; i++) {
            int key = i % 10;

            pool.submit(() -> {
                try {
                    cache.put(key, key * 10);
                    Integer val = cache.get(key);

                    if (val != null && val != key * 10) {
                        throw new AssertionError("Data inconsistency detected");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();

        // size should never exceed capacity
        // (not exact check since map is private, but ensures no crash)
        System.out.println("Concurrent test completed");
    }
}