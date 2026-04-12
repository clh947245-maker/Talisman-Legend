package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.ShadowNinjaEntity;
import com.example.examplemod.item.OniMaskItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ShadowNinjaSquadManager {
    private static final int MAX_SUMMONED_NINJAS = 6;
    private static final double BASE_SUMMON_RADIUS = 3.0D;
    private static final double RADIUS_STEP = 0.85D;
    private static final double ANGLE_STEP = (Math.PI * 2.0D) / MAX_SUMMONED_NINJAS;

    private ShadowNinjaSquadManager() {
    }

    public static void summonSquad(ServerPlayer player) {
        List<ShadowNinjaEntity> activeSummons = getCommandedNinjas(player.serverLevel().getServer(), player.getUUID(), true);
        int missingCount = MAX_SUMMONED_NINJAS - activeSummons.size();
        if (missingCount <= 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        LivingEntity commanderAnchor = OniMaskItem.getMaskAnchor(player);
        if (commanderAnchor == null) {
            return;
        }

        for (int slot = activeSummons.size(); slot < activeSummons.size() + missingCount; slot++) {
            ShadowNinjaEntity ninja = ChenMod.SHADOW_NINJA.get().create(level);
            if (ninja == null) {
                continue;
            }

            Vec3 spawnPos = findSpawnPosition(level, commanderAnchor, ninja, slot);
            ninja.assignCommander(player, true);
            ninja.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, commanderAnchor.getYRot() + 180.0F, 0.0F);
            ninja.prepareSummonFromBelow(spawnPos.y);
            ninja.setPersistenceRequired();
            level.addFreshEntity(ninja);
        }
    }

    public static void dismissAll(ServerPlayer player) {
        for (ShadowNinjaEntity ninja : getCommandedNinjas(player.serverLevel().getServer(), player.getUUID(), false)) {
            if (!ninja.isRemoved() && !ninja.isDismissing()) {
                ninja.startDismissAnimation();
            }
        }
    }

    private static List<ShadowNinjaEntity> getCommandedNinjas(net.minecraft.server.MinecraftServer server, UUID commanderUuid, boolean summonedOnly) {
        List<ShadowNinjaEntity> ninjas = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ShadowNinjaEntity ninja) || !ninja.isAlive()) {
                    continue;
                }

                if (!ninja.isCommandedBy(commanderUuid)) {
                    continue;
                }

                if (summonedOnly && !ninja.isMaskSummoned()) {
                    continue;
                }

                ninjas.add(ninja);
            }
        }
        return ninjas;
    }

    private static Vec3 findSpawnPosition(ServerLevel level, LivingEntity commanderAnchor, ShadowNinjaEntity ninja, int slot) {
        double baseAngle = Math.toRadians(commanderAnchor.getYRot()) + slot * ANGLE_STEP;
        double baseY = commanderAnchor.getY();

        for (int radiusStep = 0; radiusStep < 3; radiusStep++) {
            double radius = BASE_SUMMON_RADIUS + radiusStep * RADIUS_STEP;
            double angle = baseAngle + radiusStep * 0.33D;
            double x = commanderAnchor.getX() + Math.cos(angle) * radius;
            double z = commanderAnchor.getZ() + Math.sin(angle) * radius;

            for (int yOffset = 2; yOffset >= -4; yOffset--) {
                double y = Mth.floor(baseY) + yOffset;
                ninja.moveTo(x, y, z, commanderAnchor.getYRot() + 180.0F, 0.0F);

                BlockPos feetPos = BlockPos.containing(x, y, z);
                BlockState belowState = level.getBlockState(feetPos.below());
                if (!belowState.blocksMotion()) {
                    continue;
                }

                if (level.noCollision(ninja)) {
                    return new Vec3(x, y, z);
                }
            }
        }

        return commanderAnchor.position();
    }
}
