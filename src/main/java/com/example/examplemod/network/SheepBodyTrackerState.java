package com.example.examplemod.network;

/**
 * 客户端保存的“灵魂出窍后原身体”快照。
 * 供界面或客户端逻辑查询身体是否存在、是否存活以及所在位置。
 */
public final class SheepBodyTrackerState {
    private static boolean hasBody;
    private static boolean alive;
    private static double x;
    private static double y;
    private static double z;
    private static String dimension = "";

    private SheepBodyTrackerState() {
    }

    public static void update(boolean hasBodySnapshot, boolean bodyAlive, double bodyX, double bodyY, double bodyZ, String bodyDimension) {
        // 每次收到服务端同步包时都整体覆盖，保证字段来自同一帧状态。
        hasBody = hasBodySnapshot;
        alive = bodyAlive;
        x = bodyX;
        y = bodyY;
        z = bodyZ;
        dimension = bodyDimension == null ? "" : bodyDimension;
    }

    public static void clear() {
        // 退出场景或状态失效时恢复为默认值，避免读取到过期身体坐标。
        hasBody = false;
        alive = false;
        x = 0.0D;
        y = 0.0D;
        z = 0.0D;
        dimension = "";
    }

    public static boolean hasBody() {
        return hasBody;
    }

    public static boolean isAlive() {
        return alive;
    }

    public static double x() {
        return x;
    }

    public static double y() {
        return y;
    }

    public static double z() {
        return z;
    }

    public static String dimension() {
        return dimension;
    }
}
