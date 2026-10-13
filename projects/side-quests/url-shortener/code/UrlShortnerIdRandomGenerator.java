import java.nio.charset.StandardCharsets;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

public class UrlShortnerIdRandomGenerator {
    private final int idLength;
    private final BloomFilter filter;
    private final String collisionAppendKey;
    private final int expectedNoOfElements = 14_776_336;

    // let support only CRC32, we can also use other hash algo
    public UrlShortnerIdRandomGenerator(int idLength, String collisionAppendKey){
        if(idLength > 4){
            // as CRC32 generate hash of length 8, if we allow for len 8
            // long maxNoOfElements = 8_218_340_105_584_896L; // a-z A-Z 0-9 62 pow 8 21,83,40,10,55,84,896
            // but above is too large for this demo, so to keep it simple with idLength 4
            throw new IllegalArgumentException("Id length should be less than or equal to 4");
        }

        this.idLength = idLength;
        this.collisionAppendKey = collisionAppendKey;
        filter = new BloomFilter(expectedNoOfElements, 0.1, 5);
    }

    private String hash(String key){
        CRC32 crc = new CRC32();
        crc.update(key.getBytes());
        int hash = (int) crc.getValue();
        String hex = String.format("%08X", hash);
        return hex.substring(0, idLength + 1);
    }

    public String generate(String longUrl){
        String curHash = hash(longUrl);
        int count = 0;
        while (filter.isPresent(curHash) && count < expectedNoOfElements) {
            longUrl += collisionAppendKey;
            curHash = hash(longUrl);
            count += 1;
        }

        if(count == expectedNoOfElements){
            throw new RuntimeException("Can't able to generate ID, may be exhausted key space");
        }

        filter.add(curHash);
        return curHash;
    }

    public static void main(String[] args) {
        UrlShortnerIdRandomGenerator generator = new UrlShortnerIdRandomGenerator(4, "_retry");

        String url1 = "https://example.com/abc";
        String url2 = "https://example.com/xyz";
        String url3 = "https://example.com/abc"; // same as url1 to check repeat handling

        String id1 = generator.generate(url1);
        String id2 = generator.generate(url2);
        String id3 = generator.generate(url3);

        System.out.println("URL 1: " + url1 + " -> Short ID: " + id1);
        System.out.println("URL 2: " + url2 + " -> Short ID: " + id2);
        System.out.println("URL 3 (same as URL 1): " + url3 + " -> Short ID: " + id3);
    }

    private static class BloomFilter {
        private final int n;
        private double p = 0.5;
        private int k;
        private final long[] filter;
        private final int m;
        private final int bitSize = 64;

        BloomFilter(int expectedMaxElements, double expectedFalsePositiveProbability, int k) {
            n = expectedMaxElements;
            p = expectedFalsePositiveProbability;
            m = calculateM(n, k, p);
            filter = new long[(m + bitSize - 1) / bitSize];
            this.k = k;
        }

        void add(String key) {
            int[] bitPositions = getFilterPos(key);
            for (int pos : bitPositions) {
                int segmentPos = pos / bitSize;
                long mask = filter[segmentPos];
                mask |= 1L << (64 - (pos % bitSize));
                filter[segmentPos] = mask;
            }
        }

        boolean isPresent(String key) {
            int[] bitPositions = getFilterPos(key);
            boolean isBitSet = true;
            for (int pos : bitPositions) {
                int segmentPos = pos / bitSize;
                long mask = filter[segmentPos];
                isBitSet &= (mask & (1L << (64 - (pos % bitSize)))) > 0;
                if (!isBitSet) {
                    break;
                }
            }
            return isBitSet;
        }

        private int[] getFilterPos(String key) {
            int[] hashes = new int[k];

            CRC32 crc = new CRC32();
            crc.update(key.getBytes(StandardCharsets.UTF_8));
            int h1 = (int) crc.getValue();

            Adler32 adler = new Adler32();
            adler.update(key.getBytes(StandardCharsets.UTF_8));
            int h2 = (int) adler.getValue();

            for (int i = 0; i < k; i++) {
                int combined = (h1 + i * h2) & 0x7fffffff;
                hashes[i] = combined % m;
            }

            return hashes;
        }

        private static int calculateM(int n, int k, double p) {
            if (n <= 0 || k <= 0) {
                throw new IllegalArgumentException("n and k must be positive.");
            }
            if (p <= 0 || p >= 1) {
                throw new IllegalArgumentException("False positive probability p must be between 0 and 1.");
            }

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
    }
}
