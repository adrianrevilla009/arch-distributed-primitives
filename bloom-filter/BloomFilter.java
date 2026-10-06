import java.util.BitSet;

/** Bloom filter: measured vs. theoretical false-positive rate. Run: java -ea BloomFilter.java */
public class BloomFilter {
    private final BitSet bits;
    private final int m, k;

    BloomFilter(int expected, double fpp) {
        this.m = (int) Math.ceil(-expected * Math.log(fpp) / (Math.log(2) * Math.log(2)));
        this.k = Math.max(1, (int) Math.round((double) m / expected * Math.log(2)));
        this.bits = new BitSet(m);
    }

    // double hashing: h1 + i*h2 simulates k independent hashes
    private int index(String s, int i) {
        int h1 = s.hashCode() * 0x9E3779B1, h2 = Integer.rotateLeft(s.hashCode(), 16) * 0x85EBCA6B | 1;
        return Math.floorMod(h1 + i * h2, m);
    }

    void add(String s) { for (int i = 0; i < k; i++) bits.set(index(s, i)); }

    boolean mightContain(String s) {
        for (int i = 0; i < k; i++) if (!bits.get(index(s, i))) return false;
        return true;
    }

    static double measure(int n, double target) {
        BloomFilter f = new BloomFilter(n, target);
        for (int i = 0; i < n; i++) f.add("order-" + i);
        for (int i = 0; i < n; i++) assert f.mightContain("order-" + i) : "no false negatives";
        int fp = 0, probes = 100_000;
        for (int i = 0; i < probes; i++) if (f.mightContain("missing-" + i)) fp++;
        double rate = (double) fp / probes;
        System.out.printf("n=%d target=%.3f m=%d bits k=%d measured=%.4f%n", n, target, f.m, f.k, rate);
        return rate;
    }

    public static void main(String[] args) {
        double r1 = measure(10_000, 0.01);
        double r2 = measure(10_000, 0.001);
        assert r1 < 0.02 : "close to the 1% target";
        assert r2 < 0.003 : "close to the 0.1% target";
        assert r2 < r1 : "more bits per key, fewer false positives";

        // overfill: 10x more keys than sized for -> filter saturates
        BloomFilter small = new BloomFilter(1_000, 0.01);
        for (int i = 0; i < 10_000; i++) small.add("order-" + i);
        int fp = 0;
        for (int i = 0; i < 10_000; i++) if (small.mightContain("missing-" + i)) fp++;
        System.out.printf("overfilled 10x: measured fpp=%.3f%n", fp / 10_000.0);
        assert fp / 10_000.0 > 0.5;
        System.out.println("OK");
    }
}
