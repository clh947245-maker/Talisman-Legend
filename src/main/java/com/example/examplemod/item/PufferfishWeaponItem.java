package com.example.examplemod.item;

import com.example.examplemod.entity.ShadowNinjaEntity;
import com.example.examplemod.entity.PufferfishLaserEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class PufferfishWeaponItem extends Item {

    private static final double LASER_RANGE = 32.0D;
    private static final float LASER_DAMAGE = 8.0F;
    private static final float SHADOW_NINJA_EXECUTION_BONUS_DAMAGE = 1000.0F;
    private static final double LASER_KNOCKBACK = 0.35D;
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
        return ((PufferfishWeaponItem) stack.getItem()).fire(player, stack);
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
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private record BeamTarget(LivingEntity entity, Vec3 hitPos) {
    }
}
