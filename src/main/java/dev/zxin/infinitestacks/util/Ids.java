package dev.zxin.infinitestacks.util;

// MC 1.21.11 起 Mojang 把 ResourceLocation 统一重命名为 Identifier
import net.minecraft.resources.Identifier;

/**
 * 版本差异隔离：ResourceLocation / Identifier 的创建与解析。
 * 1.20.1-1.20.4 用 new ResourceLocation(...)；1.21+ 用 fromNamespaceAndPath；1.21.11+ 换类名。
 */
public final class Ids {

    private Ids() {
    }

    public static Identifier tryParse(String value) {
        try {
            return Identifier.tryParse(value);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Identifier of(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

}
