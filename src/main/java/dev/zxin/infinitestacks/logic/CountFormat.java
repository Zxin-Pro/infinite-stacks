package dev.zxin.infinitestacks.logic;

import java.util.Locale;

/**
 * 数量格式化：把超大数字缩写成 1.2K / 3.4M / 5.6B / 7.8T。
 *
 * <p>纯逻辑类，**不依赖任何 Minecraft 类型**，因此可以在客户端渲染热路径里随便调，
 * 也可以被单元测试直接覆盖。</p>
 *
 * <p>规则（与《接口约定文档》§3.1 的 ui 分组一致）：</p>
 * <ul>
 *   <li>不足 1000 或低于 abbreviate_threshold → 原样输出完整数字</li>
 *   <li>缩写时保留 1 位小数（值 &lt; 10 时），否则取整；并去掉多余的 “.0”</li>
 *   <li>后缀阶梯 K/M/B/T，覆盖到 10^15，足够容纳 int 最大值 2,147,483,647（2.1B）</li>
 * </ul>
 */
public final class CountFormat {

    private static final String[] SUFFIX = {"", "K", "M", "B", "T"};

    private CountFormat() {
    }

    /** 把任意非负数量缩写成短字符串；count &lt; 1000 时返回完整数字。 */
    public static String abbreviate(long count) {
        if (count <= 0L) {
            return "0";
        }
        if (count < 1000L) {
            return Long.toString(count);
        }
        double value = count;
        int tier = 0;
        while (value >= 1000.0D && tier < SUFFIX.length - 1) {
            value /= 1000.0D;
            tier++;
        }
        String text;
        if (value < 10.0D) {
            text = String.format(Locale.ROOT, "%.1f", value);
        } else {
            text = String.format(Locale.ROOT, "%.0f", value);
        }
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);
        }
        return text + SUFFIX[tier];
    }

    /**
     * 按配置格式化。
     *
     * @param count               物品数量
     * @param abbreviated         true = 允许缩写（ui.mode = ABBREVIATED）
     * @param abbreviateThreshold 低于该值始终显示完整数字（ui.abbreviate_threshold，默认 9999）
     */
    public static String format(int count, boolean abbreviated, int abbreviateThreshold) {
        if (count <= 0) {
            return "0";
        }
        if (!abbreviated || count < abbreviateThreshold) {
            return Integer.toString(count);
        }
        return abbreviate(count);
    }

    /** 把可能已经是数字字符串的文本再做一次缩写（用于拦不到 int 的钩子点）。 */
    public static String abbreviateString(String raw, boolean abbreviated, int abbreviateThreshold) {
        if (!abbreviated || raw == null || raw.isEmpty() || raw.length() <= 3) {
            return raw;
        }
        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isDigit(raw.charAt(i))) {
                return raw;
            }
        }
        try {
            long value = Long.parseLong(raw);
            if (value < abbreviateThreshold) {
                return raw;
            }
            return abbreviate(value);
        } catch (NumberFormatException notANumber) {
            return raw;
        }
    }
}
