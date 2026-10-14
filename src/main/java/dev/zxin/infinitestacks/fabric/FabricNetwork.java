package dev.zxin.infinitestacks.fabric;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.client.ClientHooks;
import dev.zxin.infinitestacks.network.NetCodec;
import dev.zxin.infinitestacks.network.Packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
    // 新载荷 API（1.20.5+）
    // ------------------------------------------------------------------

    public record SyncPayload(Packets.Sync data) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncPayload> TYPE =
                new CustomPacketPayload.Type<>(InfiniteStacks.id(Packets.CHANNEL_SYNC));

        public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
                (buf, payload) -> NetCodec.writeSync(buf, payload.data()),
                buf -> new SyncPayload(NetCodec.readSync(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestPayload() implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<RequestPayload> TYPE =
                new CustomPacketPayload.Type<>(InfiniteStacks.id(Packets.CHANNEL_REQUEST));

        public static final StreamCodec<FriendlyByteBuf, RequestPayload> CODEC = StreamCodec.of(
                (buf, payload) -> NetCodec.writeRequest(buf),
                buf -> {
                    NetCodec.readRequest(buf);
                    return new RequestPayload();
                });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 两端都要调用（注册载荷类型）。 */
    public static void registerCommon() {

        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestPayload.TYPE, RequestPayload.CODEC);

    }

    public static void registerServer() {
        Bridge.SEND_TO_CLIENT = FabricNetwork::sendToClient;
        ServerPlayNetworking.registerGlobalReceiver(RequestPayload.TYPE, (payload, context) ->
                context.server().execute(() -> handleRequest(context.player())));
    }

    public static void registerClient() {
        Bridge.SEND_TO_SERVER = FabricNetwork::sendToServer;
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientHooks.onSyncReceived(payload.data())));
    }

    private static void sendToClient(ServerPlayer player, Packets.Sync data) {
        ServerPlayNetworking.send(player, new SyncPayload(data));
    }

    private static void sendToServer(Packets.Sync ignored) {
        ClientPlayNetworking.send(new RequestPayload());
    }

    /** 客户端请求配置：把当前快照回发过去。 */
    private static void handleRequest(ServerPlayer player) {
        Bridge.SEND_TO_CLIENT.accept(player, Bridge.snapshot());
    }
}
