package dev.zxin.infinitestacks.client;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 掉落物数字标签的绘制入口（世界内、面向相机的文本）。
 *
 * <p>MC &lt; 26：在 {@code ItemEntityRenderer.render} 的 RETURN 处调用 drawInWorld，
 * 用 Font#drawInBatch 直接画。这个 10 参数重载从 1.20.1 一直存在到 1.21.11
 * （返回值在 1.21.8 由 int 变 void，当语句调用不受影响），所以全版本可编译。</p>
 *
 * <p>MC &gt;= 1.21.11：渲染管线换成 submit-node，改用
 * OrderedSubmitNodeCollector#submitNameTag，逻辑在 ItemEntityRendererSubmitMixin 里；
 * 26.2 起原版移除了 MultiBufferSource，所以本文件的绘制部分整体不生成。</p>
 *
 * <p>性能：字体用全局单例，顶点写进渲染器已有的缓冲区，不新建任何文本渲染器。</p>
 */
public final class DropLabelRenderer {

    private DropLabelRenderer() {
    }

    /** 该数量是否值得画标签（submit 路径复用）。 */
    public static boolean wantsLabel(int count) {
        return ClientStackState.dropsEnabled() && DropLabelBatch.shouldLabel(count);
    }

    /** 认领一个标签额度，返回要显示的数量；&lt;= 0 表示本帧不画（submit 路径复用）。 */
    public static long claim(double x, double y, double z, int count) {
        DropLabelBatch.refresh(ClientStackState.dropsThreshold(), ClientStackState.dropsMergeRadius());
        return DropLabelBatch.claim(x, y, z, count,
                ClientStackState.dropsMergeRadius(), ClientStackState.dropsMaxLabels());
    }

    /** 标签文本（submit 路径复用）。 */
    public static String text(long shown) {
        return DropLabelBatch.label(shown);
    }

    /** 背景色 ARGB（submit 路径复用）。 */
    public static int background() {
        return DropLabelBatch.background(ClientStackState.dropsBgOpacity());
    }
}
