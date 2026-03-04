# Madoku Craft API

Madoku Craft: API is a fabric library MOD.
It's required by other Madoku Craft MODs.
They will not function without it.

Features:

- Dedicated JSON file system.
This system manages how a MOD's JSON files are created, updated, and deleted.

- Dedicated Data system.
This system manages how a MOD saves and loads data.
It can manage global and world specific data.

- Dedicated Scheduler system.
This system manages how events are scheduled and executed.
It allows a MOD to hook into Minecraft's TICK system more consistently.

- Dedicated INFO debug system.
This system manages how INFO debugs are handled.
It allows a MOD to toggle debugs on and off without adjusting the code.

- Dedicated Time system.
This system creates a configurable time system.
It ties all API systems together and allows users to change the time cycle.