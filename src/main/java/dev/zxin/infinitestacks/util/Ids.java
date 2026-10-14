package dev.zxin.infinitestacks.util;

import net.minecraft.resources.ResourceLocation;

/**
 * 版本差异隔离：ResourceLocation / Identifier 的创建与解析。
 * 1.20.1-1.20.4 用 new ResourceLocation(...)；1.21+ 用 fromNamespaceAndPath；1.21.11+ 换类名。
 */
public final class Ids {

    private Ids() {
    }

    public static ResourceLocation tryParse(String value) {
        try {
            return ResourceLocation.tryParse(value);
        } catch (Throwable t) {
            return null;
        }
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

}
