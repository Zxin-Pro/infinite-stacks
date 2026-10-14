package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 创造模式改数量的服务端校验。
 *
 * <p>原版 {@code ServerGamePacketListenerImpl.onCreativeInventoryAction} 里写死 64：
 * 创造的物品栏里把数量改成 &gt;64 会被服务端判非法，轻则丢弃重则直接 desync。
 * 这里把那个常量抬到配置上限。</p>
 *
 * <p>方法名和常量位置在各版本之间有漂移，全部 {@code require = 0} 软处理。</p>
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @ModifyConstant(
            method = {"handleSetCreativeModeSlot", "onCreativeInventoryAction", "m_6772_"},
            constant = @Constant(intValue = 64),
            require = 0)
    private int infinitestacks$creativeLimit(int original) {
        return StackLimits.globalLimit();
    }
}
