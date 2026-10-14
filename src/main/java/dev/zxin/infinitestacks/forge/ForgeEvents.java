package dev.zxin.infinitestacks.forge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.command.StackCommand;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Forge 游戏总线（{@code FORGE_BUS}）事件集合。由主类 {@code MinecraftForge.EVENT_BUS.register(...)} 注册。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>{@code RegisterCommandsEvent} → {@link StackCommand#register}（{@code /stack} / {@code /istacks}）</li>
 *   <li>{@code PlayerEvent.PlayerLoggedInEvent} → 给该玩家发一次 {@link Bridge#snapshot()}</li>
 *   <li>绑定 {@link Bridge#RESYNC_ALL}（{@code /stack} 改配置后向全部在线玩家重新下发）</li>
 * </ul>
 *
 * <p><b>服务端权威</b>：客户端永远只是接收方，这里不做任何来自客户端的配置变更。</p>
 */
public final class ForgeEvents {

    private ForgeEvents() {
    }

    /** 由主类在构造里调用一次，把平台能力注入 {@link Bridge}。 */
    public static void bind() {
        Bridge.RESYNC_ALL = ForgeEvents::resyncAll;
    }

    // ------------------------------------------------------------------
    // 事件
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        StackCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Bridge.SERVER_PRESENT = true;
        if (event.getEntity() instanceof ServerPlayer player) {
            sendSnapshot(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 客户端缓存由客户端自己在断线时清掉（ClientStackState.clear），服务端这里无需处理。
    }

    // ------------------------------------------------------------------
    // 下发
    // ------------------------------------------------------------------

    /** 给单个玩家发一份当前生效配置的快照。 */
    public static void sendSnapshot(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // 配置里允许整体关掉同步（关掉后客户端 UI 退回原版显示，逻辑不受影响）
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
