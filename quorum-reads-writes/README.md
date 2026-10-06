# quorum-reads-writes

A replicated register with configurable N, R and W in `Quorum.java`, checked by trying every possible write and read replica subset.

## Goal

Show that a read sees the latest write exactly when R + W > N, by enumerating all cases instead of arguing it on paper.

## Run it

```
java -ea Quorum.java
```

Expected output (measured on Java 21; the real run prints 10 table lines, shortened here):

```
N=5 R=3 W=2 R+W>N=false stale read combos=10
N=5 R=3 W=3 R+W>N=true  stale read combos=0
N=5 R=2 W=4 R+W>N=true  stale read combos=0
write with 1 of 3 replicas rejected: write quorum not met
OK
```

## What it proves

- For N=5, every (R, W) pair from 1 to 5 is tested against all write subsets and all read subsets; stale reads are possible exactly when R + W <= N.
- With R=2, W=2 there are 30 stale read combinations; with R=3, W=3 there are none.
- A read returns the replica copy with the highest version among the R it asks.
- A write that can reach fewer than W replicas is rejected with `write quorum not met` (3 replicas, W=2, one reachable).

## Trade-offs

- Larger R or W gives stronger reads but needs more replicas up, which lowers availability.
- The model is one key, one write, no concurrent writers, and versions are supplied by the caller; there is no read repair or hinted handoff.
- Exhaustive enumeration is exponential in N, fine for N=5 only.

## When not to use it

- For concurrent writers you also need conflict handling, such as the vector clocks in `vector-clocks-crdt`.
- If you need linearizable behaviour under failures, quorums alone are not enough; use a consensus protocol.
