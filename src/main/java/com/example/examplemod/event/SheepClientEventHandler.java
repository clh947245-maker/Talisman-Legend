package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.lang.reflect.Field;

/**
 * 羊符咒客户端事件处理器 — NeoForge 1.21
 */
@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class SheepClientEventHandler {

    // ── 反射访问 shadowRadius ─────────────────────────────────────────────────────
    private static final Field SHADOW_RADIUS_FIELD;
    static {
        Field f = null;
        try {
            f = EntityRenderer.class.getDeclaredField("shadowRadius");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ChenMod.LOGGER.error("[SheepClientEventHandler] 无法找到 shadowRadius 字段", e);
        }
        SHADOW_RADIUS_FIELD = f;
    }

    private static void setShadowRadius(EntityRenderer<?> renderer, float value) {
        if (SHADOW_RADIUS_FIELD == null) return;
        try {
            SHADOW_RADIUS_FIELD.setFloat(renderer, value);
        } catch (IllegalAccessException e) {
            ChenMod.LOGGER.error("[SheepClientEventHandler] 无法设置 shadowRadius", e);
        }
    }

    // ── HUD 隐藏（仅保留准星，其余全部取消） ─────────────────────────────────────
    /**
     * 白名单逻辑：只有 CROSSHAIR 允许渲染，其他所有 layer 一律取消。
     * 比枚举黑名单更健壮，不会因新 layer 漏网。
     */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ChenMod.SHEEP_POWER)) return;

        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    // ── 玩家模型 + 影子隐藏 ───────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!event.getEntity().hasEffect(ChenMod.SHEEP_POWER)) return;
        setShadowRadius(event.getRenderer(), 0.0f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!event.getEntity().hasEffect(ChenMod.SHEEP_POWER)) return;
        // 恢复默认玩家影子半径（原版固定为 0.5f）
        setShadowRadius(event.getRenderer(), 0.5f);
    }

    // ── 手持物品 + 手臂隐藏（第一人称） ──────────────────────────────────────────
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.hasEffect(ChenMod.SHEEP_POWER)) return;
        event.setCanceled(true);
    }

    // ── 空手时手臂皮肤隐藏 ────────────────────────────────────────────────────────
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(ChenMod.SHEEP_POWER)) {
            event.setCanceled(true);
        }
    }
}