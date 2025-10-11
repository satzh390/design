package IdGenerator;

import java.util.HashSet;
import java.util.Set;

public class TwitterSnowFlakeIdGenerator {
    /*
     * The idea is i need 
     * -> a system that generate ~10000 ids per second
     * -> id should be unique and sortable
     * -> id not necessarily needs to be a number
     * -> id should be ordered by time, id generated in morning should be less than id generated in evening
     * -> id should be 64bit length
     * -> should work in highly distributed environment, like server span across different data center
     */
    private final int machineId;
    private final int dataCenterId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    private static final long CUSTOM_EPOCH_START = 1735689600000L; // 2025-01-01 UTC in ms

    // Bits allocation
    private static final int SEQUENCE_BITS = 12;
    private static final int MACHINE_ID_BITS = 5;
    private static final int DATA_CENTER_BITS = 5;

    private static final int MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final int DATA_CENTER_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATA_CENTER_BITS;

    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1;
    private static final long MAX_DATA_CENTER_ID = (1L << DATA_CENTER_BITS) - 1;

    public TwitterSnowFlakeIdGenerator(int machineId, int dataCenterId){
        if (machineId < 0 || machineId > MAX_MACHINE_ID)
            throw new IllegalArgumentException("Machine ID must be 0-" + MAX_MACHINE_ID);
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID)
            throw new IllegalArgumentException("DataCenter ID must be 0-" + MAX_DATA_CENTER_ID);

        this.machineId = machineId;
        this.dataCenterId = dataCenterId;
    }

     /*
      * There are multiple id generator startegy but not fit our requirement
      * -> DB based unqiue incremental id generator, like one server has odd number and even number ids.(But still not highly scalable)
            https://code.flickr.net/2010/02/08/ticket-servers-distributed-unique-primary-keys-on-the-cheap/
      * -> UUID(Universal unique identifier) hash based unique id, the probablity of collision is very less, 
           like probablity of creating one duplicate id after generating 1 billion is 50%(128 bit, not numeric and won't go with time)
        -> twitter snow flake id, idea is to split 64 bits into different segment to stasify our requirement
            |---|-----------------------|-------------|------------|-----------------------|
            1bits   41bits                    5bits        5bits               12bits
            dummy  timestamp             datacenterId   machineId      sequence number

        timestamp is epoch time
        The maximum timestamp that can be represented in 41 bits is
        2 ^ 41 - 1 = 2199023255551 milliseconds (ms), which gives us: ~ 69 years = 2199023255551 ms / 1000 / 365 days / 24 hours/ 3600 seconds. 
        This means the ID generator will work for 69 years and having a custom epoch time close to today’s date delays the overflow time. 
        After 69 years, we will need a new epoch time or adopt other techniques to migrate IDs.

        Assuming all servers are time synchornized with NTP(Network time protocol), otherwise it will cause conflicts
      */
    public synchronized long generate() {
        long now = System.currentTimeMillis();

        if (now < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID.");
        }

        if (now == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence overflow, wait for next millisecond
                now = waitNextMillis(now);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = now;
        return ((now - CUSTOM_EPOCH_START) << TIMESTAMP_SHIFT)
                | ((long) dataCenterId << DATA_CENTER_SHIFT)
                | ((long) machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long currentMillis) {
        while (currentMillis <= lastTimestamp) {
            currentMillis = System.currentTimeMillis();
        }
        return currentMillis;
    }

    public static void main(String[] args) {
        TwitterSnowFlakeIdGenerator generator = new TwitterSnowFlakeIdGenerator(1, 1);

        Set<Long> generatedIds = new HashSet<>();
        long previousId = -1;

        // Generate 20 IDs
        for (int i = 0; i < 20; i++) {
            long id = generator.generate();
            System.out.println("Generated ID: " + id);

            // Check uniqueness
            if (generatedIds.contains(id)) {
                System.out.println("Duplicate ID found!");
            }
            generatedIds.add(id);

            // Check ordering
            if (previousId != -1 && id <= previousId) {
                System.out.println("IDs are not ordered!");
            }

            previousId = id;
        }

        System.out.println("All IDs are unique and ordered.");
    }
}
