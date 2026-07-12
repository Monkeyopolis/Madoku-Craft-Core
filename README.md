## Overview

Madoku Craft: API is a Fabric library MOD required by all Madoku Craft: MODs.

## Features:

- Dedicated JSON file system.
This system manages how a MOD's JSON files are created, updated, and deleted.
It also formats how those files are written.

- Dedicated Data system.
This system manages how a MOD saves and loads data.
It manages World and Player specific data.

- Dedicated Scheduler system.
This system manages how events are scheduled and executed.
It allows a MOD to hook into Minecraft's TICK system more consistently.
The Scheduler system also has an adaptive interval that adjusts the tick rate based on the server's performance.

- Dedicated INFO debug system.
This system manages how INFO debugs are handled.
It allows a MOD to toggle debugs on and off without adjusting the code.

- Dedicated Time system.
This system creates a configurable Time system.
It allows users to customize the Day and Night cycle.

- Dedicated Season system.
This system creates a configurable Season system.
It adds seasonal changes to a world.

## Disclosure:

- Most of these systems are configurable in the CONFIG files.
- It allows you to tune or disable certain mechanics or systems to your specific needs.