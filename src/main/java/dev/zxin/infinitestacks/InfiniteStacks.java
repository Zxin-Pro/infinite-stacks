package dev.zxin.infinitestacks;

import dev.zxin.infinitestacks.util.Ids;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 公共门面：常量、命名空间工具、日志。加载器无关。 */
public final class InfiniteStacks {

    public static final String MOD_ID = "infinitestacks";
    public static final String MOD_NAME = "Infinite Stacks";
    public static final String MOD_VERSION = "1.0.0";

    /** 全部三加载器共用的日志器。 */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private InfiniteStacks() {
    }

    public static ResourceLocation id(String path) {
        return Ids.of(MOD_ID, path);
    }

    public static void info(String message, Object... args) {
        LOGGER.info("[Infinite Stacks] " + message, args);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn("[Infinite Stacks] " + message, args);
    }

    public static void error(String message, Object... args) {
        LOGGER.error("[Infinite Stacks] " + message, args);
    }
}
