package dev.zxin.infinitestacks.mixin;

import com.mojang.serialization.Codec;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.20.5+ 的 {@code MAX_STACK_SIZE} **数据组件**范围扩展。
 *
 * <p>原版组件上限是 {@code ExtraCodecs.intRange(1, 99)}，把单物品上限压回 99。
 * 与 {@link ItemStackCodecMixin} 一起，构成"组件范围 + 数量范围"双保险。</p>
 *
 * <p>注意：这里**没有**改 {@code COMMON_ITEM_COMPONENTS} 里的默认值 64 ——
 * 物品实际可用上限由 {@link dev.zxin.infinitestacks.mixin.ItemInstanceMixin}
 * 在 {@code ItemInstance#getMaxStackSize()} 上统一裁决，改默认值反而是
 * 一次大范围 {@code @ModifyConstant}，容易误伤同类的其它 64 常量。</p>
 */
@Mixin(DataComponents.class)
public abstract class DataComponentsMixin {

    @Redirect(
            method = {
                    "lambda$static$0", "lambda$static$1", "lambda$static$2",
                    "lambda$static$3", "lambda$static$4", "lambda$static$5",
                    "method_58570", "method_58569"
            },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"),
            require = 0)
    private static Codec<Integer> infinitestacks$widenRange(int min, int max) {
        return ExtraCodecs.intRange(Math.max(1, min), StackLimits.ABSOLUTE_MAX);
    }
}
