package dev.zxin.infinitestacks.neoforge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.client.ClientHooks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import net.neoforged.neoforge.network.PacketDistributor;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import org.lwjgl.glfw.GLFW;

/**
 * NeoForge 客户端初始化（**只在客户端加载**）。
 *
 * <p>由 {@link InfiniteStacksNeoForge} 触发：</p>
 * <ul>
 *   <li>MC &lt; 1.21.11：主类用 {@code FMLEnvironment.dist.isClient()} 判断后直接调 {@code init(modBus)}；</li>
 *   <li>MC ≥ 1.21.11：{@code FMLEnvironment.dist} 被移除，改由只在客户端触发的
 *       {@code FMLClientSetupEvent} 调 {@code init(modBus)}。</li>
 * </ul>
 *
 * <p>做两件事：绑定 {@link Bridge#SEND_TO_SERVER}（客户端 → 服务端请求配置），
 * 以及维护客户端 tick（检测「退出世界」边沿，清掉服务端配置缓存）。</p>
 *
 * <p><b>热键</b>：只在 MC &lt; 1.21.11 注册。原因有两条：一是
 * {@code RegisterKeyMappingsEvent} 必须挂在模组总线上、且在模组加载期触发，
 * 而 1.21.11+ 的客户端初始化已经晚于该时点；二是 {@code KeyMapping} 的分类参数
 * 在 26.x 由 {@code String} 换成了 {@code KeyMapping.Category} 对象。
 * 需要热键时按 {@code //#if} 补即可，公共代码的 {@code ClientHooks.requestConfig()}
 * 已经是现成入口。</p>
 */
public final class NeoClientSetup {

    /** 上一 tick 是否在世界里，用来检测「断开连接」这个边沿。 */
    private static boolean wasInWorld = false;

    /** 热键分类（字面量，避免依赖 lang 文件）。 */
    private static final String CATEGORY = "Infinite Stacks";

    /** 主动向服务端请求配置的快照热键。 */
    public static final KeyMapping REQUEST_CONFIG = new KeyMapping(
            "Request Stack Config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    private NeoClientSetup() {
    }

    /**
     * 仅客户端调用。
     *
     * @param modBus 模组总线（MC ≥ 1.21.11 分支下不再需要它，保留参数是为了调用点统一）
     */
    public static void init(IEventBus modBus) {
        // C2S：客户端 → 服务端，空请求载荷

        Bridge.SEND_TO_SERVER = ignored -> PacketDistributor.sendToServer(new NeoNetwork.RequestPayload());

        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(REQUEST_CONFIG));

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> onClientTick());
    }

    private static void onClientTick() {
        boolean inWorld = ClientHooks.inWorld();
        if (wasInWorld && !inWorld) {
            // 退出世界 / 断开连接：清掉服务端配置缓存，避免下一个服务器沿用旧值
            ClientHooks.onDisconnect();
        }
        wasInWorld = inWorld;

        while (REQUEST_CONFIG.consumeClick()) {
            if (inWorld) {
                ClientHooks.requestConfig();
            }
        }

    }
}
