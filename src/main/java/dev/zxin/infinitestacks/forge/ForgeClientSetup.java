package dev.zxin.infinitestacks.forge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.client.ClientHooks;
import dev.zxin.infinitestacks.network.Packets;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Forge 客户端初始化（**只在客户端加载**，由主类用 {@code FMLEnvironment.dist.isClient()} 隔离）。
 *
 * <p>做两件事：</p>
 * <ol>
 *   <li>绑定 {@link Bridge#SEND_TO_SERVER}：按键时向服务端要一份最新配置。
 *       1.20.1 用 {@code SimpleChannel#sendToServer(message)}，1.20.2+ 改用
 *       {@code Channel#send(message, PacketDistributor.SERVER.noArg())}。</li>
 *   <li>注册热键 {@code K}：在客户端 tick 里轮询 {@code KeyMapping#consumeClick()}，
 *       顺手处理「退出世界 → 清掉服务端配置缓存」这个边沿。</li>
 * </ol>
 *
 * <p>热键名用字面量而不是翻译键，避免依赖资源文件；如需本地化，在
 * {@code assets/infinitestacks/lang/*.json} 里补 {@code key.infinitestacks.request_config} 即可。</p>
 */
public final class ForgeClientSetup {

    /** 热键分类（字面量，避免依赖 lang 文件）。 */
    private static final String CATEGORY = "Infinite Stacks";

    /** 主动向服务端请求配置的快照热键。 */
    public static final KeyMapping REQUEST_CONFIG = new KeyMapping(
            "Request Stack Config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    /** 上一 tick 是否在世界里，用来检测「断开连接」这个边沿。 */
    private static boolean wasInWorld = false;

    private ForgeClientSetup() {
    }

    /** 仅客户端调用（主类构造里，{@code FMLEnvironment.dist.isClient()} 为 true 时）。 */
    public static void init(IEventBus modBus) {
        // C2S：客户端 → 服务端。请求是空载荷，参数只是 Bridge 的占位签名。

        Bridge.SEND_TO_SERVER = ignored -> ForgeNetwork.CHANNEL.sendToServer(Packets.Request.INSTANCE);

        // 热键必须在模组总线上注册（RegisterKeyMappingsEvent 在模组加载期触发）
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(REQUEST_CONFIG));

        // 客户端 tick（游戏总线）
        MinecraftForge.EVENT_BUS.register(ForgeClientSetup.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
