# 版本支持表 · Infinite Stacks 无限堆叠

> 本表由 **GitHub Actions 真实编译结果**填充，不是估计值。
> 仓库：https://github.com/Zxin-Pro/infinite-stacks

## 构建结果总览

**22 个版本分支 + main：全部 BUILD SUCCESSFUL，全部产出 JAR。** ✅

| 加载器 | 分支 | MC | Java | Gradle | 加载器版本 | 结果 |
|---|---|---|---|---|---|---|
| Forge | `forge-1.20.1` | 1.20.1 | 17 | 8.14.5 | 47.4.27 | ✅ 通过 |
| Forge | `forge-1.20.2` | 1.20.2 | 17 | 8.14.5 | 48.1.0 | ✅ 通过 |
| Forge | `forge-1.20.4` | 1.20.4 | 17 | 8.14.5 | 49.2.11 | ✅ 通过 |
| NeoForge | `neoforge-1.20.6` | 1.20.6 | 21 | 8.14.5 | 20.6.141 | ✅ 通过 |
| NeoForge | `neoforge-1.21.1` | 1.21.1 | 21 | 8.14.5 | 21.1.259 | ✅ 通过 |
| NeoForge | `neoforge-1.21.4` | 1.21.4 | 21 | 8.14.5 | 21.4.158 | ✅ 通过 |
| NeoForge | `neoforge-1.21.8` | 1.21.8 | 21 | 8.14.5 | 21.8.54 | ✅ 通过 |
| NeoForge | `neoforge-1.21.11` | 1.21.11 | 21 | 8.14.5 | 21.11.45 | ✅ 通过 |
| NeoForge | `neoforge-26.1` | 26.1 | 25 | 9.7.1 | 26.1.0.19-beta | ✅ 通过 |
| NeoForge | `neoforge-26.2` | 26.2 | 25 | 9.7.1 | 26.2.0.90 | ✅ 通过 |
| NeoForge | `neoforge-26.3` | 26.3 | 25 | 9.7.1 | 26.3.0.69-beta | ✅ 通过 |
| Fabric | `fabric-1.20.1` | 1.20.1 | 17 | 9.7.1 | API 0.92.12+1.20.1 | ✅ 通过 |
| Fabric | `fabric-1.20.2` | 1.20.2 | 17 | 9.7.1 | API 0.91.6+1.20.2 | ✅ 通过 |
| Fabric | `fabric-1.20.4` | 1.20.4 | 17 | 9.7.1 | API 0.97.3+1.20.4 | ✅ 通过 |
| Fabric | `fabric-1.20.6` | 1.20.6 | 21 | 9.7.1 | API 0.100.8+1.20.6 | ✅ 通过 |
| Fabric | `fabric-1.21.1` | 1.21.1 | 21 | 9.7.1 | API 0.116.17+1.21.1 | ✅ 通过 |
| Fabric | `fabric-1.21.4` | 1.21.4 | 21 | 9.7.1 | API 0.119.4+1.21.4 | ✅ 通过 |
| Fabric | `fabric-1.21.8` | 1.21.8 | 21 | 9.7.1 | API 0.136.1+1.21.8 | ✅ 通过 |
| Fabric | `fabric-1.21.11` | 1.21.11 | 21 | 9.7.1 | API 0.141.6+1.21.11 | ✅ 通过 |
| Fabric | `fabric-26.1` | 26.1 | 25 | 9.7.1 | API 0.145.1+26.1 | ✅ 通过 |
| Fabric | `fabric-26.2` | 26.2 | 25 | 9.7.1 | API 0.161.0+26.2 | ✅ 通过 |
| Fabric | `fabric-26.3` | 26.3 | 25 | 9.7.1 | API 0.162.0+26.3 | ✅ 通过 |

Fabric Loader 统一 **0.19.5**。Fabric 26.x 使用 `net.fabricmc.fabric-loom`（不重映射，因为 26.x 官方 jar 已去混淆），1.20.1–1.21.11 使用 `fabric-loom-remap` + 官方 Mojang mappings。

**失败/不支持：无。**

## 产物命名

- JAR：`infinitestacks-<loader>-<mc_version>-1.0.0.jar`（例：`infinitestacks-fabric-1.20.1-1.0.0.jar`）
- Actions artifact：`infinite-stacks-<loader>-<mc_version>`

## 堆叠上限的版本行为差异（重要）

| MC 范围 | 存储机制 | 默认全局上限 | 说明 |
|---|---|---|---|
| 1.20.1 – 1.20.4 | **单字节**（NBT `ByteTag("Count")` + 网络 `writeByte`） | **127** | 超过 127 需要本模组的 byte→int 双通道补丁。**必须两端同装本模组**，否则单端会出现「客户端 127 / 服务端数十亿」的数据不一致。 |
| 1.20.5 – 1.21.11 | int（数据组件 `MAX_STACK_SIZE`，网络 `VarInt`） | **2,147,483,647** | 只需放宽 `ExtraCodecs.intRange(1,99)` 与组件默认值 64。 |
| 26.1 – 26.3 | 同 1.20.5+ | **2,147,483,647** | 上限查询方法由 `ItemStack` 移到 `ItemInstance` 默认方法。 |

> ⚠️ 建议不要真的把上限设到 21 亿：数量参与大量 int 加减（split / merge / grow），21 亿附近会溢出成负数进而被判空、物品凭空消失。参考模组 Stacc 的建议是 **≤10 亿**，最安全的 2 次幂是 **536,870,912**。本模组默认按需求取 int 最大值，但配置里可以随时调低。

## 编译时踩过的坑（已修复，供复现参考）

| # | 现象 | 根因 | 修法 |
|---|---|---|---|
| 1 | `StackLimits.java:193: cannot find symbol` | `Item.canBeDepleted()` 在 **1.20.5** 就被移除了（不是 26.x） | 版本门槛改为 `MC>=12005` 并改用 `getDefaultInstance().isDamageableItem()` |
| 2 | `ContainerMixin.java:29: cannot find symbol` | 重写文件时漏了 `@Overwrite` 的 import | 补 import；并加了 `check_imports.py` 全量扫描 |
| 3 | `ForgeNetwork.java:51/52: cannot find symbol` | Forge 的 `Channel.VersionTest` 用的是**常量** `ACCEPT_VANILLA`，不是方法 `acceptVanilla()` | 改用 `Channel.VersionTest.ACCEPT_VANILLA` |
| 4 | `ItemEntityRendererSubmitMixin.java:62: CameraRenderState cannot be converted to double` | 版本号编码是 `26.1 → 26100`（不是 26001），门槛 `MC>=26002` 把 26.1 误判成 26.2+；且 **26.1 与 26.2/26.3 的 `submitNameTag` 参数个数不同**（26.1 为 8 参含 `distanceToCameraSq`，26.2+ 为 7 参） | 门槛改为 `MC>=26200` |
| 5 | `DropLabelRenderer.java:6` / `DropLabelBatch.java:170` | 26.2 起原版**移除了 `MultiBufferSource`** 与 `GameRenderer#getMainCamera()` | 26.x 改走 `OrderedSubmitNodeCollector#submitNameTag`，旧的立即模式绘制整段用 `MC<26000` 包起来 |
| 6 | `StackCommand.java:237` | 26.x 移除了 `Player#displayClientMessage(Component, boolean)` | 改用 `sendSystemMessage(Component)` |
| 7 | Forge 编译报 `Unable to locate obfuscation mapping for @Inject target` | 手动加了 `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'`，与 ForgeGradle 6 自带的 Mixin 配置冲突 | 移除手动 AP（FG6 自带并会自动生成 refmap） |
| 8 | `Injector in interface is unsupported`（Forge 1.20.1 的 Mixin 0.8.5） | `Container` 是接口，不能放 `@Inject` | 改为 `@Overwrite` 接口默认方法（参考模组 Stackable 的做法） |

## 验证方式

```bash
# 本地预检（不编译，只查结构）
python3 tools/validate.py        # 预处理残留 / 未替换 token / JSON 合法性
python3 tools/check_manifest.py  # mixin 清单与源码一致性
python3 tools/check_imports.py   # 漏 import 扫描

# 真编译：推送到 GitHub 由 Actions 跑
python3 /tmp/push_all.py
```
