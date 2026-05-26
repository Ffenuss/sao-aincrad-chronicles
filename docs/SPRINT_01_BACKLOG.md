# Sprint 01 Backlog - Co-op Foundation

Duration: 2 weeks
Objective: deliver first online co-op vertical slice (2 players, 1 floor run).

## Epic A - Networking Core
1. Define network protocol v0
- InputCommand, Snapshot, Event, Ack messages.
- Acceptance: protocol documented and versioned.

2. Implement transport layer abstraction
- Interface for connect/send/receive/disconnect.
- Acceptance: local loopback integration test passing.

3. Implement simulation tick loop
- Fixed tick rate (e.g. 20 Hz) for gameplay state updates.
- Acceptance: deterministic state progression in isolated tests.

## Epic B - Co-op Session Flow
1. Lobby create/join flow
- Host creates room, peer joins by code.
- Acceptance: two clients can enter ready state.

2. Match bootstrap
- Spawn both players, sync initial state, start timer.
- Acceptance: both clients start same floor together.

## Epic C - Replication MVP
1. Remote player movement replication
- Interpolation/extrapolation smoothing.
- Acceptance: movement appears stable under simulated latency.

2. Enemy authority replication
- Enemy AI runs on authority side, clients render snapshots.
- Acceptance: enemy HP and deaths stay consistent across clients.

3. Combat event replication
- Attack cast, hit confirm, damage numbers synced.
- Acceptance: same kill result on both clients.

## Epic D - Stability + Tooling
1. Reconnect handling (short window)
- Rejoin session within 30 seconds.
- Acceptance: rejoined player regains control without reset.

2. Debug overlay
- RTT, packet loss, tick drift, desync warnings.
- Acceptance: overlay toggle in debug build.

## Sprint Exit Criteria
1. Two devices play one full floor run from lobby to completion.
2. No blocker desync in 10 sequential QA runs.
3. Crash-free rate in internal test >= 99% over 50 sessions.
