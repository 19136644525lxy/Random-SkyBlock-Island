# Random SkyBlock Island

一个能让你的空岛方块随机变换的 Minecraft 模组！支持 Fabric 和 Forge 双平台。

## 功能特性

### 随机方块变换
- 每隔指定时间（默认3分钟），空岛上的方块会随机变为另一种方块
- 支持200+种方块，包括木材、矿石、建筑材料、彩色方块等
- 每个区域独立随机，不同区域同时变换为不同方块

### 多区域支持
- 支持添加多个变换区域，每个区域有独立的坐标范围
- **默认区域**：全局配置，所有存档共享
- **额外区域**：存档级配置，每个存档独立管理
- 使用指令管理区域：添加、删除、重命名、列出

### 倒计时持久化
- 倒计时状态保存到配置文件，退出存档后不丢失
- 服务器关闭时自动保存，重新进入自动恢复

### 配置系统
- 支持 Cloth Config API 配置界面
- 配置文件位于 `config/randomskyblockisland.json`
- 存档区域配置位于 `<存档>/rsl_areas.json`
- 可通过配置界面调整变换间隔

### 本地化支持
- 支持英文和中文
- 所有玩家可见的文本都已本地化

## 命令

### 游戏控制
| 命令 | 说明 |
|------|------|
| `/randomskyblock start` | 开始随机变换 |
| `/randomskyblock stop` | 停止随机变换 |
| `/randomskyblock reset` | 重置游戏状态（保留区域和方块配置） |

### 区域管理
| 命令 | 说明 | 保存位置 |
|------|------|---------|
| `/randomskyblock area add <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>` | 添加新区域 | 存档级 |
| `/randomskyblock area remove <name>` | 删除区域 | 存档级 |
| `/randomskyblock area rename <oldName> <newName>` | 重命名区域 | 全局/存档级 |
| `/randomskyblock area list` | 列出所有区域 | - |
| `/randomskyblock area show` | 显示默认区域 | - |
| `/randomskyblock area pos1` | 设置默认区域位置1（当前玩家位置） | 全局级 |
| `/randomskyblock area pos2` | 设置默认区域位置2（当前玩家位置） | 全局级 |
| `/randomskyblock area setfrompos <name>` | 从默认区域坐标创建新区域 | 存档级 |

### 延迟设置
| 命令 | 说明 |
|------|------|
| `/randomskyblock delay <seconds>` | 设置变换间隔（1-3600秒） |
| `/randomskyblock delay show` | 显示当前变换间隔 |

## 配置文件

### 全局配置（`config/randomskyblockisland.json`）

存储全局共享的配置：

```json
{
  "transformDelaySeconds": 180,
  "secondsRemaining": 0,
  "isRunning": false,
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
    "minecraft:oak_planks",
    ...
  ]
}
```

### 存档区域配置（`<存档>/rsl_areas.json`）

存储每个存档独立的额外区域：

```json
{
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
4. 推荐安装 Cloth Config API 和 Mod Menu 以使用配置界面

### Forge
1. 下载 Forge 版 `.jar` 文件
2. 将其放入 `.minecraft/mods` 文件夹
3. 确保已安装对应版本的 Forge
4. 推荐安装 Cloth Config API 以使用配置界面

## 前置依赖

### Fabric

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| Fabric Loader | >=0.16.2 | 必须 |
| Fabric API | * | 必须 |
| Cloth Config API | >=11.1.136 | 必须（配置界面） |
| Mod Menu | >=7.2.2 | 推荐（模组菜单） |

### Forge

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| Forge | 47.2.0 (1.20.1) | 必须 |
| Cloth Config API | >=8.2 | 必须（配置界面） |

## 已知限制

- 配置界面仅支持调整变换间隔，区域坐标通过指令管理
- 配置文件使用简单的字符串解析，复杂格式可能导致解析失败
- 默认区域不能通过 `area remove` 删除

## 构建

### Fabric
```bash
./gradlew build
```

### Forge
```bash
cd forge && ./gradlew build
```

## 许可证

MIT License - 详见 [LICENSE](LICENSE)
