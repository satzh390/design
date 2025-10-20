package design.urlShortner.code;

import FilterAlgo.BloomFilter;
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
            // but above is too large for this demo, so to keep it simple with idLength 5
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
            System.out.println(curHash);
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
}
