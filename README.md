# Madoku Craft API

Madoku Craft API is the shared Fabric module that other Madoku Craft mods rely on for configuration, persistence,
and cross-mod systems. It publishes a stable JSON feature config and a “Madoku Data” saving system that Hunger,
Health, and future mods can reuse without duplicating the boilerplate.

## Features
- `JsonFeatureSystem` keeps each feature JSON in sync with the mod version, only filling in missing defaults and
  replacing files when the major version changes.
- `MadokuSavingSystem` stores reliable “Madoku Data” JSON files that include a clear label, merge defaults, and expose
  `MadokuSavingSystem.MadokuData` for reading/writing.
- `MadokuCraftAPI` exposes two public handles: `API_FEATURE` for the versioned config and `API_DATA` for persistent state
  so downstream mods can call `.getRoot()` and `.save()` without having to roll their own file handling.

## Requirements
- Java 21
- Fabric Loader 0.18.4+
- Fabric API 0.140.2+ (or compatible subset)

## Building

Use the provided Gradle wrapper so the environment stays consistent:

```
cd Mods/Madoku Craft API
./gradlew build
```

The generated jar is under `build/libs/` and can be published for other mods to consume (e.g. via JitPack or a Maven repo).

## Publishing

- Keep `mod_version` aligned with the version you publish so feature JSON files stay in sync.
- Upload the jar to GitHub Packages, JitPack, or another Maven-compatible host and point the Hunger/Health mods at the
  published coordinates (`madoku.craft.API:madoku-craft-api:<version>`).

## Development notes

- Config defaults live in `MadokuCraftAPI.buildDefaults()` and `buildSavingDefaults()`. Update them when adding flags.
- Both `JsonFeatureSystem` and `MadokuSavingSystem` are final; use the provided `ManagedFeature` and `MadokuData`
  wrappers to keep downstream callers from accidentally overwriting the auto-managed metadata.
