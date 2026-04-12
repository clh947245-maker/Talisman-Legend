package com.example.examplemod.item;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.SheepBodyEntity;
import com.example.examplemod.magic.SheepPowerMagic;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class OniMaskItem extends ArmorItem {
    public OniMaskItem(Holder<ArmorMaterial> material) {
        super(material, Type.HELMET, new Item.Properties().stacksTo(1).setNoRepair());
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        ensureMagicBinding(stack, level);
        super.onCraftedPostProcess(stack, level);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide) {
            ensureMagicBinding(stack, level);
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.BINDING_CURSE)) {
            return false;
        }
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(this.getDescriptionId() + ".desc"));
    }

    public static boolean isOniMask(ItemStack stack) {
        return stack.getItem() instanceof OniMaskItem;
    }

    private static boolean hasEquippedOniMask(@Nullable LivingEntity entity) {
        return entity != null && isOniMask(entity.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static boolean isWearingOniMask(@Nullable LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player player && player.hasEffect(ChenMod.SHEEP_POWER)) {
            return false;
        }

        return hasEquippedOniMask(entity);
    }

    @Nullable
    public static LivingEntity getMaskAnchor(@Nullable LivingEntity entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            SheepBodyEntity body = SheepPowerMagic.getTrackedBody(player);
            if (player.hasEffect(ChenMod.SHEEP_POWER)) {
                return body != null && body.isAlive() && hasEquippedOniMask(body) ? body : null;
            }

            if (body != null && body.isAlive() && hasEquippedOniMask(body)) {
                return body;
            }
        }

        return hasEquippedOniMask(entity) ? entity : null;
    }

    private static void ensureMagicBinding(ItemStack stack, Level level) {
        if (stack.isEmpty() || !isOniMask(stack) || hasMagicBinding(stack)) {
            return;
        }

        Holder<Enchantment> magicBinding = level.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(ChenMod.MAGIC_BINDING_ENCHANTMENT);
        stack.enchant(magicBinding, 1);
    }

    public static boolean hasMagicBinding(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (Holder<Enchantment> enchantment : stack.getEnchantments().keySet()) {
            if (enchantment.is(ChenMod.MAGIC_BINDING_ENCHANTMENT)) {
                return true;
            }
        }

        return false;
    }

    public static boolean canRemove(@Nullable Entity entity, ItemStack stack) {
        return !isOniMask(stack) || MagicBindingState.hasRemovalAccess(entity);
    }
}
