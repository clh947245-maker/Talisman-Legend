package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.item.OniMaskItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GrindstoneEvent;

@EventBusSubscriber(modid = ChenMod.MODID)
public final class MagicBindingEventHandler {
    private MagicBindingEventHandler() {
    }

    @SubscribeEvent
    public static void onGrindstonePrepare(GrindstoneEvent.OnPlaceItem event) {
        if (containsMagicBoundMask(event.getTopItem()) || containsMagicBoundMask(event.getBottomItem())) {
            event.setCanceled(true);
            event.setXp(0);
            event.setOutput(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack equippedHead = event.getEntity().getItemBySlot(EquipmentSlot.HEAD);
        if (!containsMagicBoundMask(equippedHead) || OniMaskItem.canRemove(event.getEntity(), equippedHead)) {
            return;
        }

        ItemStack heldStack = event.getItemStack();
        if (heldStack.getItem() instanceof ArmorItem armorItem && armorItem.getEquipmentSlot() == EquipmentSlot.HEAD) {
            event.setCanceled(true);
        }
    }

    private static boolean containsMagicBoundMask(ItemStack stack) {
        return OniMaskItem.isOniMask(stack) && OniMaskItem.hasMagicBinding(stack);
    }
}
