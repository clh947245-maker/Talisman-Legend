package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.item.PufferfishWeaponItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;

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
        PufferfishWeaponItem.serverTickSenseMode(event.player);
    }
}
