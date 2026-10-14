package dev.zxin.infinitestacks.logic;

/**
 * 溢出安全算术。**所有涉及堆叠数量的运算都必须走这里**，
 * 禁止在别处直接写 {@code a + b}（见《接口约定文档》§9.3）。
 *
 * <p>中间量一律提升到 {@code long}，再饱和钳制回 int 范围，
 * 这样即使堆叠上限取到 int 最大值 2,147,483,647 也不会翻负。</p>
 */
public final class StackMath {

    private StackMath() {
    }

    /** 饱和加法：a + b，上限 ABSOLUTE_MAX，下限 0。 */
    public static int saturatingAdd(int a, int b) {
        long result = (long) a + (long) b;
        return clampLong(result);
    }

    /** 饱和减法：a - b，下限 0。 */
    public static int saturatingSub(int a, int b) {
        long result = (long) a - (long) b;
        return clampLong(result);
    }

    /** 把任意 long 钳制到 [0, ABSOLUTE_MAX]。 */
    public static int clampLong(long value) {
        if (value <= 0L) {
            return 0;
        }
        if (value >= (long) StackLimits.ABSOLUTE_MAX) {
            return StackLimits.ABSOLUTE_MAX;
        }
        return (int) value;
    }

    /** 把任意 int 钳制到 [0, ABSOLUTE_MAX]。 */
    public static int clamp(int value) {
        if (value <= 0) {
            return 0;
        }
        return Math.min(value, StackLimits.ABSOLUTE_MAX);
    }

    /** 两个数量相加是否会越过配置的硬上限（用于提前发溢出警告）。 */
    public static boolean willOverflow(int current, int delta, int limit) {
        return (long) current + (long) delta > (long) limit;
    }
}
