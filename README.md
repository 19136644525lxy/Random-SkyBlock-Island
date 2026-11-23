# Random SkyBlock Island

一个能让你的空岛方块随机变换的 Minecraft 模组！支持 Fabric 和 Forge 双平台（Minecraft 1.20.1）。

## 功能特性

### 随机方块变换
- 每隔指定时间（默认 3 分钟），空岛上的方块会随机变为另一种方块
- 内置 269 种方块，包括木材、矿石、建筑材料、彩色方块等
- 每个区域独立随机，不同区域同时变换为不同方块
- 大区域按每 tick 预算分批填充，避免一次性卡顿

### 多区域支持
- 支持添加多个变换区域，每个区域有独立的坐标范围
- **默认区域**：每个存档独立保存，互不影响；全局配置中的 `defaultArea` 只作为新存档的初始值
- **额外区域**：存档级配置，每个存档独立管理
- 区域坐标自动归一化；新增区域会校验名称、坐标和体积（默认上限 65536 格）

### 维度玩法锁定
- 每次 `/start` 锁定执行命令时所在的维度；一个存档同一时间只能有一个维度运行玩法
- 其他维度执行 `start`、`reset`、`delay` 或修改区域的命令会被拒绝，并提示当前玩法所在维度
- `stop`、`status`、`reload`、`blocks`、`area initial ...` 不受维度限制；控制台命令始终放行
- 重启后只有锁定维度仍加载、且至少有一个区域与目标维度高度相交时才恢复；否则安全停止并清空锁定
- 区域坐标与维度无关；切换维度后按目标维度的高度裁剪填充

### 自定义方块池
- 可通过命令把**任意已安装模组**的方块加入全局随机池
- 按 `B` 键打开方块池界面：原版物品渲染器显示所有已注册方块图标，**不依赖 JEI/REI**；网格会按窗口大小自适应铺满，支持分页、按 id、英文名或当前资源包的中文名搜索，点击图标多选，可全选结果、清空选择并一次提交
- 界面只读/可编辑状态由 `blocksPermissionLevel` 决定；服务端会一次性校验并原子保存，失败时不会留下半套配置
- 支持列出、分页、添加、移除、清空（二次确认）和恢复默认
- 方块以 `命名空间:路径` 字符串保存；模组暂时移除时该 id 会保留，恢复模组后自动重新生效
- 无效/缺失的 id 会在 `blocks list` 中或界面内标出，不会导致模组或服务器启动失败；若池中没有任何可用方块，`/start` 会拒绝启动
- 随机池上限 4096 种方块

### 倒计时持久化
- 运行状态与剩余时间保存到独立的状态文件，退出存档后不丢失
- 服务器关闭时自动保存，重新进入自动恢复
- 运行中每 10 秒落盘一次，避免每秒写盘

### 配置系统
- 支持 Cloth Config API 配置界面（可选依赖，仅用于调整变换间隔）
- 全局设置与初始默认区域位于 `config/randomskyblockisland.json`
- 运行状态位于 `config/randomskyblockisland.state.json`
- 存档默认区域与额外区域位于 `<存档>/rsl_areas.json`
- 旧版配置会自动迁移并生成 `randomskyblockisland.v1.bak.json` 备份

### 本地化支持
- 支持英文和中文
- 所有玩家可见的文本都已本地化

## 命令

### 游戏控制
| 命令 | 说明 | 权限 |
|------|------|------|
| `/randomskyblock start` | 开始随机变换 | 4 |
| `/randomskyblock stop` | 停止随机变换 | 4 |
| `/randomskyblock reset` | 重置游戏状态（保留区域和方块配置） | 4 |
| `/randomskyblock status` | 查看运行状态、倒计时、区域与方块池统计 | 无 |
| `/randomskyblock reload` | 从磁盘重载配置与区域（填充进行中时拒绝） | 4 |

### 区域管理
| 命令 | 说明 | 保存位置 |
|------|------|---------|
| `/randomskyblock area add <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>` | 添加新区域 | 存档级 |
| `/randomskyblock area remove <name>` | 删除区域 | 存档级 |
| `/randomskyblock area rename <oldName> <newName>` | 重命名区域（默认区域也可重命名） | 存档级 |
| `/randomskyblock area list [page]` | 分页列出当前存档的所有区域（每页 8 条） | - |
| `/randomskyblock areas [page]` | 查询当前存档/服务器的所有区域，含默认/命名/无效标记、体积和锁定维度（每页 8 条） | 4 |
| `/randomskyblock area show` | 显示当前存档的默认区域 | - |
| `/randomskyblock area pos1` | 设置当前存档默认区域的位置1 | 存档级 |
| `/randomskyblock area pos2` | 设置当前存档默认区域的位置2 | 存档级 |
| `/randomskyblock area setfrompos <name>` | 从当前默认区域坐标创建新区域 | 存档级 |
| `/randomskyblock area initial show` | 显示全局初始默认区域 | - |
| `/randomskyblock area initial pos1` | 设置全局初始默认区域的位置1 | 全局级 |
| `/randomskyblock area initial pos2` | 设置全局初始默认区域的位置2 | 全局级 |
| `/randomskyblock area resetdefault` | 将当前存档默认区域重置为全局初始默认区域 | 存档级 |

> 区域管理命令（`/randomskyblock area ...`）需要权限等级 4。除 `area initial ...` 外，存档级区域命令需要当前存档已加载，否则会提示 `world_not_initialized`。运行中从非锁定维度执行 `start`、`reset`、`delay` 或区域修改会被拒绝；`area show` / `area list` 仍可只读查看。

### 选区魔杖
| 命令 | 说明 | 权限 |
|------|------|------|
| `/randomskyblock wand` | 获取一把选区魔杖（木棍纹理，最多 1 把） | 4 |

- 手持魔杖右键两个点选择区域，第三次右键打开确认界面；确认后添加为命名区域
- 没有正在选择时，直接右键一个已存在的命名区域，会打开该区域的编辑界面：可改名、删除，或点击“重新选点”后重新右键两个点再保存坐标
- 编辑目标按体积最小的命名区域匹配；默认区域不参与匹配（避免误改默认区域）
- Shift + 右键清空当前选择；所有坐标和重名检查都在服务端完成
- 界面关闭时若操作尚未提交，会自动取消本次选择

### 延迟设置
| 命令 | 说明 | 权限 |
|------|------|------|
| `/randomskyblock delay <seconds>` | 设置变换间隔（1-3600 秒） | 4 |
| `/randomskyblock delay show` | 显示当前变换间隔 | 4 |

### 方块池管理
| 命令 | 说明 | 权限 |
|------|------|------|
| `/randomskyblock blocks list [page]` | 分页列出随机池（每页 20 条），标出缺失 id | 默认 2 |
| `/randomskyblock blocks add <id>` | 添加方块，例如 `minecraft:oak_log`、`mymod:my_block` | 默认 2 |
| `/randomskyblock blocks remove <id>` | 从随机池移除方块 | 默认 2 |
| `/randomskyblock blocks clear` | 清空前的提示（不会直接清空） | 默认 2 |
| `/randomskyblock blocks clear confirm` | 确认清空随机池（运行中拒绝） | 默认 2 |
| `/randomskyblock blocks reset` | 恢复内置的 269 种方块 | 默认 2 |

> 方块池命令的权限由配置项 `blocksPermissionLevel` 控制，默认 `2`（OP）。设为 `0` 后普通玩家也可管理方块池；修改文件后需执行 `/randomskyblock reload` 或重启服务器生效。`status` 对所有玩家开放。

## 配置文件

### 全局配置（`config/randomskyblockisland.json`）

存储全局共享的设置。其中 `defaultArea` 是**初始默认区域**：只在某个存档还没有自己的默认区域时用于播种；之后该存档的默认区域独立保存在 `<存档>/rsl_areas.json`，不会随全局值变化。

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

- `broadcastMode`：`all`（默认，所有玩家）、`nearby`（区域中心 `broadcastRadius` 格内）或 `summary`（所有区域填完后合并一条消息）
- `maxAreaVolume`：单个区域体积上限，默认 65536
- `blocksPerTick`：每 tick 最多填充的方块数，默认 8192

### 运行状态（`config/randomskyblockisland.state.json`）

```json
{
  "isRunning": false,
  "secondsRemaining": 0
}
```

### 存档区域配置（`<存档>/rsl_areas.json`）

存储每个存档独立的默认区域、额外区域和当前锁定维度。旧版文件没有 `defaultArea` 时，会自动以全局初始默认区域播种并写回；没有 `activeDimension` 时视为未锁定：

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

## 安装

### Fabric
1. 下载 Fabric 版 `.jar` 文件
2. 将其放入 `.minecraft/mods` 文件夹
3. 确保已安装 Fabric Loader 和 Fabric API
4. 可选安装 Cloth Config API 和 Mod Menu 以使用配置界面

### Forge
1. 下载 Forge 版 `.jar` 文件
2. 将其放入 `.minecraft/mods` 文件夹
3. 确保已安装对应版本的 Forge
4. 可选安装 Cloth Config API 以使用配置界面

## 前置依赖

### Fabric

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| Fabric Loader | >=0.16.2 | 必须 |
| Fabric API | * | 必须 |
| Cloth Config API | >=11.1.136 | 可选（仅配置界面） |
| Mod Menu | >=7.2.2 | 可选（模组菜单入口） |

### Forge

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| Forge | 47.4.22 (1.20.1) | 必须 |
| Cloth Config API | >=11.1.136 | 可选（仅配置界面） |

## 已知限制

- 同一存档同一时间只允许一个维度运行玩法；区域坐标维度无关，切换维度后按目标维度高度裁剪
- 配置界面仅支持调整变换间隔；区域通过命令或选区魔杖管理，方块池可用 `B` 键界面或命令管理
- 单个区域体积上限默认 65536；随机池上限 4096
- 填充进行中关闭服务器会留下部分区域未完成（不持久化填充队列）
- 方块池中的模组方块要求服务端和客户端都安装对应模组，否则客户端无法正确显示

## 构建

### Fabric
```bash
./gradlew build
```

### Forge
```bash
cd forge && ./gradlew build
```

### 测试（公共逻辑与一致性）
```bash
./gradlew test
```

首次构建需要联网下载 Fabric/Forge 与 Maven 依赖。Fabric 构建使用 JDK 21 作为 Gradle JVM（Loom 要求），产物编译目标为 Java 17。

## 许可证

MIT License - 详见 [LICENSE](LICENSE)
