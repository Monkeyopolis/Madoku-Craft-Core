# Madoku Craft API

Madoku Craft API is the shared Fabric module that other Madoku Craft mods rely on for configuration, persistence,
and cross-mod systems. It publishes a stable JSON feature config and a “Madoku Data” saving system that mods can reuse without duplicating the boilerplate.

## Features
- `JsonFeatureSystem` keeps each feature JSON in sync with the mod version, only filling in missing defaults and
  replacing files when the major version changes.
- `MadokuSavingSystem` stores reliable Madoku Data JSON files under each world's `madoku-data/<feature>.json`, merges defaults,
  and exposes helper APIs (`loadForWorld`, `reloadForWorld`, and `MadokuSavingSystem.MadokuData`) for reading/writing.
- `MadokuCraftAPI` exposes two public handles: `API_FEATURE` for the versioned config and `API_DATA` for persistent state
  so downstream mods can call `.getRoot()` and `.save()` without having to roll their own file handling, and it rebinds
  `API_DATA` to the active world save during `ServerLifecycleEvents.SERVER_STARTED`.

## Requirements
- Java 21
- Fabric Loader 0.18.4+
- Fabric API 0.140.2+ (or compatible subset)

## Development notes

- Config defaults live in `MadokuCraftAPI.buildDefaults()` and `buildSavingDefaults()`. Update them when adding flags.
- Both `JsonFeatureSystem` and `MadokuSavingSystem` are final; use the provided `ManagedFeature` and `MadokuData`
  wrappers to keep downstream callers from accidentally overwriting the auto-managed metadata.
- Downstream code that needs its own world-scoped state can call `MadokuSavingSystem.loadForWorld` or
  `MadokuSavingSystem.reloadForWorld`, matching the lifecycle hook the API uses to pin `API_DATA` to the current save.
