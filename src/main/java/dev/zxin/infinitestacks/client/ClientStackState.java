package dev.zxin.infinitestacks.client;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.config.StackConfig;
import dev.zxin.infinitestacks.network.Packets;

/**
 * 客户端缓存的服务端配置。
 *
 * <p><b>只用于渲染</b>，绝不参与任何判定（《接口约定文档》§9.2）。
 * 没收到同步包时回退到本地配置（单人游戏 / 内置服务器场景），
 * 这样即便对方是原版客户端，本模组也不会崩，只是显示退化为原版数字。</p>
 */
public final class ClientStackState {

    private static volatile Packets.Sync server = null;

    private ClientStackState() {
    }

    /** 收到 S2C 同步包时调用。 */
    public static void accept(Packets.Sync data) {
        if (data != null) {
            server = data;
        }
    }

    public static boolean hasServerConfig() {
        return server != null;
    }

    public static void clear() {
        server = null;
    }

    private static Packets.Sync resolved() {
        Packets.Sync cached = server;
        if (cached != null) {
            return cached;
        }
        // 回退：本地配置
        StackConfig local = Bridge.config();
        Packets.Sync fallback = new Packets.Sync();
        fallback.globalLimit = local.globalLimit;
        fallback.dropsEnabled = local.drops.enabled;
        fallback.dropsThreshold = local.drops.threshold;
        fallback.dropsMaxModels = local.drops.maxModels;
        fallback.dropsHideModelAbove = local.drops.hideModelAbove;
        fallback.dropsMergeRadius = (float) local.drops.mergeRadius;
        fallback.dropsScale = (float) local.drops.scale;
        fallback.dropsColor = local.drops.color;
        fallback.dropsBgOpacity = (float) local.drops.backgroundOpacity;
        fallback.dropsShadow = local.drops.shadow;
        fallback.uiMode = local.abbreviated() ? 0 : 1;
        fallback.uiThreshold = local.abbreviateThreshold;
        return fallback;
    }

    public static int globalLimit() {
        return resolved().globalLimit;
    }

    public static boolean dropsEnabled() {
        return resolved().dropsEnabled;
    }

    public static int dropsThreshold() {
        return resolved().dropsThreshold;
    }

    public static int dropsMaxModels() {
        return resolved().dropsMaxModels;
    }

    public static int dropsHideModelAbove() {
        return resolved().dropsHideModelAbove;
    }

    public static float dropsMergeRadius() {
        return resolved().dropsMergeRadius;
    }

    public static float dropsScale() {
        return resolved().dropsScale;
    }

    public static int dropsColor() {
        return resolved().dropsColor;
    }

    public static float dropsBgOpacity() {
        return resolved().dropsBgOpacity;
    }

    public static boolean dropsShadow() {
        return resolved().dropsShadow;
    }

    /**
     * 每帧标签上限、标签高度偏移属于**纯客户端性能/外观旋钮**，
     * 不需要与服务端一致，因此直接读本地配置。
     */
    public static int dropsMaxLabels() {
        return Math.max(1, Bridge.config().drops.maxLabels);
    }

    public static float dropsHeightOffset() {
        return (float) Bridge.config().drops.heightOffset;
    }

    public static boolean abbreviated() {
        return resolved().uiMode == 0;
    }

    public static int uiThreshold() {
        return resolved().uiThreshold;
    }
}
