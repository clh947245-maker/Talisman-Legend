package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.TigerCloneEntity;
import com.example.examplemod.magic.TigerPowerMagic;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TigerTalismanItem extends Item {
    private static final double FUSION_SEARCH_RADIUS = 5.0D;

    public static final int MAGIC_DURATION = -1;
    public static final int COOLDOWN_TICKS = 20;

    public TigerTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }

        if (player.hasEffect(ChenMod.TIGER_POWER.getHolder().orElseThrow())) {
            TigerCloneEntity clone = findNearbyOwnedClone(level, player);
            if (clone != null) {
                mergeBack(level, player, usedHand, itemStack, clone);
            } else {
                player.displayClientMessage(Component.translatable("message.chen_mod.tiger_clone_too_far"), true);
            }

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
        }

        splitTalisman(level, player, usedHand, itemStack);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    static void mergeBack(Level level, Player player, InteractionHand usedHand, ItemStack currentStack, TigerCloneEntity clone) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.POOF, clone.getX(), clone.getY() + 1.0D, clone.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
            serverLevel.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.1D);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);

        clone.discard();
        player.removeEffect(ChenMod.TIGER_POWER.getHolder().orElseThrow());
        player.setItemInHand(usedHand, TigerTalismanHalfItem.restoreFullTalisman(currentStack));
        player.displayClientMessage(Component.translatable("message.chen_mod.tiger_merge"), true);
    }

    private void splitTalisman(Level level, Player player, InteractionHand usedHand, ItemStack itemStack) {
        TigerPowerMagic.grantTigerPower(player, MAGIC_DURATION);

        TigerCloneEntity clone = new TigerCloneEntity(ChenMod.TIGER_CLONE.get(), level);
        clone.setPos(player.getX(), player.getY(), player.getZ());
        clone.setOwnerUUID(player.getUUID());
        clone.setCustomName(player.getName());
        clone.setCustomNameVisible(true);

        if (clone.getAttribute(Attributes.ATTACK_DAMAGE) != null && player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            clone.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        if (clone.getAttribute(Attributes.MAX_HEALTH) != null && player.getAttribute(Attributes.MAX_HEALTH) != null) {
            clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getAttributeValue(Attributes.MAX_HEALTH));
            clone.setHealth(player.getHealth());
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                clone.setItemSlot(slot, stack.copy());
                clone.setDropChance(slot, 0.0F);
            }
        }

        ItemStack playerHalf = TigerTalismanHalfItem.createLinkedHalf(
                itemStack,
                ChenMod.TIGER_TALISMAN_LEFT_HALF.get(),
                clone,
                player.getUUID()
        );
        ItemStack cloneHalf = TigerTalismanHalfItem.createLinkedHalf(
                itemStack,
                ChenMod.TIGER_TALISMAN_RIGHT_HALF.get(),
                clone,
                player.getUUID()
        );

        player.setItemInHand(usedHand, playerHalf);
        EquipmentSlot handSlot = usedHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        clone.setItemSlot(handSlot, cloneHalf);
        clone.setDropChance(handSlot, 0.0F);

        level.addFreshEntity(clone);
        TigerPowerMagic.trackClone(clone);
        player.displayClientMessage(Component.translatable("message.chen_mod.tiger_split"), true);
    }

    private TigerCloneEntity findNearbyOwnedClone(Level level, Player player) {
        AABB searchArea = player.getBoundingBox().inflate(FUSION_SEARCH_RADIUS);
        List<TigerCloneEntity> clones = level.getEntitiesOfClass(
                TigerCloneEntity.class,
                searchArea,
                entity -> player.getUUID().equals(entity.getOwnerUUID())
        );
        return clones.isEmpty() ? null : clones.get(0);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.tiger_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
