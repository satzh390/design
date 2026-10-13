package failureDetection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GossipProtocol {
    public static class Membership {
        private int id;
        private long heartBeatCount;
        private long lastHeartBeatAtEpoch;
        private Set<Integer> isDownAckNodes = new HashSet<>();
    }

    public static class Node {
        private final String name;
        private final int id;
        private final List<Membership> memberships = new ArrayList<>();
        private final Network network;
        private final ScheduledExecutorService heartBeatExecutor = Executors.newScheduledThreadPool(1); 
        private final ScheduledExecutorService gossipExecutor = Executors.newScheduledThreadPool(1);
        private final ConcurrentHashMap<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
        private final int noHeartBeatWaitOffset;

        public Node(String name, int id, Network network, int noHeartBeatWaitOffset){
            this.name = name;
            this.id = id;
            this.noHeartBeatWaitOffset = noHeartBeatWaitOffset;
            this.network = network;
            initHeartBeatExec();
        }

        private void initHeartBeatExec(){
            Runnable heartBeatRunnable = () -> {
                updateSelfHeartBeat();
                randomNodes().forEach(destId -> network.sendHeartBeat(memberships, id, destId));
            };
            heartBeatExecutor.scheduleAtFixedRate(heartBeatRunnable, 0, 1, TimeUnit.SECONDS);
        }

        private void updateSelfHeartBeat(){
            Optional<Membership> selfOpt = memberships.stream()
                .filter(m -> m.id == this.id)
                .findAny();
            if (selfOpt.isPresent()) {
                Membership self = selfOpt.get();
                self.heartBeatCount++;
                self.lastHeartBeatAtEpoch = System.currentTimeMillis();
                return;
            } 

            Membership newM = new Membership();
            newM.id = this.id;
            newM.heartBeatCount = 1;
            newM.lastHeartBeatAtEpoch = System.currentTimeMillis();
            memberships.add(newM);
        }

        private void updateGossipScheduledTask(int nodeId){
            Optional<Membership> nodeMembershipOptional = this.memberships.stream().filter(n -> n.id == nodeId).findAny();
            if(nodeMembershipOptional.isEmpty()){
                return;
            }

            ScheduledFuture<?> old = timers.remove(nodeId);
            if (old != null) old.cancel(false);

            // Schedule a new timeout
            ScheduledFuture<?> future = gossipExecutor.schedule(() -> {
                randomNodes().forEach(randomNode -> network.sendGossip(memberships, id, nodeId, randomNode, false));
            }, noHeartBeatWaitOffset, TimeUnit.MILLISECONDS);
            timers.put(nodeId, future);
        }

        public void heartBeat(List<Membership> memberships, int senderId){
            this.memberships.forEach(m -> updateMemberships(m));
        }

        private void updateMembershipFromGossip(List<Membership> memberships, int senderId, int suspectedNodeId){
            memberships.forEach(m -> updateMemberships(m));
            Optional<Membership> suspectedOptional = this.memberships.stream().filter(n -> n.id == suspectedNodeId).findAny();
            Membership suspectedNodeMembership = suspectedOptional.get();
            long curTime = System.currentTimeMillis();
            if((curTime - suspectedNodeMembership.lastHeartBeatAtEpoch) > noHeartBeatWaitOffset){
                suspectedNodeMembership.isDownAckNodes.add(senderId);
            }
        }
        
        public void gossip(List<Membership> memberships, int senderId, int suspectedNodeId, boolean isReply){
            updateMembershipFromGossip(memberships, senderId, suspectedNodeId);
            if(!isReply){
                network.sendGossip(this.memberships, id, suspectedNodeId, senderId, true);
            }
        }

        private void updateMemberships(Membership membership){
            Optional<Membership> curMembershipOptional = memberships.stream().filter(n -> n.id == membership.id).findAny();
            if(curMembershipOptional.isEmpty()){
                memberships.add(membership);
                return;
            }

            Membership curMembership = curMembershipOptional.get();
            if(curMembership.heartBeatCount > membership.heartBeatCount){
               return;
            }

            // update count and heartBeatEpoch, Assuming all nodes are with proper time sync using NTP(Network time protocol) with ~1-10ms delta
            curMembership.heartBeatCount = Math.max(membership.heartBeatCount, curMembership.heartBeatCount);
            curMembership.lastHeartBeatAtEpoch = Math.max(membership.lastHeartBeatAtEpoch, curMembership.lastHeartBeatAtEpoch);
            updateGossipScheduledTask(membership.id);
        }

        private List<Integer> randomNodes(){
            List<Integer> nodeIds = network.nodes.stream().filter(n -> n.id != id).map(n -> n.id).collect(Collectors.toList());
            if (nodeIds.isEmpty()) return List.of();

            int total = nodeIds.size();
            // choose half of the nodes (or at least 1), at worst case suspected nodes could have send to remaining n/2 nodes, however it will also improve convergence
            int gossipCount = Math.max(1, total / 2);
            Collections.shuffle(nodeIds);
            return nodeIds.stream()
                .limit(gossipCount)
                .collect(Collectors.toList());
        }

        public boolean isNodeDown(int nodeId){
            int networkNodeSize = network.nodes.size();
            Optional<Membership> nodeMembership = memberships.stream().filter(n -> n.id == nodeId).findAny();
            if(nodeMembership.isEmpty()){
                throw new IllegalArgumentException(String.format("No node with id %d is found in membership list", nodeId));
            }

            return nodeMembership.get().isDownAckNodes.size() >= (networkNodeSize / 2);
        }
    }

    public static class Network {
        private final List<Node> nodes;
        /**
         * In Real environment, we can config like below and seed to all nodes
         * Node1 127.0.0.x 8088 id1
         * Node2 127.0.0.x 8088 id2
         * with above each node is aware of other nodes in network or Each node starts with a few known “seed” IPs. Once connected, it learns others via gossip.
         * other ways are service discovery(require one node to maintain registry), Cloud metadata API etc
         */
        public Network(List<Node> nodes){
            this.nodes = nodes;
        }

        public void sendHeartBeat(List<Membership> memberships, int senderId, int destId){
            Optional<Node> destNodeOptional = nodes.stream().filter(n -> n.id == destId).findAny();
            if(destNodeOptional.isEmpty()){
                System.out.println(String.format("Node %d is not found in network", destId));
                return;
            }

            destNodeOptional.get().heartBeat(memberships, senderId); 
            // directly sending to node, real env it would be something tcp or other type of connection
        }

        public void sendGossip(List<Membership> memberships, int senderId, int suspectedId, int destId, boolean isReply){
            Optional<Node> destNodeOptional = nodes.stream().filter(n -> n.id == destId).findAny();
            if(destNodeOptional.isEmpty()){
                System.out.println(String.format("Node %d is not found in network", destId));
                return;
            }

            destNodeOptional.get().gossip(memberships, senderId, suspectedId, isReply); 
        }
    }

    public static void main(String[] args) throws Exception {
        int nodeCount = 4;
        int noHeartBeatWaitOffset = 3000; // 3s timeout

        // Create network first (empty list, will be filled later)
        List<Node> nodeList = new ArrayList<>();
        Network network = new Network(nodeList);

        // Create nodes
        for (int i = 0; i < nodeCount; i++) {
            Node node = new Node("Node" + i, i, network, noHeartBeatWaitOffset);
            nodeList.add(node);
        }

        // Initialize each node’s membership table (everyone knows everyone)
        long now = System.currentTimeMillis();
        for (Node node : nodeList) {
            for (int i = 0; i < nodeCount; i++) {
                Membership m = new Membership();
                m.id = i;
                m.heartBeatCount = 0;
                m.lastHeartBeatAtEpoch = now;
                node.memberships.add(m);
            }
        }

        System.out.println("System warming up...");
        TimeUnit.SECONDS.sleep(10);

        // Simulate failure of Node 2
        System.out.println("\n>>> Simulating Node2 failure (stopping heartbeats) <<<");
        nodeList.get(2).heartBeatExecutor.shutdownNow();

        // Let gossip detection happen
        TimeUnit.SECONDS.sleep(15);

        // Check who detected Node2 as down
        for (Node node : nodeList) {
            if (node.id != 2) {
                boolean isDown = node.isNodeDown(2);
                System.out.printf("Node%d view: Node2 is %s%n", node.id, isDown ? "DOWN" : "UP");
            }
        }

        // Clean up executors
        for (Node node : nodeList) {
            node.heartBeatExecutor.shutdownNow();
            node.gossipExecutor.shutdownNow();
        }

        System.out.println("\nTest complete.");
    }
}
