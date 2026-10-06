# sharding-and-partitioning

Three ways to place keys on 4 shards (hash, range and directory) compared on skew in `Sharding.java`.

## Goal

Show how the choice of partitioning scheme creates or avoids hot shards for sequential keys and skewed key names.

## Run it

```
java -ea Sharding.java
```

Expected output (measured on Java 21):

```
recent writes, range-by-time skew=4.00 vs hash skew=1.00
range by first letter: uniform=1.10 skewed names=2.84
directory moved tenant-0 to shard 3 without touching other tenants
OK
```

## What it proves

- Skew is the busiest shard's load divided by the average. For the latest 1,000 time-ordered keys, range sharding sends everything to one shard (skew 4.00), while hash sharding of the full set gives 1.00.
- Range sharding by first letter is balanced for uniformly random names (1.10) but reaches 2.84 when 60% of names start with "s".
- A directory (lookup map) lets you move `tenant-0` to shard 3 by changing one entry, leaving the other tenants where they are.

## Trade-offs

- Hash sharding balances load but breaks range scans, since neighbouring keys are scattered.
- Range sharding keeps scans cheap but needs splitting and rebalancing as hot ranges appear; none is implemented here.
- A directory needs its own highly available store, and every lookup pays for it.
- The key sets are synthetic (fixed seed), so the exact skew values are only illustrative.

## When not to use it

- With a single node or data that fits comfortably on one server, sharding adds complexity for no gain.
- For adding or removing shards with minimal movement, see `consistent-hashing` instead of the plain modulo used here.
