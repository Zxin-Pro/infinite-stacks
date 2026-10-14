package dev.zxin.infinitestacks.fabric;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.client.ClientHooks;
import dev.zxin.infinitestacks.network.NetCodec;
import dev.zxin.infinitestacks.network.Packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric 网络层。
 *
 * <p>1.20.5 起 Fabric API 换成了 CustomPayload + StreamCodec 的新接口；
 * 26.x 又把 PayloadTypeRegistry 的 playC2S/playS2C 改名成
 * serverboundPlay/clientboundPlay。三条路径都靠 {@code //#if} 分开。</p>
 *
 * <p>编解码一律走 {@link NetCodec}，它直接操作 FriendlyByteBuf。</p>
 */
public final class FabricNetwork {

    private FabricNetwork() {
    }

    // ------------------------------------------------------------------
    // 旧通道 API（1.20.1 / 1.20.2 / 1.20.4）
    // ------------------------------------------------------------------

    public static final ResourceLocation ID_SYNC = InfiniteStacks.id(Packets.CHANNEL_SYNC);
    public static final ResourceLocation ID_REQUEST = InfiniteStacks.id(Packets.CHANNEL_REQUEST);

    /** 旧 API 无需预注册载荷类型。 */
    public static void registerCommon() {
        // 故意留空
    }

    public static void registerServer() {
        Bridge.SEND_TO_CLIENT = FabricNetwork::sendToClient;
        ServerPlayNetworking.registerGlobalReceiver(ID_REQUEST,
                (server, player, handler, buf, sender) -> {
                    NetCodec.readRequest(buf);
                    server.execute(() -> handleRequest(player));
                });
    }

    public static void registerClient() {
        Bridge.SEND_TO_SERVER = FabricNetwork::sendToServer;
        ClientPlayNetworking.registerGlobalReceiver(ID_SYNC,
                (client, handler, buf, sender) -> {
                    Packets.Sync data = NetCodec.readSync(buf);
                    client.execute(() -> ClientHooks.onSyncReceived(data));
                });
    }

    private static void sendToClient(ServerPlayer player, Packets.Sync data) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        NetCodec.writeSync(buf, data);
        ServerPlayNetworking.send(player, ID_SYNC, buf);
    }

    private static void sendToServer(Packets.Sync ignored) {
        ClientPlayNetworking.send(ID_REQUEST, PacketByteBufs.empty());
    }

    /** 客户端请求配置：把当前快照回发过去。 */
    private static void handleRequest(ServerPlayer player) {
        Bridge.SEND_TO_CLIENT.accept(player, Bridge.snapshot());
    }
}
