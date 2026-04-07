package com.example.examplemod.talisman;

import com.example.examplemod.entity.PigLaserEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PigTalismanItem extends Item {

    private static final double LASER_RANGE = 32.0D;
    private static final float LASER_DAMAGE = 8.0F;
    private static final double LASER_KNOCKBACK = 0.35D;
    private static final int HIT_STUN_TICKS = 8;
    private static final int HIT_STUN_AMPLIFIER = 6;
    public static final int COOLDOWN_TICKS = 12;

    public PigTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 leftStart = computeEyeStart(player, look, -1.0D);
            Vec3 rightStart = computeEyeStart(player, look, 1.0D);
            Vec3 leftEnd = resolveBeamEnd(level, player, leftStart, look);
            Vec3 rightEnd = resolveBeamEnd(level, player, rightStart, look);

            Set<Integer> hitEntityIds = new HashSet<>();
            damageEntitiesAlongBeam(level, player, leftStart, leftEnd, hitEntityIds);
            damageEntitiesAlongBeam(level, player, rightStart, rightEnd, hitEntityIds);

            level.addFreshEntity(new PigLaserEntity(level, leftStart, leftEnd, rightStart, rightEnd));
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private static Vec3 computeEyeStart(Player player, Vec3 look, double side) {
        Vec3 up = player.getUpVector(1.0F).normalize();
        Vec3 right = up.cross(look);
        if (right.lengthSqr() < 1.0E-7D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        Vec3 eyeCenter = player.getEyePosition()
                .add(look.scale(0.25D))
                .add(up.scale(-0.10D));
        return eyeCenter.add(right.scale(0.18D * side));
    }

    private static Vec3 resolveBeamEnd(Level level, Player player, Vec3 start, Vec3 direction) {
        Vec3 target = start.add(direction.scale(LASER_RANGE));
        BlockHitResult hitResult = level.clip(new ClipContext(start, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getLocation() : target;
    }

    private static void damageEntitiesAlongBeam(Level level, Player player, Vec3 start, Vec3 end, Set<Integer> hitEntityIds) {
        AABB beamBox = new AABB(start, end).inflate(0.75D);
        Vec3 knockbackDirection = end.subtract(start);
        if (knockbackDirection.lengthSqr() > 1.0E-7D) {
            knockbackDirection = knockbackDirection.normalize();
        } else {
            knockbackDirection = player.getLookAngle().normalize();
        }
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, beamBox, entity ->
                entity.isAlive() && entity != player && !entity.isSpectator());

        for (LivingEntity target : targets) {
            if (!target.getBoundingBox().inflate(0.3D).clip(start, end).isPresent()) {
                continue;
            }
            if (!hitEntityIds.add(target.getId())) {
                continue;
            }
            target.hurt(player.damageSources().magic(), LASER_DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.15D));
            target.push(knockbackDirection.x * LASER_KNOCKBACK, 0.03D, knockbackDirection.z * LASER_KNOCKBACK);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HIT_STUN_TICKS, HIT_STUN_AMPLIFIER, false, false, true));
            target.hurtMarked = true;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.pig_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
