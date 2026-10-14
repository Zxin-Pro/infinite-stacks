package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ★ 核心 Mixin（仅 MC ≥ 26.1）。
 *
 * <p>26.x 里 {@code ItemStack.getMaxStackSize()} **不再是 ItemStack 自己的方法**，
 * 而是它实现的接口 {@code net.minecraft.world.item.ItemInstance} 的默认方法
 * （已用 class 文件解析实测确认：ItemStack 的 interfaces 列表 =
 * [DataComponentHolder, ItemInstance]，且 ItemInstance 声明了 getMaxStackSize）。
 * 所以钩子必须打在接口的默认方法上。</p>
 *
 * <p>接口里拿不到 {@code ItemStack} 的静态类型，因此用 instanceof 过滤，
 * 保证只对真正的物品堆叠生效。</p>
 */
@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true, require = 0)
    private void infinitestacks$overrideMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (!(self instanceof ItemStack)) {
            return;
        }
        ItemStack stack = (ItemStack) self;
        if (stack.isEmpty()) {
            return;
        }
        int limit = StackLimits.limitFor(stack);
        if (limit > 0) {
            cir.setReturnValue(limit);
        }
    }
}
