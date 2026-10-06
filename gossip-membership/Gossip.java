import java.util.*;

/** Gossip membership with heartbeat counters and failure detection. Run: java -ea Gossip.java */
public class Gossip {
    static final class Member {
        final int id;
        boolean alive = true;
        int heartbeat = 0;
        final Map<Integer, int[]> view = new HashMap<>();   // peer -> {heartbeat, lastUpdatedRound}
        Member(int id, int n) { this.id = id; for (int i = 0; i < n; i++) view.put(i, new int[] {0, 0}); }
    }

    final List<Member> members = new ArrayList<>();
    final Random rnd = new Random(42);
    int round = 0;

    Gossip(int n) { for (int i = 0; i < n; i++) members.add(new Member(i, n)); }

    /** One round: every live member bumps its heartbeat and pushes its view to 2 random peers. */
    void step() {
        round++;
        for (Member m : members) if (m.alive) { m.heartbeat++; m.view.put(m.id, new int[] {m.heartbeat, round}); }
        for (Member m : members) {
            if (!m.alive) continue;
            for (int i = 0; i < 2; i++) {
                Member peer = members.get(rnd.nextInt(members.size()));
                if (peer == m || !peer.alive) continue;
                m.view.forEach((id, e) -> peer.view.merge(id, new int[] {e[0], round},
                    (old, in) -> in[0] > old[0] ? in : old));
            }
        }
    }

    boolean suspects(Member observer, int target, int timeoutRounds) {
        return round - observer.view.get(target)[1] > timeoutRounds;
    }

    public static void main(String[] args) {
        int n = 32;
        Gossip g = new Gossip(n);
        g.members.get(0).heartbeat = 1;
        g.members.get(0).view.put(0, new int[] {1, 0});
        int rounds = 0;
        while (g.members.stream().anyMatch(m -> m.view.get(0)[0] < 1) && rounds < 100) { g.step(); rounds++; }
        System.out.printf("n=%d: everyone learned node 0 exists after %d rounds (log2 n = %d)%n", n, rounds, 5);
        assert rounds <= 15 : "epidemic spread is O(log n) rounds";

        for (int i = 0; i < 10; i++) g.step();
        g.members.get(7).alive = false;                       // crash
        for (int i = 0; i < 30; i++) g.step();
        long detected = g.members.stream().filter(m -> m.alive && g.suspects(m, 7, 10)).count();
        long falsePos = g.members.stream().filter(m -> m.alive && g.suspects(m, 3, 10)).count();
        System.out.printf("after crash of node 7: %d/%d live nodes suspect it, %d suspect healthy node 3%n", detected, n - 1, falsePos);
        assert detected == n - 1 : "every live node eventually suspects the crashed one";
        assert falsePos == 0;
        System.out.println("OK");
    }
}
