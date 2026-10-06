import java.util.*;

/** Tiny deterministic Raft simulation: leader election and log replication. Run: java -ea RaftDemo.java */
public class RaftDemo {
    record Entry(int term, String cmd) {}

    static final class Node {
        final int id;
        int term = 0;
        Integer votedFor = null;
        boolean up = true;
        final List<Entry> log = new ArrayList<>();
        int commit = 0;
        Node(int id) { this.id = id; }

        int lastTerm() { return log.isEmpty() ? 0 : log.get(log.size() - 1).term(); }

        boolean requestVote(int candTerm, int cand, int candLastTerm, int candLen) {
            if (candTerm < term) return false;
            if (candTerm > term) { term = candTerm; votedFor = null; }
            boolean upToDate = candLastTerm > lastTerm() || (candLastTerm == lastTerm() && candLen >= log.size());
            if (upToDate && (votedFor == null || votedFor == cand)) { votedFor = cand; return true; }
            return false;
        }

        /** Simplified AppendEntries: follower adopts the leader's log if it is not behind in term. */
        boolean append(int leaderTerm, List<Entry> leaderLog, int leaderCommit) {
            if (leaderTerm < term) return false;
            term = leaderTerm;
            log.clear(); log.addAll(leaderLog);
            commit = Math.min(leaderCommit, log.size());
            return true;
        }
    }

    final List<Node> nodes = new ArrayList<>();
    RaftDemo(int n) { for (int i = 0; i < n; i++) nodes.add(new Node(i)); }
    int majority() { return nodes.size() / 2 + 1; }

    /** Candidate bumps its term and asks everyone; returns true if it won. */
    boolean elect(int id) {
        Node c = nodes.get(id);
        c.term++; c.votedFor = id;
        int votes = 1;
        for (Node n : nodes)
            if (n != c && n.up && n.requestVote(c.term, id, c.lastTerm(), c.log.size())) votes++;
        return votes >= majority();
    }

    /** Leader appends, replicates, and commits only once a majority holds the entry. */
    boolean replicate(int leader, String cmd) {
        Node l = nodes.get(leader);
        l.log.add(new Entry(l.term, cmd));
        int acks = 1;
        for (Node n : nodes) if (n != l && n.up && n.append(l.term, l.log, l.commit)) acks++;
        if (acks >= majority()) {
            l.commit = l.log.size();
            for (Node n : nodes) if (n != l && n.up) n.append(l.term, l.log, l.commit);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        RaftDemo c = new RaftDemo(5);
        assert c.elect(0) : "node 0 wins with a full quorum";
        assert c.replicate(0, "create order-1") && c.replicate(0, "pay order-1");
        System.out.println("term " + c.nodes.get(0).term + ", committed " + c.nodes.get(0).commit + " entries on all nodes");

        c.nodes.get(0).up = false;                       // leader crashes
        assert c.elect(1) : "node 1 wins in term 2 with 4 of 5 alive";
        assert c.replicate(1, "ship order-1");
        assert c.nodes.get(1).term == 2 && c.nodes.get(2).log.size() == 3;

        c.nodes.get(0).up = true;                        // old leader rejoins, catches up
        c.replicate(1, "close order-1");
        assert c.nodes.get(0).log.equals(c.nodes.get(1).log) : "old leader's log converges";

        for (int i = 2; i < 5; i++) c.nodes.get(i).up = false;   // lose the majority
        assert !c.replicate(1, "lost write") : "no commit without majority";
        assert c.nodes.get(1).commit == 4 : "uncommitted entry must not advance commit";
        System.out.println("minority partition cannot commit; commit index stays " + c.nodes.get(1).commit);

        RaftDemo s = new RaftDemo(3);                    // stale candidate cannot win
        s.elect(0); s.replicate(0, "a"); s.replicate(0, "b");
        s.nodes.get(2).log.remove(1);                    // node 2 is behind
        Node behind = s.nodes.get(2);
        assert !s.nodes.get(1).requestVote(behind.term + 1, 2, behind.lastTerm(), behind.log.size())
            : "voters reject candidates with shorter logs";
        System.out.println("OK");
    }
}
