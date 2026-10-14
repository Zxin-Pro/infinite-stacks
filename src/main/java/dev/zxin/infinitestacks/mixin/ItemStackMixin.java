package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;
import dev.zxin.infinitestacks.logic.StackMath;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ★ 核心 Mixin（全版本）。
 *
 * <p>两件事：</p>
 * <ol>
 *   <li><b>堆叠上限</b>（仅 MC ≤ 1.21.11）：1.20.5+ 原版把上限放进 DataComponent，
 *       {@code ItemStack#getMaxStackSize()} 是唯一的读取入口，钩这里最稳。
 *       26.x 没有这个方法，改由 {@code ItemInstanceMixin} 负责，所以用 {@code //#if} 排掉。</li>
 *   <li><b>溢出保护</b>（全版本）：原版 {@code grow(int)} 是裸的 {@code count += n}，
 *       数量接近 int 最大值时会翻负 → 物品变成负数/消失。这里在真正会溢出时接管，
 *       用 long 中间量饱和钳制。只有溢出才 cancel，正常路径零行为变化。</li>
 * </ol>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "grow", at = @At("HEAD"), cancellable = true, require = 0)
    private void infinitestacks$growSafe(int amount, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.isEmpty() || amount <= 0) {
            return;
        }
        int current = self.getCount();
        int limit = StackLimits.limitFor(self);
        if (limit <= 0) {
            return;
        }
        if ((long) current + (long) amount > (long) limit) {
            self.setCount(StackMath.clampLong(limit));
            ci.cancel();
        }
    }
}
