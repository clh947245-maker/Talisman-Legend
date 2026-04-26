package com.example.examplemod.item;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.PufferfishLaserEntity;
import com.example.examplemod.entity.ShadowNinjaEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PufferfishWeaponItem extends Item {

    private static final double LASER_RANGE = 32.0D;
    private static final float LASER_DAMAGE = 8.0F;
    private static final float SHADOW_NINJA_EXECUTION_BONUS_DAMAGE = 1000.0F;
    private static final double LASER_KNOCKBACK = 0.35D;
    private static final String TAG_MODE = "PufferfishMode";
    private static final String TAG_GLOWING = "PufferfishPalaceGlow";
    private static final String TAG_SCAN_PENDING = "PufferfishPalaceScanPending";
    private static final int MODE_ATTACK = 0;
    private static final int MODE_SENSE = 1;
    private static final int PALACE_DETECTION_RANGE_BLOCKS = 5000;
    private static final int PALACE_DETECTION_RANGE_CHUNKS = Mth.ceil(PALACE_DETECTION_RANGE_BLOCKS / 16.0F);
    private static final int PALACE_CACHE_TICKS = 40;
    private static final double PALACE_FACING_DOT_THRESHOLD = 0.707106781D;
    private static final TagKey<Structure> SHENG_ZHU_PALACE_LOCATED = TagKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheng_zhu_palace_located")
    );
    private static final Map<UUID, PalaceSenseCache> PALACE_SENSE_CACHE = new HashMap<>();
    public static final int COOLDOWN_TICKS = 10;

    public PufferfishWeaponItem() {
        super(new Item.Properties().stacksTo(1).durability(50));
    }

    public static boolean isHoldingPufferfishWeapon(Player player) {
        return player != null && player.getMainHandItem().getItem() instanceof PufferfishWeaponItem;
    }

    public static boolean fireFromMainHand(Player player) {
        if (!isHoldingPufferfishWeapon(player)) {
            return false;
        }

        ItemStack stack = player.getMainHandItem();
        if (getMode(stack) != MODE_ATTACK) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.chen_mod.pufferfish_weapon.sense_active"), true);
            }
            return false;
        }

        return ((PufferfishWeaponItem) stack.getItem()).fire(player, stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isPalaceGlowing(stack) || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        toggleMode(level, player, stack);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        toggleMode(context.getLevel(), player, context.getItemInHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public static void serverTickSenseMode(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel) || !isHoldingPufferfishWeapon(player)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (getMode(stack) != MODE_SENSE) {
            setPalaceGlowing(stack, false);
            return;
        }

        if (player.tickCount % 5 != 0) {
            return;
        }

        BlockPos palacePos = findNearestPalace(serverLevel, player);
        boolean showedSenseResult = showPendingSenseResult(player, stack, palacePos);
        boolean detected = palacePos != null && isFacingPalace(player, palacePos);
        setPalaceGlowing(stack, detected);
    }

    private boolean fire(Player player, ItemStack stack) {
        Level level = player.level();
        if (level.isClientSide || player.getCooldowns().isOnCooldown(this)) {
            return false;
        }

        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-7D) {
            return false;
        }
        look = look.normalize();

        Vec3 beamStart = computeBeamStart(player, look);
        Vec3 maxBeamEnd = beamStart.add(look.scale(LASER_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(beamStart, maxBeamEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 beamEnd = blockHit.getType() == HitResult.Type.MISS ? maxBeamEnd : blockHit.getLocation();

        BeamTarget target = findFirstTarget(level, player, beamStart, beamEnd);
        if (target != null) {
            beamEnd = target.hitPos();
            target.entity().hurt(player.damageSources().indirectMagic(player, player), getLaserDamage(target.entity()));
            target.entity().push(look.x * LASER_KNOCKBACK, 0.03D, look.z * LASER_KNOCKBACK);
            target.entity().hurtMarked = true;
        }

        level.addFreshEntity(new PufferfishLaserEntity(level, beamStart, beamEnd));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        damageWeapon(player, stack);
        return true;
    }

    private static Vec3 computeBeamStart(Player player, Vec3 look) {
        Vec3 up = player.getUpVector(1.0F).normalize();
        Vec3 right = up.cross(look);
        if (right.lengthSqr() < 1.0E-7D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        double side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0D : -1.0D;
        Vec3 eyeCenter = player.getEyePosition()
                .add(look.scale(0.45D))
                .add(up.scale(-0.16D));
        return eyeCenter.add(right.scale(0.22D * side));
    }

    private static BeamTarget findFirstTarget(Level level, Player player, Vec3 beamStart, Vec3 beamEnd) {
        AABB searchBox = new AABB(beamStart, beamEnd).inflate(0.8D);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                canLaserHit(player, entity));

        BeamTarget closestTarget = null;
        double closestDistanceSqr = Double.MAX_VALUE;
        for (LivingEntity target : targets) {
            Optional<Vec3> clip = target.getBoundingBox().inflate(0.25D).clip(beamStart, beamEnd);
            if (clip.isEmpty()) {
                continue;
            }

            double distanceSqr = beamStart.distanceToSqr(clip.get());
            if (distanceSqr >= closestDistanceSqr) {
                continue;
            }

            closestDistanceSqr = distanceSqr;
            closestTarget = new BeamTarget(target, clip.get());
        }

        return closestTarget;
    }

    private static boolean canLaserHit(Player player, LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == player || entity.isSpectator()) {
            return false;
        }

        if (entity instanceof ShadowNinjaEntity) {
            return true;
        }

        return !player.isAlliedTo(entity) && !entity.isAlliedTo(player);
    }

    private static void damageWeapon(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) {
            return;
        }

        int nextDamage = stack.getDamageValue() + 1;
        if (nextDamage >= stack.getMaxDamage()) {
            stack.shrink(1);
            return;
        }

        stack.setDamageValue(nextDamage);
    }

    private static float getLaserDamage(LivingEntity target) {
        if (target instanceof ShadowNinjaEntity) {
            return target.getHealth() + target.getAbsorptionAmount() + SHADOW_NINJA_EXECUTION_BONUS_DAMAGE;
        }

        return LASER_DAMAGE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.pufferfish_weapon.desc"));
        tooltipComponents.add(Component.translatable(getModeTooltipKey(getMode(stack))).withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private static int getMode(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.getInt(TAG_MODE) == MODE_SENSE ? MODE_SENSE : MODE_ATTACK;
    }

    private static void setMode(ItemStack stack, int mode) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt(TAG_MODE, mode == MODE_SENSE ? MODE_SENSE : MODE_ATTACK);
        tag.putBoolean(TAG_GLOWING, false);
        tag.putBoolean(TAG_SCAN_PENDING, mode == MODE_SENSE);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean isScanPending(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.getBoolean(TAG_SCAN_PENDING);
    }

    private static void setScanPending(ItemStack stack, boolean pending) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.getBoolean(TAG_SCAN_PENDING) == pending) {
            return;
        }

        tag.putBoolean(TAG_SCAN_PENDING, pending);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean isPalaceGlowing(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.getBoolean(TAG_GLOWING);
    }

    private static void setPalaceGlowing(ItemStack stack, boolean glowing) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.getBoolean(TAG_GLOWING) == glowing) {
            return;
        }

        tag.putBoolean(TAG_GLOWING, glowing);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void toggleMode(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }

        int mode = getMode(stack) == MODE_ATTACK ? MODE_SENSE : MODE_ATTACK;
        setMode(stack, mode);
        player.displayClientMessage(Component.translatable(getModeMessageKey(mode)), true);
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
    }

    private static String getModeMessageKey(int mode) {
        return mode == MODE_SENSE
                ? "message.chen_mod.pufferfish_weapon.mode_sense"
                : "message.chen_mod.pufferfish_weapon.mode_attack";
    }

    private static String getModeTooltipKey(int mode) {
        return mode == MODE_SENSE
                ? "item.chen_mod.pufferfish_weapon.mode_sense"
                : "item.chen_mod.pufferfish_weapon.mode_attack";
    }

    private static boolean showPendingSenseResult(Player player, ItemStack stack, BlockPos palacePos) {
        if (!isScanPending(stack)) {
            return false;
        }

        setScanPending(stack, false);
        if (palacePos == null) {
            player.displayClientMessage(Component.translatable("message.chen_mod.pufferfish_weapon.no_magic_nearby"), true);
            return true;
        }

        double dx = palacePos.getX() + 0.5D - player.getX();
        double dz = palacePos.getZ() + 0.5D - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        player.displayClientMessage(
                Component.translatable(
                        "message.chen_mod.pufferfish_weapon.sense_complete",
                        Component.translatable(getDirectionKey(dx, dz)),
                        String.format(Locale.ROOT, "%.0f", distance)
                ),
                true
        );
        return true;
    }

    private static BlockPos findNearestPalace(ServerLevel level, Player player) {
        UUID playerId = player.getUUID();
        ResourceKey<Level> dimension = level.dimension();
        PalaceSenseCache cached = PALACE_SENSE_CACHE.get(playerId);
        if (cached != null
                && cached.dimension().equals(dimension)
                && player.tickCount - cached.tick() < PALACE_CACHE_TICKS) {
            return cached.pos();
        }

        BlockPos pos = level.findNearestMapStructure(
                SHENG_ZHU_PALACE_LOCATED,
                player.blockPosition(),
                PALACE_DETECTION_RANGE_CHUNKS,
                false
        );
        if (pos != null && player.blockPosition().distSqr(pos) > (double) PALACE_DETECTION_RANGE_BLOCKS * PALACE_DETECTION_RANGE_BLOCKS) {
            pos = null;
        }

        PALACE_SENSE_CACHE.put(playerId, new PalaceSenseCache(dimension, pos, player.tickCount));
        return pos;
    }

    private static boolean isFacingPalace(Player player, BlockPos palacePos) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-7D) {
            return false;
        }

        Vec3 toPalace = Vec3.atCenterOf(palacePos).subtract(player.position());
        Vec3 horizontalPalace = new Vec3(toPalace.x, 0.0D, toPalace.z);
        if (horizontalPalace.lengthSqr() < 1.0E-7D) {
            return true;
        }

        return horizontalLook.normalize().dot(horizontalPalace.normalize()) >= PALACE_FACING_DOT_THRESHOLD;
    }

    private static String getDirectionKey(double dx, double dz) {
        if (Math.abs(dx) < 1.0D && Math.abs(dz) < 1.0D) {
            return "direction.chen_mod.here";
        }

        double angle = Math.toDegrees(Math.atan2(dz, dx));
        int index = Mth.floor((angle + 22.5D) / 45.0D) & 7;
        return switch (index) {
            case 0 -> "direction.chen_mod.east";
            case 1 -> "direction.chen_mod.south_east";
            case 2 -> "direction.chen_mod.south";
            case 3 -> "direction.chen_mod.south_west";
            case 4 -> "direction.chen_mod.west";
            case 5 -> "direction.chen_mod.north_west";
            case 6 -> "direction.chen_mod.north";
            default -> "direction.chen_mod.north_east";
        };
    }

    private record BeamTarget(LivingEntity entity, Vec3 hitPos) {
    }

    private record PalaceSenseCache(ResourceKey<Level> dimension, BlockPos pos, int tick) {
    }
}
