# Infinite Stacks · 无限堆叠

把 Minecraft 原版 64 的物品堆叠上限抬到**接近无限**（默认 `2,147,483,647`，即 int 最大值），
工具 / 盔甲等**耐久物品不受影响**；掉落物数量巨大时客户端只渲染少量模型，其余用**数字标签**代替。

> **不添加任何新物品**，纯堆叠逻辑修改 + 配置驱动 + 客户端渲染优化。

---

## 目录

- [特性](#特性)
- [版本支持](#版本支持)
- [安装](#安装)
- [指令](#指令)
- [配置](#配置)
- [版本差异与已知限制](#版本差异与已知限制)
- [常见问题](#常见问题)
- [从源码构建](#从源码构建)
- [项目结构](#项目结构)

---

## 特性

| # | 特性 | 说明 |
|---|---|---|
| 1 | **全局堆叠上限** | 所有非耐久物品的上限从 64 改为可配置值。`1.20.5+` 默认 `2147483647`；`≤1.20.4` 因原版用单字节存储，默认保守取 `127` |
| 2 | **单物品覆盖** | `minecraft:ender_pearl=64` 保持原版，`minecraft:stick=10000` 单独提高 |
| 3 | **标签批量覆盖** | `#minecraft:planks=4096`，一次管一整类物品 |
| 4 | **指令动态调整** | `/stack set|get|item|tag|list|reset|reload|diag`，权限等级可配置（默认 2） |
| 5 | **容器全兼容** | 背包、箱子、漏斗、潜影盒、末影箱、矿车箱子、收纳袋——Shift 搬运与漏斗传输不再被 64 截断 |
| 6 | **UI 数量格式化** | 超过阈值（默认 9999）自动缩写为 `1.2K / 3.4M / 5.6B / 7.8T`，可切回完整数字 |
| 7 | **溢出保护** | 接近 int 上限时钳制并警告，杜绝 `count` 翻负导致物品消失 |
| 8 | **耐久物品豁免** | 工具 / 盔甲默认保持原版上限（可配置开启） |
| 9 | **掉落物简略渲染** | 数量超过阈值时只渲染少量模型，其余用数字标签；标签聚类 + 每帧上限，数百个堆叠不卡 |
| 10 | **服务端权威** | 上限判定、指令、配置全在服务端；客户端只拿到一份**只读**的渲染用快照 |

### 掉落物简略渲染细节

- 数量 ≥ `drops.threshold`（默认 64）→ 在掉落物上方绘制数字标签
- 单个掉落物最多渲染 `drops.max_models` 个模型（默认 5，与原版一致；调成 1 可进一步省性能）
- `drops.merge_radius`：同一半径内的**同类**掉落物合并成一个合计标签
- `drops.max_labels`：每帧标签数量硬上限（默认 256），极端场景下的性能护栏
- **不改变任何原版掉落物逻辑**：拾取、消失、合并、物理全部走原版；服务端始终持有完整数量，客户端只是少画了几个模型
- 与其他修改掉落物渲染的模组冲突时，把 `drops.enabled` 设为 `false` 即可完全关闭本模组的渲染优化

---

## 版本支持

共 **22 个版本分支**，覆盖 Forge / NeoForge / Fabric 三个加载器。

| MC 版本 | Forge | NeoForge | Fabric | Java |
|---|---|---|---|---|
| 1.20.1 | ✅ 分支 `forge-1.20.1` | — | ✅ 分支 `fabric-1.20.1` | 17 |
| 1.20.2 | ✅ 分支 `forge-1.20.2` | — | ✅ 分支 `fabric-1.20.2` | 17 |
| 1.20.4 | ✅ 分支 `forge-1.20.4` | — | ✅ 分支 `fabric-1.20.4` | 17 |
| 1.20.6 | — | ✅ 分支 `neoforge-1.20.6` | ✅ 分支 `fabric-1.20.6` | 21 |
| 1.21.1 | — | ✅ 分支 `neoforge-1.21.1` | ✅ 分支 `fabric-1.21.1` | 21 |
| 1.21.4 | — | ✅ 分支 `neoforge-1.21.4` | ✅ 分支 `fabric-1.21.4` | 21 |
| 1.21.8 | — | ✅ 分支 `neoforge-1.21.8` | ✅ 分支 `fabric-1.21.8` | 21 |
| 1.21.11 | — | ✅ 分支 `neoforge-1.21.11` | ✅ 分支 `fabric-1.21.11` | 21 |
| 26.1 | — | ✅ 分支 `neoforge-26.1` | ✅ 分支 `fabric-26.1` | 25 |
| 26.2 | — | ✅ 分支 `neoforge-26.2` | ✅ 分支 `fabric-26.2` | 25 |
| 26.3 | — | ✅ 分支 `neoforge-26.3` | ✅ 分支 `fabric-26.3` | 25 |

**加载器说明**：Forge 自 1.20.2 起分支出 NeoForge，1.20.6 及以后官方主线是 NeoForge，
因此 `forge-*` 分支只到 1.20.4；26.x 只有 NeoForge 与 Fabric 分支。

**实测构建结果**：见 [`VERSIONS.md`](VERSIONS.md)（由 GitHub Actions 每个分支实际编译得出）。

---

## 安装

1. 在 [Releases](../../releases) 或对应分支的 Actions 产物里下载 jar：
   `infinitestacks-<loader>-<mc_version>-1.0.0.jar`
2. 放进 `.minecraft/mods/`（服务端放 `server/mods/`）
3. **服务端与客户端都要装**：
   - 服务端负责权威判定与配置同步
   - 客户端负责 UI 数量缩写与掉落物标签渲染
   - 只装服务端也能跑（堆叠照常生效），只是客户端看不到缩写与标签
4. Fabric 需要额外安装 [Fabric API](https://modrinth.com/mod/fabric-api)

---

## 指令

所有子命令都需要权限等级 ≥ `general.command_permission_level`（默认 2 = 管理员）。

| 指令 | 说明 |
|---|---|
| `/stack get` | 查看当前全局上限与配置摘要 |
| `/stack set <数量>` | 修改全局堆叠上限（写入覆盖配置并同步客户端） |
| `/stack item <物品ID> <数量>` | 单物品覆盖；数量填 `0` 表示清除该条 |
| `/stack tag <标签ID> <数量>` | 标签批量覆盖，如 `/stack tag #minecraft:planks 4096` |
| `/stack list [items\|tags]` | 列出全部覆盖项 |
| `/stack reset [all\|items\|tags]` | 重置覆盖（不填 = `all`） |
| `/stack reload` | 重新读盘（手改过配置文件后用） |
| `/stack diag` | 诊断：版本、生效配置、客户端同步状态、Mixin 挂钩情况 |
| `/stack format <ABBREVIATED\|FULL>` | 切换 UI 数量显示模式 |

示例：

```
/stack set 1000000          # 全局上限改为 100 万
/stack item minecraft:ender_pearl 64
/stack tag #minecraft:planks 4096
/stack list items
/stack reset items
```

---

## 配置

配置目录：`<游戏目录>/config/`，两个文件：

### `infinitestacks.json` —— 静态配置（手工编辑）

```json
{
  "globalLimit": 2147483647,
  "affectDamageable": false,
  "containerExpansion": true,
  "nbtIntExtension": true,
  "overflowWarningThreshold": 1000000000,
  "commandPermissionLevel": 2,
  "syncToClient": true,
  "uiMode": "ABBREVIATED",
  "abbreviateThreshold": 9999,
  "drops": {
    "enabled": true,
    "threshold": 64,
    "maxModels": 5,
    "hideModelAbove": 1000000,
    "mergeRadius": 1.5,
    "maxLabels": 256,
    "scale": 1.0,
    "heightOffset": 0.55,
    "color": 16777215,
    "backgroundOpacity": 0.25,
    "shadow": true
  }
}
```

### `infinitestacks.overrides.json` —— 运行时覆盖（指令写入）

**优先级高于静态配置**。由 `/stack` 指令维护，也可以手工编辑后 `/stack reload`。

```json
{
  "version": 1,
  "globalLimit": 1000000,
  "items": { "minecraft:ender_pearl": 64, "minecraft:stick": 10000 },
  "tags": { "#minecraft:planks": 4096 }
}
```

### 配置项说明

| 键 | 默认 | 说明 |
|---|---|---|
| `globalLimit` | `2147483647`（`≤1.20.4` 为 `127`） | 全局堆叠上限，硬上限 `2147483647` |
| `affectDamageable` | `false` | 是否也让工具/盔甲吃新上限 |
| `containerExpansion` | `true` | 是否放大容器槽位上限（关掉则 Shift 搬运仍按原版 64/99 截断） |
| `nbtIntExtension` | `true`（仅 `≤1.20.4` 有意义） | 用 IntTag 存数量，突破单字节 127 |
| `overflowWarningThreshold` | `1000000000` | 超过该值发溢出警告 |
| `commandPermissionLevel` | `2` | `/stack` 所需权限等级 |
| `syncToClient` | `true` | 是否把配置同步给客户端 |
| `uiMode` | `ABBREVIATED` | `ABBREVIATED` 缩写 / `FULL` 完整数字 |
| `abbreviateThreshold` | `9999` | 超过该数量才开始缩写 |
| `drops.*` | 见上 | 掉落物标签的全部外观与性能参数 |

---

## 版本差异与已知限制

### ⚠️ 1.20.4 及以前：单字节存储

原版把物品数量写进 NBT 的 `ByteTag("Count")`，**上限 127**，超过会截断成负数导致物品消失。
本模组用 Mixin 把它改成 `IntTag`，因此可以突破 127。

**但请注意**：
- 全局上限在 `≤1.20.4` 上**默认仍是 127**（保守值）。只有管理员主动调高才会写出 IntTag。
- 一旦写出 IntTag，**原版客户端/服务端读同一份存档会把数量读成 0**（类型不符）。
  所以：**用了本模组的存档，服务端和所有客户端都必须装本模组。**
- 1.20.5+ 原版自己就换成了 IntTag，没有这个问题。

### ⚠️ 非原版工作台

部分模组的合成界面会按 `64` 硬编码重置堆叠。遇到数量被压回 64 时，
请改用**原版工作台**合成。这是对方模组的问题，本模组无法在不逐个兼容的前提下解决。

### ⚠️ 26.x 的渲染管线

1.21.11 起原版把实体渲染换成了 submit-node 管线，掉落物标签改用
`OrderedSubmitNodeCollector#submitNameTag` 实现，因此 **26.x 上标签的外观参数
（scale / 精确位置）受原版 name tag 渲染限制**，不如 1.21.8 及以前那样自由。
功能本身完整可用。

### 与其它模组的兼容

| 情况 | 处理 |
|---|---|
| 其它模组也改堆叠上限 | 后加载者生效，可能互相覆盖。本模组的上限钩子优先级固定，必要时用对方配置让路 |
| 其它模组也改掉落物渲染 | 把 `drops.enabled` 设为 `false`，本模组完全不碰渲染 |
| 性能敏感场景 | 把 `drops.maxModels` 设为 `1`、`drops.maxLabels` 调小 |

---

## 常见问题

**Q：为什么 1.20.4 上默认只有 127？**
A：因为那个版本原版用单字节存数量。默认保守是为了不破坏存档兼容性；想要更大值执行
`/stack set 1000000`，并确保服务端与所有客户端都装了本模组。

**Q：装完为什么 UI 还是显示 64？**
A：客户端没装，或者配置没同步。执行 `/stack diag` 看客户端同步状态。

**Q：Shift 搬运只搬 64 个？**
A：检查 `containerExpansion` 是否为 `true`。

**Q：物品数量变成负数 / 消失了？**
A：这是溢出。本模组已做钳制，但如果同时装了别的改堆叠的模组，可能互相绕过。
把 `globalLimit` 调到 `2000000000` 以下更安全。

**Q：掉落物标签不显示？**
A：检查 `drops.enabled`、`drops.threshold`，以及客户端是否装好（标签是纯客户端功能）。

**Q：服务端需要装吗？**
A：**需要**。上限判定、指令、配置同步全在服务端；只装客户端不生效。

---

## 从源码构建

每个版本分支都是**完整独立**的 Gradle 工程：

```bash
git clone -b neoforge-1.21.1 https://github.com/Zxin-Pro/infinite-stacks.git
cd infinite-stacks
./gradlew build
# 产物：build/libs/infinitestacks-neoforge-1.21.1-1.0.0.jar
```

**Java 要求**：1.20.1–1.20.4 用 JDK 17；1.20.6–1.21.11 用 JDK 21；26.1+ 用 JDK 25。
Fabric 分支的 **Gradle 本身**需要 JDK 25（Loom 1.18 的要求），即使编译目标是 17/21——
CI 里为此分两步安装 JDK。

### 重新生成全部 22 个分支

```bash
cd tools
python3 gen.py            # 全部
python3 gen.py fabric-1.21.1   # 只生成一个
```

---

## 项目结构

```
tools/
├── gen.py                 # 22 分支生成器（预处理器 + token 替换）
├── templates/
│   ├── common/            # 加载器无关代码（全部业务逻辑）
│   │   └── dev/zxin/infinitestacks/
│   │       ├── InfiniteStacks.java    # 常量与日志
│   │       ├── Bridge.java            # 平台桥（配置目录/发包/重同步）
│   │       ├── config/                # StackConfig / DropConfig / Overrides / ConfigManager
│   │       ├── logic/                 # ★ StackLimits（上限解析）/ StackMath（溢出安全）/ CountFormat
│   │       ├── command/               # /stack 指令树 + 权限适配
│   │       ├── network/               # 载荷定义 + 编解码
│   │       ├── client/                # 客户端配置缓存 / 标签批处理 / 标签绘制
│   │       └── mixin/                 # 堆叠逻辑 Mixin
│   ├── forge/ neoforge/ fabric/       # 三个加载器的胶水层
│   ├── resources/                     # mixins.json / mods.toml / fabric.mod.json / 语言 / 图标
│   ├── build/                         # 各加载器 build.gradle + gradle.properties
│   └── workflows/                     # 每分支 CI
├── wrapper/               # gradle-wrapper.jar + gradlew
└── main/                  # main 分支内容（本文件等）
```

### 核心设计

- **判定与渲染彻底分离**：`StackLimits` 只在服务端参与判定；客户端拿到的 `Packets.Sync`
  是**只读快照**，只用来决定"数字怎么画"。
- **单点真相**：所有上限解析都走 `StackLimits.limitFor(...)`，按
  `空 → 耐久豁免 → 物品覆盖 → 标签覆盖 → 全局` 的顺序解析并缓存。
- **所有数量运算走 `StackMath`**：中间量提升到 `long` 再饱和钳制，杜绝翻负。
- **Mixin 只做三件事**：改上限、突破单字节存储、掉落物渲染。
  其余全部走事件与网络包。
- **Mixin 一律 `require = 0`**：钩不上就静默降级，绝不因为某个版本 API 变动把游戏搞崩；
  用 `/stack diag` 排查。

---

## 许可

MIT
