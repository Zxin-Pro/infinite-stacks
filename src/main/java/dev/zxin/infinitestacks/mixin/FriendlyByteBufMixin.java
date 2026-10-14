package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ≤1.20.4 的**网络通道**扩展。
 *
 * <p>原版 {@code FriendlyByteBuf.writeItem} 用 {@code writeByte(count)} 传数量，
 * 上限 127。只改 NBT 不改网络 → 客户端显示和服务端不一致（回滚/复制物品）。</p>
 *
 * <p><b>协议设计</b>（两端都必须装本模组）：</p>
 * <pre>
 *   count &lt;= 127 : [byte count]
 *   count &gt;  127 : [byte 127][int count]
 * </pre>
 * <p>用 127 作为"后面还有一个 int"的哨兵 —— 原版正常值也可能是 127，此时多传 4 字节，
 * 无损。读侧遇到 127 就多读一个 int，再在 {@code readItem} 返回前把真实数量写回去。</p>
 */
@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin {

    private static final ThreadLocal<Integer> INFINITESTACKS_PENDING = new ThreadLocal<Integer>();

    private static final int INFINITESTACKS_SENTINEL = 127;

    @Redirect(
            method = "writeItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/FriendlyByteBuf;writeByte(I)Lnet/minecraft/network/FriendlyByteBuf;"),
            require = 0)
    private FriendlyByteBuf infinitestacks$writeCount(FriendlyByteBuf self, int count) {
        if (count > INFINITESTACKS_SENTINEL) {
            self.writeByte(INFINITESTACKS_SENTINEL);
            self.writeInt(count);
        } else {
            self.writeByte(count);
        }
        return self;
    }

    @Inject(method = "readItem", at = @At("HEAD"), require = 0)
    private void infinitestacks$resetPending(CallbackInfoReturnable<ItemStack> cir) {
        INFINITESTACKS_PENDING.remove();
    }

    @Redirect(
            method = "readItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/FriendlyByteBuf;readByte()B"),
            require = 0)
    private byte infinitestacks$readCount(FriendlyByteBuf self) {
        byte b = self.readByte();
        if (b == (byte) INFINITESTACKS_SENTINEL) {
            INFINITESTACKS_PENDING.set(self.readInt());
        } else {
            INFINITESTACKS_PENDING.remove();
        }
        return b;
    }

    @Inject(method = "readItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void infinitestacks$restoreCount(CallbackInfoReturnable<ItemStack> cir) {
        Integer pending = INFINITESTACKS_PENDING.get();
        INFINITESTACKS_PENDING.remove();
        if (pending == null || pending <= 0) {
            return;
        }
        ItemStack stack = cir.getReturnValue();
        if (stack != null && !stack.isEmpty()) {
            stack.setCount(StackLimits.clampLimit(pending));
        }
    }
}
