# arch-distributed-primitives

Eight small Java simulations of the building blocks behind distributed data systems (hashing, quorums, consensus, gossip, flow control), each with assertions that check the behaviour, so you can see how every one works in a few minutes.

## What is inside

| Folder | What it shows | Run |
| --- | --- | --- |
| [`consistent-hashing`](./consistent-hashing) | Hash ring with virtual nodes; how few keys move when a node joins | `java -ea ConsistentHashing.java` |
| [`bloom-filter`](./bloom-filter) | Measured vs. target false-positive rate, and saturation when overfilled | `java -ea BloomFilter.java` |
| [`quorum-reads-writes`](./quorum-reads-writes) | Exhaustive check that R + W > N rules out stale reads | `java -ea Quorum.java` |
| [`vector-clocks-crdt`](./vector-clocks-crdt) | Vector clocks detect concurrent updates; a G-Counter converges | `java -ea VectorClocksCrdt.java` |
| [`raft-concepts-demo`](./raft-concepts-demo) | Simplified Raft election, replication, failover and majority rule | `java -ea RaftDemo.java` |
| [`gossip-membership`](./gossip-membership) | Heartbeat gossip spreading news in O(log n) rounds and detecting a crash | `java -ea Gossip.java` |
| [`backpressure`](./backpressure) | Unbounded vs. bounded queue and load shedding with a slow consumer | `java -ea Backpressure.java` |
| [`sharding-and-partitioning`](./sharding-and-partitioning) | Hash, range and directory sharding: skew and hot shards | `java -ea Sharding.java` |

Run each command from inside its folder. The examples use a tiny Orders domain (order ids, order events) where keys are needed.

## Prerequisites

- Java 21 (single-file source launch, no build tool, no dependencies)

## How to read it

Start with `consistent-hashing`, then `quorum-reads-writes` and `raft-concepts-demo`. Each folder is one file; a run prints a few measurements and `OK`, or fails with an `AssertionError`.
