# Random SkyBlock Island

一个能让你的空岛方块随机变换的 Minecraft Fabric 模组！

## 功能特性

### 随机方块变换
- 每隔指定时间（默认3分钟），空岛上的方块会随机变为另一种方块
- 支持200+种方块，包括木材、矿石、建筑材料、彩色方块等
- 所有区域会在同一时间变换为同一种方块

### 多区域支持
- 支持添加多个变换区域，每个区域可以有不同的坐标范围
- 使用指令管理区域：添加、删除、重命名、列出
- 至少保留一个区域，不能删除最后一个区域

### 配置系统
- 支持 Cloth Config API 配置界面（需要安装 Mod Menu）
- 配置文件自动生成在 `config/randomskyblockisland.json`
- 可自定义变换间隔和区域坐标

### 倒计时显示
- 使用 Bossbar 显示剩余变换时间
- 进度条实时更新
- 新玩家加入时自动显示

### 本地化支持
- 支持英文和中文
- 所有玩家可见的文本都已本地化

## 命令

### 游戏控制
| 命令 | 说明 |
|------|------|
| `/randomskyblock start` | 开始随机变换 |
| `/randomskyblock stop` | 停止随机变换 |

### 区域管理
| 命令 | 说明 |
|------|------|
| `/randomskyblock area add <name> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>` | 添加新区域 |
| `/randomskyblock area remove <name>` | 删除区域 |
| `/randomskyblock area rename <oldName> <newName>` | 重命名区域 |
| `/randomskyblock area list` | 列出所有区域 |
| `/randomskyblock area show` | 显示第一个区域 |
| `/randomskyblock area pos1` | 设置位置1（当前玩家位置） |
| `/randomskyblock area pos2` | 设置位置2（当前玩家位置） |
| `/randomskyblock area setfrompos <name>` | 从位置创建新区域 |

### 延迟设置
| 命令 | 说明 |
|------|------|
| `/randomskyblock delay <seconds>` | 设置变换间隔（1-3600秒） |
| `/randomskyblock delay show` | 显示当前变换间隔 |

## 配置文件

首次运行后会生成 `config/randomskyblockisland.json`：

```json
{
  "transformDelaySeconds": 180,
  "areas": [
    {
      "name": "default",
      "minX": -1,
      "minY": 64,
      "minZ": -4,
      "maxX": 4,
      "maxY": 66,
      "maxZ": 1
    }
  ],
  "allowedBlocks": [
    "minecraft:oak_log",
    "minecraft:oak_planks",
    ...
  ]
}
```

## 安装

1. 下载模组 `.jar` 文件
2. 将其放入 `.minecraft/mods` 文件夹
3. 确保已安装 Fabric Loader 和 Fabric API
4. 推荐安装 Cloth Config API 和 Mod Menu 以使用配置界面

## 前置依赖

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| Fabric Loader | >=0.16.2 | 必须 |
| Fabric API | * | 必须 |
| Cloth Config API | >=11.1.136 | 必须（配置界面） |
| Mod Menu | >=7.2.2 | 推荐（模组菜单） |

## 已知限制

- 配置界面目前仅支持编辑第一个区域
- 额外区域需要通过指令管理
- 配置文件使用简单的字符串解析，复杂格式可能导致解析失败

## 构建

```bash
./gradlew build
```

构建产物位于 `build/libs/rsl-1.0.0.jar`

## 许可证

MIT License - 详见 [LICENSE](LICENSE)
