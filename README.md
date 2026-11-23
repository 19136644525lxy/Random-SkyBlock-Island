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
- 按 `B` 键打开方块池界面（键位可改，见下方「界面与按键」）：原版物品渲染器显示所有已注册方块图标，**不依赖 JEI/REI**；网格会按窗口大小自适应铺满，支持分页、按 id、英文名或当前资源包的中文名搜索，点击图标多选，可全选结果、清空选择并一次提交
- 界面底部“查看当前池/显示全部方块”按钮可在完整方块列表与当前随机池之间切换；当前池视图可直接点击方块标记删除，支持“全选待删”，删除整个池需要再次确认；搜索、分页始终可用
- “不想看见”进入隐藏多选：选好后再次点击即可从本客户端的方块池界面隐藏这些方块（不删除模组、不改服务端随机池）；“不可见方块池”列出隐藏项，点击恢复显示；“刷新”重读隐藏列表并重新拉取服务端快照，界面会显示刷新中，成功后显示刷新成功，超时给出提示。分页箭头在首页/末页自动禁用，右上角提供关闭按钮。隐藏列表按存档/服务器分别保存在客户端 `config/rsl-hidden-blocks/<上下文哈希>.json`；升级后首次打开某个存档/服务器时，会读取旧版 `config/rsl-hidden-blocks.json` 作为初始列表并写入该上下文（旧文件保留，其他上下文首次打开也会继承）
- 按 `0` 打开玩法菜单（键位可改，见下方「界面与按键」）：提供开始、停止、重置、选区魔杖、区域列表、授权权限、延迟设置、查看状态和关闭；菜单按钮与命令等价，结果直接显示在菜单下方，命令仍可直接输入，也不会改变选区魔杖的右键选点流程
- 鼠标悬停方块图标时会有轻微上浮、脉冲高亮和光晕动画；标题、搜索、统计和底部操作区分层显示
- 界面是否可打开、可编辑由独立的方块池 ACL 决定，**不检查原版权限等级**；4 级 OP 未被授权时同样无法打开。服务端在发送快照和接受修改前都会校验 ACL，未授权时关闭界面
- 授权由房主/服主/控制台通过 `/randomskyblock access ...` 管理；服务端会一次性校验并原子保存，失败时不会留下半套配置
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

## 界面与按键

模组的所有界面默认用快捷键打开，键位可在「选项 → 控制 → 按键绑定 → 随机空岛」中修改：

| 按键 | 功能 | 打开的界面 |
|------|------|-----------|
| `B` | 打开方块池 | 方块池界面 |
| `0` | 打开玩法菜单 | 玩法菜单（随机空岛玩法） |

### 玩法菜单（`0`）

游戏内操作面板，按钮与命令等价，操作结果显示在面板下方：

- **第一排**：开始 / 停止 / 重置
- **第二排**：选区魔杖（获取魔杖）/ 区域列表（打开区域列表界面）/ 授权权限（打开方块池授权界面）
- **第三排**：延迟输入框 + 设置延迟 / 查看状态 / 关闭
- **授权权限**：进入在线玩家列表，可多选玩家，再选择「授权编辑 / 授权所有者 / 撤销」，执行前需要二次确认（与 `/randomskyblock access ...` 等价）

### 方块池界面（`B`）

用于选择随机方块池。底部按钮在宽屏是一排、窄屏自动分为两排，从左到右依次是：

- **全选结果**：选中当前筛选出的全部方块；**清空选择**：取消所有选中；**应用更改**：提交本次修改
- **查看当前池**：在完整方块列表与当前随机池之间切换；当前池视图里可直接点击方块标记删除
- **不想看见**：进入隐藏多选，选好后再次点击即可从本客户端界面隐藏这些方块（不删除模组、不改服务端随机池）
- **不可见方块池**：列出该存档/服务器已隐藏的方块，点击恢复显示
- **刷新**：重读隐藏列表并重新拉取服务端快照
- **隐藏池**：打开隐藏池管理界面，可给当前隐藏池重命名，或从其他存档的隐藏池复制一份过来
- 分页箭头在首页/末页自动禁用，右上角提供关闭按钮

打开方块池前会先向服务端请求快照：没有权限时只在快捷栏提示「没有方块池权限」，不会再闪一下界面。

### 选区魔杖

选区魔杖不是界面快捷键，而是一个物品：在玩法菜单或 `/randomskyblock wand` 获取后，手持右键选择两个点，第三次右键打开确认界面（详见下节「选区魔杖」命令说明）。

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
| `/randomskyblock blocks list [page]` | 分页列出随机池（每页 20 条），标出缺失 id | ACL Editor |
| `/randomskyblock blocks add <id>` | 添加方块，例如 `minecraft:oak_log`、`mymod:my_block` | ACL Editor |
| `/randomskyblock blocks remove <id>` | 从随机池移除方块 | ACL Editor |
| `/randomskyblock blocks clear` | 清空前的提示（不会直接清空） | ACL Editor |
| `/randomskyblock blocks clear confirm` | 确认清空随机池（运行中拒绝） | ACL Editor |
| `/randomskyblock blocks reset` | 恢复内置的 269 种方块 | ACL Editor |

### 方块池授权
| 命令 | 说明 | 权限 |
|------|------|------|
| `/randomskyblock access list` | 列出方块池 ACL | Owner / 控制台 / 房主 |
| `/randomskyblock access grant <player>` | 授予 Editor（可打开并编辑） | Owner / 控制台 / 房主 |
| `/randomskyblock access grantowner <player>` | 授予 Owner（可继续授权） | Owner / 控制台 / 房主 |
| `/randomskyblock access revoke <player>` | 撤销授权（房主与控制台始终保留） | Owner / 控制台 / 房主 |

> 方块池 ACL 与 `/randomskyblock blocks ...` 完全独立于原版权限等级：即使玩家是 4 级 OP，未在 ACL 中也不能打开界面或执行 `blocks` 命令。控制台与单机房主始终拥有 Owner。其他游戏/区域命令仍使用 4 级权限。`status` 对所有玩家开放，并显示当前玩家的方块池权限级别。

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

Apache License 2.0 - 详见 [LICENSE](LICENSE)
