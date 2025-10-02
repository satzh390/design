package Hashing;

import java.util.*;
import java.util.Map.Entry;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ConsistentHasher {
    public static class Server {
        public String name;
        public int id;
        public Map<BigInteger, String> storage = new HashMap<>();

        public Server(String name, int id){
            this.name = name;
            this.id = id;
        }
    }

    private final int virtualNodeCountPerServer;
    private final TreeMap<BigInteger, Server> hashRing = new TreeMap<>();
    private final HashSet<Integer> serverIds = new HashSet<>();
    private final MessageDigest md;

    public ConsistentHasher(int virtualNodeCountPerServer) throws NoSuchAlgorithmException {
        this.virtualNodeCountPerServer = virtualNodeCountPerServer;
        md = MessageDigest.getInstance("SHA-1");
    }   


    private BigInteger hash(String key){
        byte[] digest = md.digest(key.getBytes());
        return new BigInteger(1, digest);
    }

    public void add(String key, String value){
        if(serverIds.size() == 0){
            throw new IllegalStateException("Currently no server exist to add value, Please add a server to add value!");
        }

        BigInteger h = hash(key);
        Server server = findNextServerPosition(h).getValue();
        server.storage.put(h, value);
    }

    private boolean isKeyInRange(BigInteger key, BigInteger start, BigInteger end){
        if(start.compareTo(end) < 0){
            return key.compareTo(start) > 0 && key.compareTo(end) <= 0;
        } else { // wrap-around
            return key.compareTo(start) > 0 || key.compareTo(end) <= 0;
        }
    }

    public void remove(String key){
        if(serverIds.size() == 0){
            throw new IllegalStateException("Currently no server exist to remove value, Please add a server!");
        }

        BigInteger h = hash(key);
        Server server = findNextServerPosition(h).getValue();
        server.storage.remove(h);
    }

    public void addServer(String name, int id) {
        if(serverIds.contains(id)){
            throw new IllegalArgumentException(String.format("Server with id {} already exist. Please provide unique server id", id));
        }

        Server s = new Server(name, id);
        for(int i = 0; i < virtualNodeCountPerServer; i++){
            String vNodeId = virtualNodeId(id, i);
            BigInteger h = hash(vNodeId);
            Entry<BigInteger, Server> srcNode = findPrevServerPosition(h);
            hashRing.put(h, s);
            if(srcNode == null){
                continue;
            }

            moveValuesToNewServer(h, s, srcNode);
        }

        serverIds.add(id);
    }

    private Entry<BigInteger, Server> findNextServerPosition(BigInteger position){
        Entry<BigInteger, Server> greater = hashRing.ceilingEntry(position);
        Entry<BigInteger, Server> lowest = hashRing.firstEntry();
        return Optional.ofNullable(greater).orElse(lowest);
    }

    private Entry<BigInteger, Server> findPrevServerPosition(BigInteger position){
        Entry<BigInteger, Server> previous = hashRing.lowerEntry(position);
        Entry<BigInteger, Server> greatest = hashRing.lastEntry();
        return Optional.ofNullable(previous).orElse(greatest);
    }

    private void moveValuesToNewServer(BigInteger hash, Server s, Entry<BigInteger, Server> srcNode){
        Server prevServer = srcNode.getValue();
        Iterator<Map.Entry<BigInteger, String>> it = prevServer.storage.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<BigInteger, String> entry = it.next();
            BigInteger keyHash = entry.getKey();                
            if(isKeyInRange(keyHash, srcNode.getKey(), hash)){
                s.storage.put(keyHash, entry.getValue());
                it.remove();
            }
        }
    }

    public void removeServer(int id, boolean forceRemove) {
        if(!serverIds.contains(id)){
            throw new IllegalArgumentException(String.format("Server with id {} not found", id));
        }

        if(serverIds.size() == 1 && !forceRemove){
            throw new IllegalArgumentException("Atleast one server is needed in the ring. Still want to force remove use forceRemove arg!");
        }

        BigInteger firstVirtualNodeHash = hash(virtualNodeId(id, 0));
        Server s = hashRing.get(firstVirtualNodeHash);
        hashRing.remove(firstVirtualNodeHash);

        for(int i = 1; i < virtualNodeCountPerServer; i++){
            String vNodeId = virtualNodeId(id, i);
            BigInteger h = hash(vNodeId);
            hashRing.remove(h);
        }

        for(Entry<BigInteger, String> e: s.storage.entrySet()){
            BigInteger h = e.getKey();
            Server nextServer = findNextServerPosition(h).getValue();
            nextServer.storage.put(h, e.getValue());
        }
        serverIds.remove(id);
    }

    private String virtualNodeId(int serverId, int vNodeId){
        return String.format("v_node_%d_%d", serverId, vNodeId);
    }

    private void printValues(){
        System.out.println("No Of virtual nodes - " + hashRing.size());
        System.out.println("Hash Ring");
        for (Entry<BigInteger, Server> entry : hashRing.entrySet()){
            System.out.println(String.format("Virtual node %s belongs to %s", entry.getKey(), entry.getValue().name));
        }

        Set<Integer> printed = new HashSet<>();
        for (Entry<BigInteger, Server> entry : hashRing.entrySet()) {
            Server s = entry.getValue();
            // System.out.println("Server " + s.id + " vnode -> " + entry.getKey());
            if (printed.contains(s.id)) continue; // avoid printing duplicates 
            printed.add(s.id);
            System.out.println(s.name + " stores keys:");
            if (s.storage != null && !s.storage.isEmpty()) {
                s.storage.forEach((k, v) -> System.out.println("  " + k + " -> " + v));
            } else {
                System.out.println("  (no keys)");
            }
        }
    }

     public static void main(String[] args) throws Exception {
        // Create ConsistentHasher with 3 virtual nodes per server
        ConsistentHasher hasher = new ConsistentHasher(200);

        // Add servers
        hasher.addServer("Server0", 0);
        hasher.addServer("Server1", 1);
        hasher.addServer("Server2", 2);

        // Add some keys
        String[] keys = {"apple", "banana", "cherry", "date", "eggplant", "fig", "grape"};
        for (String key : keys) {
            hasher.add(key, "Value_of_" + key);
        }

        // Print server storage after adding keys
        System.out.println("=== Storage after adding keys ===");
        hasher.printValues();

        // Remove a key
        System.out.println("\nRemoving key 'cherry'");
        hasher.remove("cherry");

        // Add a new server
        System.out.println("\nAdding Server3");
        hasher.addServer("Server3", 3);

        // Print server storage after adding new server
        System.out.println("\n=== Storage after adding Server3 ===");
        hasher.printValues();

        // Remove a server
        System.out.println("\nRemoving Server2");
        hasher.removeServer(2, true);

        // Print storage after removal
        System.out.println("\n=== Storage after removing Server2 ===");
        hasher.printValues();
    }
}

/** Replication not implemented
 * If you want the nodes to look more “randomly scattered” (instead of clumped):

Increase virtual nodes per server

With 1–5 vnodes per server, clustering is very visible.

With 100–200 vnodes per server, the spread evens out (law of large numbers).

Use a strong hash function

MD5 is common (fast & evenly spread).

SHA-256 also works, but heavier.

Don’t use String.hashCode() — it’s too weak and biased.

Replication must pick distinct servers

Already discussed: when choosing replicas, skip duplicate servers so you don’t end up with Server0, Server0.

🔹 Single vs Double Hashing

In normal consistent hashing, you take the key (say "user123") → run one hash function (e.g. MD5, SHA-1) → get a 128-bit number → place it on the ring.

With virtual nodes, you hash "serverName#i" once for each i and place those vnodes.

But sometimes you notice clumping — some servers get unlucky and multiple of their vnodes land too close together.

🔹 Two-Time (Double) Hashing

There are two different ideas people mean by this:

1. Double Hash Function for Key Placement

Hash the same key twice with two independent hash functions (e.g. SHA-1, MD5).
Take the min (or choose based on some rule).

This is used in Rendezvous Hashing (HRW hashing) and Power of Two Choices load balancing.

It improves balance because the key doesn’t always go to the “first” vnode — it has a choice.

2. Generate Vnodes with Double Hashing

Instead of hash(server + "#" + i) for vnode i, you can generate vnodes like this:

H = hash(serverName)
VNode(i) = H + i * hash2(serverName)  (mod 2^m)


This ensures vnodes for a server are spread more evenly rather than clustering, because you’re not re-hashing arbitrary strings but stepping around the space with a second independent hash.
This is used in some distributed DBs (like DynamoDB, Cassandra variants) to reduce clumping.

🔹 Does it spread more evenly?

Yes, but…

Two hashing (with distinct functions) reduces clustering because collisions are less likely.

It doesn’t make it perfectly uniform, because hash outputs are still pseudorandom.

The real win comes from having more vnodes and skipping duplicate servers in replication.

Double hashing just reduces the chance that one server’s vnodes sit next to each other too often.

🔹 Practical Recommendation

If you only have a few vnodes (say 10), double hashing helps smooth them out.

If you have 100+ vnodes per server, one good hash (MD5/SHA-1) is enough; randomness already averages out.

If you want very even distribution per key, consider Rendezvous Hashing (HRW) instead of consistent hashing — it naturally balances by always picking the “highest hash” server, no ring traversal.
 */