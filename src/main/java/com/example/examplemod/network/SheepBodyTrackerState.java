package com.example.examplemod.network;

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
        hasBody = hasBodySnapshot;
        alive = bodyAlive;
        x = bodyX;
        y = bodyY;
        z = bodyZ;
        dimension = bodyDimension == null ? "" : bodyDimension;
    }

    public static void clear() {
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
