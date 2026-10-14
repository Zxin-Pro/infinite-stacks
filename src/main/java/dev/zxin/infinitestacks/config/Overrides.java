package dev.zxin.infinitestacks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运行时覆盖（唯一真源：{@code config/infinitestacks.overrides.json}）。
 *
 * <p>由 {@code /stack} 指令写入，也可以手工编辑后 {@code /stack reload}。
 * <b>优先级高于静态配置</b>（{@code config/infinitestacks.json}）。</p>
 *
 * <p>字段用装箱类型表示「未覆盖」，Gson 读不到就保持 null，合并时跳过。</p>
 */
public class Overrides {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public int version = 1;

    public Integer globalLimit;
    public Integer commandPermissionLevel;
    public Boolean affectDamageable;
    public Boolean containerExpansion;
    public Boolean nbtIntExtension;
    public String uiMode;
    public Integer abbreviateThreshold;
    public DropConfig drops;

    /** 物品 ID → 上限。0 表示清除该条覆盖。 */
    public Map<String, Integer> items = new LinkedHashMap<String, Integer>();

    /** 标签 ID → 上限。0 表示清除该条覆盖。 */
    public Map<String, Integer> tags = new LinkedHashMap<String, Integer>();

    // ------------------------------------------------------------------
    // 合并
    // ------------------------------------------------------------------

    /** 把覆盖合并进一份静态配置，返回**新的**生效配置（不修改入参）。 */
    public StackConfig merge(StackConfig base) {
        StackConfig out = base == null ? StackConfig.defaults() : base.copy();
        if (globalLimit != null && globalLimit > 0) {
            out.globalLimit = globalLimit;
        }
        if (commandPermissionLevel != null) {
            out.commandPermissionLevel = commandPermissionLevel;
        }
        if (affectDamageable != null) {
            out.affectDamageable = affectDamageable;
        }
        if (containerExpansion != null) {
            out.containerExpansion = containerExpansion;
        }
        if (nbtIntExtension != null) {
            out.nbtIntExtension = nbtIntExtension;
        }
        if (uiMode != null) {
            out.uiMode = uiMode;
        }
        if (abbreviateThreshold != null && abbreviateThreshold > 0) {
            out.abbreviateThreshold = abbreviateThreshold;
        }
        if (drops != null) {
            out.drops = drops.copy();
        }
        if (items != null) {
            out.items.putAll(items);
        }
        if (tags != null) {
            out.tags.putAll(tags);
        }
        out.clamp();
        return out;
    }

    /** 写入覆盖文件。 */
    public void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            InfiniteStacks.error("写出覆盖配置失败: {}", e.toString());
        }
    }

    /** 读盘；文件不存在或损坏则返回空覆盖（不抛异常）。 */
    public static Overrides load() {
        Path path = file();
        try {
            if (Files.exists(path)) {
                String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                Overrides parsed = GSON.fromJson(json, Overrides.class);
                if (parsed != null) {
                    parsed.sanitize();
                    return parsed;
                }
            }
        } catch (Exception e) {
            InfiniteStacks.error("读取覆盖配置失败，按空覆盖处理: {}", e.toString());
        }
        return new Overrides();
    }

    private void sanitize() {
        if (items == null) {
            items = new LinkedHashMap<String, Integer>();
        }
        if (tags == null) {
            tags = new LinkedHashMap<String, Integer>();
        }
        items.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isEmpty() || e.getValue() == null);
        tags.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isEmpty() || e.getValue() == null);
    }

    public static Path file() {
        return Bridge.configDir().resolve("infinitestacks.overrides.json");
    }

    /** 静态配置文件路径。 */
    public static Path baseFile() {
        return Bridge.configDir().resolve("infinitestacks.json");
    }

    /** 读静态配置；不存在则写出一份默认值并返回。 */
    public static StackConfig loadBase() {
        Path path = baseFile();
        try {
            if (Files.exists(path)) {
                String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                StackConfig parsed = GSON.fromJson(json, StackConfig.class);
                if (parsed != null) {
                    parsed.clamp();
                    return parsed;
                }
            }
            StackConfig fresh = StackConfig.defaults();
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(fresh).getBytes(StandardCharsets.UTF_8));
            return fresh;
        } catch (Exception e) {
            InfiniteStacks.error("读取静态配置失败，使用默认值: {}", e.toString());
            return StackConfig.defaults();
        }
    }

    /** 把静态配置写回磁盘（默认值首次生成时用）。 */
    public static void saveBase(StackConfig config) {
        try {
            Path path = baseFile();
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(config).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            InfiniteStacks.error("写出静态配置失败: {}", e.toString());
        }
    }
}
