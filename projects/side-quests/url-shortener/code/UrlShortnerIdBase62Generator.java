import java.util.zip.CRC32;

public class UrlShortnerIdBase62Generator {
    char[] base62Map = new char[62];

    public UrlShortnerIdBase62Generator() {
        int i = 0;
        for (; i < 26; i++) {
            base62Map[i] = (char) ('a' + i);
        }

        for (; i < 52; i++) {
            base62Map[i] = (char) ('A' + (i % 26));
        }

        for (; i < 62; i++) {
            base62Map[i] = (char) ('0' + (i - 52));
        }
    }

    private long hash(String key) {
        CRC32 crc = new CRC32();
        crc.update(key.getBytes());
        return crc.getValue();
    }

    public String generate(String key) {
        // just using normal hash to give some randomness, whereas any id generator can be used(if generator is seq, it would easy to predict next url id)
        long generatedId = hash(key);
        StringBuilder sb = new StringBuilder();

        int base = base62Map.length;
        while (generatedId > 0) {
            int m = (int) (generatedId % base);
            sb.append(base62Map[m]);
            generatedId /= base;
        }

        return sb.reverse().toString(); // reverse for more natural order
    }

    public static void main(String[] args) {
        UrlShortnerIdBase62Generator generator = new UrlShortnerIdBase62Generator();

        String[] testUrls = {
            "https://example.com",
            "https://openai.com/research",
            "https://github.com",
            "https://news.ycombinator.com",
            "https://example.com" // same URL should give same short ID
        };

        for (String url : testUrls) {
            String shortId = generator.generate(url);
            System.out.println("Original: " + url);
            System.out.println("Short ID: " + shortId);
            System.out.println("------------------------");
        }
    }
}

