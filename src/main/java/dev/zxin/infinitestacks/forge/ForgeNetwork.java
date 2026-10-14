package dev.zxin.infinitestacks.forge;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.network.NetCodec;
import dev.zxin.infinitestacks.network.Packets;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Forge 网络胶水层（1.20.1 / 1.20.2 / 1.20.4）。
 *
 * <p>载荷直接复用公共代码的 {@link Packets.Sync} / {@link Packets.Request}，
 * 编解码**一律**走 {@link NetCodec}（线格式见《接口约定文档》§5），
 * 所以三个加载器的字节流完全一致。</p>
 *
 * <p>版本差异（见《接口约定文档》§8 断裂点登记表）：</p>
 * <ul>
 *   <li><b>1.20.1</b>：{@code NetworkRegistry.newSimpleChannel(ResourceLocation, Supplier, Predicate, Predicate)}
 *       + {@code SimpleChannel.registerMessage(id, Class, encoder, decoder, consumer)}，
 *       发送顺序是 {@code send(target, message)} / {@code sendToServer(message)}。</li>
 *   <li><b>1.20.2 / 1.20.4</b>：{@code ChannelBuilder.named(...).networkProtocolVersion(1)
 *       .clientAcceptedVersions(Channel.VersionTest.ACCEPT_VANILLA).serverAcceptedVersions(...).simpleChannel()}，
 *       注册走 {@code messageBuilder(Class, id)}；发送顺序反过来，是 {@code send(message, target)}。</li>
 * </ul>
 *
 * <p>消息 id 与通道名**冻结**：0 = S2C {@code sync_config}，1 = C2S {@code request_config}。</p>
 */
public final class ForgeNetwork {

    /** 1.20.1 旧 API 的协议版本字符串。 */
    private static final String PROTOCOL = "1";

    /** 消息 id（冻结）：0 = S2C 配置快照。 */
    private static final int ID_SYNC = 0;

    /** 消息 id（冻结）：1 = C2S 空请求。 */
    private static final int ID_REQUEST = 1;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            InfiniteStacks.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ForgeNetwork() {
    }

    /** 由主类在构造里调用一次：注册两条消息并绑定 {@link Bridge#SEND_TO_CLIENT}。 */
    public static void register() {

        CHANNEL.registerMessage(ID_SYNC, Packets.Sync.class,
                (message, buffer) -> NetCodec.writeSync(buffer, message),
                NetCodec::readSync,
                (message, context) -> {
                    context.get().enqueueWork(() -> handleSync(message));
                    context.get().setPacketHandled(true);
                });

        CHANNEL.registerMessage(ID_REQUEST, Packets.Request.class,
                (message, buffer) -> NetCodec.writeRequest(buffer),
                NetCodec::readRequest,
                (message, context) -> {
                    context.get().enqueueWork(() -> handleRequest(context.get().getSender()));
                    context.get().setPacketHandled(true);
                });

        Bridge.SEND_TO_CLIENT = (player, data) -> {
            if (player != null && data != null) {
                // 1.20.1：Channel#send(target, message)
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), data);
            }
        };

    }

    // ------------------------------------------------------------------
    // 处理
    // ------------------------------------------------------------------

    /**
     * 客户端收到 S2C 配置快照：只更新渲染用缓存，**不参与任何判定**。
     *
     * <p>这里用全限定名引用 {@code client.ClientHooks}（客户端专用类）：
     * 方法体只在客户端执行时才会触发类加载，专用服务端永远不会碰到它。</p>
     */
    private static void handleSync(Packets.Sync data) {
        try {
            dev.zxin.infinitestacks.client.ClientHooks.onSyncReceived(data);
        } catch (Throwable t) {
            InfiniteStacks.warn("处理配置同步包失败: {}", t.toString());
        }
    }

    /** 服务端收到 C2S 空请求：把当前生效配置回发给请求者。 */
    private static void handleRequest(ServerPlayer sender) {
        ForgeEvents.sendSnapshot(sender);
    }
}
