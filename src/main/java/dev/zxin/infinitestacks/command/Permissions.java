package dev.zxin.infinitestacks.command;

import dev.zxin.infinitestacks.InfiniteStacks;

import net.minecraft.commands.CommandSourceStack;

import java.lang.reflect.Method;

/**
 * ★ 权限检查适配层。
 *
 * <p>版本差异全部关在这里，别处不许直接调权限 API：</p>
 * <ul>
 *   <li>MC ≤ 1.21.8：{@code CommandSourceStack#hasPermission(int)} 直接可用</li>
 *   <li>MC ≥ 1.21.11：该方法被删除，改成 {@code permissions()} 返回
 *       {@code net.minecraft.server.permissions.PermissionSet}，等级由
 *       {@code LevelBasedPermissionSet#level()} 给出（ALL/MODERATOR/GAMEMASTER/ADMIN/OWNER，
 *       序号 0-4 正好对应原版权限等级）</li>
 * </ul>
 *
 * <p>为了避免在 26.x 上被又一次改名打死，这里用反射兜底：
 * 先试 {@code hasPermission(int)}，再试 {@code permissions().level().ordinal()}。
 * 两条路都走不通时**返回 false**（安全侧失败），并打一条警告日志。</p>
 */
public final class Permissions {

    private static volatile Method HAS_PERMISSION_INT;
    private static volatile boolean HAS_PERMISSION_INT_RESOLVED = false;

    private static volatile Method PERMISSIONS;
    private static volatile Method LEVEL;
    private static volatile boolean PERMISSIONS_RESOLVED = false;

    private Permissions() {
    }

    /** 当前调用者是否满足所需的原版权限等级（0-4）。 */
    public static boolean has(CommandSourceStack source, int level) {
        if (source == null) {
            return false;
        }
        int required = Math.max(0, Math.min(4, level));
        if (required == 0) {
            return true;
        }

        // 路线 1：直接 hasPermission(int)
        Method direct = resolveDirect();
        if (direct != null) {
            try {
                Object result = direct.invoke(source, required);
                return result instanceof Boolean && (Boolean) result;
            } catch (Throwable ignored) {
                // 落到路线 2
            }
        }

        // 路线 2：permissions() → level() → ordinal()
        Method permissions = resolvePermissions();
        Method levelGetter = resolveLevel();
        if (permissions != null && levelGetter != null) {
            try {
                Object set = permissions.invoke(source);
                if (set == null) {
                    return false;
                }
                Object permissionLevel = levelGetter.invoke(set);
                if (permissionLevel instanceof Enum<?>) {
                    return ((Enum<?>) permissionLevel).ordinal() >= required;
                }
            } catch (Throwable t) {
                InfiniteStacks.error("权限等级解析失败: {}", t.toString());
            }
        }

        InfiniteStacks.warn("当前 MC 版本上权限检查无可用路径，按无权限处理（/stack 指令需要权限等级 {}）", required);
        return false;
    }

    private static Method resolveDirect() {
        if (HAS_PERMISSION_INT_RESOLVED) {
            return HAS_PERMISSION_INT;
        }
        try {
            Method m = CommandSourceStack.class.getMethod("hasPermission", int.class);
            HAS_PERMISSION_INT = m;
        } catch (Throwable notPresent) {
            HAS_PERMISSION_INT = null;
        }
        HAS_PERMISSION_INT_RESOLVED = true;
        return HAS_PERMISSION_INT;
    }

    private static Method resolvePermissions() {
        if (PERMISSIONS_RESOLVED) {
            return PERMISSIONS;
        }
        try {
            PERMISSIONS = CommandSourceStack.class.getMethod("permissions");
        } catch (Throwable notPresent) {
            PERMISSIONS = null;
        }
        PERMISSIONS_RESOLVED = true;
        return PERMISSIONS;
    }

    private static Method resolveLevel() {
        if (LEVEL != null) {
            return LEVEL;
        }
        Method permissions = resolvePermissions();
        if (permissions == null) {
            return null;
        }
        try {
            Class<?> setType = permissions.getReturnType();
            LEVEL = setType.getMethod("level");
        } catch (Throwable notPresent) {
            LEVEL = null;
        }
        return LEVEL;
    }
}
