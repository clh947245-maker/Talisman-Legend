package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.SheepBodyEntity;
import com.example.examplemod.network.packet.SheepBodyTrackerPayload;
import com.example.examplemod.network.packet.SheepDisguisePayload;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import com.example.examplemod.network.ModNetwork;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SheepPowerMagic extends MobEffect {
    public static final double RETURN_TRIGGER_RADIUS = 3.0D;

    private static final ResourceLocation SHEEP_FLIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_power_flight");
    private static final ResourceLocation SHEEP_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheep_power_speed");
    private static final double SPEED_MULTIPLIER = 1.5D;

    private static final Map<UUID, UUID> ACTIVE_BODIES = new ConcurrentHashMap<>();
    private static final Map<UUID, BodyTrackerSnapshot> LAST_BODY_SNAPSHOTS = new ConcurrentHashMap<>();
    private static final Set<UUID> SKIP_RESTORE_ON_REMOVAL = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, UUID> PENDING_RETURN_BODIES = new ConcurrentHashMap<>();
    private static final Map<UUID, DisguiseIdentity> DISGUISED_IDENTITIES = new ConcurrentHashMap<>();

    private static Field jumpingField;

    static {
        try {
            jumpingField = LivingEntity.class.getDeclaredField("jumping");
            jumpingField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ChenMod.LOGGER.error("Unable to access jumping field in SheepPowerMagic.", e);
        }
    }

    public SheepPowerMagic() {
        super(MobEffectCategory.BENEFICIAL, 0xA8D8FF);
    }

    private record BodyTrackerSnapshot(Vec3 position, ResourceKey<Level> dimension, boolean alive) {
    }

    private record DisguiseIdentity(UUID skinSourceUUID, Component displayName) {
    }

    public static void grantSheepPower(LivingEntity entity, int duration) {
        if (entity == null) {
            return;
        }
        entity.addEffect(new MobEffectInstance(ChenMod.SHEEP_POWER.getHolder().orElseThrow(), duration, 0, false, false, true));
    }

    public static void spawnBody(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        SheepBodyEntity existingBody = getTrackedBody(player);
        if (existingBody != null) {
            existingBody.discard();
        }
        ACTIVE_BODIES.remove(player.getUUID());

        SheepBodyEntity body = new SheepBodyEntity(ChenMod.SHEEP_BODY.get(), serverLevel);
        body.copyStateFrom(player);
        if (serverLevel.addFreshEntity(body)) {
            ACTIVE_BODIES.put(player.getUUID(), body.getUUID());
            updateBodySnapshot(player.getUUID(), body.position(), serverLevel.dimension(), true);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) {
            return true;
        }

        player.noPhysics = true;
        removeAllOtherEffects(player);

        if (player.invulnerableTime < 20) {
            player.invulnerableTime = 20;
        }

        // 灵魂状态下接触火焰或岩浆时不应进入燃烧状态。
        player.clearFire();

        if (!player.getAbilities().invulnerable) {
            player.getAbilities().invulnerable = true;
            player.onUpdateAbilities();
        }var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null && !speedAttribute.hasModifier(SHEEP_SPEED_ID)) {
            speedAttribute.addTransientModifier(
                    new AttributeModifier(
                            SHEEP_SPEED_ID,
                            SPEED_MULTIPLIER,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        if (isJumping(player) && !player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        if (!player.getAbilities().flying) {
            if (player.isShiftKeyDown()) {
                player.setDeltaMovement(player.getDeltaMovement().x, -0.6D, player.getDeltaMovement().z);
            } else {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
            }
        }

        if (player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 2 == 0) {
            syncBodyTracker(serverPlayer);
        }

        return true;
    }

    public static void restorePlayer(Player player) {
        if (player == null) {
            return;
        }

        SheepBodyEntity body = consumePendingReturnBody(player);
        if (body == null || !body.isAlive()) {
            body = getTrackedBody(player);
        }

        if (body != null && body.isAlive()) {
            body.applyStoredStateTo(player);
            applyBodyIdentityToPlayer(player, body);
            clearTrackedBodyForOwner(body);
            body.discard();
        }
        ACTIVE_BODIES.remove(player.getUUID());
        LAST_BODY_SNAPSHOTS.remove(player.getUUID());
        PENDING_RETURN_BODIES.remove(player.getUUID());
        clearSoulState(player);
    }

    public static void clearSoulState(Player player) {
        if (player == null) {
            return;
        }

        player.noPhysics = false;
        player.invulnerableTime = 0;
        player.clearFire();

        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().invulnerable = false;
        }var speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(SHEEP_SPEED_ID);
        }

        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    public static void discardTrackedBody(Player player) {
        if (player == null) {
            return;
        }

        SheepBodyEntity body = getTrackedBody(player);
        if (body != null) {
            body.discard();
        }
        ACTIVE_BODIES.remove(player.getUUID());
        LAST_BODY_SNAPSHOTS.remove(player.getUUID());
        PENDING_RETURN_BODIES.remove(player.getUUID());
    }

    public static void markSkipRestore(Player player) {
        if (player != null) {
            SKIP_RESTORE_ON_REMOVAL.add(player.getUUID());
        }
    }

    public static boolean consumeSkipRestore(Player player) {
        return player != null && SKIP_RESTORE_ON_REMOVAL.remove(player.getUUID());
    }

    public static void onTrackedBodyDeath(SheepBodyEntity body) {
        if (body == null) {
            return;
        }
        UUID ownerUUID = body.getOwnerUUID();
        if (ownerUUID == null) {
            return;
        }
        ACTIVE_BODIES.remove(ownerUUID, body.getUUID());
        updateBodySnapshot(ownerUUID, body.position(), body.level().dimension(), false);
    }

    public static boolean isNearTrackedBody(Player player) {
        if (player == null) {
            return false;
        }

        double maxDistanceSqr = RETURN_TRIGGER_RADIUS * RETURN_TRIGGER_RADIUS;
        return !player.level().getEntitiesOfClass(
                SheepBodyEntity.class,
                player.getBoundingBox().inflate(RETURN_TRIGGER_RADIUS),
                body -> body.isAlive()
                        && player.getUUID().equals(body.getOwnerUUID())
                        && body.distanceToSqr(player) <= maxDistanceSqr
        ).isEmpty();
    }

    /**
     * 判断玩家附近是否存在任意可进入的分身。
     * 这里不再校验主人 UUID，因此无主分身和他人分身都可作为进入目标。
     */
    public static boolean isNearReturnableBody(Player player) {
        return getNearestReturnableBody(player) != null;
    }

    /**
     * 选取玩家附近最近的活着分身，作为“灵魂回归/附身”的目标。
     */
    public static SheepBodyEntity getNearestReturnableBody(Player player) {
        if (player == null) {
            return null;
        }

        double maxDistanceSqr = RETURN_TRIGGER_RADIUS * RETURN_TRIGGER_RADIUS;
        List<SheepBodyEntity> nearbyBodies = player.level().getEntitiesOfClass(
                SheepBodyEntity.class,
                player.getBoundingBox().inflate(RETURN_TRIGGER_RADIUS),
                body -> body.isAlive() && body.distanceToSqr(player) <= maxDistanceSqr
        );

        SheepBodyEntity nearestBody = null;
        double nearestDistanceSqr = Double.MAX_VALUE;
        for (SheepBodyEntity body : nearbyBodies) {
            double distanceSqr = body.distanceToSqr(player);
            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearestBody = body;
            }
        }
        return nearestBody;
    }

    /**
     * 标记本次效果移除后应进入的目标分身。
     * 这样在统一的 restorePlayer 流程里，就能把玩家恢复到指定分身而不是默认只回自己的身体。
     */
    public static void setPendingReturnBody(Player player, SheepBodyEntity body) {
        if (player == null || body == null) {
            return;
        }
        PENDING_RETURN_BODIES.put(player.getUUID(), body.getUUID());
    }

    /**
     * 获取玩家当前应保存到分身上的外观来源 UUID。
     * 若玩家已经借用了他人身体，则继续沿用借来的皮肤来源。
     */
    public static UUID getCurrentAppearanceUUID(Player player) {
        if (player == null) {
            return null;
        }

        DisguiseIdentity disguiseIdentity = getCurrentIdentity(player);
        return disguiseIdentity.skinSourceUUID() == null ? player.getUUID() : disguiseIdentity.skinSourceUUID();
    }

    public static Component getCurrentDisplayName(Player player) {
        if (player == null) {
            return null;
        }

        DisguiseIdentity disguiseIdentity = getCurrentIdentity(player);
        return disguiseIdentity.displayName() == null ? player.getName().copy() : disguiseIdentity.displayName().copy();
    }

    public static Component getDisguiseDisplayName(Player player) {
        if (player == null) {
            return null;
        }
        DisguiseIdentity disguiseIdentity = DISGUISED_IDENTITIES.get(player.getUUID());
        return disguiseIdentity == null ? null : disguiseIdentity.displayName();
    }

    private boolean isJumping(LivingEntity entity) {
        if (jumpingField == null) {
            return false;
        }
        try {
            return jumpingField.getBoolean(entity);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private void removeAllOtherEffects(Player player) {
        List<MobEffectInstance> activeEffects = new ArrayList<>(player.getActiveEffects());
        for (MobEffectInstance effectInstance : activeEffects) {
            if (effectInstance.is(ChenMod.SHEEP_POWER.getHolder().orElseThrow())) {
                continue;
            }
            Holder<MobEffect> effectHolder = effectInstance.getEffect();
            player.removeEffect(effectHolder);
        }
    }

    public static SheepBodyEntity getTrackedBody(Player player) {
        if (player == null || player.getServer() == null) {
            return null;
        }

        UUID bodyUUID = ACTIVE_BODIES.get(player.getUUID());
        if (bodyUUID == null) {
            return null;
        }

        for (ServerLevel serverLevel : player.getServer().getAllLevels()) {
            Entity entity = serverLevel.getEntity(bodyUUID);
            if (entity instanceof SheepBodyEntity bodyEntity) {
                return bodyEntity;
            }
        }

        ACTIVE_BODIES.remove(player.getUUID());
        return null;
    }

    private static SheepBodyEntity consumePendingReturnBody(Player player) {
        if (player == null || player.getServer() == null) {
            return null;
        }

        UUID bodyUUID = PENDING_RETURN_BODIES.remove(player.getUUID());
        if (bodyUUID == null) {
            return null;
        }

        for (ServerLevel serverLevel : player.getServer().getAllLevels()) {
            Entity entity = serverLevel.getEntity(bodyUUID);
            if (entity instanceof SheepBodyEntity bodyEntity) {
                return bodyEntity;
            }
        }

        return null;
    }

    private static void clearTrackedBodyForOwner(SheepBodyEntity body) {
        UUID ownerUUID = body.getOwnerUUID();
        if (ownerUUID == null) {
            return;
        }

        ACTIVE_BODIES.remove(ownerUUID, body.getUUID());
        LAST_BODY_SNAPSHOTS.remove(ownerUUID);
        PENDING_RETURN_BODIES.remove(ownerUUID);
    }

    private static void applyBodyIdentityToPlayer(Player player, SheepBodyEntity body) {
        if (player == null || body == null) {
            return;
        }

        UUID appearanceUUID = body.resolveAppearanceUUID();
        Component displayName = body.resolveDisplayName();

        boolean useOwnSkin = appearanceUUID == null || appearanceUUID.equals(player.getUUID());
        boolean useOwnName = displayName == null || displayName.equals(player.getName());

        if (useOwnSkin && useOwnName) {
            clearDisguiseIdentity(player);
            return;
        }

        setDisguiseIdentity(player, useOwnSkin ? null : appearanceUUID, useOwnName ? player.getName() : displayName);
    }

    private static void setDisguiseIdentity(Player player, UUID skinSourceUUID, Component displayName) {
        if (player == null) {
            return;
        }

        DISGUISED_IDENTITIES.put(player.getUUID(), new DisguiseIdentity(skinSourceUUID, displayName));
        refreshIdentityDisplays(player);
        syncDisguiseIdentity(player);
    }

    public static void clearDisguiseIdentity(Player player) {
        if (player == null) {
            return;
        }

        DISGUISED_IDENTITIES.remove(player.getUUID());
        refreshIdentityDisplays(player);
        syncDisguiseIdentity(player);
    }

    public static UUID getDisguiseSkinSource(UUID playerUUID) {
        DisguiseIdentity disguiseIdentity = DISGUISED_IDENTITIES.get(playerUUID);
        return disguiseIdentity == null ? null : disguiseIdentity.skinSourceUUID();
    }

    public static Component getDisguiseDisplayName(UUID playerUUID) {
        DisguiseIdentity disguiseIdentity = DISGUISED_IDENTITIES.get(playerUUID);
        return disguiseIdentity == null ? null : disguiseIdentity.displayName();
    }

    private static DisguiseIdentity getCurrentIdentity(Player player) {
        DisguiseIdentity disguiseIdentity = DISGUISED_IDENTITIES.get(player.getUUID());
        if (disguiseIdentity != null) {
            return disguiseIdentity;
        }
        return new DisguiseIdentity(player.getUUID(), player.getName());
    }

    public static void syncDisguiseIdentityTo(ServerPlayer recipient, Player target) {
        if (recipient == null || target == null) {
            return;
        }

        DisguiseIdentity disguiseIdentity = DISGUISED_IDENTITIES.get(target.getUUID());
        ModNetwork.sendToPlayer(recipient, createDisguisePayload(target.getUUID(), disguiseIdentity));
    }

    public static void syncAllDisguiseIdentitiesTo(ServerPlayer recipient) {
        if (recipient == null) {
            return;
        }

        for (Map.Entry<UUID, DisguiseIdentity> entry : DISGUISED_IDENTITIES.entrySet()) {
            ModNetwork.sendToPlayer(recipient, createDisguisePayload(entry.getKey(), entry.getValue()));
        }
    }

    private static void refreshIdentityDisplays(Player player) {
        player.refreshDisplayName();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.refreshTabListName();
        }
    }

    private static void syncDisguiseIdentity(Player player) {
        if (player == null || player.getServer() == null) {
            return;
        }

        SheepDisguisePayload payload = createDisguisePayload(player.getUUID(), DISGUISED_IDENTITIES.get(player.getUUID()));
        for (ServerPlayer serverPlayer : player.getServer().getPlayerList().getPlayers()) {
            ModNetwork.sendToPlayer(serverPlayer, payload);
        }
    }

    private static SheepDisguisePayload createDisguisePayload(UUID playerUUID, DisguiseIdentity disguiseIdentity) {
        boolean active = disguiseIdentity != null;
        UUID skinSourceUUID = active ? disguiseIdentity.skinSourceUUID() : null;
        Component displayName = active ? disguiseIdentity.displayName() : null;
        return new SheepDisguisePayload(
                playerUUID,
                active,
                skinSourceUUID == null ? "" : skinSourceUUID.toString(),
                displayName
        );
    }

    private static void syncBodyTracker(ServerPlayer player) {
        SheepBodyEntity body = getTrackedBody(player);
        BodyTrackerSnapshot snapshot;
        if (body != null) {
            snapshot = updateBodySnapshot(player.getUUID(), body.position(), body.level().dimension(), body.isAlive());
        } else {
            snapshot = LAST_BODY_SNAPSHOTS.get(player.getUUID());
        }

        if (snapshot == null) {
            ModNetwork.sendToPlayer(player, new SheepBodyTrackerPayload(false, false, 0.0D, 0.0D, 0.0D, ""));
            return;
        }

        ModNetwork.sendToPlayer(
                player,
                new SheepBodyTrackerPayload(
                        true,
                        snapshot.alive(),
                        snapshot.position().x,
                        snapshot.position().y,
                        snapshot.position().z,
                        snapshot.dimension().location().toString()
                )
        );
    }

    private static BodyTrackerSnapshot updateBodySnapshot(UUID playerUUID, Vec3 position, ResourceKey<Level> dimension, boolean alive) {
        BodyTrackerSnapshot snapshot = new BodyTrackerSnapshot(position, dimension, alive);
        LAST_BODY_SNAPSHOTS.put(playerUUID, snapshot);
        return snapshot;
    }
}
