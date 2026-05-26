# SAO Aincrad Content Blueprint (Implemented Route v0.3)

## Main Story Route (Current)
1. Floor 1 `Town of Beginnings`
- Narrative: player onboarding, party formation.
- Main NPC: Klein (tutorial lead).
- Secondary NPC: local trader.
- Unlock: talk to Klein -> Floor 2.

2. Floor 2 `Tolbana Outskirts`
- Narrative: first raid prep.
- Main NPC: Asuna (early raid ally).
- Boss: floor guardian.
- Unlock: boss clear -> Floor 10.

3. Floor 10 `Forest of Reflection`
- Narrative: intel gathering.
- Main NPC: Argo (information broker).
- Unlock: Argo dialogue -> Floor 22.

4. Floor 22 `Elf Campaign Front`
- Narrative: elf campaign bridge chapter.
- Main NPC: Kizmel.
- Unlock: Kizmel dialogue -> Floor 25.

5. Floor 25 `Dark Elf Castle`
- Narrative: fortress assault.
- Main NPC: Dark Elf Scout.
- Boss: castle guardian.
- Unlock: boss clear -> Floor 35.

6. Floor 35 `Forest of Wandering`
- Narrative: event hunt chapter.
- Main NPC: Silica.
- Unlock: Silica dialogue -> Floor 47.

7. Floor 47 `Cathedral District`
- Narrative: strategy and command chapter.
- Main NPC: Heathcliff.
- Unlock: Heathcliff dialogue -> Floor 50.

8. Floor 50 `Algade Labyrinth`
- Narrative: mid-game front line push.
- Main NPC: Agil.
- Boss: labyrinth guardian.
- Unlock: boss clear -> Floor 55.

9. Floor 55 `Grandzam Lower Town`
- Narrative: gear reinforcement chapter.
- Main NPC: Lisbeth.
- Unlock: Lisbeth dialogue -> Floor 67.

10. Floor 67 `Ruined Battleground`
- Narrative: attrition warfare chapter.
- Main NPC: Klein (frontline version).
- Boss: battleground guardian.
- Unlock: boss clear -> Floor 74.

11. Floor 74 `Crimson Approach`
- Narrative: assault staging zone.
- Main NPC: Asuna (assault lead).
- Boss: gatekeeper guardian.
- Unlock: boss clear -> Floor 75.

12. Floor 75 `Granzam Fortress`
- Narrative: high-floor command battle.
- Main NPC: Asuna.
- Boss: fortress commander.
- Unlock: boss clear -> Floor 90.

13. Floor 90 `Sky Bridge Citadel`
- Narrative: final ascent.
- Main NPC: Agil.
- Boss: sky bridge guardian.
- Unlock: boss clear -> Floor 100.

14. Floor 100 `Ruby Palace`
- Narrative: endgame duel.
- Main NPC: Kayaba echo.
- Boss: Heathcliff.
- Result: Aincrad clear condition.

## Combat Layer Implemented
- 8-direction facing for player and enemy rendering.
- 8-direction animation rows for `idle/run/attack/dodge/death` for player.
- Multiple attack styles:
  - `LIGHT`: fast, short range.
  - `HEAVY`: slower, longer range.
  - `SPIN`: widest commitment window.
- Direction-aware hitboxes and co-op mirrored range handling.

## Remaining Work to Reach "Anime-level" Product
1. Replace procedural floor visuals with hand-authored tile sets per location.
2. Replace generated pixel sprites with authored character sheets (Kirito/Asuna/Klein/Lisbeth/Agil/etc).
3. Add boss phase scripting unique per floor (telegraphs, AOE markers, punish windows).
4. Add quest log UI with chapter cinematics and voiced/dialogue portraits.
5. Add social hub features for online co-op parties (matchmaking, party sync, revive mechanics).
6. Add itemization depth: weapon trees, crafting, upgrade materials by floor ecology.
