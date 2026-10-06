import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Fast producer, slow consumer: unbounded queue vs. bounded queue with backpressure. Run: java -ea Backpressure.java */
public class Backpressure {
    static final int ITEMS = 200;

    /** Returns peak queue depth. A bounded queue blocks the producer in put(); unbounded never does. */
    static int run(BlockingQueue<Integer> q) throws Exception {
        AtomicInteger peak = new AtomicInteger();
        Thread consumer = Thread.ofPlatform().start(() -> {
            try {
                for (int i = 0; i < ITEMS; i++) { q.take(); Thread.sleep(2); }   // slow work
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        for (int i = 0; i < ITEMS; i++) {
            q.put(i);                                                            // blocks when full
            peak.accumulateAndGet(q.size(), Math::max);
        }
        consumer.join();
        return peak.get();
    }

    /** Load shedding alternative: reject instead of block. */
    static int shed(int capacity) {
        BlockingQueue<Integer> q = new ArrayBlockingQueue<>(capacity);
        int rejected = 0;
        for (int i = 0; i < ITEMS; i++) if (!q.offer(i)) rejected++;           // no consumer: overload
        return rejected;
    }

    public static void main(String[] args) throws Exception {
        int unbounded = run(new LinkedBlockingQueue<>());
        int bounded = run(new ArrayBlockingQueue<>(10));
        System.out.printf("peak depth: unbounded=%d bounded(10)=%d%n", unbounded, bounded);
        assert bounded <= 10 : "bounded queue caps memory";
        assert unbounded > 50 : "unbounded queue absorbs the whole burst";

        int rejected = shed(10);
        System.out.printf("load shedding with capacity 10: %d of %d rejected%n", rejected, ITEMS);
        assert rejected == ITEMS - 10;
        System.out.println("OK");
    }
}
