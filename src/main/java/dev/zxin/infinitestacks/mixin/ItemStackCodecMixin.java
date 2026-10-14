package dev.zxin.infinitestacks.mixin;

import com.mojang.serialization.Codec;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.20.5+ 的**数量 codec 范围**扩展。
 *
 * <p>原版把堆叠数量写死在 {@code ExtraCodecs.intRange(1, 99)} 里。
 * 不改这个，存档/指令/结构里任何 &gt;99 的数量都会在反序列化时被拒（
 * "Item stack with count of X was larger than maximum"），物品直接消失。</p>
 *
 * <p>lambda 序号在各版本之间会漂移（{@code lambda$static$1} / {@code lambda$static$3} /
 * Fabric 的 {@code method_57371}），所以这里把已知名字全列上，并全部 {@code require = 0}
 * —— 命中哪个算哪个，命中不了也不会崩，只是退回原版范围。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackCodecMixin {

    @Redirect(
            method = {
                    "lambda$static$0", "lambda$static$1", "lambda$static$2",
                    "lambda$static$3", "lambda$static$4", "lambda$static$5",
                    "method_57371", "method_33283", "method_57370"
            },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"),
            require = 0)
    private static Codec<Integer> infinitestacks$widenRange(int min, int max) {
        return ExtraCodecs.intRange(Math.max(0, min), StackLimits.ABSOLUTE_MAX);
    }
}
