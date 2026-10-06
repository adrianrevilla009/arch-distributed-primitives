# raft-concepts-demo

A deterministic, single-threaded 5-node simulation of Raft elections and log replication in `RaftDemo.java`.

## Goal

Walk through leader election, replication, failover and the majority rule in one readable file, without timers or a network.

## Run it

```
java -ea RaftDemo.java
```

Expected output (measured on Java 21):

```
term 1, committed 2 entries on all nodes
minority partition cannot commit; commit index stays 4
OK
```

## What it proves

- Node 0 wins term 1, then replicates "create order-1" and "pay order-1" to a majority and commits both.
- After node 0 crashes, node 1 wins term 2 with 4 of 5 nodes alive and commits a third entry; the old leader rejoins and its log equals the new leader's.
- With 3 of 5 nodes down, a write is not committed and the commit index stays at 4.
- A voter rejects a candidate whose log is shorter, so a stale node cannot become leader (`requestVote` in `RaftDemo.java`).

## Trade-offs

- Elections are triggered by hand: there are no heartbeats, timeouts or randomized delays.
- `append` copies the leader's whole log instead of checking prevLogIndex and prevLogTerm, so conflicting-log repair is not shown.
- Calls are direct method calls: no message loss, delay or reordering. It is not a Raft implementation and was only run as this single scripted scenario.

## When not to use it

- Never as a consensus library; use a proven one such as etcd's raft package or Apache Ratis.
- To study safety under network faults, use a model checker or a Jepsen-style test instead.
