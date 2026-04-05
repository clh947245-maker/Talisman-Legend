package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.SheepPowerMagic;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 灵魂状态事件监听器（NeoForge 1.21）
 */
@EventBusSubscriber(modid = ChenMod.MODID)
public class SheepPowerEventHandler {

    private static boolean isInSoulState(Player player) {
        return player != null && player.hasEffect(ChenMod.SHEEP_POWER);
    }

    // ── 隐身：所有生物对玩家本体的可见性清零 ────────────────────────────
    @SubscribeEvent
    public static void onLivingVisibility(LivingVisibilityEvent event) {
        if (!(event.getLookingEntity() instanceof Player player)) return;
        if (!isInSoulState(player)) return;
        event.modifyVisibility(0.0);
    }

    // ── 禁止使用物品 ──────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isInSoulState(event.getEntity())) event.setCanceled(true);
    }

    // ── 禁止与方块交互（右键）────────────────────────────────────────────
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isInSoulState(event.getEntity())) event.setCanceled(true);
    }

    // ── 禁止挖掘方块（左键）──────────────────────────────────────────────
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isInSoulState(event.getEntity())) event.setCanceled(true);
    }

    // ── 禁止与实体交互（右键：骑乘、交易等）─────────────────────────────
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isInSoulState(event.getEntity())) event.setCanceled(true);
    }

    // ── 禁止精确与实体交互（右键特定部位）───────────────────────────────
    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isInSoulState(event.getEntity())) event.setCanceled(true);
    }

    // ── 禁止攻击实体 ──────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isInSoulState(event.getEntity())) event.setCanceled(true);
    }

    // ── 禁止打开物品栏 ────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onOpenContainer(PlayerContainerEvent.Open event) {
        if (isInSoulState(event.getEntity())) {
            event.getEntity().closeContainer();
        }
    }

    // ── 效果被主动移除时恢复状态（喝牛奶、/effect clear 等）─────────────
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) return;
        if (!event.getEffectInstance().is(ChenMod.SHEEP_POWER)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        SheepPowerMagic.restorePlayer(player);
    }

    // ── 效果自然到期时恢复状态 ────────────────────────────────────────────
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (!event.getEffectInstance().is(ChenMod.SHEEP_POWER)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        SheepPowerMagic.restorePlayer(player);
    }
}
