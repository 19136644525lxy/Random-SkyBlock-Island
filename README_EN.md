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
- Press `B` to open the block-pool screen (rebindable, see "Screens and Keybinds" below): all registered blocks are drawn with the vanilla item renderer, **with no JEI/REI dependency**; the grid adapts to the window and supports paging, search by id, English name, or the name supplied by the active resource pack, click icons for multi-select, select all results, clear the selection, and submit the whole batch
- The â€œView current pool / Show all blocksâ€?button switches between the full block list and the current random pool; in the current-pool view, click blocks to mark them for removal, use â€œMark allâ€?for the current results, and confirm a second time before emptying the whole pool; search and paging stay available
- â€œHide selectedâ€?enters a hide-selection mode: select blocks, then click it again to hide them from this clientâ€™s pool screen only (the mod block is not deleted and the server random pool is unchanged). â€œHidden blocksâ€?lists hidden entries and clicking one restores it; â€œRefreshâ€?reloads the hidden list and requests a fresh server snapshot; the screen shows refreshing, then a success message, and reports a timeout if no snapshot arrives. Page arrows are disabled on the first/last page, and a close button sits at the top right. The hidden list is stored per save/server in `config/rsl-hidden-blocks/<context-hash>.json`. On first open after upgrading, a context with no file yet reads the old global `config/rsl-hidden-blocks.json` as its initial list and writes it into that context (the old file is kept; other contexts also inherit it on first open)
- Press `0` to open the gameplay menu (rebindable, see "Screens and Keybinds" below): Start, Stop, Reset, Selection wand, Area list, Manage access, delay setting, status and Close. The menu buttons are equivalent to commands and their results appear below the panel; direct commands still work, and the wand's right-click selection flow is unchanged
- Hovering a block icon gives a gentle lift, pulsing outline and glow; the title, search, stats and footer controls are visually separated
- Opening and editing the screen uses a dedicated block-pool ACL and **never checks vanilla permission levels**; a level-4 operator without an ACL entry is still denied. The server checks the ACL before sending a snapshot and before accepting mutations, and closes the screen when denied
- Access is managed by the host/server owner/console with `/randomskyblock access ...`; the server validates and saves the batch atomically, so a rejected id never leaves a half-applied config
- List (paginated), add, remove, clear (with confirmation), and reset to defaults
- Blocks are stored as `namespace:path` strings; ids from temporarily removed mods are kept and become active again when the mod returns
- Invalid/missing ids are marked by `blocks list` or the screen and never prevent the mod or server from starting; `/start` refuses to run if the pool has no usable block
- The pool is capped at 4,096 entries

### Egg Mode
- When enabled, each random transform has a chance (default 20%) to spawn a separate small egg island near a random online player (default 24-50 blocks away)
- The egg island is filled with one block randomly chosen from the â€œegg poolâ€? no island spawns while the egg pool is empty
- Spawn placement avoids existing areas (default minimum distance 16 blocks), requires the target cube to be all air with 8 clear blocks above, and never sits below player height
- Default volume is 27 (3Ã—3Ã—3) and can be set to 1-4,096; â€œOther pools â†?Eggâ€?in the block-pool screen edits the egg pool and its parameters and offers a 3D volume preview
- Off by default; hosts/owners can toggle it with `/randomskyblock egg ...` or the gameplay menu. In multiplayer one random player is chosen
- A successful spawn broadcasts the egg island coordinates to everyone

### Reset and Area Snapshot
- Before the first `/start`, the current blocks of every area are recorded to `<world>/rsl_snapshot.bin` (palette-compressed)
- `/randomskyblock reset` stops the game, restores area blocks to the pre-start state from the snapshot, and clears the last egg island
- The snapshot is recorded only before the first start and is not overwritten by later starts; entities, inventories and container contents are not restored
- Resetting requires permission level 4 and is independent of the block-pool ACL

### Container Stick and NBT Container Pool
- The â€œContainer stickâ€?button in the gameplay menu gives a container stick; Shift + right-click a chest/barrel containing items to add that container (with contents) to the random pool
- A naming screen appears first: rename the container or leave it empty; the block-pool screen shows the custom name
- Container entries only participate in areas with a volume of 27 or less, so large areas are not flooded with containers; their contents are restored when placed
- Click container entries in the block-pool screen to mark them for removal

### Sounds
- Opening/closing screens, block selection, apply results, start/stop/reset, egg spawns, access changes and captures all play vanilla sound feedback
- Adjust with `/randomskyblock sound on|off|volume <0-100>` or the â€œSoundâ€?button in the gameplay menu
- Settings are stored globally (`soundEnabled`, `soundVolume`)

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

## Screens and Keybinds

All screens are opened with keybinds, which can be changed in **Options â†?Controls â†?Key Binds â†?Random SkyBlock Island**:

| Key | Action | Screen |
|-----|--------|--------|
| `B` | Open block pool | Block pool screen |
| `0` | Open gameplay menu | Gameplay menu (Random SkyBlock Island) |

### Gameplay Menu (`0`)

An in-game control panel; its buttons are equivalent to commands, and results appear below the panel:

- **Row 1**: Start / Stop / Reset
- **Row 2**: Selection wand (give a wand) / Area list (open the area list screen) / Manage access (open the block-pool access screen) / Egg (toggle egg mode) / Container stick (give a stick) / Sound (toggle sounds)
- **Row 3**: delay field + Set delay / Status / Close
- **Manage access**: shows online players, lets you multi-select them and then choose "Grant editor / Grant owner / Revoke", with a second confirmation before applying (same as `/randomskyblock access ...`)
- **Egg**: toggles egg mode; detailed parameters live under â€œOther poolsâ€?in the block-pool screen
- **Container stick**: gives a container stick; **Sound**: toggles all sounds

### Block Pool Screen (`B`)

Used to choose the random block pool. The footer is one row on wide screens and wraps to two rows on narrow ones; buttons from left to right:

- **Select all results**: select every block in the current filter; **Clear selection**: deselect everything; **Apply changes**: submit the batch
- **View current pool**: switch between the full block list and the current random pool; in the pool view, click blocks to mark them for removal
- **Hide selected**: enter hide-selection mode; click again to hide those blocks from this client's screen only (mod blocks stay installed and the server pool is unchanged)
- **Hidden blocks**: list blocks hidden for this save/server and click to restore them
- **Refresh**: reload the hidden list and request a fresh server snapshot
- **Hidden pools**: open the hidden-pool manager to rename the current pool or copy one from another save
- **Other pools**: open the egg-pool screen to edit the egg pool, spawn chance, volume and minimum area distance, with a 3D volume preview
- Page arrows are disabled on the first/last page, and a close button sits at the top right.

Before opening, the client asks the server for a snapshot first: without permission it only shows "no block-pool permission" in the action bar instead of briefly opening the screen.

### Selection Wand

The selection wand is an item, not a screen keybind: obtain it from the gameplay menu or `/randomskyblock wand`, then right-click while holding it to pick two corners and right-click a third time to open the confirmation screen (see the commands section below).

## Commands

### Game Control
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock start` | Start random transformation | 4 |
| `/randomskyblock stop` | Stop random transformation | 4 |
| `/randomskyblock reset` | Stop the game and restore areas to the pre-start state (keeps area and block config) | 4 |
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

### Egg Mode
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock egg status` | Show egg toggle, chance, radius, volume, distance and egg-pool stats | ACL Owner |
| `/randomskyblock egg on` / `off` / `toggle` | Enable / disable / toggle egg mode | 4 |
| `/randomskyblock egg chance <1-100>` | Set the spawn chance per transform (percent) | ACL Owner |
| `/randomskyblock egg radius <min> <max>` | Set the min/max distance from the player | ACL Owner |
| `/randomskyblock egg volume <1-4096>` | Set the egg island volume (cube edge rounded up) | ACL Owner |
| `/randomskyblock egg distance <1-256>` | Set the minimum distance from existing areas | ACL Owner |
| `/randomskyblock egg pool list` | List blocks in the egg pool | ACL Owner |
| `/randomskyblock egg pool add <id>` | Add a block to the egg pool | ACL Owner |
| `/randomskyblock egg pool remove <id>` | Remove a block from the egg pool | ACL Owner |
| `/randomskyblock egg pool clear` | Empty the egg pool | ACL Owner |

### Sounds
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock sound` | Show sound toggle and volume | 4 |
| `/randomskyblock sound on` / `off` | Enable / disable sounds | 4 |
| `/randomskyblock sound volume <0-100>` | Set sound volume (percent) | 4 |

### Block Pool Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock blocks list [page]` | List the pool in pages of 20, marking missing ids | ACL Editor |
| `/randomskyblock blocks add <id>` | Add a block, e.g. `minecraft:oak_log` or `mymod:my_block` | ACL Editor |
| `/randomskyblock blocks remove <id>` | Remove a block from the pool | ACL Editor |
| `/randomskyblock blocks clear` | Show a clear confirmation prompt (does not clear) | ACL Editor |
| `/randomskyblock blocks clear confirm` | Confirm clearing the pool (refused while running) | ACL Editor |
| `/randomskyblock blocks reset` | Restore the 269 built-in blocks | ACL Editor |

### Block-pool access
| Command | Description | Permission |
|---------|-------------|------------|
| `/randomskyblock access list` | List the block-pool ACL | Owner / console / host |
| `/randomskyblock access grant <player>` | Grant Editor (can open and edit) | Owner / console / host |
| `/randomskyblock access grantowner <player>` | Grant Owner (can grant further access) | Owner / console / host |
| `/randomskyblock access revoke <player>` | Revoke access (host and console always remain) | Owner / console / host |

> The block-pool ACL and `/randomskyblock blocks ...` are completely independent of vanilla permission levels: a level-4 operator without an ACL entry cannot open the screen or run `blocks` commands. The console and the single-player host always act as Owner. Other game/area commands still use level 4. `status` is open to everyone and shows the callerâ€™s pool access level.

## Config Files

### Global Settings (`config/randomskyblockisland.json`)

Global settings. `defaultArea` is the **initial** default area: it is used only to seed a save that does not have its own default area yet. After that, the save's default area is stored independently in `<world>/rsl_areas.json` and no longer follows the global value.

```json
{
  "configVersion": 3,
  "transformDelaySeconds": 180,
  "maxAreaVolume": 65536,
  "blocksPerTick": 8192,
  "broadcastMode": "all",
  "broadcastRadius": 64,
  "soundEnabled": true,
  "soundVolume": 0.7,
  "eggModeEnabled": false,
  "eggChance": 0.2,
  "eggMinRadius": 24,
  "eggMaxRadius": 50,
  "eggVolume": 27,
  "eggMinDistance": 16,
  "eggPool": [],
  "nbtPool": [],
  "poolAccess": [
    {
      "uuid": "00000000-0000-0000-0000-000000000000",
      "name": "ExampleOwner",
      "role": "OWNER"
    }
  ],
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
    {
      "id": "minecraft:oak_log",
      "state": "minecraft:oak_log"
    },
    {
      "id": "minecraft:oak_planks",
      "state": "minecraft:oak_planks"
    }
  ]
}
```

- `broadcastMode`: `all` (default, every player), `nearby` (within `broadcastRadius` of the area center), or `summary` (one combined message after all areas finish)
- `maxAreaVolume`: maximum volume per area, default 65,536
- `blocksPerTick`: maximum blocks filled per tick, default 8,192
- `soundEnabled` / `soundVolume`: sound toggle and volume (0-1)
- `eggModeEnabled` / `eggChance` / `eggMinRadius` / `eggMaxRadius` / `eggVolume` / `eggMinDistance`: egg-mode parameters
- `eggPool`: egg-pool block ids; `nbtPool`: pool entries with container NBT (id/state/nbt)
- `allowedBlocks`: the normal random pool, stored as entry objects since v3 (`id`, `state`, optional `nbt`); legacy string arrays migrate automatically

### Runtime State (`config/randomskyblockisland.state.json`)

```json
{
  "isRunning": false,
  "secondsRemaining": 0,
  "eggIslandPresent": false
}
```

### Per-Save Areas (`<world>/rsl_areas.json`)

Stores the save's default area, extra areas, and the currently locked dimension. When an old file has no `defaultArea`, it is seeded from the global initial area and written back automatically; a missing `activeDimension` means unlocked.

```json
{
  "configVersion": 3,
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

### Area Snapshot (`<world>/rsl_snapshot.bin`)

Area blocks recorded before the first start, used to restore on reset. Binary format (palette + indices) and never edited by hand; when the file is missing, `reset` only stops the game and reports that no snapshot exists.

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
- Container entries only participate in areas with a volume of 27 or less; snapshots do not restore entities, inventories or container contents
- Egg islands spawn randomly near players and may appear in hard-to-spot places (coordinates are broadcast on spawn)

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

Apache License 2.0 - See [LICENSE](LICENSE)
