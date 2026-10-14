package dev.zxin.infinitestacks.config;

/** 掉落物渲染优化配置（《接口约定文档》§3.1 的 drops 分组）。 */
public class DropConfig {

    /** 总开关。与其他修改掉落物渲染的模组冲突时可关掉。 */
    public boolean enabled = true;

    /** 数量 ≥ 该值才绘制数字标签。 */
    public int threshold = 64;

    /** 单个掉落物最多渲染几个模型（原版固定最多 5 个）。 */
    public int maxModels = 5;

    /** 数量 ≥ 该值时完全不渲染物品模型，只留标签；0 = 从不隐藏。 */
    public int hideModelAbove = 1000000;

    /** 标签聚类半径（方块）。同一半径内的同种掉落物只画一个合计标签；0 = 关闭聚类。 */
    public double mergeRadius = 1.5D;

    /** 每帧标签数量硬上限（性能护栏，超出部分直接不画）。 */
    public int maxLabels = 256;

    /** 标签缩放。 */
    public double scale = 1.0D;

    /** 标签相对实体高度（方块）。 */
    public double heightOffset = 0.55D;

    /** 文字颜色，0xRRGGBB。 */
    public int color = 0xFFFFFF;

    /** 背景不透明度 0..1；0 = 不画背景。 */
    public double backgroundOpacity = 0.25D;

    /** 是否画文字阴影。 */
    public boolean shadow = true;

    public DropConfig copy() {
        DropConfig out = new DropConfig();
        out.enabled = this.enabled;
        out.threshold = this.threshold;
        out.maxModels = this.maxModels;
        out.hideModelAbove = this.hideModelAbove;
        out.mergeRadius = this.mergeRadius;
        out.maxLabels = this.maxLabels;
        out.scale = this.scale;
        out.heightOffset = this.heightOffset;
        out.color = this.color;
        out.backgroundOpacity = this.backgroundOpacity;
        out.shadow = this.shadow;
        return out;
    }

    public void clamp() {
        threshold = clampInt(threshold, 1, Integer.MAX_VALUE, 64);
        maxModels = clampInt(maxModels, 1, 8, 5);
        hideModelAbove = clampInt(hideModelAbove, 0, Integer.MAX_VALUE, 1000000);
        maxLabels = clampInt(maxLabels, 1, 4096, 256);
        mergeRadius = clampDouble(mergeRadius, 0.0D, 8.0D, 1.5D);
        scale = clampDouble(scale, 0.25D, 4.0D, 1.0D);
        heightOffset = clampDouble(heightOffset, 0.0D, 3.0D, 0.55D);
        backgroundOpacity = clampDouble(backgroundOpacity, 0.0D, 1.0D, 0.25D);
        if (color < 0 || color > 0xFFFFFF) {
            color = 0xFFFFFF;
        }
    }

    static int clampInt(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    static double clampDouble(double value, double min, double max, double fallback) {
        if (Double.isNaN(value) || value < min || value > max) {
            return fallback;
        }
        return value;
    }
}
