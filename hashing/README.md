# Hashing & Consistent Hashing Designs

This folder collects my designs and implementations around **hashing**, especially *consistent hashing* and related techniques. The goal is to illustrate both the theory and practical code, with clean structure and clear explanations.

## 📂 Contents

| File / Component | Purpose |
|------------------|---------|
| `ConsistentHasher.java` | A Java implementation of a consistent hashing ring with virtual nodes, add/remove server, key distribution, and migrations. |
| (Optional) Rendezvous hashing code | Placeholder or future implementation to compare with consistent hashing. |
| Test / demo classes | Sample `main(...)` usage showing how virtual nodes, data movement, and ring state evolve. |

## 🧠 How It Works: Consistent Hashing Overview

1. **Virtual Nodes**  
   Each real server is represented by many “vnode” positions on the hash ring. This reduces clustering and balances load more evenly.

2. **Key Placement**  
   Compute `hash(key)`, then find the next vnode clockwise on the ring. The real server associated with that vnode holds the key.

3. **Adding Servers**  
   When you insert a new server (with its vnodes), only a **small range** of keys migrates from the previous server to the new server — not all keys.

4. **Removing Servers**  
   When removing a server, you remove all its vnodes and reassign its keys to the next servers in the ring.

5. **Edge Cases**  
   - Wrap-around: if `hash(key)` is greater than any vnode, it wraps to the first vnode in the ring.  
   - Ensuring no two vnodes collide.  
   - Handling no servers / last-server removal.

## 📈 How to Use

1. Add servers by name and id.  
2. Insert key–value pairs via `add(key, value)`.  
3. Remove keys or servers as needed.  
4. Observe storage distribution and migrations via the demo `main()`.

## Other concepts

1. **Coordinator node**
   Reference to Cassandra: This refers to the node that a client connects to — it can be any node within the cluster. If the connected (coordinator) node is responsible for the key, it writes the data locally and forwards it to the other replicas. Based on the configured consistency level, the coordinator sends a response to the client once the required number of acknowledgments have been received.


