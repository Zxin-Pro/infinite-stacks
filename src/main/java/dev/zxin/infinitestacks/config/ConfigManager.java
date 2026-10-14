package dev.zxin.infinitestacks.config;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.logic.StackLimits;

/**
 * 配置管理：静态配置 + 运行时覆盖 → 生效配置。
 *
 * <p>流程：读 {@code infinitestacks.json} → 读 {@code infinitestacks.overrides.json}
 * → 合并 → 注入 {@link StackLimits} 与 {@link Bridge} → 通知客户端重同步。</p>
 *
 * <p>加载器在启动、配置文件重载、{@code /stack} 变更后调用 {@link #reload()}。</p>
 */
public final class ConfigManager {

    private static volatile StackConfig base = StackConfig.defaults();
    private static volatile Overrides overrides = new Overrides();
    private static volatile StackConfig effective = StackConfig.defaults();

    private ConfigManager() {
    }

    public static StackConfig base() {
        return base;
    }

    public static Overrides overrides() {
        return overrides;
    }

    public static StackConfig effective() {
        return effective;
    }

    /**
     * 重新读盘并应用。
     *
     * @param resyncToClients 是否顺带把新配置推给所有在线客户端
     */
    public static synchronized void reload(boolean resyncToClients) {
        try {
            base = Overrides.loadBase();
            overrides = Overrides.load();
            StackConfig merged = overrides.merge(base);
            effective = merged;
            Bridge.setConfig(merged);
            StackLimits.apply(merged);
            InfiniteStacks.info(
                    "配置已生效: globalLimit={} affectDamageable={} uiMode={} 物品覆盖={} 标签覆盖={} 掉落阈值={}",
                    merged.globalLimit, merged.affectDamageable, merged.uiMode,
                    merged.items.size(), merged.tags.size(), merged.drops.threshold);
        } catch (Throwable t) {
            InfiniteStacks.error("配置加载异常，回退默认值: {}", t.toString());
            base = StackConfig.defaults();
            overrides = new Overrides();
            effective = base.copy();
            Bridge.setConfig(effective);
            StackLimits.apply(effective);
        }
        if (resyncToClients) {
            resyncAll();
        }
    }

    public static void reload() {
        reload(true);
    }

    /** 只把当前配置推给客户端（不改磁盘）。 */
    public static void resyncAll() {
        try {
            Bridge.RESYNC_ALL.run();
        } catch (Throwable t) {
            InfiniteStacks.error("重同步失败: {}", t.toString());
        }
    }

    /** 写回覆盖文件并立即生效（{@code /stack set} 等指令走这条路）。 */
    public static synchronized void commitOverrides(boolean resyncToClients) {
        overrides.save();
        StackConfig merged = overrides.merge(base);
        effective = merged;
        Bridge.setConfig(merged);
        StackLimits.apply(merged);
        if (resyncToClients) {
            resyncAll();
        }
    }

    public static void commitOverrides() {
        commitOverrides(true);
    }

    /** 重置覆盖（{@code /stack reset all}）。 */
    public static synchronized void resetOverrides() {
        overrides = new Overrides();
        commitOverrides(true);
    }
}
