# AGENTS.md

## 全局规则

- 所有内部思考、分析、规划过程必须使用中文。
- 对用户的最终说明默认使用中文；代码、命令、标识符、日志原文保持原样。
- 本规则优先于模型默认的英文思考习惯；英文仅允许出现在代码、命令、标识符、日志原文、外部资料引用中。
- 如果发现自己在用英文思考，应立即改用中文继续，不需要向用户解释切换过程。

## 项目速览

- 项目：`F:/Work/Q/random-skyblock-island-template-1.20.1`，Minecraft 1.20.1，Fabric + Forge 双端。
- 共享逻辑在 `common/src/main/java/yifei/rsl/common/`，纯 Java，禁止 `import net.minecraft.*`。
- 两端适配层：`src/main/java/yifei/rsl/`（Fabric）、`forge/src/main/java/yifei/rsl/`（Forge）；不使用 Architectury。
- 当前基线：`main @ 221e51d`；工作区有未提交改动时先看 `git status`，不要擅自 commit。
- 详细经验：`docs/开发经验.md`；命令与配置：`README.md`。

## 构建与验收

- Gradle 运行 JVM 必须是 JDK 21；根 `gradle.properties` 固定 `org.gradle.java.home=C:/Program Files/Java/jdk-21`，项目编译 release 为 17。
- Fabric：`.\gradlew.bat clean test build --console=plain`
- Forge：`cd forge; .\gradlew.bat clean build --console=plain`
- 当前基线：Fabric 88 tests、16 个 test XML、0 failures；Forge build 通过，仅有 `ResourceLocation(String,String)` 弃用警告。
- 只改 common 也要跑 Forge build；新增命令/语言键要跑 `ParityTest`。

## 目录与数据

- 配置：`config/randomskyblockisland.json`（设置/初始默认区域/方块池/ACL）、`config/randomskyblockisland.state.json`（运行状态）。
- 存档：`<存档>/rsl_areas.json`（每存档默认区域、命名区域、`activeDimension`）。
- 客户端隐藏池：`config/rsl-hidden-blocks/<上下文哈希>.json`；旧 `rsl-hidden-blocks.json` 只作迁移种子。
- 内置方块池：`common/src/main/resources/rsl/default_blocks.json`，当前 269 项。

## 双端同步约定

- 新命令/语言键/网络包/配置字段必须 Fabric 与 Forge 成对修改。
- 映射差异：`Commands`/`CommandManager`、`CommandSourceStack`/`ServerCommandSource`、`Component`/`Text`、`getPlayerOrException()`/`getPlayerOrThrow()`、`ServerLevel`/`ServerWorld`、`FMLPaths.CONFIGDIR`/`FabricLoader.getConfigDir()`。
- Forge 客户端类必须用 `DistExecutor` 或客户端入口隔离，避免专用服务器解析。
- 语言文件四份（两端 zh_cn/en_us）都要同步；`ParityTest` 会检查键集合与命令 literal/argument。

## 容易踩的坑

- 不要恢复“每秒全量写配置”；设置变更才写 settings，状态每 10 秒写 state。
- 空方块池时 `/start` 必须拒绝；`transform` 也要守卫，不能 `nextInt(0)`。
- 区域坐标统一走 `AreaSpec.normalized()`；体积用 `long` 饱和，别用 int。
- 方块池 ACL 独立于原版 OP 4：`RslSettings.poolAccess` + `PoolPermissions`，不要重新引入 `blocksPermissionLevel`。
- 隐藏列表是客户端显示过滤，不写入服务端 settings，不影响随机池。
- 维度锁定用 `<存档>/rsl_areas.json` 的 `activeDimension`；找不到世界时提示 `dimension_missing` 并停止，不要静默回主世界。
- 写配置失败要回滚内存并返回失败；`reload` 失败要恢复 settings/areas/activeDimension。
- Fabric `Screen` 用 `close()`，不是 `onClose()`；Forge `ResourceLocation(String,String)` 只是弃用警告。
- 不要把 `libs/` 本地 jar 重新加回编译依赖；干净克隆要能靠 Maven 构建。

## 交付前检查

- [ ] Fabric `clean test build` 通过
- [ ] Forge `clean build` 通过
- [ ] 两端语言键/命令树一致
- [ ] 旧配置/旧区域/旧隐藏列表迁移路径有测试或手测
- [ ] 客户端类未进入专用服务器路径
- [ ] 不提交 `build/`、`.gradle/`、本地 jar
- [ ] 明确说明未实机验证的部分
