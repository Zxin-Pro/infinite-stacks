package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ★ 核心 Mixin（仅 MC ≤ 1.20.4）。
 *
 * <p>1.20.5 起原版把堆叠上限搬进了 DataComponent，{@code Item} 上不再有
 * {@code getMaxStackSize()}，因此这个 Mixin 只在 1.20.1-1.20.4 生效，
 * 新版由 {@code ItemInstanceMixin} 接管。</p>
 *
 * <p>注入点选 {@code RETURN} 而不是 {@code HEAD}，好处是能顺手拿到**原版真实返回值**
 * 并存进 {@link StackLimits}，这样「耐久物品不受影响」分支可以直接复用，
 * 不需要用 {@code @Shadow} 去碰私有字段 {@code maxStackSize}（字段名跨版本会变）。</p>
 */
@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true, require = 0)
    private void infinitestacks$overrideMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        Item self = (Item) (Object) this;
        Integer vanilla = cir.getReturnValue();
        if (vanilla != null) {
            StackLimits.rememberVanilla(self, vanilla);
        }
        int limit = StackLimits.limitForItem(self);
        if (limit > 0) {
            cir.setReturnValue(limit);
        }
    }
}
