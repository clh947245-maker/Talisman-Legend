package com.example.examplemod.talisman;

import com.example.examplemod.entity.PigLaserEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PigTalismanItem extends Item {

    private static final double LASER_RANGE = 32.0D;
    private static final float LASER_DAMAGE = 8.0F;
    private static final double LASER_KNOCKBACK = 0.35D;
    private static final double BLOCK_BREAK_STEP = 0.30D;
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
            Vec3 leftEnd = resolveBeamEnd(leftStart, look);
            Vec3 rightEnd = resolveBeamEnd(rightStart, look);

            Set<BlockPos> brokenBlocks = new HashSet<>();
            breakBlocksAlongBeam(level, player, leftStart, leftEnd, brokenBlocks);
            breakBlocksAlongBeam(level, player, rightStart, rightEnd, brokenBlocks);
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

    private static Vec3 resolveBeamEnd(Vec3 start, Vec3 direction) {
        return start.add(direction.scale(LASER_RANGE));
    }

    private static void breakBlocksAlongBeam(Level level, Player player, Vec3 start, Vec3 end, Set<BlockPos> brokenBlocks) {
        Vec3 delta = end.subtract(start);
        double totalLength = delta.length();
        if (totalLength < 1.0E-7D) {
            return;
        }

        Vec3 direction = delta.scale(1.0D / totalLength);
        int samples = Math.max(1, Mth.ceil(totalLength / BLOCK_BREAK_STEP));
        for (int i = 0; i <= samples; i++) {
            double traveled = Math.min(totalLength, i * BLOCK_BREAK_STEP);
            BlockPos blockPos = BlockPos.containing(start.add(direction.scale(traveled))).immutable();
            if (!brokenBlocks.add(blockPos)) {
                continue;
            }
            if (canLaserBreakBlock(level, player, blockPos)) {
                level.destroyBlock(blockPos, false, player);
            }
        }
    }

    private static boolean canLaserBreakBlock(Level level, Player player, BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && !state.getCollisionShape(level, blockPos).isEmpty()
                && state.getDestroySpeed(level, blockPos) >= 0.0F
                && level.mayInteract(player, blockPos)
                && state.canEntityDestroy(level, blockPos, player);
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
            target.hurt(player.damageSources().indirectMagic(player, player), LASER_DAMAGE);
            target.setDeltaMovement(target.getDeltaMovement().scale(0.15D));
            target.push(knockbackDirection.x * LASER_KNOCKBACK, 0.03D, knockbackDirection.z * LASER_KNOCKBACK);
            target.hurtMarked = true;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.pig_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
