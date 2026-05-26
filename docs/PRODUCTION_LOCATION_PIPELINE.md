# Production Location Pipeline

## Goal

Build locations as production-ready game scenes instead of ad-hoc backgrounds or disconnected collision maps.

Every important floor should come from one authoritative scene description that drives:

- the painted backdrop
- blocked/navigation data
- player spawn
- enemy spawn points
- NPC placement
- exit triggers

## Quality Target

The visual target is "anime-inspired game zone", not debug space and not raw prototype tiles.

For each important location, we want:

- a strong visual silhouette
- readable roads and plazas
- intentional landmark placement
- collision that matches what the player sees
- safe combat space around the spawn point
- enemy pressure placed outside the immediate spawn lane

## File Structure

For a production floor, the canonical asset set should be:

- `core/assets/maps/floorN_scene.json`
- `core/assets/maps/floorN_scene.png`
- `core/assets/maps/floorN.tmx`

Optional supporting assets:

- `core/assets/maps/floorN_mask.png`
- `core/assets/maps/floorN_palette.json`
- `core/assets/maps/floorN_reference.png`

## Pipeline

1. Define the location in `floorN_scene.json`.
2. Generate the painted scene image from that spec.
3. Generate the TMX map from the same spec.
4. Use one blocked layer as the gameplay truth for movement.
5. Keep spawn, enemies, exits, and NPCs in object layers generated from the same scene spec.
6. Validate that all gameplay objects sit in free cells before building.

## Scene Spec Rules

The scene spec should describe:

- map size
- palette and visual theme
- road network
- central landmarks
- large structures
- decorative structures
- blocked footprints
- spawn point
- enemy spawns
- NPCs
- exits

Rules:

- large visual objects must have matching blocked footprints
- player spawn must always be in a guaranteed safe lane
- enemy spawns must not overlap blocked space
- exits must connect to a visible road or gate
- the main route from spawn to the first landmark must remain readable

## Floor 1 Standard

`Floor 1` is the benchmark scene for the rest of the project.

It should function as:

- a true Town of Beginnings style spawn zone
- a safe onboarding area
- a visually attractive hub
- a reusable template for later city floors

## Next Production Steps

1. Keep improving `floor1_scene.json` until the layout, readability, and feel are correct.
2. Replace purely procedural painting with curated scene art generation and manual polish where needed.
3. Extend the same pipeline to `floor2`, then to the story-critical route.
