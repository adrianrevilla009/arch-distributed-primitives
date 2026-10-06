import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Consistent hash ring with virtual nodes and a rebalance demo. Run: java -ea ConsistentHashing.java */
public class ConsistentHashing {
    static final class Ring {
        private final TreeMap<Long, String> ring = new TreeMap<>();
        private final int vnodes;
        Ring(int vnodes) { this.vnodes = vnodes; }
        void add(String node) { for (int i = 0; i < vnodes; i++) ring.put(hash(node + "#" + i), node); }
        void remove(String node) { ring.values().removeIf(node::equals); }
        String lookup(String key) {
            Map.Entry<Long, String> e = ring.ceilingEntry(hash(key));
            return (e != null ? e : ring.firstEntry()).getValue();
        }
    }

    static long hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) h = (h << 8) | (d[i] & 0xff);
            return h;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    static Map<String, String> assign(Ring r, int keys) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < keys; i++) m.put("order-" + i, r.lookup("order-" + i));
        return m;
    }

    static double imbalance(Ring r, int keys) {
        Map<String, Integer> counts = new HashMap<>();
        assign(r, keys).values().forEach(n -> counts.merge(n, 1, Integer::sum));
        return (double) Collections.max(counts.values()) / (keys / (double) counts.size());
    }

    static double moved(Map<String, String> a, Map<String, String> b) {
        long n = a.entrySet().stream().filter(e -> !e.getValue().equals(b.get(e.getKey()))).count();
        return (double) n / a.size();
    }

    public static void main(String[] args) {
        int keys = 20_000;
        Ring one = new Ring(1), many = new Ring(200);
        for (String n : List.of("a", "b", "c", "d")) { one.add(n); many.add(n); }
        double i1 = imbalance(one, keys), i200 = imbalance(many, keys);
        System.out.printf("max/avg load: 1 vnode=%.2f, 200 vnodes=%.2f%n", i1, i200);
        assert i200 < i1 && i200 < 1.3 : "vnodes should smooth the load";

        Map<String, String> before = assign(many, keys);
        many.add("e");
        double addMoved = moved(before, assign(many, keys));
        System.out.printf("add 5th node: %.1f%% of keys moved (ideal 20%%)%n", addMoved * 100);
        assert addMoved > 0.10 && addMoved < 0.30;

        // modulo hashing for contrast: nearly everything moves
        long mod = 0;
        for (int i = 0; i < keys; i++) if (Math.floorMod(hash("order-" + i), 4) != Math.floorMod(hash("order-" + i), 5)) mod++;
        System.out.printf("hash %% N, 4 -> 5 nodes: %.1f%% moved%n", 100.0 * mod / keys);
        assert mod / (double) keys > 0.6;

        many.remove("e");
        assert assign(many, keys).equals(before) : "removing the node restores the old mapping";
        System.out.println("OK");
    }
}
