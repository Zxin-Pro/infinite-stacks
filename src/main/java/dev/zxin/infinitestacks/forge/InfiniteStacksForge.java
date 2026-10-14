package dev.zxin.infinitestacks.forge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.config.ConfigManager;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge 入口（1.20.1 / 1.20.2 / 1.20.4）。
 *
 * <p>只做四件事，业务逻辑一律在 {@code dev.zxin.infinitestacks} 公共代码里：</p>
 * <ol>
 *   <li>把配置目录注入 {@link Bridge#CONFIG_DIR}（Forge 用 {@code FMLPaths.CONFIGDIR}）；</li>
 *   <li>首次读盘 {@link ConfigManager#reload(boolean)}（此时还没有玩家，不需要下发）；</li>
 *   <li>注册网络（{@link ForgeNetwork}）与游戏总线事件（{@link ForgeEvents}）；</li>
 *   <li>仅客户端时跑 {@link ForgeClientSetup#init()}。</li>
 * </ol>
 *
 * <p><b>配置全部走 JSON</b>（{@code config/infinitestacks.json} +
 * {@code config/infinitestacks.overrides.json}），因此这里**不注册任何 ForgeConfigSpec**。</p>
 */
@Mod(InfiniteStacks.MOD_ID)
public final class InfiniteStacksForge {

    public InfiniteStacksForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1) 配置目录 + 首次读盘。Bridge.CONFIG_DIR 必须在任何 Overrides/ConfigManager 调用之前就位。
        Bridge.CONFIG_DIR = () -> FMLPaths.CONFIGDIR.get();
        ConfigManager.reload(false);

        // 2) 网络：注册消息并绑定 Bridge.SEND_TO_CLIENT（服务端 → 客户端）
        ForgeNetwork.register();

        // 3) 游戏总线：指令注册、玩家进服下发快照、Bridge.RESYNC_ALL
        ForgeEvents.bind();
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);

        // 4) 客户端专用初始化：C2S 发送器 + 热键。用 FMLEnvironment.dist 判断，
        //    这样 ForgeClientSetup（客户端类）不会被专用服务端加载。
        if (FMLEnvironment.dist.isClient()) {
            ForgeClientSetup.init(modBus);
        }

        InfiniteStacks.info("Infinite Stacks 已加载 (Forge)");
    }
}
