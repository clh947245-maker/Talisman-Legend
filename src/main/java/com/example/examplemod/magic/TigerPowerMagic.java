package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.TigerCloneEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TigerPowerMagic extends MobEffect {
    private static final Map<UUID, UUID> ACTIVE_CLONES = new ConcurrentHashMap<>();
    private static final Map<UUID, CloneTrackerSnapshot> LAST_CLONE_SNAPSHOTS = new ConcurrentHashMap<>();

    public TigerPowerMagic() {
        super(MobEffectCategory.BENEFICIAL, 0xFFA500);
    }

    public record CloneTrackerSnapshot(Vec3 position, ResourceKey<Level> dimension) {
    }

    public static void grantTigerPower(LivingEntity entity, int duration) {
        if (entity == null) {
            return;
        }

        entity.addEffect(new MobEffectInstance(
                ChenMod.TIGER_POWER,
                duration,
                0,
                true,
                true,
                true
        ));
    }

    public static void trackClone(TigerCloneEntity clone) {
        if (clone == null) {
            return;
        }

        UUID cloneUUID = clone.getUUID();
        UUID ownerUUID = clone.getOwnerUUID();
        if (ownerUUID != null) {
            ACTIVE_CLONES.put(ownerUUID, cloneUUID);
        }
        LAST_CLONE_SNAPSHOTS.put(cloneUUID, new CloneTrackerSnapshot(clone.position(), clone.level().dimension()));
    }

    public static void handleCloneRemoved(TigerCloneEntity clone, Entity.RemovalReason reason) {
        if (clone == null) {
            return;
        }

        UUID ownerUUID = clone.getOwnerUUID();
        UUID cloneUUID = clone.getUUID();
        if (ownerUUID != null) {
            ACTIVE_CLONES.remove(ownerUUID, cloneUUID);
        }

        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            LAST_CLONE_SNAPSHOTS.put(cloneUUID, new CloneTrackerSnapshot(clone.position(), clone.level().dimension()));
            return;
        }

        LAST_CLONE_SNAPSHOTS.remove(cloneUUID);
    }

    @Nullable
    public static UUID getActiveCloneUUID(Player player) {
        if (player == null) {
            return null;
        }
        return ACTIVE_CLONES.get(player.getUUID());
    }

    @Nullable
    public static TigerCloneEntity getTrackedClone(@Nullable MinecraftServer server, @Nullable UUID cloneUUID) {
        if (server == null || cloneUUID == null) {
            return null;
        }

        for (ServerLevel serverLevel : server.getAllLevels()) {
            Entity entity = serverLevel.getEntity(cloneUUID);
            if (entity instanceof TigerCloneEntity tigerClone) {
                return tigerClone;
            }
        }

        return null;
    }

    @Nullable
    public static CloneTrackerSnapshot getCloneSnapshot(@Nullable UUID cloneUUID) {
        if (cloneUUID == null) {
            return null;
        }
        return LAST_CLONE_SNAPSHOTS.get(cloneUUID);
    }
}
