package dev.zxin.infinitestacks.fabric;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.client.ClientHooks;
import dev.zxin.infinitestacks.config.ConfigManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric 客户端入口。
 *
 * <p>只做三件事：读本地配置做回退、注册客户端接收器、断开时清缓存。
 * 按键绑定这里没有注册——{@code KeyMapping} 的构造器在 1.21.9+/26.x
 * 把 category 参数换成了 {@code KeyMapping.Category}，跨 11 个版本风险太高，
 * 客户端的「请求同步」目前由服务端进服时主动下发覆盖，功能不受影响。</p>
 */
public class InfiniteStacksFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Bridge.CONFIG_DIR = () -> FabricLoader.getInstance().getConfigDir();

        // 客户端也读一份本地配置：既作为「服务端没装/还没同步」时的回退，
        // 也提供纯客户端的渲染旋钮（标签上限、高度偏移等）。
        ConfigManager.reload(false);

        FabricNetwork.registerClient();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientHooks.onDisconnect());
    }
}
