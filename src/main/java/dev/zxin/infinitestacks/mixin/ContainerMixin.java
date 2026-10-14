package dev.zxin.infinitestacks.mixin;

import dev.zxin.infinitestacks.logic.StackLimits;

import net.minecraft.world.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 容器上限。
 *
 * <p>原版 {@code Container.getMaxStackSize()} 默认返回 <b>99</b>，箱子/漏斗/机器槽位全走它。
 * 1.20.5+ 起 {@code Container.setItem} 内部还有
 * {@code itemStack.limitSize(this.getMaxStackSize(itemStack))} —— 这就是"放进去就变 99"的元凶，
 * 所以只改物品上限是没用的，必须把这个也抬起来。</p>
 *
 * <p><b>为什么用 {@code @Overwrite} 而不是 {@code @Inject}：</b>
 * Forge 1.20.1 自带的 Mixin 0.8.5 不支持在接口里放注入器
 * （会报 {@code Injector in interface is unsupported}），而 {@code Container} 是接口，
 * 只能做成接口 mixin。{@code @Overwrite} 接口默认方法在 Mixin 0.8.5 和 0.8.7+ 上都可用，
 * 这也是参考模组 Stackable 在 Forge 分支上的做法。</p>
 */
@Mixin(Container.class)
public interface ContainerMixin {

    @Overwrite
    default int getMaxStackSize() {
        if (!StackLimits.containerExpansion()) {
            return 99;
        }
        int limit = StackLimits.globalLimit();
        return Math.max(99, limit);
    }

}
