package dev.zxin.infinitestacks.client.mixin;

import dev.zxin.infinitestacks.client.DropLabelBatch;

import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;

import net.minecraft.client.renderer.entity.ItemEntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ★ 掉落物「模型数量」钳制 Mixin（全版本）。
 *
 * <p>原版本来就只在数量 &gt; 48 时最多画 5 个模型，但它写死成 5、不可配置。
 * 这里把返回值交给 {@code drops.max_models} 收口（默认 5 与原版一致，
 * 设成 1 可以把极端堆叠的开销压到最低）。</p>
 *
 * <p>目标方法在三代 API 里换了三次名字/归属：</p>
 * <ul>
 *   <li>1.20.1 / 1.20.4：{@code ItemEntityRenderer#getRenderAmount(ItemStack)}</li>
 *   <li>1.20.6 / 1.21.1：{@code ItemEntityRenderer#getRenderedAmount(int)}</li>
 *   <li>1.21.4+：搬到了渲染状态上 {@code ItemClusterRenderState#getRenderedAmount(int)}</li>
 * </ul>
 *
 * <p>一律 {@code require = 0}：这是**纯优化**，即使某个版本钩不上也绝不拖垮游戏。</p>
 */

@Mixin(ItemClusterRenderState.class)

public abstract class ItemRenderAmountMixin {

    @Inject(method = "getRenderedAmount", at = @At("RETURN"), cancellable = true, require = 0)
    private void infinitestacks$clampAmount(int count, CallbackInfoReturnable<Integer> cir) {
        Integer vanilla = cir.getReturnValue();
        if (vanilla == null) {
            return;
        }
        cir.setReturnValue(DropLabelBatch.clampModels(vanilla, count));
    }

}
