# backpressure

A fast producer and a slow consumer connected by a queue in `Backpressure.java`, run with an unbounded queue, a bounded queue and load shedding.

## Goal

Show how a bounded queue slows the producer down to the consumer's pace, and how load shedding rejects work instead of waiting.

## Run it

```
java -ea Backpressure.java
```

Expected output (measured on Java 21; the unbounded depth varies slightly between runs):

```
peak depth: unbounded=199 bounded(10)=10
load shedding with capacity 10: 190 of 200 rejected
OK
```

## What it proves

- The producer sends 200 items while the consumer takes one every 2 ms. With a `LinkedBlockingQueue` the backlog grows to about 199 items.
- With an `ArrayBlockingQueue` of capacity 10, `put` blocks the producer and the depth never exceeds 10.
- With no consumer at all, `offer` on a capacity-10 queue rejects 190 of 200 items; this is the load-shedding alternative to blocking.
- The assertions require a bounded depth of at most 10 and an unbounded depth above 50.

## Trade-offs

- Blocking pushes the slowdown upstream; callers must tolerate waiting or time out.
- Shedding keeps latency low but loses work, so the sender needs retries or an acceptable loss.
- The timing uses `Thread.sleep(2)`, so the unbounded peak depends on scheduling and is only asserted loosely.

## When not to use it

- When the producer cannot be slowed (for example external events), buffer durably in a log or broker instead.
- For cross-process flow control, an in-memory queue does not apply; use credit-based or rate-limit protocols.
