package dev.zxin.infinitestacks.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置数据模型（Gson 友好的纯 POJO，**不依赖任何加载器类型**）。
 *
 * <p>这是「静态配置」部分，写进 {@code config/infinitestacks.json}。
 * 运行时由 {@code /stack} 指令改动的部分写进
 * {@code config/infinitestacks.overrides.json}（见 {@link Overrides}），后者优先级更高。</p>
 *
 * <p>键名与《接口约定文档》§3.1 完全一致，三加载器共用同一份模型。</p>
 */
public class StackConfig {

    // ---- general ----
    /** 全局堆叠上限。1.20.4 及以前默认 127（单字节 NBT 限制），1.20.5+ 默认 int 最大值。 */
    public int globalLimit = 127;

    /** 是否也提升耐久物品（工具/盔甲）。默认 false。 */
    public boolean affectDamageable = false;

    /** 是否扩展容器/槽位的上限（否则 Shift 点击一次只搬 64）。 */
    public boolean containerExpansion = true;

    /** 仅 MC ≤ 1.20.4 生效：NBT 用 IntTag 存数量以突破 127。会破坏原版存档兼容。 */
    public boolean nbtIntExtension = true;

    /** 数量超过该值给操作者发溢出警告。 */
    public int overflowWarningThreshold = 1000000000;

    /** /stack 指令所需权限等级，0-4。 */
    public int commandPermissionLevel = 2;

    /** 是否把有效配置同步给客户端（关掉后客户端 UI 会退回原版显示）。 */
    public boolean syncToClient = true;

    // ---- ui ----
    /** ABBREVIATED 或 FULL。 */
    public String uiMode = "ABBREVIATED";

    /** 低于该值始终显示完整数字。 */
    public int abbreviateThreshold = 9999;

    // ---- drops ----
    public DropConfig drops = new DropConfig();

    // ---- 覆盖表（静态配置里也可以写，运行时覆盖会与之合并） ----
    /** 物品 ID → 上限。键形如 {@code minecraft:stick}。 */
    public Map<String, Integer> items = new LinkedHashMap<String, Integer>();

    /** 标签 ID → 上限。键形如 {@code #minecraft:planks}。 */
    public Map<String, Integer> tags = new LinkedHashMap<String, Integer>();

    public static StackConfig defaults() {
        StackConfig cfg = new StackConfig();
        cfg.clamp();
        return cfg;
    }

    public boolean abbreviated() {
        return !"FULL".equalsIgnoreCase(uiMode);
    }

    public StackConfig copy() {
        StackConfig out = new StackConfig();
        out.globalLimit = this.globalLimit;
        out.affectDamageable = this.affectDamageable;
        out.containerExpansion = this.containerExpansion;
        out.nbtIntExtension = this.nbtIntExtension;
        out.overflowWarningThreshold = this.overflowWarningThreshold;
        out.commandPermissionLevel = this.commandPermissionLevel;
        out.syncToClient = this.syncToClient;
        out.uiMode = this.uiMode;
        out.abbreviateThreshold = this.abbreviateThreshold;
        out.drops = this.drops == null ? new DropConfig() : this.drops.copy();
        out.items = this.items == null ? new LinkedHashMap<String, Integer>() : new LinkedHashMap<String, Integer>(this.items);
        out.tags = this.tags == null ? new LinkedHashMap<String, Integer>() : new LinkedHashMap<String, Integer>(this.tags);
        return out;
    }

    /** 把越界/非法的值拉回合法范围。读盘后、同步前都要调。 */
    public void clamp() {
        if (globalLimit < 1) {
            globalLimit = 1;
        }
        if (globalLimit > 2147483647) {
            globalLimit = 2147483647;
        }
        commandPermissionLevel = DropConfig.clampInt(commandPermissionLevel, 0, 4, 2);
        overflowWarningThreshold = DropConfig.clampInt(overflowWarningThreshold, 1, 2147483647, 1000000000);
        abbreviateThreshold = DropConfig.clampInt(abbreviateThreshold, 1, 2147483647, 9999);
        if (uiMode == null || !("ABBREVIATED".equalsIgnoreCase(uiMode) || "FULL".equalsIgnoreCase(uiMode))) {
            uiMode = "ABBREVIATED";
        } else {
            uiMode = uiMode.toUpperCase(java.util.Locale.ROOT);
        }
        if (drops == null) {
            drops = new DropConfig();
        }
        drops.clamp();
        if (items == null) {
            items = new LinkedHashMap<String, Integer>();
        }
        if (tags == null) {
            tags = new LinkedHashMap<String, Integer>();
        }
        items.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isEmpty() || e.getValue() == null);
        tags.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isEmpty() || e.getValue() == null);
    }
}
