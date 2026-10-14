package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ★ 容器兼容 Mixin（全版本，受 {@code general.container_expansion} 控制）。
 *
 * <p>原版 {@code Slot#getMaxStackSize()} 返回的是**容器自己的**上限
 * （箱子/漏斗是 64，玩家背包是 99）。不改它的话，Shift 一键搬运、
 * 漏斗传输、快速移动都只会搬 64 个，超大堆叠会被硬生生截断。</p>
 *
 * <p>注意：这里**只放大上限**，不改变任何搬运算法本身；
 * 关掉 {@code container_expansion} 即可完全回到原版行为。</p>
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true, require = 0)
    private void infinitestacks$expandSlotLimit(CallbackInfoReturnable<Integer> cir) {
        if (!StackLimits.containerExpansion()) {
            return;
        }
        int limit = StackLimits.globalLimit();
        if (limit > 64) {
            cir.setReturnValue(limit);
        }
    }
}
