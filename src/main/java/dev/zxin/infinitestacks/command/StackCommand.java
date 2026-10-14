package dev.zxin.infinitestacks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.zxin.infinitestacks.Bridge;
import dev.zxin.infinitestacks.InfiniteStacks;
import dev.zxin.infinitestacks.config.ConfigManager;
import dev.zxin.infinitestacks.config.Overrides;
import dev.zxin.infinitestacks.config.StackConfig;
import dev.zxin.infinitestacks.logic.CountFormat;
import dev.zxin.infinitestacks.logic.StackLimits;
import dev.zxin.infinitestacks.network.Packets;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /stack} 指令树（别名 {@code /istacks}）。
 *
 * <p>接口见《接口约定文档》§6。**所有变更只在服务端执行**，并且只在服务端改完配置后
 * 才把新的快照推给客户端——客户端永远不是权威。</p>
 */
public final class StackCommand {

    private StackCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("stack")
                .requires(source -> Permissions.has(source, Bridge.config().commandPermissionLevel));

        root.then(Commands.literal("get").executes(StackCommand::runGet));
        root.then(Commands.literal("set")
                .then(Commands.argument("limit", IntegerArgumentType.integer(1, StackLimits.ABSOLUTE_MAX))
                        .executes(StackCommand::runSet)));
        root.then(Commands.literal("item")
                .then(Commands.argument("itemId", StringArgumentType.string())
                        .then(Commands.argument("limit", IntegerArgumentType.integer(0, StackLimits.ABSOLUTE_MAX))
                                .executes(StackCommand::runItem))));
        root.then(Commands.literal("tag")
                .then(Commands.argument("tagId", StringArgumentType.string())
                        .then(Commands.argument("limit", IntegerArgumentType.integer(0, StackLimits.ABSOLUTE_MAX))
                                .executes(StackCommand::runTag))));
        root.then(Commands.literal("list").executes(ctx -> runList(ctx, 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1, 999))
                        .executes(ctx -> runList(ctx, IntegerArgumentType.getInteger(ctx, "page")))));
        root.then(Commands.literal("reset")
                .executes(ctx -> runReset(ctx, "all"))
                .then(Commands.literal("all").executes(ctx -> runReset(ctx, "all")))
                .then(Commands.literal("items").executes(ctx -> runReset(ctx, "items")))
                .then(Commands.literal("tags").executes(ctx -> runReset(ctx, "tags"))));
        root.then(Commands.literal("reload").executes(StackCommand::runReload));
        root.then(Commands.literal("diag").executes(StackCommand::runDiag));
        root.then(Commands.literal("format")
                .then(Commands.literal("ABBREVIATED").executes(ctx -> runFormat(ctx, "ABBREVIATED")))
                .then(Commands.literal("FULL").executes(ctx -> runFormat(ctx, "FULL"))));

        dispatcher.register(root);
        dispatcher.register(Commands.literal("istacks").redirect(dispatcher.register(root)));
    }

    // ------------------------------------------------------------------
    // 子指令
    // ------------------------------------------------------------------

    private static int runGet(CommandContext<CommandSourceStack> ctx) {
        StackConfig cfg = Bridge.config();
        reply(ctx, "§e无限堆叠 §7· 当前状态");
        reply(ctx, "§7全局上限: §f" + cfg.globalLimit
                + " §8(" + CountFormat.abbreviate(cfg.globalLimit) + ")");
        reply(ctx, "§7影响耐久物品: §f" + (cfg.affectDamageable ? "是" : "否")
                + " §7| 容器扩展: §f" + (cfg.containerExpansion ? "开" : "关"));
        reply(ctx, "§7覆盖条目: §f" + cfg.items.size() + " §7个物品 / §f" + cfg.tags.size() + " §7个标签");
        reply(ctx, "§7客户端同步: §f" + (cfg.syncToClient ? "开" : "关"));
        return 1;
    }

    private static int runSet(CommandContext<CommandSourceStack> ctx) {
        int limit = IntegerArgumentType.getInteger(ctx, "limit");
        Overrides overrides = ConfigManager.overrides();
        overrides.globalLimit = limit;
        ConfigManager.commitOverrides(true);
        reply(ctx, "§a全局堆叠上限已设为 §f" + limit + " §7(" + CountFormat.abbreviate(limit) + ")");
        warnIfExtreme(ctx, limit);
        return 1;
    }

    private static int runItem(CommandContext<CommandSourceStack> ctx) {
        String itemId = normalise(StringArgumentType.getString(ctx, "itemId"));
        int limit = IntegerArgumentType.getInteger(ctx, "limit");
        Overrides overrides = ConfigManager.overrides();
        if (limit <= 0) {
            overrides.items.remove(itemId);
            ConfigManager.commitOverrides(true);
            reply(ctx, "§a已清除 §f" + itemId + " §a的单物品覆盖");
            return 1;
        }
        overrides.items.put(itemId, limit);
        ConfigManager.commitOverrides(true);
        reply(ctx, "§a已把 §f" + itemId + " §a的上限设为 §f" + limit + " §7(" + CountFormat.abbreviate(limit) + ")");
        return 1;
    }

    private static int runTag(CommandContext<CommandSourceStack> ctx) {
        String raw = StringArgumentType.getString(ctx, "tagId");
        String tagId = raw.startsWith("#") ? normalise(raw.substring(1)) : "#" + normalise(raw);
        int limit = IntegerArgumentType.getInteger(ctx, "limit");
        Overrides overrides = ConfigManager.overrides();
        if (limit <= 0) {
            overrides.tags.remove(tagId);
            ConfigManager.commitOverrides(true);
            reply(ctx, "§a已清除 §f" + tagId + " §a的标签覆盖");
            return 1;
        }
        overrides.tags.put(tagId, limit);
        ConfigManager.commitOverrides(true);
        reply(ctx, "§a已把标签 §f" + tagId + " §a的上限设为 §f" + limit + " §7(" + CountFormat.abbreviate(limit) + ")");
        return 1;
    }

    private static int runList(CommandContext<CommandSourceStack> ctx, int page) {
        StackConfig cfg = Bridge.config();
        List<String> lines = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : cfg.items.entrySet()) {
            lines.add("§7物品 §f" + entry.getKey() + " §8= §f" + entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : cfg.tags.entrySet()) {
            lines.add("§7标签 §f" + entry.getKey() + " §8= §f" + entry.getValue());
        }
        if (lines.isEmpty()) {
            reply(ctx, "§7当前没有任何覆盖条目（全部走全局上限 " + cfg.globalLimit + "）");
            return 1;
        }
        int perPage = 8;
        int pages = (lines.size() + perPage - 1) / perPage;
        int current = Math.min(Math.max(1, page), pages);
        reply(ctx, "§e覆盖列表 §8(" + current + "/" + pages + "，共 " + lines.size() + " 条)");
        int from = (current - 1) * perPage;
        for (int i = from; i < Math.min(from + perPage, lines.size()); i++) {
            reply(ctx, lines.get(i));
        }
        return 1;
    }

    private static int runReset(CommandContext<CommandSourceStack> ctx, String scope) {
        Overrides overrides = ConfigManager.overrides();
        if ("all".equals(scope)) {
            ConfigManager.resetOverrides();
        } else if ("items".equals(scope)) {
            overrides.items.clear();
            ConfigManager.commitOverrides(true);
        } else {
            overrides.tags.clear();
            ConfigManager.commitOverrides(true);
        }
        reply(ctx, "§a已重置覆盖: §f" + scope);
        return 1;
    }

    private static int runReload(CommandContext<CommandSourceStack> ctx) {
        ConfigManager.reload(true);
        StackConfig cfg = Bridge.config();
        reply(ctx, "§a配置已重读并下发客户端。全局上限 = §f" + cfg.globalLimit);
        return 1;
    }

    private static int runDiag(CommandContext<CommandSourceStack> ctx) {
        StackConfig cfg = Bridge.config();
        reply(ctx, "§e无限堆叠 §7· 自检");
        reply(ctx, "§7版本: §f" + InfiniteStacks.MOD_VERSION + " §8(编译目标 MC) "
                + "§7| Java 预处理器分支已生效");
        reply(ctx, "§7StackLimits 全局: §f" + StackLimits.globalLimit()
                + " §7| 生效配置全局: §f" + cfg.globalLimit);
        reply(ctx, "§7运行时覆盖文件: §f" + Overrides.file().toAbsolutePath());
        reply(ctx, "§7静态配置文件: §f" + Overrides.baseFile().toAbsolutePath());
        reply(ctx, "§7掉落物标签: §f" + (cfg.drops.enabled ? "开" : "关")
                + " §7阈值=§f" + cfg.drops.threshold
                + " §7最多模型=§f" + cfg.drops.maxModels
                + " §7聚类半径=§f" + cfg.drops.mergeRadius
                + " §7每帧上限=§f" + cfg.drops.maxLabels);
        reply(ctx, "§7UI 模式: §f" + cfg.uiMode + " §7(阈值 " + cfg.abbreviateThreshold + ")");
        return 1;
    }

    private static int runFormat(CommandContext<CommandSourceStack> ctx, String mode) {
        Overrides overrides = ConfigManager.overrides();
        overrides.uiMode = mode;
        ConfigManager.commitOverrides(true);
        reply(ctx, "§a数字显示模式已切换为 §f" + mode);
        return 1;
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    private static void reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static String normalise(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.indexOf(':') < 0) {
            value = "minecraft:" + value;
        }
        return value;
    }

    /** 接近 int 最大值时提醒操作者（每人 30 秒节流，避免刷屏）。 */
    private static void warnIfExtreme(CommandContext<CommandSourceStack> ctx, int limit) {
        if ((long) limit < (long) Bridge.config().overflowWarningThreshold) {
            return;
        }
        ServerPlayer player = null;
        try {
            if (ctx.getSource().getEntity() instanceof ServerPlayer) {
                player = (ServerPlayer) ctx.getSource().getEntity();
            }
        } catch (Throwable ignored) {
            // 命令方块/控制台没有实体，忽略
        }
        String warning = "§6[警告] 堆叠上限已超过 " + Bridge.config().overflowWarningThreshold
                + "，极大量堆叠在部分模组容器中可能被截断，请先备份存档。";
        reply(ctx, warning);
        if (player != null) {

            player.displayClientMessage(Component.literal(warning), false);

        }
    }

    /** 供加载器在玩家进服时构造一份快照。 */
    public static Packets.Sync currentSnapshot() {
        return Bridge.snapshot();
    }

    /** 调试用：把一个堆叠的解析结果转成可读文本。 */
    public static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem() + " -> limit=" + StackLimits.limitFor(stack)
                + " vanilla=" + StackLimits.vanillaLimitOf(stack.getItem());
    }
}
