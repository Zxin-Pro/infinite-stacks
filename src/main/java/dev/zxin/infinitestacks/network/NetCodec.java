package dev.zxin.infinitestacks.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 纯字节编解码。三加载器共用同一份线格式，保证跨端一致。
 *
 * <p>直接用原版的 {@link FriendlyByteBuf}（Forge 的 FriendlyByteBuf 与 Fabric 的
 * PacketByteBuf 都是同一个类），因此不需要任何适配层。</p>
 *
 * <p>服务端是唯一权威：客户端收到的只是**渲染用**快照。</p>
 */
public final class NetCodec {

    private NetCodec() {
    }

    // ------------------------------------------------------------------
    // S2C
    // ------------------------------------------------------------------

    public static void writeSync(FriendlyByteBuf buf, Packets.Sync data) {
        Packets.Sync src = data == null ? new Packets.Sync() : data;

        int version = Packets.PROTOCOL_VERSION;
        boolean omit = src.overridesOmitted || estimate(src) > Packets.MAX_BYTES;
        if (omit) {
            version |= Packets.FLAG_OVERRIDES_OMITTED;
        }

        buf.writeVarInt(version);
        buf.writeVarInt(src.globalLimit);
        buf.writeVarInt(src.permissionLevel);
        buf.writeBoolean(src.affectDamageable);
        buf.writeBoolean(src.dropsEnabled);
        buf.writeVarInt(src.dropsThreshold);
        buf.writeVarInt(src.dropsMaxModels);
        buf.writeVarInt(src.dropsHideModelAbove);
        buf.writeFloat(src.dropsMergeRadius);
        buf.writeFloat(src.dropsScale);
        buf.writeVarInt(src.dropsColor);
        buf.writeFloat(src.dropsBgOpacity);
        buf.writeBoolean(src.dropsShadow);
        buf.writeVarInt(src.uiMode);
        buf.writeVarInt(src.uiThreshold);

        if (!omit) {
            writeMap(buf, src.items);
            writeMap(buf, src.tags);
        }
    }

    public static Packets.Sync readSync(FriendlyByteBuf buf) {
        Packets.Sync out = new Packets.Sync();
        int version = buf.readVarInt();
        boolean omit = (version & Packets.FLAG_OVERRIDES_OMITTED) != 0;

        out.overridesOmitted = omit;
        out.globalLimit = buf.readVarInt();
        out.permissionLevel = buf.readVarInt();
        out.affectDamageable = buf.readBoolean();
        out.dropsEnabled = buf.readBoolean();
        out.dropsThreshold = buf.readVarInt();
        out.dropsMaxModels = buf.readVarInt();
        out.dropsHideModelAbove = buf.readVarInt();
        out.dropsMergeRadius = buf.readFloat();
        out.dropsScale = buf.readFloat();
        out.dropsColor = buf.readVarInt();
        out.dropsBgOpacity = buf.readFloat();
        out.dropsShadow = buf.readBoolean();
        out.uiMode = buf.readVarInt();
        out.uiThreshold = buf.readVarInt();

        if (!omit) {
            out.items = readMap(buf);
            out.tags = readMap(buf);
        }
        return out;
    }

    // ------------------------------------------------------------------
    // C2S（空载荷）
    // ------------------------------------------------------------------

    public static void writeRequest(FriendlyByteBuf buf) {
        buf.writeVarInt(Packets.PROTOCOL_VERSION);
    }

    public static Packets.Request readRequest(FriendlyByteBuf buf) {
        buf.readVarInt();
        return Packets.Request.INSTANCE;
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private static void writeMap(FriendlyByteBuf buf, Map<String, Integer> map) {
        Map<String, Integer> src = map == null ? java.util.Collections.<String, Integer>emptyMap() : map;
        buf.writeVarInt(src.size());
        for (Map.Entry<String, Integer> entry : src.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue() == null ? 0 : entry.getValue());
        }
    }

    private static Map<String, Integer> readMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Integer> out = new LinkedHashMap<String, Integer>();
        if (size < 0 || size > 4096) {
            return out;
        }
        for (int i = 0; i < size; i++) {
            out.put(buf.readUtf(), buf.readVarInt());
        }
        return out;
    }

    private static int estimate(Packets.Sync data) {
        int size = 64;
        size += data.items.size() * 40;
        size += data.tags.size() * 40;
        return size;
    }
}
