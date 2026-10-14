package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.Bridge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ★ 突破 1.20.4 及以前「单字节数量」上限的 Mixin（仅 MC ≤ 1.20.4）。
 *
 * <p>原版把数量写成 {@code CompoundTag.putByte("Count", (byte) count)}，
 * 读的时候 {@code getByte("Count")}。>127 会被截断成负数 → 物品直接消失。
 * 这里改成用 IntTag 存：</p>
 *
 * <ul>
 *   <li>{@code save} 的 RETURN 处：数量 &gt; 127 时覆盖成 {@code putInt("Count", n)}</li>
 *   <li>{@code of} 的 {@code getByte} 调用：若发现是 IntTag，先返回 1（保证物品不为空、
 *       不会被原版的 count==0 检查丢掉）</li>
 *   <li>{@code of} 的 RETURN 处：再把真实数量 setCount 回去</li>
 * </ul>
 *
 * <p><b>兼容性警告</b>：写入 IntTag 后，**原版客户端/服务端读同一个存档会看到数量 0**
 * （getByte 读到类型不符返回 0）。因此该功能由 {@code nbt_int_extension} 控制，
 * 默认在 1.20.4 及以前是开启的，但全局上限默认仍为 127——只有管理员主动把上限调高，
 * 才会真正写出 IntTag。文档必须保留这条警告。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackNbtMixin {

    @Inject(method = "save", at = @At("RETURN"), require = 0)
    private void infinitestacks$saveIntCount(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (!Bridge.config().nbtIntExtension) {
            return;
        }
        CompoundTag out = cir.getReturnValue();
        ItemStack self = (ItemStack) (Object) this;
        if (out == null || self.isEmpty()) {
            return;
        }
        int count = self.getCount();
        if (count > 127) {
            out.putInt("Count", count);
        }
    }

    /** 读到 IntTag 时先给个占位值 1，真实值在 RETURN 处回填。 */
    @Redirect(method = "of", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getByte(Ljava/lang/String;)B"))
    private static byte infinitestacks$readCountPlaceholder(CompoundTag tag, String key) {
        if ("Count".equals(key) && tag.contains("Count", Tag.TAG_INT)) {
            return 1;
        }
        return tag.getByte(key);
    }

    @Inject(method = "of", at = @At("RETURN"), cancellable = true, require = 0)
    private static void infinitestacks$restoreCount(CompoundTag tag, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (tag.contains("Count", Tag.TAG_INT)) {
            stack.setCount(tag.getInt("Count"));
        }
    }
}
