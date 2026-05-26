# SAO Aincrad Chronicles - Product Definition (Phase 0)

## 1) Product Vision
Create an online co-op action RPG inspired by SAO's Aincrad arc, focused on fast sword combat, floor progression, boss raids, party play, and emotional story moments.

Core promise:
- Feel the tension of clearing Aincrad floor by floor.
- Fight in real-time with timing, skill chains, and dangerous bosses.
- Progress as a team in online co-op with meaningful party roles.

## 2) Experience Pillars
1. Combat Precision
- Every fight must reward timing, spacing, dodging, and skill sequencing.

2. Party Dependence
- Co-op should be valuable, not optional flavor: revive, aggro control, burst windows, support skills.

3. Floor Conquest
- Progression is structured by floors, with each floor feeling distinct and memorable.

4. SAO-Style Narrative Pressure
- Clear goals, dangerous stakes, recognizable character dynamics, and boss-gate progression.

5. Long-Term Growth
- Build identity through gear, skills, roles, and social progression over time.

## 3) Target Product Shape (v1.0)
- Platform: Android (primary)
- Mode: Online co-op party PvE (2-4 players)
- Loop: town prep -> floor run -> elite encounters -> boss clear -> rewards -> progression

## 4) Design Boundaries
In scope for v1:
- Party co-op PvE
- Real-time combat with active skills and dodge/parry windows
- 15+ playable floors with bosses
- Gear progression and crafting basics
- Quest line with chapter progression

Out of scope for v1:
- Full 100-floor content parity
- PvP
- Complex guild wars
- Cross-platform release

## 5) Success Metrics (KPI)
- Crash-free sessions: >= 99.0%
- Median FPS on target devices: >= 55 FPS
- Tutorial completion: >= 80%
- D1 retention: >= 35%
- D7 retention: >= 12%
- Co-op session completion (party enters and clears/finishes run): >= 60%
- 95th percentile match start time: <= 45 sec

## 6) Definition of Done for v1 Release
Product is "release-ready" only if all are true:
1. Content
- At least 15 floors, each with unique encounter profile.
- At least 6 boss fights with distinct mechanics.

2. Systems
- Stable online co-op (2-4 players), reconnect and resume within active run.
- Combat includes basic combo, dodge, parry/counter, and at least 8 active skills.
- Progression includes levels, equipment tiers, and crafting baseline.

3. Quality
- No P0 or P1 gameplay blockers open.
- Crash-free sessions >= 99% for 7 consecutive days in beta.
- Performance targets met on agreed device matrix.

4. Operations
- Telemetry dashboards for funnels, session stability, and economy.
- Versioned live config for balance parameters.

## 7) Player Fantasy Checklist (SAO Feel)
Each major release candidate must satisfy:
- "I feel risk in combat."
- "I feel stronger after every few sessions."
- "Party coordination makes a visible difference."
- "Bosses feel like events, not stat checks."
- "Story objective is always clear."
