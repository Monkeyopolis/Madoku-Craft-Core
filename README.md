## Madoku Craft: API

Madoku Craft: API is the shared Fabric system layer used by Madoku Craft.
It provides the infrastructure that the main mod builds on for config, data, time, scheduling, and debug handling.

## Features

- Dedicated JSON system.
This system manages how managed JSON files are created, updated, and normalized.

- Dedicated data system.
This system manages how a mod saves and loads world data.
It supports persistent global and world-specific state.

- Dedicated scheduler system.
This system manages how events are scheduled and executed.
It keeps long-running tasks aligned with the game's tick loop.

- Dedicated debug system.
This system manages how INFO debugs are handled.
It allows debug domains and metrics to be toggled without code changes.

- Dedicated clock and time system.
This system manages the shared gameplay clock and the configurable world-time model used by Madoku Craft.

- Dedicated season system.
This system manages season state, seasonal precipitation, and season sync.
