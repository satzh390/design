package LLD.LRU;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Lru {

    public static class Node {
        int key;
        int value;
        Node prev;
        Node next;

        public Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    private int capacity;
    private Node head = new Node(0, 0);
    private Node tail = new Node(0, 0);
    private final ReentrantLock lock = new ReentrantLock();
    private Map<Integer, Node> cache = new ConcurrentHashMap<>();

    public Lru(int capacity) {
        if(capacity <= 0){
            throw new IllegalArgumentException("Capacity can not be negative or zero");
        }

        this.capacity = capacity; 
        head.next = tail;
        tail.prev = head;
    }

    public Integer get(int key){
        try {
            lock.lock();
            if(!cache.containsKey(key)){
                return null;
            }

            Node n = cache.get(key);
            remove(n);
            insert(n);
            return n.value;
        } finally {
            lock.unlock();
        }
    }

    public int put(int key, int value){
       try {
            lock.lock(); // simple lock to make it thread safe
            Node n = new Node(key, value);
            if(!cache.containsKey(key)){
                if(cache.size() == capacity){
                    remove(tail.prev);
                }
            } else {
                n = cache.get(key);
                n.value = value;
                remove(n);
            }
        
            insert(n);
            cache.put(key, n);  
            return n.value;
       } finally {
            lock.unlock();
       }
    }
    
    private void remove(Node node){
        Node prev = node.prev;
        Node nxt = node.next;

        node.prev.next = nxt;
        nxt.prev = prev;
        node.prev = null;
        node.next = null;
    }

    private void insert(Node node){
        Node first = head.next;

        first.prev = node;
        node.next = first;
        head.next = node;
        node.prev = head;
    }
}
