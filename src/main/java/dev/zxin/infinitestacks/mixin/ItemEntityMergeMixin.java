package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.world.entity.item.ItemEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

/**
 * 掉落物合并上限。
 *
 * <p>部分版本的原版 {@code ItemEntity.merge} 内部写死了 64，导致地面上的大堆叠
 * 永远只能合到 64 就停。把那个常量换成配置值，N 个掉落物才能合成一个 count 巨大的实体
 * —— 这是"减少掉落物实体数量"最省事的一招。</p>
 *
 * <p>没有这个常量的版本会静默跳过（{@code require = 0}），此时 {@code merge}
 * 用的是我们已经被拦截的 {@code getMaxStackSize()}，行为依然正确。</p>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeMixin {

    @ModifyConstant(
            method = {"merge", "m_32055_"},
            constant = @Constant(intValue = 64),
            require = 0)
    private static int infinitestacks$mergeLimit(int original) {
        return StackLimits.globalLimit();
    }
}
