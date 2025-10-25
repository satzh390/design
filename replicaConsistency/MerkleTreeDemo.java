package replicaConsistency;

import java.security.MessageDigest;
import java.util.*;

public class MerkleTreeDemo {

    // Utility: SHA-256 hashing
    private static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Node in Merkle Tree
    private static class MerkleNode {
        String hash;
        MerkleNode left, right;
        List<String> items; // Only for leaves

        MerkleNode(List<String> items) {
            this.items = new ArrayList<>(items);
            this.hash = sha256(String.join(",", items));
        }

        MerkleNode(MerkleNode left, MerkleNode right) {
            this.left = left;
            this.right = right;
            this.hash = sha256((left != null ? left.hash : "") + (right != null ? right.hash : ""));
        }

        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    // Merkle Tree
    private static class MerkleTree {
        MerkleNode root;
        int blockSize = 2; // items per leaf block

        MerkleTree(List<String> data) {
            Collections.sort(data); // ensure deterministic ordering
            this.root = buildTree(data);
        }

        private MerkleNode buildTree(List<String> items) {
            if (items.isEmpty()) return null;
            List<MerkleNode> leaves = new ArrayList<>();
            for (int i = 0; i < items.size(); i += blockSize) {
                List<String> block = items.subList(i, Math.min(i + blockSize, items.size()));
                leaves.add(new MerkleNode(block));
            }
            while (leaves.size() > 1) {
                List<MerkleNode> parents = new ArrayList<>();
                for (int i = 0; i < leaves.size(); i += 2) {
                    if (i + 1 < leaves.size()) {
                        parents.add(new MerkleNode(leaves.get(i), leaves.get(i + 1)));
                    } else {
                        parents.add(leaves.get(i));
                    }
                }
                leaves = parents;
            }
            return leaves.get(0);
        }

        String getRootHash() {
            return root != null ? root.hash : "";
        }
    }

    // Server class
    private static class Server {
        private String name;
        private Set<String> data;
        private MerkleTree merkleTree;

        public Server(String name) {
            this.name = name;
            this.data = new HashSet<>();
            rebuildTree();
        }

        public void addData(String item) {
            data.add(item);
            rebuildTree();
        }

        /**
         *  In practice, no DB builds a Merkle tree per row when you have millions or billions of rows. That would be too expensive. Instead:
            Data is partitioned into blocks or segments (e.g., ranges of IDs, pages, SSTables, or storage blocks).
            Each block gets a hash.
            Then the Merkle tree is built over these block hashes.
            This means:
            Adding or deleting a single row only affects the block containing that row.
            You only need to update hashes along the path from that block up to the root.
         */
        private void rebuildTree() {
            List<String> sortedData = new ArrayList<>(data);
            this.merkleTree = new MerkleTree(sortedData);
        }

        public String getRootHash() {
            return merkleTree.getRootHash();
        }

        // Compare and sync recursively
        public void sync(Server other) {
            syncNodes(this.merkleTree.root, other.merkleTree.root, other);
        }

        private void syncNodes(MerkleNode nodeA, MerkleNode nodeB, Server other) {
            if (nodeA == null || nodeB == null) return;

            if (nodeA.hash.equals(nodeB.hash)) return; // already in sync

            if (nodeA.isLeaf() && nodeB.isLeaf()) {
                // Only transfer missing items
                for (String item : nodeB.items) {
                    if (!data.contains(item)) {
                        System.out.println(name + " syncing item: " + item + " from " + other.name);
                        data.add(item);
                    }
                }
                rebuildTree(); // update hashes
            } else {
                // Recurse into children
                syncNodes(nodeA.left, nodeB.left, other);
                syncNodes(nodeA.right, nodeB.right, other);
            }
        }
    }

    // Demo
    public static void main(String[] args) {
        Server serverA = new Server("ServerA");
        Server serverB = new Server("ServerB");

        serverA.addData("apple");
        serverA.addData("banana");
        serverA.addData("date");
        serverB.addData("apple");
        serverB.addData("cherry");
        serverB.addData("date");

        System.out.println("Before sync:");
        System.out.println("ServerA root: " + serverA.getRootHash());
        System.out.println("ServerB root: " + serverB.getRootHash());

        serverA.sync(serverB);
        serverB.sync(serverA);

        System.out.println("After sync:");
        System.out.println("ServerA data: " + serverA.data);
        System.out.println("ServerB data: " + serverB.data);
    }
}
