package filter;
import java.nio.charset.StandardCharsets;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

public class BloomFilter {
    private final int n;
    private double p = 0.5;
    private int k;
    private final long[] filter;
    private final int m;
    private final int bitSize = 64;

    public BloomFilter(int expectedMaxElements, double expectedFalsePositiveProbability, int k){
        n = expectedMaxElements;
        p = expectedFalsePositiveProbability;
        m = calculateM(n, k, p);
        filter = new long[(m + bitSize - 1) / bitSize]; // to reduce memory size(use bit instead of byte)
        this.k = k;
    }

    public BloomFilter(int expectedMaxElements, double expectedFalsePositiveProbability){
       this(expectedMaxElements, expectedFalsePositiveProbability, 6);
    }

    public void add(String key){
        int[] bitPositions = getFilterPos(key);
        for(int i = 0; i < bitPositions.length; i++){
            int pos = bitPositions[i];
            int segmentPos = pos / bitSize;
            long mask = filter[segmentPos];
            mask = mask | (1L << (64 -(pos % bitSize)));
            filter[segmentPos] = mask;
        }
    }

    public boolean isPresent(String key){
        int[] bitPositions = getFilterPos(key);
        boolean isBitSet = true;
        for(int i = 0; i < bitPositions.length && isBitSet; i++){
            int pos = bitPositions[i];
            int segmentPos = pos / bitSize;
            long mask = filter[segmentPos];
            isBitSet &= (mask & (1L << (64 - (pos % bitSize)))) > 0;
        }
        return isBitSet;
    }

    private int[] getFilterPos(String key) {
        int[] hashes = new int[k];

        // Hash 1: CRC32
        CRC32 crc = new CRC32();
        crc.update(key.getBytes(StandardCharsets.UTF_8));
        int h1 = (int) crc.getValue();

        // Hash 2: Adler32
        Adler32 adler = new Adler32();
        adler.update(key.getBytes(StandardCharsets.UTF_8));
        int h2 = (int) adler.getValue();

        for (int i = 0; i < k; i++) {
            int combined = (h1 + i * h2) & 0x7fffffff; // ensure non-negative
            hashes[i] = combined % m;
        }

        return hashes;
    }

    /**
     * Calculate Bloom filter bit array size m (limited to int)
     *
     * @param n Number of expected elements
     * @param k Number of hash functions
     * @param p Desired false positive probability (0 < p < 1)
     * @return required bit array size m (int)
     * @throws IllegalArgumentException if calculation exceeds Integer.MAX_VALUE
     */
    public static int calculateM(int n, int k, double p) {
        if (n <= 0 || k <= 0) {
            throw new IllegalArgumentException("n and k must be positive.");
        }
        if (p <= 0 || p >= 1) {
            throw new IllegalArgumentException("False positive probability p must be between 0 and 1.");
        }

        // Compute m using formula: m = - (k * n) / ln(1 - p^(1/k))
        /**
         *  P = (1 - (e ^ (-kn/m))) ^ k;
         *  take kth root
         *  take natural log 
         *  result in above formula
         */
        double denominator = Math.log(1 - Math.pow(p, 1.0 / k));
        if (denominator == 0.0) {
            throw new IllegalArgumentException("Invalid parameters: denominator = 0");
        }

        double mDouble = -((double) k * n) / denominator;
        if (mDouble > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "Calculated m exceeds Integer.MAX_VALUE. Reduce n or k, or increase allowed false positive probability p."
            );
        }

        return (int) Math.ceil(mDouble);
    }

    public static void main(String[] args) {
        BloomFilter bloom = new BloomFilter(1_000_000, 0.01);

        // Test keys
        String[] keysToAdd = {"apple", "banana", "cherry", "date", "elderberry", "strawberry"};
        String[] keysNotAdded = {"fig", "grape", "honeydew"};

        // Add keys
        for (String key : keysToAdd) {
            bloom.add(key);
        }

        // Test presence
        System.out.println("Testing added keys:");
        for (String key : keysToAdd) {
            System.out.println(key + ": " + bloom.isPresent(key)); // should be true
        }

        System.out.println("\nTesting not-added keys:");
        for (String key : keysNotAdded) {
            System.out.println(key + ": " + bloom.isPresent(key)); // mostly false, may have some false positives
        }
    }
}
