package com.example.examplemod.network;

import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端绵羊伪装状态缓存。
 * 用于记录某个玩家当前伪装时展示的皮肤来源和显示名称。
 */
public final class SheepDisguiseState {
    // 伪装玩家 UUID -> 皮肤来源玩家 UUID
    private static final Map<UUID, UUID> SKIN_SOURCES = new ConcurrentHashMap<>();
    // 伪装玩家 UUID -> 客户端展示名称
    private static final Map<UUID, Component> DISPLAY_NAMES = new ConcurrentHashMap<>();

    private SheepDisguiseState() {
    }

    public static void update(UUID playerUUID, boolean active, UUID skinSourceUUID, Component displayName) {
        // 伪装关闭时直接清理缓存，避免客户端继续显示旧数据。
        if (!active) {
            clear(playerUUID);
            return;
        }

        if (skinSourceUUID != null) {
            SKIN_SOURCES.put(playerUUID, skinSourceUUID);
        } else {
            SKIN_SOURCES.remove(playerUUID);
        }

        if (displayName != null) {
            DISPLAY_NAMES.put(playerUUID, displayName);
        } else {
            DISPLAY_NAMES.remove(playerUUID);
        }
    }

    public static void clear(UUID playerUUID) {
        SKIN_SOURCES.remove(playerUUID);
        DISPLAY_NAMES.remove(playerUUID);
    }

    public static void clearAll() {
        SKIN_SOURCES.clear();
        DISPLAY_NAMES.clear();
    }

    public static UUID getSkinSource(UUID playerUUID) {
        return SKIN_SOURCES.get(playerUUID);
    }

    public static Component getDisplayName(UUID playerUUID) {
        return DISPLAY_NAMES.get(playerUUID);
    }
}
