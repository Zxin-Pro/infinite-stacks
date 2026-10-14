package dev.zxin.infinitestacks.client;

import net.minecraft.client.Minecraft;

/**
 * 客户端公共入口：网络接收、按键、每帧清理。
 * 各加载器的 client 初始化器把平台钩子接到这里。
 */
public final class ClientHooks {

    private ClientHooks() {
    }

    /** 收到 S2C 配置同步（由各加载器在收到包时调用）。 */
    public static void onSyncReceived(dev.zxin.infinitestacks.network.Packets.Sync data) {
        ClientStackState.accept(data);
    }

    /** 断开连接时清掉服务端配置缓存。 */
    public static void onDisconnect() {
        ClientStackState.clear();
    }

    /** 按键：主动向服务端要一份最新配置。 */
    public static void requestConfig() {
        try {
            dev.zxin.infinitestacks.Bridge.SEND_TO_SERVER.accept(new dev.zxin.infinitestacks.network.Packets.Sync());
        } catch (Throwable ignored) {
            // 客户端未接好网络层时静默忽略
        }
    }

    /** 当前是否在世界里（避免在主菜单空跑）。 */
    public static boolean inWorld() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.level != null;
    }
}
