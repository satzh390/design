package replicaConsistency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class VectorClock {
    public static class Version {
        public int serverId;
        public int count;

        public Version(int serverId, int count){
            this.serverId = serverId;
            this.count = count;
        }

        @Override
        public String toString() {
            return String.format("[S%d:%d]", serverId, count);
        }
    }

    public static class Data {
        public String data;
        public LinkedList<Version> versions;

        public Data(String data, LinkedList<Version> versions){
            this.data = data;
            this.versions = versions;
        }

        @Override
        public String toString() {
            return data + " " + versions;
        }
    }
        

    public static class Node {
        public Map<Integer, LinkedList<Data>> store = new HashMap<>();
        public int serverId;

        public Node(int serverId){
            this.serverId = serverId;
        }

        private LinkedList<Data> deepCopyList(LinkedList<Data> src) {
            LinkedList<Data> dst = new LinkedList<>();
            for (Data d : src) {
                LinkedList<Version> vc = new LinkedList<>();
                for (Version v : d.versions) 
                    vc.add(new Version(v.serverId, v.count));
                dst.add(new Data(d.data, vc));
            }
            return dst;
        }

        // Implementing only the vector clock logic to detect conflicts.
        // Assume that synchronization is triggered due to an inconsistency detected by an algorithm like a Merkle tree.
        //
        // In a consistent hashing setup, suppose x...y is the key range owned by S1,
        // and its replicas are the next two servers (since replica count = 3), say S3 and S4.
        // In this case, we only need to compare the range x...y in replicas S3 and S4,
        // because S3 and S4 each hold only partial data.
        //
        // For example, S3 might cover a3..x..y..b3 and S4 might cover a4..x..y..b4.
        // Therefore, we should send the specific range (x...y) that needs synchronization,
        // and each replica should navigate to the corresponding subtree node for that range and start comparing from there.    
        public void sync(Map<Integer, LinkedList<Data>> otherStore){
            for(Entry<Integer, LinkedList<Data>> entry: otherStore.entrySet()){
                int key = entry.getKey();
                if(!store.containsKey(key)){
                    store.put(key, deepCopyList(entry.getValue()));
                    continue;
                }

                store.put(key, compareAndBuild(store.get(key), deepCopyList(entry.getValue())));
            }
        }

        private LinkedList<Data> compareAndBuild(LinkedList<Data> selfList, LinkedList<Data> otherList){
            LinkedList<Data> merged = new LinkedList<>();
            Data self = selfList.getLast();
            Data other = otherList.getLast();

            if (isAncestor(self.versions, other.versions)) {
                // other is newer
                merged.add(other);
            } else if (isAncestor(other.versions, self.versions)) {
                // self is newer
                merged.add(self);
            } else {
                // conflict detected
                merged.add(self);
                merged.add(other);
                System.out.println("Conflict detected between versions: " + self + " and " + other);
            }

            return merged;
        }

        private boolean isAncestor(List<Version> v1, List<Version> v2) {
            Map<Integer, Integer> m1 = new HashMap<>();
            for (Version v : v1) m1.put(v.serverId, v.count);

            Map<Integer, Integer> m2 = new HashMap<>();
            for (Version v : v2) m2.put(v.serverId, v.count);

            boolean atLeastOneSmaller = false;

            // union of all server ids
            Set<Integer> allServers = new HashSet<>();
            allServers.addAll(m1.keySet());
            allServers.addAll(m2.keySet());

            for (Integer s : allServers) {
                int c1 = m1.getOrDefault(s, 0);
                int c2 = m2.getOrDefault(s, 0);

                if (c1 > c2) return false;            // v1 cannot be ancestor
                if (c1 < c2) atLeastOneSmaller = true;
            }

            return atLeastOneSmaller; // true only if strictly smaller in at least one server
        }

        public void add(int key, String value){
            if(!store.containsKey(key)){
                LinkedList<Version> versions = new LinkedList<>();
                LinkedList<Data> data = new LinkedList<>();
                versions.add(new Version(serverId, 1));
                data.add(new Data(value, versions));
                store.put(key, data);
                return;
            }

            LinkedList<Data> entries = store.get(key);
            // for simplicity, if we have more than one entry that means conflict and we are not allowing to update until resolve the conflict
            if(entries.size() > 1){
                throw new IllegalStateException(String.format("Please resolve conflict before update data with key %s", key));
            }

            Data entry = entries.removeFirst();
            entry.data = value;
            Version lastVersion = entry.versions.getFirst();
            if(lastVersion.serverId == serverId){
                lastVersion.count++; // if last version is from same server update the counter
            }else {
                // if not same server, create a new entry with lastCount + 1
                entry.versions.addFirst(new Version(serverId, lastVersion.count + 1));
            }
            entries.add(entry);
        }

        public void printStore() {
            System.out.println("Node " + serverId + " store:");
            for (Map.Entry<Integer, LinkedList<Data>> e : store.entrySet()) {
                System.out.println("  Key " + e.getKey() + " -> " + e.getValue());
            }
        }
    }

    public static void main(String[] args) {
        Node s1 = new Node(1);
        Node s2 = new Node(2);
        Node s3 = new Node(3);

        System.out.println("=== SCENARIO 1: Sequential updates (no conflict) ===");
        s1.add(10, "A1");
        s2.sync(s1.store); // s2 pulls s1’s data
        s2.add(10, "A2");  // s2 updates after syncing from s1 (ancestor)
        s1.sync(s2.store); // s1 syncs back (should merge cleanly, no conflict)
        s1.printStore();

        System.out.println("\n=== SCENARIO 2: Concurrent updates (conflict expected) ===");
        s1 = new Node(1);
        s2 = new Node(2);
        s1.add(20, "B1");
        s2.add(20, "B2");  // same key, updated independently → conflict expected

        System.out.println("\nBefore Sync:");
        s1.printStore();
        s2.printStore();

        s1.sync(s2.store);

        System.out.println("\nAfter Sync:");
        s1.printStore();

        System.out.println("\n=== SCENARIO 3: Conflict propagation across replicas ===");
        s3.sync(s1.store); // s3 joins and syncs with s1 (inherits conflict)
        s3.printStore();

        System.out.println("\n=== SCENARIO 4: One replica resolves and updates ===");
        // Let’s say client resolves conflict at s3 (chooses merged value “B3”)
        LinkedList<Data> conflictEntries = s3.store.get(20);
        if (conflictEntries.size() > 1) {
            // manual resolution
            LinkedList<Version> mergedVC = new LinkedList<>(conflictEntries.get(0).versions);
            mergedVC.addAll(conflictEntries.get(1).versions);
            Data resolved = new Data("B3", mergedVC);
            LinkedList<Data> resolvedList = new LinkedList<>();
            resolvedList.add(resolved);
            s3.store.put(20, resolvedList);
            System.out.println("Conflict resolved at S3: merged as 'B3'");
        }

        s3.add(20, "B4"); // S3 updates after resolution

        System.out.println("\nAfter Resolution + Update at S3:");
        s3.printStore();

        System.out.println("\n=== SCENARIO 5: S3 syncs with S1 and S2 (should converge) ===");
        s1.sync(s3.store);
        s2.sync(s3.store);

        System.out.println("\nAfter Final Convergence:");
        s1.printStore();
        s2.printStore();
        s3.printStore();
    }
}
