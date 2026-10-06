import java.util.*;

/** Quorum replication: R + W > N guarantees reads see the latest write. Run: java -ea Quorum.java */
public class Quorum {
    record Versioned(int version, String value) {}

    static final class Cluster {
        final Versioned[] replicas;
        final int n, r, w;
        Cluster(int n, int r, int w) {
            this.n = n; this.r = r; this.w = w;
            replicas = new Versioned[n];
            Arrays.fill(replicas, new Versioned(0, "none"));
        }

        /** Write reaches only the chosen W replicas (the others are slow or partitioned). */
        void write(int version, String value, List<Integer> reachable) {
            if (reachable.size() < w) throw new IllegalStateException("write quorum not met");
            for (int i : reachable.subList(0, w)) replicas[i] = new Versioned(version, value);
        }

        /** Read asks R replicas and returns the highest version seen. */
        Versioned read(List<Integer> reachable) {
            if (reachable.size() < r) throw new IllegalStateException("read quorum not met");
            return reachable.subList(0, r).stream().map(i -> replicas[i]).max(Comparator.comparingInt(Versioned::version)).get();
        }
    }

    /** Try every write subset against every read subset; count stale reads. */
    static int staleReads(int n, int r, int w) {
        int stale = 0;
        for (int wm = 0; wm < (1 << n); wm++) {
            if (Integer.bitCount(wm) != w) continue;
            for (int rm = 0; rm < (1 << n); rm++) {
                if (Integer.bitCount(rm) != r) continue;
                Cluster c = new Cluster(n, r, w);
                c.write(1, "paid", members(wm, n));
                if (c.read(members(rm, n)).version() != 1) stale++;
            }
        }
        return stale;
    }

    static List<Integer> members(int mask, int n) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) if ((mask & (1 << i)) != 0) l.add(i);
        return l;
    }

    public static void main(String[] args) {
        int n = 5;
        for (int w = 1; w <= n; w++)
            for (int r = 1; r <= n; r++) {
                int stale = staleReads(n, r, w);
                boolean strong = r + w > n;
                if (r == 2 || r == 3) System.out.printf("N=%d R=%d W=%d R+W>N=%-5b stale read combos=%d%n", n, r, w, strong, stale);
                assert strong == (stale == 0) : "R+W>N iff no stale read is possible";
            }

        Cluster c = new Cluster(3, 2, 2);
        c.write(1, "paid", List.of(0, 1, 2));
        try { c.write(2, "shipped", List.of(0)); assert false; } catch (IllegalStateException expected) {
            System.out.println("write with 1 of 3 replicas rejected: " + expected.getMessage());
        }
        System.out.println("OK");
    }
}
