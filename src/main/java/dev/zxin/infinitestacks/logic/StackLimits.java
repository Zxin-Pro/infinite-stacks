package dev.zxin.infinitestacks.logic;

import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.config.StackConfig;
import dev.zxin.infinitestacks.util.Ids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ★ 堆叠上限解析中枢。
 *
 * <p>查找顺序（首个命中即返回，见《接口约定文档》§4）：</p>
 * <ol>
 *   <li>空堆叠 → 0</li>
 *   <li>耐久物品且 {@code affectDamageable == false} → 原版上限</li>
 *   <li>{@code items[itemId]} 覆盖存在且 &gt; 0 → 该值</li>
 *   <li>任一 {@code tags[tagId]} 命中 → 其中最小值（保证确定性）</li>
 *   <li>→ 全局上限 globalLimit</li>
 * </ol>
 *
 * <p>结果一律钳制到 [1, 2147483647]。</p>
 *
 * <p><b>缓存</b>：按 {@link Item} 缓存计算结果（{@link ConcurrentHashMap}）。
 * 任何配置变更都必须调用 {@link #invalidate()}，否则改配置不生效。</p>
 *
 * <p><b>线程安全</b>：volatile 字段 + 不可变快照；服务端主线程与渲染线程都会读。</p>
 */
public final class StackLimits {

    /** 硬上限：int 最大值。配置不得越过它。 */
    public static final int ABSOLUTE_MAX = 2147483647;

    /** 拿不到原版上限时的兜底值。 */
    public static final int FALLBACK_VANILLA = 64;

    private static final Map<Item, Integer> LIMIT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Item, Integer> VANILLA_CACHE = new ConcurrentHashMap<>();
    private static final Map<Item, Boolean> DAMAGEABLE_CACHE = new ConcurrentHashMap<>();

    private static volatile int globalLimit = FALLBACK_VANILLA;
    private static volatile boolean affectDamageable = false;
    private static volatile boolean containerExpansion = true;
    private static volatile Map<String, Integer> itemRules = Collections.emptyMap();
    private static volatile List<TagRule> tagRules = Collections.emptyList();

    private StackLimits() {
    }

    // ------------------------------------------------------------------
    // 配置注入
    // ------------------------------------------------------------------

    /** 用一份已校验的配置快照重建全部缓存。加载器在启动、重载、指令改配置后调用。 */
    public static void apply(StackConfig config) {
        StackConfig cfg = config == null ? StackConfig.defaults() : config;
        cfg.clamp();
        globalLimit = cfg.globalLimit;
        affectDamageable = cfg.affectDamageable;
        containerExpansion = cfg.containerExpansion;
        itemRules = Collections.unmodifiableMap(new java.util.HashMap<String, Integer>(cfg.items));
        List<TagRule> rules = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cfg.tags.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            TagKey<Item> tag = makeTag(entry.getKey());
            if (tag != null) {
                rules.add(new TagRule(tag, StackMath.clamp(entry.getValue())));
            } else {
                InfiniteStacks.LOGGER.warn("[Infinite Stacks] 无法解析标签覆盖: {}", entry.getKey());
            }
        }
        tagRules = Collections.unmodifiableList(rules);
        invalidate();
    }

    /** 清空所有派生缓存。配置变更后必须调用。 */
    public static void invalidate() {
        LIMIT_CACHE.clear();
    }

    // ------------------------------------------------------------------
    // 查询入口
    // ------------------------------------------------------------------

    /** 解析这个堆叠的上限；空堆叠返回 0。 */
    public static int limitFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return limitForItem(stack.getItem());
    }

    /** 解析这个物品的上限（不带堆叠上下文）。 */
    public static int limitForItem(Item item) {
        if (item == null) {
            return 0;
        }
        Integer cached = LIMIT_CACHE.get(item);
        if (cached != null) {
            return cached;
        }
        int computed = compute(item);
        LIMIT_CACHE.put(item, computed);
        return computed;
    }

    private static int compute(Item item) {
        if (isDamageable(item) && !affectDamageable) {
            return StackMath.clamp(vanillaLimitOf(item));
        }
        Integer exact = itemRules.get(keyOf(item));
        if (exact != null && exact > 0) {
            return StackMath.clamp(exact);
        }
        int best = Integer.MAX_VALUE;
        boolean matched = false;
        for (TagRule rule : tagRules) {
            if (matches(item, rule.tag)) {
                best = Math.min(best, rule.limit);
                matched = true;
            }
        }
        if (matched) {
            return StackMath.clamp(best);
        }
        return StackMath.clamp(globalLimit);
    }

    // ------------------------------------------------------------------
    // 原版上限
    // ------------------------------------------------------------------

    /**
     * 物品的原版堆叠上限（不含本模组的任何干预）。
     *
     * <p>1.20.5 起 {@code Item.getMaxStackSize()} 被 DataComponent 取代，
     * 改为 {@code Item.getDefaultMaxStackSize()}；26.x 起 {@code ItemStack} 也没有
     * {@code getMaxStackSize()} 了（改由 {@code ItemInstance} 接口默认方法提供）。</p>
     */
    public static int vanillaLimitOf(Item item) {
        if (item == null) {
            return FALLBACK_VANILLA;
        }
        Integer cached = VANILLA_CACHE.get(item);
        if (cached != null) {
            return cached;
        }
        int value = FALLBACK_VANILLA;
        try {

            value = item.getDefaultMaxStackSize();

        } catch (Throwable t) {
            value = FALLBACK_VANILLA;
        }
        if (value <= 0) {
            value = FALLBACK_VANILLA;
        }
        VANILLA_CACHE.put(item, value);
        return value;
    }

    /** 由 ItemMixin 回填真实原版上限（仅 1.20.4 及以前需要）。 */
    public static void rememberVanilla(Item item, int value) {
        if (item != null && value > 0 && !VANILLA_CACHE.containsKey(item)) {
            VANILLA_CACHE.put(item, value);
        }
    }

    /** 该物品是否为耐久物品（工具/盔甲/弓…）。 */
    public static boolean isDamageable(Item item) {
        if (item == null) {
            return false;
        }
        Boolean cached = DAMAGEABLE_CACHE.get(item);
        if (cached != null) {
            return cached;
        }
        boolean result = false;
        try {

            // 1.20.5 起 Item.canBeDepleted() 被移除，改为从 MAX_DAMAGE 组件判断
            ItemStack probe = item.getDefaultInstance();
            result = probe != null && !probe.isEmpty() && probe.isDamageableItem();

        } catch (Throwable t) {
            result = false;
        }
        DAMAGEABLE_CACHE.put(item, result);
        return result;
    }

    // ------------------------------------------------------------------
    // 配置只读视图（网络同步 / 指令输出用）
    // ------------------------------------------------------------------

    public static int globalLimit() {
        return StackMath.clamp(globalLimit);
    }

    public static boolean affectDamageable() {
        return affectDamageable;
    }

    public static boolean containerExpansion() {
        return containerExpansion;
    }

    /** 把任意整数裁剪到 [1, ABSOLUTE_MAX]（0 及负数一律抬到 1）。 */
    public static int clampLimit(int value) {
        if (value < 1) {
            return 1;
        }
        return Math.min(value, ABSOLUTE_MAX);
    }

    public static Map<String, Integer> itemRules() {
        return itemRules;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private static String keyOf(Item item) {
        try {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static boolean matches(Item item, TagKey<Item> tag) {
        try {
            ItemStack probe = item.getDefaultInstance();
            return probe != null && !probe.isEmpty() && probe.is(tag);
        } catch (Throwable t) {
            return false;
        }
    }

    private static TagKey<Item> makeTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String value = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            return TagKey.create(Registries.ITEM, Ids.tryParse(value));
        } catch (Throwable t) {
            return null;
        }
    }

    /** 标签覆盖规则（预解析成 TagKey，避免热路径反复解析字符串）。 */
    private static final class TagRule {
        final TagKey<Item> tag;
        final int limit;

        TagRule(TagKey<Item> tag, int limit) {
            this.tag = tag;
            this.limit = limit;
        }
    }
}
