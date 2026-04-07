package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.SheepBodyEntity;
import com.example.examplemod.magic.SheepPowerMagic;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInSoulState(player)) return;
        if (event.getLookingEntity() == player) return;
        if (event.getLookingEntity() instanceof Mob mob) {
            SheepBodyEntity body = SheepPowerMagic.getTrackedBody(player);
            if (body != null && body.isAlive() && body.level() == mob.level()) return;
        }
        event.modifyVisibility(0.0);
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) return;
        if (!isInSoulState(player)) return;

        SheepBodyEntity body = SheepPowerMagic.getTrackedBody(player);
        if (body == null || !body.isAlive() || body.level() != event.getEntity().level()) return;
        if (!event.getEntity().canAttack(body) || event.getEntity().isAlliedTo(body)) return;

        event.setNewAboutToBeSetTarget(body);
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
    public static void onItemEntityPickup(ItemEntityPickupEvent.Pre event) {
        if (isInSoulState(event.getPlayer())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (!isInSoulState(player)) {
            return;
        }

        if (!event.getEntity().getItem().isEmpty()) {
            player.getInventory().placeItemBackInInventory(event.getEntity().getItem().copy());
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        }
        event.setCanceled(true);
    }

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
        if (SheepPowerMagic.consumeSkipRestore(player)) return;
        SheepPowerMagic.restorePlayer(player);
    }

    // ── 效果自然到期时恢复状态 ────────────────────────────────────────────
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null) return;
        if (!event.getEffectInstance().is(ChenMod.SHEEP_POWER)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (SheepPowerMagic.consumeSkipRestore(player)) return;
        SheepPowerMagic.restorePlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        var disguiseName = SheepPowerMagic.getDisguiseDisplayName(event.getEntity().getUUID());
        if (disguiseName != null) {
            event.setDisplayname(disguiseName.copy());
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        var disguiseName = SheepPowerMagic.getDisguiseDisplayName(event.getEntity().getUUID());
        if (disguiseName != null) {
            event.setDisplayName(disguiseName.copy());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SheepPowerMagic.syncAllDisguiseIdentitiesTo(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getTarget() instanceof Player targetPlayer) {
            SheepPowerMagic.syncDisguiseIdentityTo(serverPlayer, targetPlayer);
        }
    }
}
