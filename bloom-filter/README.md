# bloom-filter

A Bloom filter built on a `BitSet` in `BloomFilter.java`, sized from an expected key count and a target false-positive rate.

## Goal

Show that the false-positive rate you measure matches the rate you sized for, and what happens when the filter holds far more keys than planned.

## Run it

```
java -ea BloomFilter.java
```

Expected output (measured on Java 21):

```
n=10000 target=0.010 m=95851 bits k=7 measured=0.0105
n=10000 target=0.001 m=143776 bits k=10 measured=0.0009
overfilled 10x: measured fpp=0.995
OK
```

## What it proves

- For 10,000 order keys and a 1% target, the filter uses 95,851 bits and 7 hashes, and 100,000 probes of absent keys give 1.05% false positives.
- Tightening the target to 0.1% needs 143,776 bits and 10 hashes, and the measured rate falls to 0.09%.
- Keys that were added are always found: the run asserts there are no false negatives.
- A filter sized for 1,000 keys that receives 10,000 answers "maybe" for 99.5% of absent keys, so it stops being useful.

## Trade-offs

- It answers "definitely not present" or "probably present"; it cannot delete keys or list them.
- The k hash positions come from double hashing over `String.hashCode()`, which is fine for the demo but weaker than a real hash such as Murmur.
- The size must be chosen up front; growing needs a rebuild or a scalable variant, which is not included.

## When not to use it

- When a false positive is expensive and you need exact membership, use a set or an index.
- When keys must be removed over time, a plain Bloom filter does not support it.
