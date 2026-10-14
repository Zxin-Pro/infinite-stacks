package dev.zxin.infinitestacks.fabric;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.command.StackCommand;
import dev.zxin.infinitestacks.config.ConfigManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric 主入口（服务端 + 客户端都会执行 {@link #onInitialize()}）。
 *
 * <p>职责：把加载器能力注入 {@link Bridge}，然后交给公共代码。</p>
 */
public class InfiniteStacksFabric implements ModInitializer {

    /** 当前服务器实例，供 RESYNC_ALL 使用。 */
    private static volatile MinecraftServer server;

    @Override
    public void onInitialize() {
        // 1. 平台桥
        Bridge.CONFIG_DIR = () -> FabricLoader.getInstance().getConfigDir();
        Bridge.SERVER_PRESENT = true;

        // 2. 读配置（此时不重同步——网络还没注册）
        ConfigManager.reload(false);

        // 3. 网络：两端都要注册载荷类型
        FabricNetwork.registerCommon();
        FabricNetwork.registerServer();

        // 4. 指令
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> StackCommand.register(dispatcher));

        // 5. 服务器生命周期（给 RESYNC_ALL 用）
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);

        Bridge.RESYNC_ALL = () -> {
            MinecraftServer current = server;
            if (current == null) {
                return;
            }
            for (ServerPlayer player : current.getPlayerList().getPlayers()) {
                Bridge.SEND_TO_CLIENT.accept(player, Bridge.snapshot());
            }
        };

        // 6. 玩家进服时下发一次配置
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) ->
                Bridge.SEND_TO_CLIENT.accept(handler.getPlayer(), Bridge.snapshot()));

        InfiniteStacks.info("Infinite Stacks (Fabric) 初始化完成 · 全局堆叠上限 = {}",
                Bridge.config().globalLimit);
    }
}
