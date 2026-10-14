package dev.zxin.infinitestacks.client;

import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.logic.CountFormat;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ★ 掉落物数字标签的「批处理」中枢。
 *
 * <p>性能目标是：几百个堆叠同屏时不掉帧。三件事保证这一点：</p>
 * <ol>
 *   <li><b>复用共享资源</b>：字体一律用 {@code Minecraft.getInstance().font}（全局单例），
 *       顶点写进渲染器已经开好的 {@link net.minecraft.client.renderer.MultiBufferSource}，
 *       绝不每个掉落物 new 一个文本渲染器 / 开一个新缓冲区。</li>
 *   <li><b>标签聚类</b>：按 {@code drops.merge_radius} 把空间上相邻的同格掉落物归成一簇，
 *       整簇只画一个标签，数量取簇内合计。既省 draw call 又更可读。</li>
 *   <li><b>每帧硬上限</b>：{@code drops.max_labels} 兜底，超出直接不画。</li>
 * </ol>
 *
 * <p>簇表按帧刷新（约 60Hz），聚类扫描是 O(已加载物品实体数)，与渲染本身同阶。</p>
 */
public final class DropLabelBatch {

    private static final Map<Long, Cluster> CLUSTERS = new HashMap<Long, Cluster>();
    private static final Set<Long> DRAWN = new HashSet<Long>();
    private static long lastScanNanos = 0L;
    private static int budgetUsed = 0;

    private DropLabelBatch() {
    }

    /** 一簇的合计信息。 */
    private static final class Cluster {
        long total;
        int members;
    }

    /**
     * 每帧重建簇表。由第一个被渲染的掉落物触发；同一帧内重复调用会被快速短路。
     *
     * @param threshold 低于该数量不参与聚类
     */
    public static void refresh(double threshold, float mergeRadius) {
        long now = System.nanoTime();
        long frameNanos = 16_000_000L;
        if (now - lastScanNanos < frameNanos && !CLUSTERS.isEmpty()) {
            return;
        }
        lastScanNanos = now;
        CLUSTERS.clear();
        DRAWN.clear();
        budgetUsed = 0;

        if (mergeRadius <= 0.0F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }
        int cell = Math.max(1, (int) Math.round(mergeRadius * 2.0F));
        try {
            for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof net.minecraft.world.entity.item.ItemEntity)) {
                    continue;
                }
                net.minecraft.world.entity.item.ItemEntity item =
                        (net.minecraft.world.entity.item.ItemEntity) entity;
                int count = item.getItem().getCount();
                if ((double) count < threshold) {
                    continue;
                }
                long key = cellKey(entity.getX(), entity.getY(), entity.getZ(), cell);
                Cluster cluster = CLUSTERS.get(key);
                if (cluster == null) {
                    cluster = new Cluster();
                    CLUSTERS.put(key, cluster);
                }
                cluster.total += count;
                cluster.members++;
            }
        } catch (Throwable t) {
            InfiniteStacks.error("掉落物聚类扫描失败（不影响游戏）: {}", t.toString());
            CLUSTERS.clear();
        }
    }

    /**
     * 决定这个掉落物是否该画标签，返回要显示的数量。
     *
     * @return 需要显示的数量；&lt;= 0 表示本帧不画
     */
    public static long claim(double x, double y, double z, int count, float mergeRadius, int maxLabels) {
        if (budgetUsed >= maxLabels) {
            return -1L;
        }
        if (mergeRadius <= 0.0F) {
            budgetUsed++;
            return count;
        }
        int cell = Math.max(1, (int) Math.round(mergeRadius * 2.0F));
        long key = cellKey(x, y, z, cell);
        Cluster cluster = CLUSTERS.get(key);
        if (cluster == null) {
            budgetUsed++;
            return count;
        }
        if (!DRAWN.add(key)) {
            // 这一簇已经画过了
            return -1L;
        }
        budgetUsed++;
        return cluster.total > 0L ? cluster.total : count;
    }

    /** 把数量转成标签文本（缩写 / 完整由客户端配置决定）。 */
    public static String label(long count) {
        int safe = (int) Math.min(count, (long) Integer.MAX_VALUE);
        return CountFormat.format(safe, ClientStackState.abbreviated(), ClientStackState.uiThreshold());
    }

    /** 该数量是否达到显示阈值。 */
    public static boolean shouldLabel(int count) {
        return count > 0 && count >= ClientStackState.dropsThreshold();
    }

    /**
     * 单个掉落物实际渲染的模型数量：原版上限 5，本模组按 drops.max_models 收紧；
     * 数量超过 drops.hide_model_above 时进一步收敛到 1，避免极端堆叠拖慢渲染。
     */
    public static int clampModels(int vanillaAmount, int count) {
        int cap = ClientStackState.dropsMaxModels();
        if (cap < 1) {
            cap = 1;
        }
        int hideAbove = ClientStackState.dropsHideModelAbove();
        if (hideAbove > 0 && count >= hideAbove) {
            return 1;
        }
        return Math.max(1, Math.min(vanillaAmount, cap));
    }

    /** 共享的字体实例（全局单例，不新建）。 */
    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /** 背景色（ARGB），opacity &lt;= 0 时返回 0 表示不画背景。 */
    public static int background(double opacity) {
        if (opacity <= 0.0D) {
            return 0;
        }
        int alpha = (int) Math.round(Math.min(1.0D, opacity) * 255.0D) << 24;
        return alpha;
    }

    /** 把位姿摆到标签位置并面向相机。调用方负责 push/pop。 */
    public static void orient(PoseStack poseStack, float scale, double heightOffset) {
        Minecraft mc = Minecraft.getInstance();
        poseStack.translate(0.0D, heightOffset, 0.0D);

        // 26.2 起原版移除了 GameRenderer#getMainCamera()；26.x 走 submitNameTag 路径，
        // 不会调用到这里，所以这一行干脆不生成。
        poseStack.mulPose(mc.gameRenderer.getMainCamera().rotation());

        poseStack.scale(0.025F * scale, -0.025F * scale, 0.025F * scale);
    }

    private static long cellKey(double x, double y, double z, int cell) {
        long gx = (long) Math.floor(x / cell);
        long gy = (long) Math.floor(y / cell);
        long gz = (long) Math.floor(z / cell);
        return ((gx & 0x3FFFFFL) << 42) | ((gy & 0xFFFFFL) << 22) | (gz & 0x3FFFFFL);
    }
}
