import java.util.*;
import java.util.function.ToIntFunction;

/** Hash vs. range vs. directory sharding: skew and hot keys. Run: java -ea Sharding.java */
public class Sharding {
    static final int SHARDS = 4;

    static double skew(List<String> keys, ToIntFunction<String> shardOf) {
        int[] load = new int[SHARDS];
        for (String k : keys) load[shardOf.applyAsInt(k)]++;
        return (double) Arrays.stream(load).max().getAsInt() / (keys.size() / (double) SHARDS);
    }

    static int hashShard(String k) { return Math.floorMod(k.hashCode(), SHARDS); }

    /** Range sharding on the key's lexical position (a-f, g-m, n-s, t-z). */
    static int rangeShard(String k) { return Math.min(SHARDS - 1, Math.max(0, (k.charAt(0) - 'a') * SHARDS / 26)); }

    public static void main(String[] args) {
        // order ids are sequential timestamps -> all recent writes share a prefix
        List<String> sequential = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) sequential.add("2026-10-03T" + String.format("%05d", i));
        List<String> byCustomer = new ArrayList<>();
        Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) byCustomer.add("" + (char) ('a' + r.nextInt(26)) + i);
        List<String> skewedNames = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) skewedNames.add((i % 10 < 6 ? "s" : "" + (char) ('a' + r.nextInt(26))) + i);

        // range sharding by time: every new key lands on the same shard
        ToIntFunction<String> timeRange = k -> Math.min(SHARDS - 1, Integer.parseInt(k.substring(11)) * SHARDS / 10_000);
        double seqRange = skew(sequential.subList(9_000, 10_000), timeRange);
        double seqHash = skew(sequential, Sharding::hashShard);
        System.out.printf("recent writes, range-by-time skew=%.2f vs hash skew=%.2f%n", seqRange, seqHash);
        assert seqRange == SHARDS && seqHash < 1.1 : "time-ranged shards create a hot tail shard";

        double uniformRange = skew(byCustomer, Sharding::rangeShard);
        double skewedRange = skew(skewedNames, Sharding::rangeShard);
        System.out.printf("range by first letter: uniform=%.2f skewed names=%.2f%n", uniformRange, skewedRange);
        assert uniformRange < 1.3 && skewedRange > 2.0;

        // directory (lookup table): explicit placement lets you move one hot tenant without rehashing
        Map<String, Integer> directory = new HashMap<>();
        for (int i = 0; i < 8; i++) directory.put("tenant-" + i, i % SHARDS);
        directory.put("tenant-0", 3);
        assert directory.get("tenant-0") == 3 && directory.get("tenant-1") == 1;
        System.out.println("directory moved tenant-0 to shard 3 without touching other tenants");
        System.out.println("OK");
    }
}
