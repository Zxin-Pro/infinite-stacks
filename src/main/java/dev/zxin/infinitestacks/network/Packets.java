package dev.zxin.infinitestacks.network;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网络载荷的数据定义（纯数据，不含任何加载器类型）。
 * 通道 ID 与线格式见《接口约定文档》§5，**冻结不可改**。
 */
public final class Packets {

    /** S2C 通道：服务端 → 客户端推送有效配置。 */
    public static final String CHANNEL_SYNC = "sync_config";

    /** C2S 通道：客户端 → 服务端请求最新配置（空载荷）。 */
    public static final String CHANNEL_REQUEST = "request_config";

    /** 协议版本。改动线格式必须 +1。 */
    public static final int PROTOCOL_VERSION = 1;

    /** 载荷硬上限 32 KiB，超过则省略覆盖表。 */
    public static final int MAX_BYTES = 32 * 1024;

    /** 版本号高位标记：置位表示「覆盖表被省略」。 */
    public static final int FLAG_OVERRIDES_OMITTED = 0x80;

    private Packets() {
    }

    /** S2C：服务端权威配置快照。客户端只拿它做渲染，**不参与任何判定**。 */
    public static final class Sync {
        public int globalLimit = 64;
        public int permissionLevel = 2;
        public boolean affectDamageable = false;
        public boolean dropsEnabled = true;
        public int dropsThreshold = 64;
        public int dropsMaxModels = 5;
        public int dropsHideModelAbove = 1000000;
        public float dropsMergeRadius = 1.5F;
        public float dropsScale = 1.0F;
        public int dropsColor = 0xFFFFFF;
        public float dropsBgOpacity = 0.25F;
        public boolean dropsShadow = true;
        /** 0 = ABBREVIATED，1 = FULL。 */
        public int uiMode = 0;
        public int uiThreshold = 9999;
        public Map<String, Integer> items = new LinkedHashMap<String, Integer>();
        public Map<String, Integer> tags = new LinkedHashMap<String, Integer>();
        /** 载荷超限、覆盖表被省略时为 true。 */
        public boolean overridesOmitted = false;
    }

    /** C2S：空载荷，仅表示「请给我一份配置」。 */
    public static final class Request {
        public static final Request INSTANCE = new Request();

        private Request() {
        }
    }
}
