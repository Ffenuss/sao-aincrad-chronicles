# Implementation Plan (Execution Start)

## Phase 1 - Networked Co-op Foundation (Sprint 1-2)
Goal: first playable online co-op slice with 2 players on one floor.

### Tasks
1. Network architecture
- Add server-authoritative simulation model.
- Define message protocol for input, state snapshot, and events.

2. Session flow
- Lobby create/join, ready-check, match start.
- Party state synchronization (HP/position/skills).

3. Replication MVP
- Player transform replication with interpolation.
- Enemy state replicated from host/server authority.
- Combat hit events and damage reconciliation.

4. Fault tolerance
- Basic reconnect within 30 seconds.
- Timeout and session cleanup handling.

### Completion Criteria
- Two devices can join one session and clear one floor together.
- No desync that blocks completion in 10 consecutive test runs.

## Phase 2 - Combat 2.0
Goal: transform prototype combat into skill-based system.

### Tasks
1. Skill framework (data-driven)
- Skill definitions (cost, cooldown, cast, hitbox, iframes).
- Skill execution state machine.

2. Enemy telegraphs and counterplay
- Wind-up/active/recovery states.
- Dodge/parry validation windows.

3. Role archetypes
- DPS, bruiser, support starter kits.

### Completion Criteria
- At least 8 active skills implemented.
- Bosses require timing/coordination rather than raw stats.

## Phase 3 - Content + Story Pipeline
Goal: produce floors and story at repeatable speed.

### Tasks
1. Floor template pipeline
- Standardized TMX layers and validation checks.

2. Quest and dialogue system v2
- Branching quest states, chapter progression, event triggers.

3. Encounter library
- Elite packs, miniboss patterns, scripted boss phases.

### Completion Criteria
- 15 floors shipped in consistent quality bar.
- Main story path complete for v1 chapter arc.

## Phase 4 - Economy + Progression
Goal: long-term motivation and build identity.

### Tasks
1. Gear rarity and stat curves
2. Crafting and upgrade sinks
3. Reward pacing and anti-inflation controls

### Completion Criteria
- 10+ hour progression loop remains meaningful.

## Phase 5 - Beta Hardening
Goal: release quality.

### Tasks
1. Device performance optimization
2. Crash/ANR reduction
3. Telemetry dashboards
4. Regression and soak testing

### Completion Criteria
- Meets PRODUCT_DEFINITION.md KPIs and release DoD.
