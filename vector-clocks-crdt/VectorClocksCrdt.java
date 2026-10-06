import java.util.*;

/** Vector clocks detect concurrency; a G-Counter CRDT converges without coordination. Run: java -ea VectorClocksCrdt.java */
public class VectorClocksCrdt {
    enum Order { BEFORE, AFTER, EQUAL, CONCURRENT }

    record VectorClock(Map<String, Integer> t) {
        VectorClock tick(String node) {
            Map<String, Integer> m = new TreeMap<>(t);
            m.merge(node, 1, Integer::sum);
            return new VectorClock(m);
        }
        VectorClock merge(VectorClock o) {
            Map<String, Integer> m = new TreeMap<>(t);
            o.t.forEach((k, v) -> m.merge(k, v, Math::max));
            return new VectorClock(m);
        }
        Order compare(VectorClock o) {
            boolean less = false, greater = false;
            Set<String> keys = new TreeSet<>(t.keySet());
            keys.addAll(o.t.keySet());
            for (String k : keys) {
                int a = t.getOrDefault(k, 0), b = o.t.getOrDefault(k, 0);
                if (a < b) less = true;
                if (a > b) greater = true;
            }
            return less && greater ? Order.CONCURRENT : less ? Order.BEFORE : greater ? Order.AFTER : Order.EQUAL;
        }
    }

    /** Grow-only counter: each node increments its own slot; merge = element-wise max. */
    static final class GCounter {
        final Map<String, Long> slots = new TreeMap<>();
        void increment(String node) { slots.merge(node, 1L, Long::sum); }
        long value() { return slots.values().stream().mapToLong(Long::longValue).sum(); }
        void merge(GCounter o) { o.slots.forEach((k, v) -> slots.merge(k, v, Math::max)); }
    }

    public static void main(String[] args) {
        VectorClock empty = new VectorClock(Map.of());
        VectorClock a1 = empty.tick("A");
        VectorClock a2 = a1.tick("A");
        VectorClock b1 = a1.tick("B");          // B saw A's first event
        VectorClock c1 = a1.tick("C");          // C also saw it, independently of B
        assert a1.compare(a2) == Order.BEFORE;
        assert a2.compare(a1) == Order.AFTER;
        assert a2.compare(b1) == Order.CONCURRENT : "A's 2nd and B's 1st event are concurrent";
        assert b1.compare(c1) == Order.CONCURRENT;
        VectorClock merged = b1.merge(c1).tick("A");
        assert merged.compare(b1) == Order.AFTER && merged.compare(c1) == Order.AFTER;
        System.out.println("concurrent updates detected: " + a2.t() + " || " + b1.t());

        GCounter x = new GCounter(), y = new GCounter(), z = new GCounter();
        for (int i = 0; i < 3; i++) x.increment("x");
        for (int i = 0; i < 5; i++) y.increment("y");
        z.increment("z");
        // merge in different orders, with duplicates: idempotent, commutative, associative
        GCounter r1 = new GCounter(), r2 = new GCounter();
        for (GCounter g : List.of(x, y, z, y)) r1.merge(g);
        for (GCounter g : List.of(z, x, x, y)) r2.merge(g);
        System.out.println("replica totals: " + r1.value() + " and " + r2.value());
        assert r1.value() == 9 && r2.value() == 9 && r1.slots.equals(r2.slots);
        System.out.println("OK");
    }
}
