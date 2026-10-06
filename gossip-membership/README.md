# gossip-membership

A round-based gossip protocol with heartbeat counters and a timeout failure detector in `Gossip.java`, simulating 32 members with a fixed random seed.

## Goal

Show that gossip spreads information to everyone in roughly log2(n) rounds and that a crashed member is eventually suspected by all live members.

## Run it

```
java -ea Gossip.java
```

Expected output (measured on Java 21; the seed is fixed so it is repeatable):

```
n=32: everyone learned node 0 exists after 5 rounds (log2 n = 5)
after crash of node 7: 31/31 live nodes suspect it, 0 suspect healthy node 3
OK
```

## What it proves

- Each round every live member bumps its heartbeat and pushes its view to 2 random peers; all 32 members learn about node 0 within 5 rounds (the assertion allows up to 15).
- After node 7 stops, all 31 live members suspect it once its heartbeat has not advanced for more than 10 rounds.
- A healthy node (node 3) is suspected by nobody in that run.
- Views merge by taking the higher heartbeat per member, so stale gossip never overwrites newer data.

## Trade-offs

- A fixed timeout of 10 rounds trades detection speed against false suspicions; real systems use adaptive detectors such as phi-accrual.
- Rounds are synchronous and messages are never lost; the "log2 n = 5" in the output is a constant printed by the code, not computed.
- Results come from one seed (42) and one crash; it is not a statistical study.

## When not to use it

- For small clusters, a central registry or a consensus-backed membership list is simpler.
- When you need a strongly consistent member list, gossip only gives eventual agreement.
