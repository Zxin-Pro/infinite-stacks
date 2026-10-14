package dev.zxin.infinitestacks.client.mixin;

import dev.zxin.infinitestacks.client.ClientStackState;
import dev.zxin.infinitestacks.logic.CountFormat;

import net.minecraft.client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ★ 物品栏 / 容器里「超大数量」的缩写显示（全版本）。
 *
 * <p>原版把数量字符串化后直接丢给字体渲染：{@code 1000000} 会横向撑爆整个槽位。
 * 这里把那个 int→String 的转换截下来，超过阈值就换成 {@code 1M}。</p>
 *
 * <p>钩子选择说明：不重写整个装饰方法（那样要复刻耐久条、冷却遮罩等一大坨逻辑，
 * 跨版本必碎），而是只截断**数量文本生成**这一处。同时挂
 * {@code String.valueOf(int)} 与 {@code Integer.toString(int)} 两个 redirect，
 * 哪个命中用哪个；两个都命中也不会出错——{@link CountFormat#abbreviateString}
 * 对已经是 {@code 1.2M} 的输入直接原样返回，是幂等的。</p>
 *
 * <p>26.x 起 {@code GuiGraphics} 改名 {@code GuiGraphicsExtractor}、
 * {@code renderItemDecorations} 改名 {@code itemDecorations}，数量文本在私有方法
 * {@code itemCount} 内生成，故目标方法整体切换。</p>
 */

@Mixin(GuiGraphics.class)

public abstract class GuiGraphicsMixin {

    @Redirect(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
    private String infinitestacks$abbreviateCount(int value) {
        return CountFormat.abbreviateString(
                Integer.toString(value),
                ClientStackState.abbreviated(),
                ClientStackState.uiThreshold());
    }

    @Redirect(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            require = 0,
            at = @At(value = "INVOKE", target = "Ljava/lang/Integer;toString(I)Ljava/lang/String;"))
    private String infinitestacks$abbreviateCountAlt(int value) {
        return CountFormat.abbreviateString(
                Integer.toString(value),
                ClientStackState.abbreviated(),
                ClientStackState.uiThreshold());
    }

}
