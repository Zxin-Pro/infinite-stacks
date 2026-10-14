package dev.zxin.infinitestacks.neoforge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * NeoForge 事件/下发逻辑集合。
 *
 * <p>事件的**注册**放在 {@link InfiniteStacksNeoForge} 的构造里（一行一个，便于对照
 * 《接口约定文档》），这里只放被回调的实现，避免主类被业务细节撑爆。</p>
 *
 * <ul>
 *   <li>{@link #bind()}：绑定 {@link Bridge#RESYNC_ALL}；</li>
 *   <li>{@link #onPlayerLoggedIn(Player)}：玩家进服时发一次配置快照；</li>
 *   <li>{@link #sendSnapshot(ServerPlayer)}：单个玩家下发（C2S 请求也走它）；</li>
 *   <li>{@link #resyncAll()}：全部在线玩家重新下发。</li>
 * </ul>
 */
public final class NeoEvents {

    private NeoEvents() {
    }

    /** 由主类在构造里调用一次，把平台能力注入 {@link Bridge}。 */
    public static void bind() {
        Bridge.RESYNC_ALL = NeoEvents::resyncAll;
    }

    // ------------------------------------------------------------------
    // 事件实现
    // ------------------------------------------------------------------

    /** {@code PlayerEvent.PlayerLoggedInEvent} 的实现。 */
    public static void onPlayerLoggedIn(Player player) {
        Bridge.SERVER_PRESENT = true;
        if (player instanceof ServerPlayer serverPlayer) {
            sendSnapshot(serverPlayer);
        }
    }

    // ------------------------------------------------------------------
    // 下发
    // ------------------------------------------------------------------

    /** 给单个玩家发一份当前生效配置的快照。 */
    public static void sendSnapshot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // 配置里允许整体关掉同步（关掉后客户端 UI 退回原版显示，游戏逻辑不受影响）
        if (!Bridge.config().syncToClient) {
            return;
        }
        try {
            Bridge.SEND_TO_CLIENT.accept(player, Bridge.snapshot());
        } catch (Throwable t) {
            InfiniteStacks.warn("向 {} 下发配置失败: {}", player.getName().getString(), t.toString());
        }
    }

    /** 向所有在线玩家重新下发配置（{@link Bridge#RESYNC_ALL} 的实现）。 */
    public static void resyncAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        Bridge.SERVER_PRESENT = true;
        if (!Bridge.config().syncToClient) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSnapshot(player);
        }
    }
}
