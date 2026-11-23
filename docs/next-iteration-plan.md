# Random SkyBlock Island 下一阶段规划（维度玩法锁定 / 选区物品 / 方块池界面）

> 基线：当前双端已完成配置 v2、按存档独立默认区域、方块池、批量填充、`status`/`reload`。
> 当前构建：Fabric `42 tests / 8 suites / 0 failures`，双端 clean build 通过。
> 本文档是下一阶段设计与明日执行计划；本轮只新增文档，不改代码。

## 0. 目标与成功标准

### 目标

1. **维度玩法锁定**：随机变化可发生在主世界、下界、末地或模组维度；但一个存档的一次玩法只能锁定一个维度。其他维度执行玩法指令时，提示当前启用维度，并要求前往该维度或关闭玩法。
2. **选区物品**：用 `rsl:selection_wand` 右键选点、确认新添/删除区域，替代长期依赖 `/area pos1`、`/area pos2` 的坐标输入方式；命令保留为备用。
3. **方块池界面**：保留现有指令；新增快捷键界面，可列出全部已注册方块（含模组方块），像 JEI 一样显示方块图标，支持中英文搜索、资源包中文翻译检索、点击快速添加、多选批量添加/移除。

### 成功标准

- 三个功能在 Fabric 与 Forge 行为一致，专用服务器不加载客户端类。
- 旧 `rsl_areas.json`、旧全局配置无需手动迁移即可使用。
- 维度锁定、选区物品、方块池 UI 都有明确的失败提示，不产生半写入或跨存档污染。
- Fabric 单测通过，双端 clean build 通过；至少完成一次单人/专用服务器手测矩阵。

## 1. 现状与约束（已核对代码）

| 事实 | 位置/影响 |
|---|---|
| `AreaSpec` 只有 name/六个坐标，没有维度字段 | `common/src/main/java/yifei/rsl/common/config/AreaSpec.java:15-21` |
| 随机变换固定取主世界 | `src/main/java/yifei/rsl/game/IslandGame.java:153`，Forge 对称 |
| 每存档区域文件已有 `defaultArea` 与 `areas` | `common/src/main/java/yifei/rsl/common/config/AreasFile.java:11-18` |
| 每存档默认区域已独立，全局 `defaultArea` 仅初始值 | 当前已实现 |
| 运行状态/倒计时仍在全局 `randomskyblockisland.state.json` | 切存档时存在状态串用风险，本次只做安全兜底 |
| 没有物品、快捷键、网络包 | 全仓库 grep 无 `Item`/`KeyBinding`/`Payload`/`SimpleChannel` 命中 |
| 现有客户端代码只有 Cloth 配置界面 | `.../config/ModConfigScreen.java` |
| 双端共享纯 Java 层 | `common/src/main/java`，两端 Gradle `sourceSets` 引入 |
| 技术基线 | MC 1.20.1、Java 17、Fabric API、Forge 47.4.22；不引入 Architectury |

### 本次采用的关键假设

- “一次玩法”定义为 `/randomskyblock start` 到 `/randomskyblock stop` 之间。
- 区域坐标是**维度无关的模板**：锁定哪个维度，这些坐标就在哪个维度生效；不新增“每维度独立区域集”。
- `/stop`、`/status`、`/reload`、`/blocks ...` 允许在任意维度执行；`start`、`reset`、`delay`、`area` 的存档级操作受维度锁定限制。
- 控制台命令视为管理员操作，绕过玩家维度限制；`/start` 无玩家来源时使用当前 active 维度，否则主世界。
- 方块池界面默认快捷键 `B`，可在原版“控制”里改键。

## 2. 功能 A：维度玩法锁定

### A1. 数据模型

在每存档 `rsl_areas.json` 增加可选字段，不提升 `configVersion`：

```json
{
  "configVersion": 2,
  "activeDimension": "minecraft:the_nether",
  "defaultArea": { "name": "default", "minX": -1, "...": "..." },
  "areas": []
}
```

- `activeDimension`：`namespace:path`；仅在玩法运行/暂停恢复期间存在；`/stop` 后清空。
- 旧文件没有该字段时为 `null`，按“未锁定”处理。
- 计时状态仍是全局 `state`；若 `state.isRunning=true` 但 `activeDimension` 缺失或维度已不存在，启动时安全停止并提示，不猜测维度。

### A2. 状态机

| 状态 | activeDimension | 行为 |
|---|---|---|
| Idle | null | `/start` 以执行者所在维度锁定；控制台用主世界 |
| Running | D | 仅 D 内允许玩法操作；其他维度只允许 stop/status/reload/blocks |
| Stopping | null | `/stop` 清空并保存；倒计时清零 |
| Restart | D | 存档加载时若 `isRunning` 且 D 存在，恢复到 D；否则停止并提示 |

`/reset` 等同先 stop 再重置；必须清空 activeDimension。

### A3. 区域与维度

- 区域仍只存坐标；启动时用 activeDimension 对应的 `ServerWorld`/`ServerLevel` 填充。
- 用 `FillCursor(area, world.getBottomY(), world.getTopY())` 做高度裁剪；若所有区域都完全在维度高度之外，拒绝启动并提示，而不是静默空转。
- 下界/末地高度范围不同，必须在启动前校验“至少一个区域与目标维度高度相交”。
- 不处理跨维度边界/世界边界传送；只保证变换世界正确。

### A4. 命令与提示

| 命令 | 其他维度执行 |
|---|---|
| `/randomskyblock start` | 拒绝，提示 active 维度 |
| `/randomskyblock stop` | 允许（紧急关闭） |
| `/randomskyblock reset` | 拒绝 |
| `/randomskyblock delay ...` | 拒绝 |
| `/randomskyblock area add/remove/rename/pos1/pos2/setfrompos/resetdefault` | 拒绝 |
| `/randomskyblock area initial ...` | 允许（只改全局初始值） |
| `/randomskyblock area show/list` | 建议拒绝，避免误读当前维度区域 |
| `/randomskyblock blocks ...` | 允许（方块池是全局设置） |
| `/randomskyblock status` | 允许，显示 active 维度 |
| `/randomskyblock reload` | 允许；若运行中 active 维度失效则拒绝并保留旧配置 |
| `/randomskyblock wand` | 允许发放，但物品使用受维度限制 |

提示键建议：

- `rsl.message.dimension_locked`：`本玩法已在 %s 启用；请前往该维度游玩，或执行 /randomskyblock stop 关闭玩法。`
- `rsl.message.dimension_restore_failed`
- `rsl.message.dimension_missing`
- `rsl.message.dimension_status`

维度显示名用 `dimension.<namespace>.<path>` 翻译键；缺失时回退到 id。

### A5. 平台实现点

- `AreasFile` 增加 `public String activeDimension;`，`copy()`/`sanitize()` 同步。
- `ModConfig` 增加：`getActiveDimension()`、`hasActiveDimension()`、`setActiveDimension(String)`、`clearActiveDimension()`、`isActiveDimension(ServerWorld/ServerLevel)`。
- `IslandGame.start(ServerWorld)` 代替无参 `start()`；`StartCommand` 传入玩家世界。
- `IslandGame.transformIsland` 用 activeDimension 解析世界；世界缺失时 stop 并提示。
- `DimensionGuard`（平台层）：统一做玩家维度、权限、存档加载检查，避免每个命令重复。
- `status` 增加 active 维度行；`reload` 前后保留/回滚 activeDimension。

### A6. 验收

- 主世界 `/start` 后，主世界正常变换；下界玩家执行 `/start`、`/area`、`/delay` 被拒绝并看到 active 维度。
- 下界 `/start` 后只在对应下界区域变换；末地玩家 `/start` 被拒绝。
- 其他维度 `/stop` 能关闭；`/status` 显示 active 维度。
- 服务器重启后能恢复到 active 维度；存档切换不会把旧维度带入新存档。
- active 维度被移除（模组卸载）时安全停止并提示。

## 3. 功能 B：选区物品

### B1. 物品

- ID：`rsl:selection_wand`，最大堆叠 1，不消耗。
- 获取：`/randomskyblock wand`（权限 4）；后续可加入创造模式物品栏，本期不做。
- 资源：`assets/rsl/models/item/selection_wand.json`、`assets/rsl/textures/item/selection_wand.png`；Fabric/Forge 各自注册。
- 纹理：以原版 `minecraft:item/stick` 为基底派生 16×16 贴图（棕色木杆 + 发光端/符文），不覆盖原版贴图，资源包可覆盖；模型用 `item/handheld`，第一版可先用木棍贴图占位。
- NBT：`rsl:pos1`、`rsl:pos2`（int array）、`rsl:dim`（string）。

### B2. 交互流程

1. 物品右键方块：
   - 未选 pos1：记录 pos1；actionbar 显示坐标。
   - 已有 pos1、无 pos2：记录 pos2；显示体积；超过 `maxAreaVolume` 时提示但保留选择。
   - 已有两个点：打开“区域确认”界面。
2. `Shift + 右键`：清空两个点，actionbar 提示。
3. 右键点在空中：只提示当前选择，不打开界面。
4. 确认界面：
   - 显示坐标、体积、默认名称（`area_1`、`area_2`…）。
   - “添加区域”：调用现有 `addArea` 逻辑。
   - “删除区域”：仅当右键目标位于某命名区域内时可用；该区域名作为删除目标。
   - “取消”。
5. 选区轮廓：客户端渲染两点包围盒（绿色/红色表示体积合法/超限）；MVP 可先只在 actionbar 显示，轮廓作为当天可选。

### B3. 服务端校验

- 手持物品、权限 4、当前维度与 active 维度一致。
- 两个点必须在同一维度；`rsl:dim` 与当前维度不一致时清空并要求重选。
- 名称合法、坐标合法、体积在 `maxAreaVolume` 内、不重复。
- 添加/删除后 `saveSaveAreas()` 失败必须回滚内存并提示 `area_save_failed`。
- 客户端界面只发请求，不直接改服务端配置；服务端重算体积与名称。

### B4. 网络与界面

- `WandActionC2S`：action（ADD/DELETE）、name、min/max、dimension、targetArea。
- `WandActionResultS2C`：success、messageKey、saved area。
- 客户端 `AreaWandScreen` 只负责输入与展示；错误时保留界面。
- Fabric 用 `UseBlockCallback` + `ClientPlayNetworking`/`ServerPlayNetworking`；Forge 用 `PlayerInteractEvent.RightClickBlock` + `SimpleChannel`。
- 物品右键取消原版放置/使用行为，避免误放方块。

### B5. 验收

- 生存模式拿到魔杖，右键两次选点，第三次打开界面，命名后添加成功。
- 超体积、重名、空名、跨维度选择都有明确错误。
- 右键已存在区域内部可删除；默认区域受保护。
- 两个维度分别选择不会串数据。
- 未加载存档/无权限时有提示，不打开可写界面。

## 4. 功能 C：方块池界面

### C1. 入口与布局

- 快捷键：`key.rsl.block_pool`，默认 `B`，原版控制可改；仅在游戏内、无其他界面时打开。
- 备选入口：`/randomskyblock blocks gui` 或配置界面按钮；本期至少保证快捷键。
- 布局：
  - 顶部：标题、搜索框、筛选（全部 / 已在池 / 未添加）、按模组筛选。
  - 中部：可滚动网格，9 列 × 5 行（可随 GUI 缩放调整），每格显示方块图标、名称 tooltip、池内勾选/选中高亮。
  - 底部：结果数、已选数、“添加选中”“移除选中”“全选当前结果”、关闭。
  - 只读模式：无 `blocksPermissionLevel` 时按钮禁用，顶部显示“只读：需要权限 N”。

### C2. 数据同步

- 打开界面：C2S `PoolRequest`；S2C `PoolSnapshot`（全部允许 id、池上限、canEdit、权限等级）。
- 修改：C2S `PoolMutation`（add 列表、remove 列表，单包最多 512 个 id）；S2C `PoolUpdate`（最新快照、失败 id/原因）。
- 服务端在服务器线程执行；新增 `ModConfig.applyBlockMutations(add, remove)`，一次校验、一次 `saveSettings()`，失败整体回滚。
- 权限、池上限 4096、非法 id、空气方块、重复项全部服务端判定；客户端只做预校验。
- 每玩家限流（建议 250ms/次），防止连点刷包。

### C3. 方块枚举与图标

- 不引入 JEI/REI 依赖；只枚举客户端 `Registries.BLOCK` / `ForgeRegistries.BLOCKS` 的全部非空气方块，并用原版 `ItemStack` / `BlockRenderManager` 渲染，效果类似 JEI，但零硬依赖。
- 图标优先用方块物品 `ItemStack` 的 GUI 渲染（最接近 JEI 的 3D 物品模型）；没有物品的方块回退到 `BlockRenderManager.renderSingleBlock`。
- 缓存每个方块的图标；只渲染可见格子；搜索/翻页不重复建资源。
- 识别失败的方块显示占位图标与 id，不崩溃。

### C4. 搜索与中英文

- 建立索引字段：`namespace:path`、path、当前语言名称、`zh_cn`/`zh_tw` 名称、模组名。
- 当前语言名称用 `I18n`/`Component` 读取。
- 中文检索：从资源包读取所有 `lang/zh_cn.json`、`lang/zh_tw.json`，合并成 `descriptionId -> 中文名`；按资源包优先级覆盖。这样即使客户端语言是英文，也能用中文搜索到资源包提供的中文翻译。
- 搜索规则：忽略大小写、空格分词、多个词 AND、子串匹配；中文按原文子串匹配；本期不做拼音。
- 显示名称仍用当前语言；tooltip 同时显示 id 与中文名（若与当前名不同）。
- 资源包/语言切换后，下次打开界面重建索引。

### C5. 点击、多选与批量

- 左键单击：快速添加/移除该方块（满足“图标点击快速添加”）。
- `Ctrl + 左键`：切换多选。
- `Shift + 左键`：从上次点击项范围多选。
- “全选当前结果”“清空选择”。
- “添加选中”“移除选中”批量提交，最多 512/包；结果失败项单独提示。
- 池内方块显示勾选角标；选中项显示蓝框；已达 4096 上限时禁止添加并提示。

### C6. 性能与边界

- 仅渲染可见格子；图标缓存 LRU 512；搜索输入 100ms 防抖。
- 大包（数千方块）不一次性构建纹理，按需渲染。
- 客户端没有对应模组时不会看到该方块；服务端仍以自身注册表校验。
- 界面打开期间池被命令修改：关闭/重开刷新；不做实时推送。
- 断开连接、切换语言、窗口缩放、全屏切换不崩溃。

### C7. 验收

- 快捷键打开界面，列出原版 + 已装模组方块；图标可辨认。
- 英文界面输入中文能搜到资源包有 `zh_cn` 翻译的方块；中文界面输入英文 id 也能搜到。
- 单击添加、Ctrl 多选、Shift 范围选、批量添加/移除均生效并持久化。
- 无权限只读；池满、非法 id、服务端保存失败有错误提示。
- 4096 方块下界面可滚动、搜索无明显卡顿。

## 5. 技术拆分与改动面

### 5.1 common（纯逻辑，优先）

- `AreasFile.activeDimension`（可选字符串）。
- `DimensionId`：`namespace:path` 解析/校验/显示名键。
- `WandSelection`：两个点 + 维度 + 体积/合法性（纯数据）。
- `BlockPoolSnapshot`、`PoolMutationResult`：网络 DTO 的纯数据部分。
- `LanguageIndex`：输入 `Map<String,String>` 中文翻译表，提供搜索匹配；资源读取留在平台层。
- 单测：activeDimension 往返/旧文件兼容、维度 id 校验、选区体积、搜索索引中英文/多词/空查询。

### 5.2 Fabric

- 物品注册、`UseBlockCallback`、`AreaWandScreen`、选区轮廓渲染。
- `KeyBindingHelper` 注册 `B`；`BlockPoolScreen`。
- `PayloadTypeRegistry` 注册 `PoolRequest`/`PoolSnapshot`/`PoolMutation`/`PoolUpdate`/`WandAction`；`ServerPlayNetworking` 处理。
- 客户端类全部只在 client 入口/客户端事件中加载；专用服务器不解析。

### 5.3 Forge

- `DeferredRegister<Item>` 或 `RegistryObject` 注册魔杖；`PlayerInteractEvent.RightClickBlock`。
- `RegisterKeyMappingsEvent` 注册快捷键；`BlockPoolScreen`。
- `SimpleChannel` 注册同套消息；服务端 handler 切服务器线程。
- `DistExecutor`/客户端事件隔离专用服务器。

### 5.4 配置/存档/语言

- `rsl_areas.json` 增加 `activeDimension`，不升版本，旧文件兼容。
- 新增语言键（中英同步）：维度锁定/恢复失败/状态、魔杖发放/选点/清除/添加/删除/失败、界面标题/搜索/筛选/只读/权限/池满/选中/批量结果等。
- README/README_EN 增加维度限制、魔杖、快捷键界面、权限与资源包中文检索说明。

## 6. 明日执行计划（建议）

> 三个功能体量不同：A 可在半天内落地；B 约 1 天；C 是多日任务。明天优先保证 A 完成、B 可用、C 有可运行骨架。

### 上午 09:00–12:00

1. common 增加 `activeDimension`/`DimensionId`；Fabric/Forge `ModConfig` 读写与启动恢复。
2. `IslandGame.start(ServerWorld)` + `transformIsland` 按 active 维度取世界；区域高度校验。
3. `DimensionGuard` 接入 start/reset/delay/area；`status` 显示维度；补语言与测试。
4. 双端 build/test，先交付 A。

### 下午 13:00–15:00

5. 注册 `rsl:selection_wand`（双端）+ 模型/贴图 + `/randomskyblock wand`。
6. NBT 选点、右键流程、维度/权限守卫、actionbar；先不做 GUI，验证选区数据。

### 下午 15:00–17:30

7. `AreaWandScreen` + `WandAction` 包：添加/删除区域、保存失败回滚。
8. 若时间允许，选区包围盒轮廓。

### 傍晚 17:30–18:00

9. C 的协议与界面骨架：`BlockPoolScreen` + `PoolSnapshot` + 列表/图标渲染；搜索/多选排到后续。
10. 双端 build/test，记录未完成项。

### 后续 2–4 天（C 的主体）

- D2：完整方块枚举、图标缓存、滚动网格、单击快速添加。
- D3：中英文搜索、资源包 `zh_cn`/`zh_tw` 索引、筛选/模组分组。
- D4：多选/范围选/批量提交、只读权限、限流、性能与边界测试。
- D5：README、语言、验收矩阵、双端发布构建。

## 7. 测试与验收

### 自动测试

- common：维度 id、`AreasFile.activeDimension` 迁移、`WandSelection` 体积/维度、`LanguageIndex` 中英文搜索、`BlockPoolSnapshot` 编解码。
- 两端：命令/语言键一致性（现有 ParityTest 扩展新 literal/argument）。
- Fabric `gradlew test`；双端 `clean build`。
- 新增网络 DTO 的纯编码测试；GUI 渲染做手动/客户端验证。

### 手动矩阵

- 单人：主世界/下界/末地各启动一次；跨维度指令；重启恢复；切存档。
- 专用服务器：无客户端类加载；权限 0/2/4；魔杖发放与使用；UI 添加/移除。
- 资源包：英文语言 + 中文资源包；中文搜索；无 `zh_cn` 时回退。
- 大批量：4096 池、数千方块列表、多选 512 个、保存失败注入。

## 8. 风险与待确认

### 风险

- 资源包语言加载顺序/API 在 Fabric/Forge 映射差异；需要先做 1 个最小客户端实验。
- `BlockRenderManager` 3D 图标在 GUI 中的光照/缩放/性能；必要时退回物品模型图标。
- 网络包在双端的注册时序（客户端未装模组/连接中）需要防御。
- 快捷键 `B` 可能与其他模组冲突；提供改键与 `/blocks gui` 备选。
- activeDimension 与全局计时状态不完全一致；本次用安全停止兜底，后续可把运行状态迁移到每存档。
- 专用服务器解析客户端类：必须用入口隔离/DistExecutor，不能只靠 `@SideOnly` 思路。

### 待确认（如无异议按假设执行）

1. 区域坐标维度无关、锁定维度决定生效世界（当前假设）。
2. `/stop` 允许跨维度；`start/reset/delay/area` 不允许。
3. 控制台绕过维度限制。
4. 魔杖通过 `/randomskyblock wand` 发放，暂不加创造物品栏。
5. 方块池 UI 默认 `B`，单击快速添加，Ctrl/Shift 多选。
6. 中文检索只做资源包 `zh_cn`/`zh_tw` 子串匹配，不做拼音。

## 9. 明确不做

- 每维度独立区域集、每维度独立计时/方块池。
- 拼音搜索、模糊拼音、同义词。
- JEI/REI 硬依赖或复用其内部渲染器。
- 每区域权重、每区域独立方块池、撤销/回滚历史。
- 完整的区域可视化编辑器（只做魔杖选点 + 确认界面）。
- 本次不修改现有命令语义，不提交 git（除非另行要求）。
