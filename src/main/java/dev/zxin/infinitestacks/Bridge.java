package dev.zxin.infinitestacks;

import dev.zxin.infinitestacks.config.StackConfig;
import dev.zxin.infinitestacks.network.Packets;

import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 平台桥：把加载器相关能力（配置目录、发包、重同步）注入共用代码。
 *
 * <p>每个加载器在初始化时必须填好下面这些钩子；没填的保持空实现，
 * 因此共用代码永远不会因为平台未初始化而 NPE。</p>
 */
public final class Bridge {

    /** 配置目录（config/）。加载器注入。 */
    public static volatile Supplier<Path> CONFIG_DIR = () -> Paths.get("config");

    /** S2C 发送器。加载器注入。 */
    public static volatile BiConsumer<ServerPlayer, Packets.Sync> SEND_TO_CLIENT = (player, data) -> {
    };

    /** C2S 发送器。只有客户端会注入。 */
    public static volatile Consumer<Packets.Sync> SEND_TO_SERVER = data -> {
    };

    /** 向全部在线玩家重新下发配置。加载器注入。 */
    public static volatile Runnable RESYNC_ALL = () -> {
    };

    /** 服务端是否在跑（专用服务器为 true，客户端内置服务器也为 true）。 */
    public static volatile boolean SERVER_PRESENT = false;

    /** 当前生效的配置（静态配置 + 运行时覆盖合并后的结果）。 */
    private static volatile StackConfig effective = StackConfig.defaults();

    private Bridge() {
    }

    public static StackConfig config() {
        return effective;
    }

    public static void setConfig(StackConfig config) {
        effective = config == null ? StackConfig.defaults() : config;
    }

    public static Path configDir() {
        Path dir = CONFIG_DIR.get();
        return dir == null ? Paths.get("config") : dir;
    }

    /** 把当前配置打包成同步载荷。 */
    public static Packets.Sync snapshot() {
        StackConfig cfg = effective;
        Packets.Sync out = new Packets.Sync();
        out.globalLimit = cfg.globalLimit;
        out.permissionLevel = cfg.commandPermissionLevel;
        out.affectDamageable = cfg.affectDamageable;
        out.dropsEnabled = cfg.drops.enabled;
        out.dropsThreshold = cfg.drops.threshold;
        out.dropsMaxModels = cfg.drops.maxModels;
        out.dropsHideModelAbove = cfg.drops.hideModelAbove;
        out.dropsMergeRadius = (float) cfg.drops.mergeRadius;
        out.dropsScale = (float) cfg.drops.scale;
        out.dropsColor = cfg.drops.color;
        out.dropsBgOpacity = (float) cfg.drops.backgroundOpacity;
        out.dropsShadow = cfg.drops.shadow;
        out.uiMode = cfg.abbreviated() ? 0 : 1;
        out.uiThreshold = cfg.abbreviateThreshold;
        out.items = new java.util.LinkedHashMap<String, Integer>(cfg.items);
        out.tags = new java.util.LinkedHashMap<String, Integer>(cfg.tags);
        return out;
    }
}
