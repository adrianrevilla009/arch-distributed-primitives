# consistent-hashing

A consistent hash ring with virtual nodes in `ConsistentHashing.java`, plus a rebalance demo over 20,000 order keys.

## Goal

Show how a hash ring keeps most keys in place when a node is added, and how virtual nodes even out the load across nodes.

## Run it

```
java -ea ConsistentHashing.java
```

Expected output (measured on Java 21):

```
max/avg load: 1 vnode=2.92, 200 vnodes=1.12
add 5th node: 20.7% of keys moved (ideal 20%)
hash % N, 4 -> 5 nodes: 79.7% moved
OK
```

## What it proves

- With 1 virtual node per server the busiest node carries 2.92 times the average load; with 200 it drops to 1.12.
- Adding a fifth node to the 200-vnode ring moves about 20.7% of keys, close to the ideal 1/5.
- Plain `hash % N` going from 4 to 5 nodes moves about 79.7% of keys.
- Removing the new node restores the exact previous key mapping (`Ring.remove` in `ConsistentHashing.java`).

## Trade-offs

- Virtual nodes cost memory and lookup-table size: 200 entries per server here, kept in a `TreeMap`.
- The numbers depend on the MD5-based hash; a different hash gives slightly different percentages.
- No replication or weights: every node gets the same number of virtual nodes.

## When not to use it

- For a fixed, small set of nodes that never changes, `hash % N` is simpler and enough.
- When you need explicit control over where a key lives, use a directory (see `sharding-and-partitioning`).
