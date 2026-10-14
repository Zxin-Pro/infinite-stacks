package dev.zxin.infinitestacks.neoforge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.network.NetCodec;
import dev.zxin.infinitestacks.network.Packets;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge 网络胶水层（1.20.6 → 26.3）。
 *
 * <p>用原版 1.20.5+ 的 {@link CustomPacketPayload} + NeoForge 的
 * {@link PayloadRegistrar}（{@code event.registrar("1")}）。载荷只是
 * {@link Packets.Sync} / {@link Packets.Request} 的薄包装，序列化**一律**交给
 * {@link NetCodec}，因此线格式与 Forge/Fabric 完全一致（《接口约定文档》§5）。</p>
 *
 * <p>通道名**冻结**：{@code infinitestacks:sync_config}（S2C）与
 * {@code infinitestacks:request_config}（C2S）。</p>
 *
 * <p>版本差异：客户端发包在 1.21.5 起从 {@code PacketDistributor#sendToServer}
 * 换成 {@code net.neoforged.neoforge.client.network.ClientPacketDistributor#sendToServer}
 * （见 {@link NeoClientSetup}，用 {@code //#if MC>=12105} 分支）。服务端 → 客户端这一侧
 * 一直用 {@code PacketDistributor#sendToPlayer}，没有变化。</p>
 */
public final class NeoNetwork {

    /** 协议版本字符串（{@code event.registrar(...)}）。 */
    private static final String PROTOCOL = "1";

    private NeoNetwork() {
    }

    /** 模组总线监听：{@code modBus.addListener(NeoNetwork::register)}。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        // S2C：服务端 → 客户端，配置快照。处理器只在客户端执行，
        // 所以这里引用客户端类 ClientHooks 不会影响专用服务端（方法体懒解析）。
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> handleSync(payload.data())));

        // C2S：客户端 → 服务端，空请求。服务端收到后把配置回发给请求者。
        registrar.playToServer(RequestPayload.TYPE, RequestPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        NeoEvents.sendSnapshot(serverPlayer);
                    }
                }));

        // 注入「服务端 → 单个客户端」的发送能力
        Bridge.SEND_TO_CLIENT = (player, data) -> {
            if (player != null && data != null) {
                PacketDistributor.sendToPlayer(player, new SyncPayload(data));
            }
        };
    }

    // ------------------------------------------------------------------
    // 处理
    // ------------------------------------------------------------------

    /** 客户端收到 S2C 配置快照：只更新渲染用缓存，**不参与任何判定**。 */
    private static void handleSync(Packets.Sync data) {
        try {
            dev.zxin.infinitestacks.client.ClientHooks.onSyncReceived(data);
        } catch (Throwable t) {
            InfiniteStacks.warn("处理配置同步包失败: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------
    // 载荷
    // ------------------------------------------------------------------

    /**
     * S2C 载荷：{@code infinitestacks:sync_config}。
     *
     * <p>线格式完全由 {@link NetCodec#writeSync} / {@link NetCodec#readSync} 决定。</p>
     */
    public record SyncPayload(Packets.Sync data) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncPayload> TYPE =
                new CustomPacketPayload.Type<SyncPayload>(InfiniteStacks.id(Packets.CHANNEL_SYNC));

        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buffer, SyncPayload payload) -> NetCodec.writeSync(buffer, payload.data()),
                (RegistryFriendlyByteBuf buffer) -> new SyncPayload(NetCodec.readSync(buffer)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * C2S 载荷：{@code infinitestacks:request_config}（空载荷，线格式只有一个 varint 协议版本）。
     */
    public record RequestPayload() implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<RequestPayload> TYPE =
                new CustomPacketPayload.Type<RequestPayload>(InfiniteStacks.id(Packets.CHANNEL_REQUEST));

        public static final StreamCodec<RegistryFriendlyByteBuf, RequestPayload> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buffer, RequestPayload payload) -> NetCodec.writeRequest(buffer),
                (RegistryFriendlyByteBuf buffer) -> {
                    NetCodec.readRequest(buffer);
                    return new RequestPayload();
                });

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
