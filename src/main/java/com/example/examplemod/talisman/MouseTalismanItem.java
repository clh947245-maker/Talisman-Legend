package com.example.examplemod.talisman;

import com.example.examplemod.entity.MouseBeamEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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

public class MouseTalismanItem extends Item {

    private static final double BEAM_RANGE = 20.0D;
    private static final double ENTITY_HIT_PADDING = 0.2D;
    public static final int COOLDOWN_TICKS = 8;

    public MouseTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            Vec3 direction = player.getLookAngle().normalize();
            Vec3 start = player.getEyePosition().add(direction.scale(0.35D));
            MouseBeamImpact impact = resolveBeamEnd(level, player, start, direction);

            level.addFreshEntity(new MouseBeamEntity(level, start, impact.end(), impact.impactType(), impact.blockPos()));
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }

    private static MouseBeamImpact resolveBeamEnd(Level level, Player player, Vec3 start, Vec3 direction) {
        Vec3 maxEnd = start.add(direction.scale(BEAM_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 bestEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : maxEnd;
        int impactType = blockHit.getType() == HitResult.Type.BLOCK ? 2 : 0;
        BlockPos impactBlockPos = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getBlockPos() : null;
        double bestDistanceSqr = start.distanceToSqr(bestEnd);

        AABB searchBox = new AABB(start, bestEnd).inflate(ENTITY_HIT_PADDING);
        List<Entity> candidates = level.getEntities(player, searchBox, entity ->
                entity.isAlive() && entity.isPickable() && !entity.isSpectator());

        for (Entity candidate : candidates) {
            Optional<Vec3> hitPoint = candidate.getBoundingBox().inflate(ENTITY_HIT_PADDING).clip(start, bestEnd);
            if (hitPoint.isEmpty()) {
                continue;
            }

            double hitDistanceSqr = start.distanceToSqr(hitPoint.get());
            if (hitDistanceSqr < bestDistanceSqr) {
                bestDistanceSqr = hitDistanceSqr;
                bestEnd = hitPoint.get();
                impactType = 1;
                impactBlockPos = null;
            }
        }

        return new MouseBeamImpact(bestEnd, impactType, impactBlockPos);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.mouse_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private record MouseBeamImpact(Vec3 end, int impactType, BlockPos blockPos) {
    }
}
