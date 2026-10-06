# vector-clocks-crdt

Immutable vector clocks and a grow-only counter (G-Counter) in `VectorClocksCrdt.java`.

## Goal

Show how a vector clock tells "happened before" from "concurrent", and how a CRDT counter gives the same total on every replica without coordination.

## Run it

```
java -ea VectorClocksCrdt.java
```

Expected output (measured on Java 21):

```
concurrent updates detected: {A=2} || {A=1, B=1}
replica totals: 9 and 9
OK
```

## What it proves

- `compare` returns BEFORE, AFTER, EQUAL or CONCURRENT: A's second event `{A=2}` and B's first `{A=1, B=1}` are concurrent, while `{A=1}` is before `{A=2}`.
- Merging two concurrent clocks and ticking again gives a clock that is after both inputs.
- Two replicas that merge the counters x (3), y (5) and z (1) in different orders, with duplicates, both end at 9 with identical slots.
- Merge is an element-wise max, so repeating or reordering merges changes nothing.

## Trade-offs

- A vector clock grows with the number of writers; there is no pruning here.
- A G-Counter only counts up. Decrements need a PN-Counter, which is not implemented.
- Detecting a conflict does not resolve it; the caller must decide which value wins.

## When not to use it

- With a single writer or a central sequencer, a plain version number is simpler.
- When updates must be strictly ordered or constrained (for example a balance that cannot go below zero), a CRDT counter is the wrong tool.
