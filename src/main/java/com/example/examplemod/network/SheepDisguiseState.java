package com.example.examplemod.network;

import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SheepDisguiseState {
    private static final Map<UUID, UUID> SKIN_SOURCES = new ConcurrentHashMap<>();
    private static final Map<UUID, Component> DISPLAY_NAMES = new ConcurrentHashMap<>();

    private SheepDisguiseState() {
    }

    public static void update(UUID playerUUID, boolean active, UUID skinSourceUUID, Component displayName) {
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
