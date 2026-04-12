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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.UUID;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class SheepClientEventHandler {
    private static final int SUICIDE_KEY = GLFW.GLFW_KEY_K;
    private static final int SUICIDE_BOX_WIDTH = 82;
    private static final int SUICIDE_BOX_HEIGHT = 18;
    private static final int SUICIDE_BOX_MARGIN = 12;

    private static boolean wasJumpKeyDown;
    private static boolean wasSuicideKeyDown;
    private static boolean wasInSoulState;
    private static boolean renderingDisguisePlayer;
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
        return player != null && player.hasEffect(ChenMod.SHEEP_POWER);
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

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (!isSoulState(player)) {
            return;
        }

        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (!isSoulState(player)) {
            return;
        }
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            return;
        }

        Component tracker = buildBodyTracker(player);
        Component status = buildBodyStatus();
        int trackerX = (minecraft.getWindow().getGuiScaledWidth() - minecraft.font.width(tracker)) / 2;
        int trackerY = 12;
        event.getGuiGraphics().drawString(minecraft.font, tracker, trackerX, trackerY, 0xB9F2FF, true);
        int statusX = (minecraft.getWindow().getGuiScaledWidth() - minecraft.font.width(status)) / 2;
        int statusY = trackerY + 10;
        int statusColor = SheepBodyTrackerState.hasBody()
                ? (SheepBodyTrackerState.isAlive() ? 0xFFAEEA94 : 0xFFFF8A8A)
                : 0xFFE5D7A3;
        event.getGuiGraphics().drawString(minecraft.font, status, statusX, statusY, statusColor, true);

        Component suicideButton = Component.translatable("message.chen_mod.sheep_suicide_button", "K");
        int boxRight = minecraft.getWindow().getGuiScaledWidth() - SUICIDE_BOX_MARGIN;
        int boxLeft = boxRight - SUICIDE_BOX_WIDTH;
        int boxTop = SUICIDE_BOX_MARGIN;
        int boxBottom = boxTop + SUICIDE_BOX_HEIGHT;
        event.getGuiGraphics().fill(boxLeft, boxTop, boxRight, boxBottom, 0xA0202020);
        event.getGuiGraphics().fill(boxLeft, boxTop, boxRight, boxTop + 1, 0xFFE57A7A);
        int buttonTextX = boxLeft + (SUICIDE_BOX_WIDTH - minecraft.font.width(suicideButton)) / 2;
        int buttonTextY = boxTop + (SUICIDE_BOX_HEIGHT - 8) / 2;
        event.getGuiGraphics().drawString(minecraft.font, suicideButton, buttonTextX, buttonTextY, 0xFFF4D0D0, false);

        if (!SheepPowerMagic.isNearReturnableBody(player)) {
            return;
        }

        Component prompt = Component.translatable(
                "message.chen_mod.sheep_return_prompt",
                minecraft.options.keyJump.getTranslatedKeyMessage()
        );
        int x = (minecraft.getWindow().getGuiScaledWidth() - minecraft.font.width(prompt)) / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() / 2 + 16;
        event.getGuiGraphics().drawString(minecraft.font, prompt, x, y, 0xFFFFFF, true);
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!event.getEntity().hasEffect(ChenMod.SHEEP_POWER)) {
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
        if (!event.getEntity().hasEffect(ChenMod.SHEEP_POWER)) {
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

        if (matchesBlockedSoulShortcut(event, minecraft)) {
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
        boolean jumpKeyDown = minecraft.options.keyJump.isDown();
        boolean suicideKeyDown = InputConstants.isKeyDown(window, SUICIDE_KEY);

        if (!isSoulState(player)) {
            if (wasInSoulState) {
                SheepBodyTrackerState.clear();
            }
            wasJumpKeyDown = jumpKeyDown;
            wasSuicideKeyDown = suicideKeyDown;
            wasInSoulState = false;
            return;
        }

        if (!wasInSoulState) {
            SheepBodyTrackerState.clear();
        }

        if (minecraft.screen != null && !(minecraft.screen instanceof PauseScreen)) {
            if (minecraft.screen instanceof AbstractContainerScreen<?>) {
                player.closeContainer();
            }
            minecraft.setScreen(null);
        }

        suppressSoulShortcuts(minecraft);

        if (minecraft.screen == null && SheepPowerMagic.isNearReturnableBody(player) && jumpKeyDown && !wasJumpKeyDown) {
            PacketDistributor.sendToServer(new SheepReturnPayload());
        }

        if (minecraft.screen == null && suicideKeyDown && !wasSuicideKeyDown) {
            PacketDistributor.sendToServer(new SheepSuicidePayload());
        }

        wasJumpKeyDown = jumpKeyDown;
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
