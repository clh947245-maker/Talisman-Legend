package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.client.renderer.SheepDisguisePlayerRenderer;
import com.example.examplemod.magic.SheepPowerMagic;
import com.example.examplemod.network.SheepDisguiseState;
import com.example.examplemod.network.SheepBodyTrackerState;
import com.example.examplemod.network.packet.SheepReturnPayload;
import com.example.examplemod.network.packet.SheepSuicidePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.example.examplemod.network.ModNetwork;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.UUID;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class SheepClientEventHandler {
    private static final int RETURN_KEY = GLFW.GLFW_KEY_H;
    private static final int SUICIDE_KEY = GLFW.GLFW_KEY_K;
    private static final int SUICIDE_BOX_WIDTH = 82;
    private static final int SUICIDE_BOX_HEIGHT = 18;
    private static final int SUICIDE_BOX_MARGIN = 12;
    private static final int HUD_BACKGROUND = 0xAA101820;
    private static final int HUD_PANEL_BORDER = 0x66B9F2FF;
    private static final int HUD_TEXT = 0xFFEAF8FF;
    private static final int HUD_ACCENT = 0xFF80D8FF;
    private static final int HUD_PROMPT = 0xFFFFF0A8;
    private static final int HUD_ALIVE = 0xFF9BE27A;
    private static final int HUD_DEAD = 0xFFFF7F7F;
    private static final int HUD_MISSING = 0xFFE7D28E;

    private static boolean wasReturnKeyDown;
    private static boolean wasSuicideKeyDown;
    private static boolean wasInSoulState;
    private static boolean renderingDisguisePlayer;
    private static int lastHudRenderTick = -100;
    private static int lastFallbackMessageTick = -100;
    private static SheepDisguisePlayerRenderer wideDisguiseRenderer;
    private static SheepDisguisePlayerRenderer slimDisguiseRenderer;

    private static final Field SHADOW_RADIUS_FIELD;

    static {
        Field f = null;
        try {
            f = EntityRenderer.class.getDeclaredField("shadowRadius");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ChenMod.LOGGER.error("[SheepClientEventHandler] Unable to access shadowRadius", e);
        }
        SHADOW_RADIUS_FIELD = f;
    }

    private static boolean isSoulState(Player player) {
        return player != null && player.hasEffect(ChenMod.SHEEP_POWER.getHolder().orElseThrow());
    }

    private static boolean isAllowedSoulKey(KeyMapping keyMapping, Minecraft minecraft) {
        return keyMapping == minecraft.options.keyUp
                || keyMapping == minecraft.options.keyDown
                || keyMapping == minecraft.options.keyLeft
                || keyMapping == minecraft.options.keyRight
                || keyMapping == minecraft.options.keyJump
                || keyMapping == minecraft.options.keyShift
                || keyMapping == minecraft.options.keySprint;
    }

    private static void suppressSoulShortcuts(Minecraft minecraft) {
        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            if (isAllowedSoulKey(keyMapping, minecraft)) {
                continue;
            }

            keyMapping.setDown(false);
            while (keyMapping.consumeClick()) {
                // Drain queued shortcut presses so held hotkeys do not leak through on later ticks.
            }
        }
    }

    private static boolean matchesBlockedSoulShortcut(InputEvent.Key event, Minecraft minecraft) {
        for (KeyMapping keyMapping : minecraft.options.keyMappings) {
            if (isAllowedSoulKey(keyMapping, minecraft)) {
                continue;
            }

            if (keyMapping.matches(event.getKey(), event.getScanCode())) {
                return true;
            }
        }

        return false;
    }

    public static void initDisguiseRenderers(EntityRendererProvider.Context context) {
        if (wideDisguiseRenderer == null) {
            wideDisguiseRenderer = new SheepDisguisePlayerRenderer(context, false);
        }
        if (slimDisguiseRenderer == null) {
            slimDisguiseRenderer = new SheepDisguisePlayerRenderer(context, true);
        }
    }

    private static void setShadowRadius(EntityRenderer<?> renderer, float value) {
        if (SHADOW_RADIUS_FIELD == null) {
            return;
        }
        try {
            SHADOW_RADIUS_FIELD.setFloat(renderer, value);
        } catch (IllegalAccessException e) {
            ChenMod.LOGGER.error("[SheepClientEventHandler] Unable to set shadowRadius", e);
        }
    }

    private static String getDirectionKey(double dx, double dz) {
        if (Math.abs(dx) < 1.0D && Math.abs(dz) < 1.0D) {
            return "direction.chen_mod.here";
        }

        double angle = Math.toDegrees(Math.atan2(dz, dx));
        int index = Mth.floor((angle + 22.5D) / 45.0D) & 7;
        return switch (index) {
            case 0 -> "direction.chen_mod.east";
            case 1 -> "direction.chen_mod.south_east";
            case 2 -> "direction.chen_mod.south";
            case 3 -> "direction.chen_mod.south_west";
            case 4 -> "direction.chen_mod.west";
            case 5 -> "direction.chen_mod.north_west";
            case 6 -> "direction.chen_mod.north";
            default -> "direction.chen_mod.north_east";
        };
    }

    private static Component buildBodyTracker(Player player) {
        if (!SheepBodyTrackerState.hasBody()) {
            return Component.translatable("message.chen_mod.sheep_body_tracker_missing");
        }

        int bodyX = Mth.floor(SheepBodyTrackerState.x());
        int bodyY = Mth.floor(SheepBodyTrackerState.y());
        int bodyZ = Mth.floor(SheepBodyTrackerState.z());

        ResourceLocation syncedDimension = ResourceLocation.tryParse(SheepBodyTrackerState.dimension());
        if (syncedDimension == null || !player.level().dimension().location().equals(syncedDimension)) {
            return Component.translatable(
                    "message.chen_mod.sheep_body_tracker_other_dimension",
                    bodyX,
                    bodyY,
                    bodyZ
            );
        }

        double dx = SheepBodyTrackerState.x() - player.getX();
        double dz = SheepBodyTrackerState.z() - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        return Component.translatable(
                "message.chen_mod.sheep_body_tracker",
                Component.translatable(getDirectionKey(dx, dz)),
                String.format(Locale.ROOT, "%.1f", distance),
                bodyX,
                bodyY,
                bodyZ
        );
    }

    private static Component buildBodyStatus() {
        String statusKey;
        if (!SheepBodyTrackerState.hasBody()) {
            statusKey = "message.chen_mod.sheep_body_status_missing";
        } else if (SheepBodyTrackerState.isAlive()) {
            statusKey = "message.chen_mod.sheep_body_status_alive";
        } else {
            statusKey = "message.chen_mod.sheep_body_status_dead";
        }
        return Component.translatable("message.chen_mod.sheep_body_status", Component.translatable(statusKey));
    }

    private static boolean isNearSyncedReturnBody(Player player) {
        if (player == null || !SheepBodyTrackerState.hasBody() || !SheepBodyTrackerState.isAlive()) {
            return false;
        }

        ResourceLocation syncedDimension = ResourceLocation.tryParse(SheepBodyTrackerState.dimension());
        if (syncedDimension == null || !player.level().dimension().location().equals(syncedDimension)) {
            return false;
        }

        double dx = SheepBodyTrackerState.x() - player.getX();
        double dy = SheepBodyTrackerState.y() - player.getY();
        double dz = SheepBodyTrackerState.z() - player.getZ();
        double maxDistance = SheepPowerMagic.RETURN_TRIGGER_RADIUS;
        return dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance;
    }

    private static boolean canPromptReturn(Player player) {
        return SheepPowerMagic.isNearReturnableBody(player) || isNearSyncedReturnBody(player);
    }

    public static void renderSoulHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!isSoulState(player) || minecraft.options.hideGui) {
            return;
        }
        lastHudRenderTick = player.tickCount;

        Font font = minecraft.font;
        Component title = Component.translatable("effect.chen_mod.sheep_power");
        Component tracker = buildBodyTracker(player);
        Component status = buildBodyStatus();
        int statusColor = !SheepBodyTrackerState.hasBody()
                ? HUD_MISSING
                : SheepBodyTrackerState.isAlive() ? HUD_ALIVE : HUD_DEAD;

        int panelWidth = Math.max(218, Math.max(font.width(tracker), font.width(status)) + 28);
        int panelX = (minecraft.getWindow().getGuiScaledWidth() - panelWidth) / 2;
        int panelY = 12;
        int panelHeight = 48;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, HUD_BACKGROUND);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 1, HUD_PANEL_BORDER);
        graphics.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, HUD_PANEL_BORDER);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelHeight, HUD_PANEL_BORDER);
        graphics.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, HUD_PANEL_BORDER);
        graphics.fill(panelX + 8, panelY + 8, panelX + 11, panelY + panelHeight - 8, statusColor);

        graphics.drawString(font, title, panelX + 18, panelY + 7, HUD_ACCENT, false);
        graphics.drawString(font, status, panelX + 18, panelY + 21, statusColor, false);
        graphics.drawString(font, tracker, panelX + 18, panelY + 34, HUD_TEXT, false);

        Component suicideButton = Component.translatable("message.chen_mod.sheep_suicide_button", "K");
        int boxRight = minecraft.getWindow().getGuiScaledWidth() - SUICIDE_BOX_MARGIN;
        int boxLeft = boxRight - SUICIDE_BOX_WIDTH;
        int boxTop = SUICIDE_BOX_MARGIN;
        int boxBottom = boxTop + SUICIDE_BOX_HEIGHT;
        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0x99201618);
        graphics.fill(boxLeft, boxTop, boxRight, boxTop + 1, HUD_DEAD);
        graphics.drawString(
                font,
                suicideButton,
                boxLeft + (SUICIDE_BOX_WIDTH - font.width(suicideButton)) / 2,
                boxTop + (SUICIDE_BOX_HEIGHT - font.lineHeight) / 2,
                0xFFFFD6D6,
                false
        );

        if (canPromptReturn(player)) {
            Component prompt = Component.translatable("message.chen_mod.sheep_return_prompt", "H");
            int promptWidth = font.width(prompt) + 22;
            int promptX = (minecraft.getWindow().getGuiScaledWidth() - promptWidth) / 2;
            int promptY = minecraft.getWindow().getGuiScaledHeight() / 2 + 18;
            graphics.fill(promptX, promptY, promptX + promptWidth, promptY + 20, 0xB0222618);
            graphics.fill(promptX, promptY, promptX + promptWidth, promptY + 1, HUD_PROMPT);
            graphics.drawString(font, prompt, promptX + 11, promptY + 6, HUD_PROMPT, false);
        }
    }

    private static void updateFallbackSoulMessage(Player player) {
        if (player.tickCount - lastHudRenderTick <= 20 || player.tickCount - lastFallbackMessageTick < 20) {
            return;
        }
        lastFallbackMessageTick = player.tickCount;

        Component message = Component.empty()
                .append(buildBodyStatus())
                .append(" | ")
                .append(buildBodyTracker(player));
        if (canPromptReturn(player)) {
            message = message.copy()
                    .append(" | ")
                    .append(Component.translatable("message.chen_mod.sheep_return_prompt", "H"));
        }
        message = message.copy()
                .append(" | ")
                .append(Component.translatable("message.chen_mod.sheep_suicide_button", "K"));

        player.displayClientMessage(message, true);
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!event.getEntity().hasEffect(ChenMod.SHEEP_POWER.getHolder().orElseThrow())) {
            if (renderingDisguisePlayer) {
                return;
            }

            if (!(event.getEntity() instanceof AbstractClientPlayer clientPlayer)) {
                return;
            }

            UUID skinSourceUUID = SheepDisguiseState.getSkinSource(clientPlayer.getUUID());
            if (skinSourceUUID == null || wideDisguiseRenderer == null || slimDisguiseRenderer == null) {
                return;
            }

            event.setCanceled(true);
            renderingDisguisePlayer = true;
            try {
                SheepDisguisePlayerRenderer renderer = usesSlimDisguiseModel(skinSourceUUID) ? slimDisguiseRenderer : wideDisguiseRenderer;
                renderer.render(
                        clientPlayer,
                        clientPlayer.getYRot(),
                        event.getPartialTick(),
                        event.getPoseStack(),
                        event.getMultiBufferSource(),
                        event.getPackedLight()
                );
            } finally {
                renderingDisguisePlayer = false;
            }
            return;
        }
        setShadowRadius(event.getRenderer(), 0.0f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (!event.getEntity().hasEffect(ChenMod.SHEEP_POWER.getHolder().orElseThrow())) {
            return;
        }
        setShadowRadius(event.getRenderer(), 0.5f);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Player player = Minecraft.getInstance().player;
        if (!isSoulState(player)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Player player = Minecraft.getInstance().player;
        if (isSoulState(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        Player player = Minecraft.getInstance().player;
        if (!isSoulState(player)) {
            return;
        }
        if (event.getNewScreen() != null && !(event.getNewScreen() instanceof PauseScreen)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isSoulState(minecraft.player) || minecraft.screen != null) {
            return;
        }

        if (event.getKey() != RETURN_KEY && matchesBlockedSoulShortcut(event, minecraft)) {
            suppressSoulShortcuts(minecraft);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (isSoulState(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (minecraft.level == null) {
            SheepDisguiseState.clearAll();
        }
        long window = minecraft.getWindow().getWindow();
        boolean returnKeyDown = InputConstants.isKeyDown(window, RETURN_KEY);
        boolean suicideKeyDown = InputConstants.isKeyDown(window, SUICIDE_KEY);

        if (!isSoulState(player)) {
            if (wasInSoulState) {
                SheepBodyTrackerState.clear();
            }
            lastHudRenderTick = -100;
            lastFallbackMessageTick = -100;
            wasReturnKeyDown = returnKeyDown;
            wasSuicideKeyDown = suicideKeyDown;
            wasInSoulState = false;
            return;
        }

        if (!wasInSoulState) {
            SheepBodyTrackerState.clear();
        }

        // 客户端也持续清掉燃烧状态，避免岩浆/火焰接触导致本地仍显示着火特效。
        player.clearFire();

        if (minecraft.screen != null && !(minecraft.screen instanceof PauseScreen)) {
            if (minecraft.screen instanceof AbstractContainerScreen<?>) {
                player.closeContainer();
            }
            minecraft.setScreen(null);
        }

        suppressSoulShortcuts(minecraft);
        updateFallbackSoulMessage(player);

        if (minecraft.screen == null && canPromptReturn(player) && returnKeyDown && !wasReturnKeyDown) {
            ModNetwork.sendToServer(new SheepReturnPayload());
        }

        if (minecraft.screen == null && suicideKeyDown && !wasSuicideKeyDown) {
            ModNetwork.sendToServer(new SheepSuicidePayload());
        }

        wasReturnKeyDown = returnKeyDown;
        wasSuicideKeyDown = suicideKeyDown;
        wasInSoulState = true;
    }

    @SubscribeEvent
    public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        Component disguiseName = SheepDisguiseState.getDisplayName(event.getEntity().getUUID());
        if (disguiseName != null) {
            event.setDisplayname(disguiseName.copy());
        }
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Component disguiseName = SheepDisguiseState.getDisplayName(player.getUUID());
        if (disguiseName != null) {
            event.setContent(disguiseName.copy());
        }
    }

    private static boolean usesSlimDisguiseModel(UUID skinSourceUUID) {
        if (Minecraft.getInstance().getConnection() == null) {
            return false;
        }

        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(skinSourceUUID);
        if (playerInfo == null) {
            return false;
        }

        return playerInfo.getSkin().model() == PlayerSkin.Model.SLIM;
    }
}
