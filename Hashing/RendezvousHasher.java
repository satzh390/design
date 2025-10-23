package hashing;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class RendezvousHasher {

    public static class Server {
        public String name;
        public int id;
        public Map<String, String> storage = new HashMap<>();

        public Server(String name, int id) {
            this.name = name;
            this.id = id;
        }
    }

    private final List<Server> servers = new ArrayList<>();
    private final MessageDigest md;
    private final int replicationFactor; // number of servers per key

    public RendezvousHasher(int replicationFactor) throws NoSuchAlgorithmException {
        this.replicationFactor = replicationFactor;
        md = MessageDigest.getInstance("SHA-1");
    }

    private BigInteger hash(String key) {
        byte[] digest = md.digest(key.getBytes());
        return new BigInteger(1, digest);
    }

    public void addServer(String name, int id) {
        for (Server s : servers) {
            if (s.id == id) {
                throw new IllegalArgumentException("Server ID already exists");
            }
        }
        servers.add(new Server(name, id));
    }

    public void removeServer(int id) {
        servers.removeIf(s -> s.id == id);
    }

    private List<Server> getReplicas(String key) {
        PriorityQueue<Map.Entry<Server, BigInteger>> pq = new PriorityQueue<>(
                Comparator.comparing(Map.Entry<Server, BigInteger>::getValue).reversed()
        );

        for (Server s : servers) {
            BigInteger score = hash(key + "#" + s.id);
            pq.add(new AbstractMap.SimpleEntry<>(s, score));
        }

        List<Server> result = new ArrayList<>();
        Set<Integer> added = new HashSet<>();
        while (!pq.isEmpty() && result.size() < replicationFactor) {
            Server s = pq.poll().getKey();
            if (!added.contains(s.id)) {
                result.add(s);
                added.add(s.id);
            }
        }
        return result;
    }

    public void add(String key, String value) {
        if (servers.isEmpty()) {
            throw new IllegalStateException("No servers available");
        }

        List<Server> replicas = getReplicas(key);
        for (Server s : replicas) {
            s.storage.put(key, value);
        }
    }

    public void remove(String key) {
        for (Server s : servers) {
            s.storage.remove(key);
        }
    }

    public void printValues() {
        for (Server s : servers) {
            System.out.println(s.name + " stores keys:");
            if (s.storage.isEmpty()) {
                System.out.println("  (no keys)");
            } else {
                s.storage.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        RendezvousHasher rh = new RendezvousHasher(2); // replication factor 2

        rh.addServer("Server0", 0);
        rh.addServer("Server1", 1);
        rh.addServer("Server2", 2);

        String[] keys = {"apple", "banana", "cherry", "date", "eggplant", "fig", "grape"};
        for (String key : keys) {
            rh.add(key, "Value_of_" + key);
        }

        System.out.println("=== Storage after adding keys ===");
        rh.printValues();

        System.out.println("\nRemoving key 'cherry'");
        rh.remove("cherry");

        System.out.println("\nAdding Server3");
        rh.addServer("Server3", 3);

        System.out.println("\n=== Storage after adding Server3 ===");
        rh.printValues();
    }
}
