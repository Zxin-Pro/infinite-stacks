package dev.zxin.infinitestacks.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.zxin.infinitestacks.client.DropLabelRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ★ 掉落物数字标签 Mixin（MC ≤ 1.21.8）。
 *
 * <p>在 {@code ItemEntityRenderer#render} 的 RETURN 处补画数量标签。选 RETURN 而不是
 * HEAD 的理由：原版方法内部已经把 poseStack 平移/旋转/缩放做完又还原了，
 * 我们在 RETURN 处拿到的是干净的「实体局部坐标」位姿，自己再摆一次即可，
 * 不会和原版的模型绘制互相污染。</p>
 *
 * <p>1.21.2 起原版移除了 {@code entityYaw} 参数（渲染状态重构），
 * 1.21.4 起 render 的入参从实体换成了 {@code ItemEntityRenderState}，
 * 两条路径用 {@code //#if} 分开。</p>
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"), require = 0)
    private void infinitestacks$drawCountLabel(ItemEntityRenderState state, PoseStack poseStack,
                                               MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (state == null) {
            return;
        }
        DropLabelRenderer.drawInWorld(poseStack, buffer, packedLight, state.x, state.y, state.z, state.count);
    }

}
