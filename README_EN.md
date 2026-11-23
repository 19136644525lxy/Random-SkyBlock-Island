# Random SkyBlock Island

A Minecraft mod that randomly transforms your skyblock island blocks! Supports both Fabric and Forge on Minecraft 1.20.1.

## Features

### Random Block Transformation
- At specified intervals (default 3 minutes), island blocks randomly transform into another block type
- 269 built-in block types, including wood, ores, building materials, colored blocks, and more
- Each area transforms independently with a different random block
- Large areas are filled in per-tick batches to avoid a single-tick stall

### Multi-Area Support
- Add multiple transformation areas, each with independent coordinate ranges
- **Default Area**: Stored per save and independent between saves; the global `defaultArea` is only the initial value for new saves
- **Extra Areas**: Per-save config, managed independently per world
- Coordinates are normalized automatically; new areas are validated for name, coordinates, and volume (default limit: 65,536 blocks)

### Dimension Lock
- Each `/start` locks the dimension where the command was run; a save can run the game in only one dimension at a time
- From another dimension, `start`, `reset`, `delay`, and area mutations are rejected with a message naming the active dimension
- `stop`, `status`, `reload`, `blocks`, and `area initial ...` are not dimension-restricted; console commands are always allowed
- On restart the game resumes only if the locked dimension is loaded and at least one area intersects its height; otherwise it stops safely and clears the lock
- Area coordinates are dimension-independent; filling is clipped to the target dimension's height

### Custom Block Pool
- Add blocks from **any installed mod** to the global random pool via commands
- Press `B` to open the block-pool screen: all registered blocks are drawn with the vanilla item renderer, **with no JEI/REI dependency**; the grid adapts to the window and supports paging, search by id, English name, or the name supplied by the active resource pack, click icons for multi-select, select all results, clear the selection, and submit the whole batch
- The screen is read-only or editable according to `blocksPermissionLevel`; the server validates and saves the batch atomically, so a rejected id never leaves a half-applied config
- List (paginated), add, remove, clear (with confirmation), and reset to defaults
- Blocks are stored as `namespace:path` strings; ids from temporarily removed mods are kept and become active again when the mod returns
- Invalid/missing ids are marked by `blocks list` or the screen and never prevent the mod or server from starting; `/start` refuses to run if the pool has no usable block
- The pool is capped at 4,096 entries

### Countdown Persistence
- Running state and remaining time are stored in a separate state file and survive world reloads
- Auto-saves on server shutdown and restores on rejoin
- Flushes to disk every 10 seconds while running instead of every second

### Config System
- Optional Cloth Config API screen (only adjusts the transformation interval)
- Global settings and the initial default area at `config/randomskyblockisland.json`
- Runtime state at `config/randomskyblockisland.state.json`
- Per-save default area and extra areas at `<world>/rsl_areas.json`
- Legacy config is migrated automatically with a `randomskyblockisland.v1.bak.json` backup

### Localization
- Supports English and Chinese
- All player-facing text is localized

## Commands

### Game Control
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock start` | Start random transformation | 4 |
| `/randomskyblock stop` | Stop random transformation | 4 |
| `/randomskyblock reset` | Reset game state (preserves areas and block config) | 4 |
| `/randomskyblock status` | Show running state, countdown, area and block pool stats | none |
| `/randomskyblock reload` | Reload config and areas from disk (refused while filling) | 4 |

### Area Management
| Command | Description | Save Location |
|---------|-------------|---------------|
| `/randomskyblock area add <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>` | Add new area | Per-save |
| `/randomskyblock area remove <name>` | Remove area | Per-save |
| `/randomskyblock area rename <oldName> <newName>` | Rename area (including the save default) | Per-save |
| `/randomskyblock area list [page]` | List all areas of the current save in pages of 8 | - |
| `/randomskyblock areas [page]` | Query all areas of the current save/server with default/named/invalid markers, volume, and locked dimension (8 per page) | 4 |
| `/randomskyblock area show` | Show the current save default area | - |
| `/randomskyblock area pos1` | Set the current save default area position 1 | Per-save |
| `/randomskyblock area pos2` | Set the current save default area position 2 | Per-save |
| `/randomskyblock area setfrompos <name>` | Create a new area from the current default area coordinates | Per-save |
| `/randomskyblock area initial show` | Show the global initial default area | - |
| `/randomskyblock area initial pos1` | Set the global initial default area position 1 | Global |
| `/randomskyblock area initial pos2` | Set the global initial default area position 2 | Global |
| `/randomskyblock area resetdefault` | Reset the save default area to the global initial area | Per-save |

> Area management commands (`/randomskyblock area ...`) require permission level 4. Except for `area initial ...`, save-level area commands require a loaded save and otherwise report `world_not_initialized`. While running, `start`, `reset`, `delay`, and area mutations from a non-active dimension are rejected; `area show` / `area list` remain read-only and allowed.

### Selection Wand
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock wand` | Get a selection wand (stick texture, max stack 1) | 4 |

- Right-click two points with the wand, then right-click a third time to open the confirmation screen; confirm to add a named area
- With no active selection, right-clicking an existing named area opens its edit screen: rename, delete, or click **Reselect** to pick two new points and save the new coordinates
- The edit target is the smallest named area containing the click; the default area is excluded to avoid accidental edits
- Shift + right-click clears the current selection; all coordinate and duplicate-name checks run on the server
- Closing the screen before submitting automatically cancels the current selection

### Delay Settings
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock delay <seconds>` | Set transformation interval (1-3600 seconds) | 4 |
| `/randomskyblock delay show` | Show current interval | 4 |

### Block Pool Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock blocks list [page]` | List the pool in pages of 20, marking missing ids | 2 by default |
| `/randomskyblock blocks add <id>` | Add a block, e.g. `minecraft:oak_log` or `mymod:my_block` | 2 by default |
| `/randomskyblock blocks remove <id>` | Remove a block from the pool | 2 by default |
| `/randomskyblock blocks clear` | Show a clear confirmation prompt (does not clear) | 2 by default |
| `/randomskyblock blocks clear confirm` | Confirm clearing the pool (refused while running) | 2 by default |
| `/randomskyblock blocks reset` | Restore the 269 built-in blocks | 2 by default |

> The block pool permission is controlled by `blocksPermissionLevel` (default `2`, i.e. operators). Set it to `0` to let regular players manage the pool; after editing the file, run `/randomskyblock reload` or restart the server. `status` is open to everyone.

## Config Files

### Global Settings (`config/randomskyblockisland.json`)

Global settings. `defaultArea` is the **initial** default area: it is used only to seed a save that does not have its own default area yet. After that, the save's default area is stored independently in `<world>/rsl_areas.json` and no longer follows the global value.

```json
{
  "configVersion": 2,
  "transformDelaySeconds": 180,
  "maxAreaVolume": 65536,
  "blocksPerTick": 8192,
  "broadcastMode": "all",
  "broadcastRadius": 64,
  "blocksPermissionLevel": 2,
  "defaultArea": {
    "name": "default",
    "minX": -1,
    "minY": 64,
    "minZ": -4,
    "maxX": 4,
    "maxY": 66,
    "maxZ": 1
  },
  "allowedBlocks": [
    "minecraft:oak_log",
    "minecraft:oak_planks"
  ]
}
```

- `broadcastMode`: `all` (default, every player), `nearby` (within `broadcastRadius` of the area center), or `summary` (one combined message after all areas finish)
- `maxAreaVolume`: maximum volume per area, default 65,536
- `blocksPerTick`: maximum blocks filled per tick, default 8,192

### Runtime State (`config/randomskyblockisland.state.json`)

```json
{
  "isRunning": false,
  "secondsRemaining": 0
}
```

### Per-Save Areas (`<world>/rsl_areas.json`)

Stores the save's default area, extra areas, and the currently locked dimension. When an old file has no `defaultArea`, it is seeded from the global initial area and written back automatically; a missing `activeDimension` means unlocked.

```json
{
  "configVersion": 2,
  "activeDimension": "minecraft:overworld",
  "defaultArea": {
    "name": "default",
    "minX": -1,
    "minY": 64,
    "minZ": -4,
    "maxX": 4,
    "maxY": 66,
    "maxZ": 1
  },
  "areas": [
    {
      "name": "area1",
      "minX": 10,
      "minY": 64,
      "minZ": 10,
      "maxX": 20,
      "maxY": 66,
      "maxZ": 20
    }
  ]
}
```

## Installation

### Fabric
1. Download the Fabric `.jar` file
2. Place it in the `.minecraft/mods` folder
3. Ensure Fabric Loader and Fabric API are installed
4. Optionally install Cloth Config API and Mod Menu for the config screen

### Forge
1. Download the Forge `.jar` file
2. Place it in the `.minecraft/mods` folder
3. Ensure the corresponding Forge version is installed
4. Optionally install Cloth Config API for the config screen

## Dependencies

### Fabric

| Dependency | Version | Required |
|------------|---------|----------|
| Fabric Loader | >=0.16.2 | Yes |
| Fabric API | * | Yes |
| Cloth Config API | >=11.1.136 | Optional (config screen only) |
| Mod Menu | >=7.2.2 | Optional (mod menu entry) |

### Forge

| Dependency | Version | Required |
|------------|---------|----------|
| Forge | 47.4.22 (1.20.1) | Yes |
| Cloth Config API | >=11.1.136 | Optional (config screen only) |

## Known Limitations

- Only one dimension can run the game per save at a time; area coordinates are dimension-independent and are clipped to the target dimension's height
- The config screen only adjusts the interval; areas are managed via commands or the selection wand, and the block pool via the `B` screen or commands
- Per-area volume is limited to 65,536 by default; the pool is limited to 4,096 entries
- Shutting down while a fill is in progress leaves that area partially filled (the fill queue is not persisted)
- Modded blocks require the mod on both server and client; otherwise the client cannot display them

## Build

### Fabric
```bash
./gradlew build
```

### Forge
```bash
cd forge && ./gradlew build
```

### Tests (shared logic and parity)
```bash
./gradlew test
```

The first build needs network access for Fabric/Forge and Maven dependencies. The Fabric build uses JDK 21 as the Gradle JVM (required by Loom) while compiling to Java 17.

## License

MIT License - See [LICENSE](LICENSE)
