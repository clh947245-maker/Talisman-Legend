package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.item.PufferfishWeaponItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ChenMod.MODID)
public final class PufferfishWeaponEventHandler {

    private PufferfishWeaponEventHandler() {
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!PufferfishWeaponItem.isHoldingPufferfishWeapon(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        PufferfishWeaponItem.fireFromMainHand(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!PufferfishWeaponItem.isHoldingPufferfishWeapon(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        PufferfishWeaponItem.fireFromMainHand(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        PufferfishWeaponItem.serverTickSenseMode(event.getEntity());
    }
}
