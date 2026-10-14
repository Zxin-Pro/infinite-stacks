package dev.zxin.infinitestacks.neoforge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.command.StackCommand;
import dev.zxin.infinitestacks.config.ConfigManager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge 入口（1.20.6 / 1.21.1 / 1.21.4 / 1.21.8 / 1.21.11 / 26.1 / 26.2 / 26.3）。
 *
 * <p>公共代码不动，这里只做装配：</p>
 * <ol>
 *   <li>{@link Bridge#CONFIG_DIR} ← {@code FMLPaths.CONFIGDIR.get()}；</li>
 *   <li>{@link ConfigManager#reload(boolean)} 首次读盘（无玩家，不下发）；</li>
 *   <li>模组总线：{@link NeoNetwork#register}（{@code RegisterPayloadHandlersEvent}）；</li>
 *   <li>游戏总线：{@code RegisterCommandsEvent} → {@link StackCommand#register}，
 *       {@code PlayerEvent.PlayerLoggedInEvent} → 给该玩家发一次 {@link Bridge#snapshot()}，
 *       并绑定 {@link Bridge#RESYNC_ALL}；</li>
 *   <li>客户端初始化 {@link NeoClientSetup#init}。</li>
 * </ol>
 *
 * <p><b>物理端判断的版本差异</b>（《接口约定文档》§8 断裂点 9）：
 * {@code net.neoforged.fml.loading.FMLEnvironment.dist} 在 MC ≥ 1.21.11 被移除。
 * 1.21.11 起改用只在客户端触发的 {@code FMLClientSetupEvent}（它注册在模组总线上，
 * 专用服务端永远不会收到该事件），因此 {@link NeoClientSetup} 这个客户端类在服务端
 * 永远不会被加载。{@code ModContainer#getEventBus()} 拿到的仍是模组总线，无法判断物理端，
 * 所以没有采用它。</p>
 *
 * <p><b>配置全部走 JSON</b>（{@code config/infinitestacks.json} +
 * {@code config/infinitestacks.overrides.json}），因此这里**不注册任何 ModConfigSpec**，
 * 也就无所谓 26.3 的 {@code ModConfig.Type.COMMON → SYNCED} 改名。</p>
 */
@Mod(InfiniteStacks.MOD_ID)
public final class InfiniteStacksNeoForge {

    public InfiniteStacksNeoForge(IEventBus modBus, ModContainer container) {
        // 1) 配置目录 + 首次读盘
        Bridge.CONFIG_DIR = () -> FMLPaths.CONFIGDIR.get();
        ConfigManager.reload(false);

        // 2) 网络（模组总线）
        modBus.addListener(NeoNetwork::register);

        // 3) 游戏总线：事件 + Bridge.RESYNC_ALL
        NeoEvents.bind();
        NeoForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) -> StackCommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedInEvent event) -> NeoEvents.onPlayerLoggedIn(event.getEntity()));

        // 4) 客户端初始化

        if (FMLEnvironment.dist.isClient()) {
            NeoClientSetup.init(modBus);
        }

        InfiniteStacks.info("Infinite Stacks 已加载 (NeoForge)");
    }
}
