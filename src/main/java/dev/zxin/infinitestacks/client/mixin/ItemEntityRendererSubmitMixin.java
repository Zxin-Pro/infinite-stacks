package dev.zxin.infinitestacks.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.zxin.infinitestacks.client.DropLabelRenderer;

import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;

import net.minecraft.client.renderer.state.level.CameraRenderState;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ★ 掉落物数字标签 Mixin（MC ≥ 1.21.11）。
 *
 * <p>1.21.11 起实体渲染改成了 submit-node 管线：{@code ItemEntityRenderer} 上再也没有
 * {@code render}，只有 {@code submit(state, poseStack, SubmitNodeCollector, CameraRenderState)}；
 * {@code MultiBufferSource} 也不再出现在这条路径里。所以这里不再自己画文字，
 * 而是把数量包装成一个 <b>name tag</b> 交给 {@code OrderedSubmitNodeCollector#submitNameTag}
 * ——它本来就要处理「面向相机、自动定位、深度正确」这些事，省掉一大坨易碎的矩阵数学。</p>
 *
 * <p>版本差异：{@code submitNameTag} 在 1.21.11 是 8 参（多一个
 * {@code distanceToCameraSq}），26.x 收敛成 7 参；{@code CameraRenderState} 的包名也从
 * {@code ...renderer.state} 挪到了 {@code ...renderer.state.level}。</p>
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererSubmitMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN"), require = 0)
    private void infinitestacks$submitCountLabel(ItemEntityRenderState state, PoseStack poseStack,
                                                 SubmitNodeCollector collector, CameraRenderState camera,
                                                 CallbackInfo ci) {
        if (state == null || collector == null) {
            return;
        }
        long shown = DropLabelRenderer.claim(state.x, state.y, state.z, state.count);
        if (shown <= 0L) {
            return;
        }
        OrderedSubmitNodeCollector ordered = collector.order(0);
        if (ordered == null) {
            return;
        }
        ordered.submitNameTag(
                poseStack,
                new Vec3(0.0D, 0.6D, 0.0D),
                state.lightCoords,
                Component.literal(DropLabelRenderer.text(shown)),
                true,
                DropLabelRenderer.background(),

                state.distanceToCameraSq,
                camera);

    }

}
