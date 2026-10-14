# 无限堆叠 Infinite Stacks · 架构与接口约定文档

> **本文件是唯一的接口契约。** 所有子代理（Forge/NeoForge、Fabric、CI、文档、验证）产出的代码与脚本
> 必须严格遵守本文件。发现冲突由主代理裁决并在此更新，**不得各写各的**。
>
> 版本：v1.0（2026-10-14）　适用：Minecraft 1.20.1 → 26.3，Forge / NeoForge / Fabric，共 22 个分支

---

## 1. 标识符（全局唯一，禁止改动）

| 项 | 值 |
|---|---|
| mod id | `infinitestacks` |
| 显示名 | `Infinite Stacks` / `无限堆叠` |
| Java 包根 | `dev.zxin.infinitestacks` |
| 主类（公共门面） | `dev.zxin.infinitestacks.InfiniteStacks` |
| 版本号 | `1.0.0`（`mod_version`） |
| 作者 / 组 | `Zxin-Pro` / `dev.zxin` |
| 归档名 | `infinitestacks-<loader>-<mc_version>-1.0.0.jar` |
| GitHub 仓库 | `Zxin-Pro/infinite-stacks` |
| License | MIT |

**分支命名（22 个）**：`forge-1.20.1` `forge-1.20.2` `forge-1.20.4`
`neoforge-1.20.6` `neoforge-1.21.1` `neoforge-1.21.4` `neoforge-1.21.8` `neoforge-1.21.11` `neoforge-26.1` `neoforge-26.2` `neoforge-26.3`
`fabric-1.20.1` `fabric-1.20.2` `fabric-1.20.4` `fabric-1.20.6` `fabric-1.21.1` `fabric-1.21.4` `fabric-1.21.8` `fabric-1.21.11` `fabric-26.1` `fabric-26.2` `fabric-26.3`
另有 `main` 分支持有文档、生成器、CI 总说明。

---

## 2. 源码目录布局（每个版本分支完全相同）

```
<branch>/
├── build.gradle                 加载器各自的构建脚本
├── settings.gradle
├── gradle.properties            版本号（唯一真源）
├── gradlew / gradlew.bat / gradle/wrapper/
├── LICENSE / .gitignore
├── .github/workflows/build.yml
└── src/main/
    ├── java/dev/zxin/infinitestacks/
    │   ├── InfiniteStacks.java          公共门面（常量 + id() + 日志）
    │   ├── Bridge.java                  ★平台桥（加载器注入能力）
    │   ├── config/
    │   │   ├── StackConfig.java         配置数据模型（纯 POJO，Gson 友好）
    │   │   ├── DropConfig.java          掉落物渲染配置子对象
    │   │   └── Overrides.java           运行时覆盖存储（JSON 读写）
    │   ├── logic/
    │   │   ├── StackLimits.java         ★堆叠上限解析（含缓存）
    │   │   ├── StackMath.java           饱和算术 / 溢出保护
    │   │   └── CountFormat.java         ★数量格式化（1.2K / 3.4M / 5.6B）
    │   ├── command/
    │   │   ├── StackCommand.java        /stack 指令树（平台无关）
    │   │   └── Permissions.java         权限检查适配（版本差异隔离在此）
    │   ├── network/
    │   │   ├── SyncConfigPacket.java    S2C 配置同步（数据）
    │   │   ├── RequestConfigPacket.java C2S 请求同步（数据）
    │   │   └── NetCodec.java            纯字节编解码（不依赖加载器）
    │   ├── mixin/                       ★服务端+客户端共用 Mixin
    │   │   ├── ItemMixin.java           Item#getMaxStackSize
    │   │   ├── ItemStackMixin.java      ItemStack#getMaxStackSize / grow / shrink
    │   │   ├── ItemStackNbtMixin.java   ItemStack#save / #of → IntTag（仅 MC<=12004）
    │   │   ├── ContainerMixin.java      BaseContainerBlockEntity#getMaxStackSize
    │   │   └── SlotMixin.java           Slot#getMaxStackSize
    │   └── client/
    │       ├── ClientStackState.java    客户端缓存的「服务端配置」
    │       ├── DropLabelRenderer.java   ★掉落物数字标签绘制
    │       ├── DropLabelBatch.java      ★标签聚类（批处理）
    │       └── mixin/
    │           ├── ItemEntityRendererMixin.java  渲染模型数钳制 + 挂标签
    │           └── GuiGraphicsMixin.java         物品栏数量缩写显示
    └── resources/
        ├── infinitestacks.mixins.json            通用 Mixin 配置
        ├── infinitestacks.client.mixins.json     客户端 Mixin 配置
        ├── pack.mcmeta
        ├── icon.png
        ├── assets/infinitestacks/lang/{en_us,zh_cn}.json
        └── （加载器元数据见 §7）
```

### 加载器专属包

| 加载器 | 包 | 类 |
|---|---|---|
| Forge | `dev.zxin.infinitestacks.forge` | `InfiniteStacksForge`（@Mod 主类）、`ForgeConfig`、`ForgeNetwork`、`ForgeClientSetup`、`ForgeEvents` |
| NeoForge | `dev.zxin.infinitestacks.neoforge` | `InfiniteStacksNeoForge`、`NeoConfig`、`NeoNetwork`、`NeoClientSetup`、`NeoEvents` |
| Fabric | `dev.zxin.infinitestacks.fabric` | `InfiniteStacksFabric`（ModInitializer）、`FabricConfig`、`FabricNetwork`、`InfiniteStacksFabricClient`（ClientModInitializer）、`FabricClientSetup` |

---

## 3. 配置键约定（**键名冻结**，三加载器一致）

### 3.1 静态配置（加载器原生 spec / Fabric JSON）
Fabric 写到 `config/infinitestacks.json`；Forge/NeoForge 用原生 ConfigSpec（同一份键名、同一份分组）。

| 分组 | 键 | 类型 | 默认 | 范围 | 说明 |
|---|---|---|---|---|---|
| `general` | `global_stack_limit` | int | **见 §3.3** | 1 … 2147483647 | 全局堆叠上限 |
| `general` | `affect_damageable` | bool | `false` | | 是否也提升耐久物品 |
| `general` | `container_expansion` | bool | `true` | | 是否扩展容器/槽位上上限 |
| `general` | `nbt_int_extension` | bool | **见 §3.3** | | 仅 MC<=12004：NBT 用 IntTag 存数量 |
| `general` | `overflow_warning_threshold` | int | `1000000000` | 1 … 2147483647 | 超过则给玩家警告 |
| `general` | `command_permission_level` | int | `2` | 0 … 4 | `/stack` 权限等级 |
| `general` | `sync_to_client` | bool | `true` | | 是否把配置同步给客户端 |
| `ui` | `mode` | enum | `ABBREVIATED` | `ABBREVIATED`/`FULL` | 数量显示模式 |
| `ui` | `abbreviate_threshold` | int | `9999` | 1 … 2147483647 | 超过该值才缩写 |
| `ui` | `full_digits_up_to` | int | `999999` | 0 … 2147483647 | 完整数字最大位数上限（0=不限） |
| `drops` | `enabled` | bool | `true` | | 掉落物渲染优化总开关 |
| `drops` | `threshold` | int | `64` | 1 … 2147483647 | 数量≥该值才显示标签 |
| `drops` | `max_models` | int | `5` | 1 … 8 | 单个掉落物最多渲染几个模型 |
| `drops` | `hide_model_above` | int | `1000000` | 0 … 2147483647 | 超过则完全不渲染模型（0=不隐藏） |
| `drops` | `merge_radius` | double | `1.5` | 0 … 8 | 标签聚类半径（0=关闭聚类） |
| `drops` | `max_labels` | int | `256` | 1 … 4096 | 每帧标签硬上限（性能护栏） |
| `drops` | `scale` | double | `1.0` | 0.25 … 4 | 标签缩放 |
| `drops` | `height_offset` | double | `0.55` | 0 … 3 | 标签相对实体高度 |
| `drops` | `color` | int(ARGB) | `0xFFFFFF` | | 文字颜色 |
| `drops` | `background_opacity` | double | `0.25` | 0 … 1 | 背景不透明度（0=无背景） |
| `drops` | `shadow` | bool | `true` | | 是否画阴影 |

### 3.2 运行时覆盖（**唯一真源**：`config/infinitestacks.overrides.json`，三加载器一致）
由 `/stack` 指令写入，也可手工编辑后 `/stack reload`。**优先级高于静态配置。**

```json
{
  "version": 1,
  "globalLimit": 2147483647,
  "commandPermissionLevel": 2,
  "affectDamageable": false,
  "items": { "minecraft:ender_pearl": 64, "minecraft:stick": 10000 },
  "tags":  { "#minecraft:planks": 4096, "#c:ingots": 2048 }
}
```
- `items` 键 = 完整物品 ID（`namespace:path`）；`tags` 键 = `#` 前缀的标签 ID。
- 值为 `0` 表示「回落全局上限」。
- 文件不存在 → 用静态配置的值，并在首次写入时创建。

### 3.3 版本相关默认值（**必须区分**，这是本项目最关键的一条）

| 条件 | `global_stack_limit` 默认 | `nbt_int_extension` 默认 | 理由 |
|---|---|---|---|
| MC ≤ 1.20.4 | **127** | **true** | 原版 NBT 用 `ByteTag("Count")`，>127 溢出。默认保守取 127；置 true 后由本模组用 IntTag 扩展，可安全提升到 int 上限（**破坏原版存档兼容，文档必须警告**） |
| MC ≥ 1.20.5 | **2147483647** | 不适用 | 原版改用 `IntTag("count")` |

> 生成器通过 `{{DEFAULT_GLOBAL_LIMIT}}` / `{{NBT_EXT_DEFAULT}}` 两个 token 注入，代码里**不得写死**。

---

## 4. 堆叠上限解析规则（`StackLimits`）

**查找顺序（首个命中即返回）：**
1. `stack.isEmpty()` → `0`
2. 物品是耐久物品且 `affectDamageable == false` → **原版上限**（`vanillaLimit(item)`，通常 1 或 16）
3. `items[itemId]` 覆盖存在且 > 0 → 该值
4. 任一 `tags[tagId]` 覆盖匹配（`stack.is(tag)`）且 > 0 → 该值（多个命中取**最小**，保证确定性）
5. → `globalLimit`

**统一钳制**：结果一律 `clamp(1, ABSOLUTE_MAX)`，`ABSOLUTE_MAX = 2147483647`。

**缓存**：`StackLimits` 维护 `Item → int` 的缓存（`ConcurrentHashMap`，key 用 `Item`）。任何配置变更必须调用
`StackLimits.invalidate()`。

**耐久判定**：`stack.isDamageableItem()`（1.20.1-1.21.x）／`item.components().has(DataComponents.MAX_DAMAGE)`（1.20.5+）。
版本差异封装在 `StackLimits.vanillaLimit(ItemStack)` 内部。

---

## 5. 网络协议约定（**包 ID 冻结**）

| 方向 | 通道 ID | 载荷 | 用途 |
|---|---|---|---|
| S2C | `infinitestacks:sync_config` | `SyncConfigPacket` | 服务端把有效配置推给客户端；进服时、`/stack` 变更后、`/stack reload` 后 |
| C2S | `infinitestacks:request_config` | `RequestConfigPacket`（空） | 客户端主动请求（按键 / 客户端打开界面时） |

**线格式（`NetCodec`，自定义、与加载器无关）**：
```
sync_config  : [varint version=1]
               [varint globalLimit]
               [varint permissionLevel]
               [bool   affectDamageable]
               [bool   dropsEnabled]
               [varint dropsThreshold]
               [varint dropsMaxModels]
               [float  dropsMergeRadius]
               [float  dropsScale]
               [varint dropsColor]
               [float  dropsBgOpacity]
               [varint itemOverrideCount]  × { [utf id] [varint limit] }
               [varint tagOverrideCount]   × { [utf id] [varint limit] }
               [varint uiMode]  // 0=ABBREVIATED 1=FULL
               [varint uiThreshold]
request_config: 空
```
- `utf` = `writeUtf`/`readUtf`（长度前缀 UTF-8）。
- 载荷 **总大小硬上限 32 KiB**；超限则只发核心字段（省略覆盖表）并把 `version` 置为 `1|0x80`。

**权威性**：服务端是唯一权威。客户端 **永不** 用本地配置改变判定，只用于渲染。服务端在
`ServerPlayer` 进服（`PlayerLoggedInEvent` / `ServerPlayConnectionEvents.JOIN`）时主动下发一次。

---

## 6. 指令约定（`/stack`，别名 `/istacks`）

| 指令 | 权限 | 作用 |
|---|---|---|
| `/stack get` | 2 | 查看当前有效全局上限与来源 |
| `/stack set <limit>` | 2 | 设置全局上限（写入 overrides.json） |
| `/stack item <itemId> <limit>` | 2 | 单物品覆盖；`limit=0` 清除 |
| `/stack tag <#tagId> <limit>` | 2 | 标签批量覆盖；`limit=0` 清除 |
| `/stack list [page]` | 2 | 列出全部覆盖 |
| `/stack reset [all\|items\|tags]` | 2 | 恢复默认 |
| `/stack reload` | 2 | 重新读盘 + 重新下发客户端 |
| `/stack diag` | 2 | 自检：打印生效的 Mixin/缓存/网络状态 |
| `/stack format <ABBREVIATED\|FULL>` | 2 | 切换数字显示模式 |

权限等级由 `command_permission_level` 决定（默认 2）。**返回文本一律走 `Component.translatable`**，
翻译键前缀 `command.infinitestacks.`。

---

## 7. 加载器元数据约定

| 加载器 | 文件 | Mixin 声明方式 |
|---|---|---|
| Forge | `META-INF/mods.toml` | JAR manifest `MixinConfigs: infinitestacks.mixins.json,infinitestacks.client.mixins.json` |
| NeoForge | `META-INF/neoforge.mods.toml` | `[[mixins]] config="…"` 两段 |
| Fabric | `fabric.mod.json` | `"mixins": ["infinitestacks.mixins.json", {"config":"infinitestacks.client.mixins.json","environment":"client"}]` |

`pack.mcmeta` 的 `pack_format` 由生成器按版本注入（`{{PACK_FORMAT}}`）。

---

## 8. 版本分支策略（预处理指令，**必须使用**）

公共代码用 `//#if` / `//#elif` / `//#else` / `//#endif` 预处理（生成器 `tools/gen.py` 展开），
文件级排除用 `//@only <条件>`。

**可用条件**：`MC >= 12005`、`MC >= 12100`、`MC >= 12111`、`MC >= 26000`、`MC >= 26300`、
`LOADER_FORGE`、`LOADER_NEOFORGE`、`LOADER_FABRIC`、`JAVA >= 21`。

`MC` 编码：`1.20.1→12001`、`1.20.4→12004`、`1.20.5→12005`、`1.21.1→12101`、`26.1→26001`。

### 已知断裂点登记表（**任何新发现的断裂点都必须登记在此**）

| # | 断裂点 | 影响分支 | 统一写法 |
|---|---|---|---|
| 1 | `ResourceLocation` → `Identifier` | MC ≥ 1.21.11 | `//#if MC>=12111` 用 `Identifier.fromNamespaceAndPath` |
| 2 | `CommandSourceStack#hasPermission(int)` 消失 | MC ≥ 1.21.11 | 全部隔离在 `command/Permissions.java` |
| 3 | `ItemStack` 数量存储：`ByteTag("Count")` → `IntTag("count")` | 分界 1.20.5 | `//#if MC>=12005` |
| 4 | `Item#getMaxStackSize()` 存在性 | MC ≥ 1.20.5 由 DataComponent 接管 | `ItemMixin` 用 `//@only MC<12005` |
| 5 | NeoForge `ModConfig.Type.COMMON` → `SYNCED` | 仅 26.3 | `//#if MC>=26300` |
| 6 | Fabric `PayloadTypeRegistry.playC2S()/playS2C()` → `serverboundPlay()/clientboundPlay()` | Fabric 26.x | `//#if MC>=26000` |
| 7 | `ItemEntityRenderer#render` 的 `entityYaw` 参数移除 | MC ≥ 1.21.2 | `//#if MC>=12102` |
| 8 | NeoForge 1.21.5+ 客户端发包 `ClientPacketDistributor.sendToServer` | NeoForge ≥ 1.21.5 | `//#if MC>=12105` |
| 9 | `FMLEnvironment.dist` 移除 | MC ≥ 1.21.11 | `//#if MC<12111` |
| 10 | Loom 1.18 要求 Gradle 跑在 JVM 25 | 所有 Fabric 分支 | CI 里先装编译 JDK、后装 Gradle JVM |

---

## 9. 服务端权威 / 防作弊约定

1. `/stack` 的所有变更 **只在服务端执行**（即使客户端发起也走命令，服务端判定权限）。
2. `getMaxStackSize` 的返回值 **客户端不参与判定**：客户端只在收到 `SyncConfigPacket` 后更新**渲染用缓存**；
   若客户端未收到（例如原版客户端），渲染退化为显示原版数字，**不影响任何游戏逻辑**。
3. 溢出保护：所有数量运算走 `StackMath`（`long` 中间量 + 饱和钳制），**禁止**直接 `a + b`。
4. 数量写入前一律 `clamp(0, ABSOLUTE_MAX)`，并在超过 `overflow_warning_threshold` 时给**操作者**发警告（每人 30 秒节流）。
5. 服务端 **不得** 信任客户端上报的任何上限值。

---

## 10. 编译与 CI 约定

- 每个版本分支：`.github/workflows/build.yml`，触发 `push` / `pull_request` / `workflow_dispatch`，
  跑 `./gradlew build --stacktrace --no-daemon`，上传 `build/libs/*.jar`。
- main 分支：`.github/workflows/build-all.yml`，matrix 22 项，artifact 名
  `infinite-stacks-<loader>-<mc>-<run_number>`。
- JDK：1.20.1–1.20.4 → 17；1.20.6–1.21.11 → 21；26.x → 25。**先装编译 JDK、后装 Gradle JVM**。
- 推送方式：优先 `git push`；失败走 GitHub Git Data API（`tools/` 里的推送脚本）。

---

## 11. 变更记录

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-10-14 | v1.0 | 初版，冻结标识符/目录/配置键/包 ID/断裂点表 |
