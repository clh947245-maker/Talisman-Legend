package com.example.examplemod.client.gui;

import com.example.examplemod.magic.transformation.ITransformation;
import com.example.examplemod.magic.transformation.TransformationManager;
import com.example.examplemod.network.packet.TransformationSelectionPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class MonkeyRadialMenu extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<Integer, LivingEntity> dummyEntities = new HashMap<>();
    private int selectedIndex = 0;
    private float openingProgress = 0.0f;
    private float entityRotation = 0.0f;
    
    // Grid layout constants
    private static final int GRID_START_X = 20;
    private static final int GRID_START_Y = 25;
    private static final int GRID_ITEM_WIDTH = 36;
    private static final int GRID_ITEM_HEIGHT = 45;
    
    private int scrollOffset = 0;

    private double lastMouseX = 0;
    private double lastMouseY = 0;

    public MonkeyRadialMenu() {
        super(Component.literal("Monkey Transformation Selection"));
    }

    @Override
    protected void init() {
        super.init();
        openingProgress = 0.0f;
        // Reset selection to valid range if needed
        int count = TransformationManager.getTransformationCount();
        LOGGER.info("MonkeyRadialMenu initialized with {} transformations", count);
        
        if (selectedIndex >= count) selectedIndex = 0;
        if (selectedIndex < 0) selectedIndex = 0;
        
        scrollOffset = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // Opening animation
        openingProgress = Mth.lerp(0.2f, openingProgress, 1.0f);
        entityRotation += 2.0f; // Continuous rotation

        // Check if Tab is released
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_TAB) == GLFW.GLFW_RELEASE) {
            onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int count = TransformationManager.getTransformationCount();
        if (count == 0) return false;

        if (scrollY < 0) {
            // Scroll down -> Next
            selectedIndex = (selectedIndex + 1) % count;
        } else if (scrollY > 0) {
            // Scroll up -> Previous
            selectedIndex = (selectedIndex - 1 + count) % count;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int count = TransformationManager.getTransformationCount();
        
        // Ensure valid selection
        if (count > 0) {
            if (selectedIndex >= count) selectedIndex = 0;
            if (selectedIndex < 0) selectedIndex = 0;
        } else {
            selectedIndex = -1;
        }

        // Calculate grid layout
        int maxPerRow = (this.width - 2 * GRID_START_X) / GRID_ITEM_WIDTH;
        if (maxPerRow < 1) maxPerRow = 1;

        // Auto-scroll logic
        int selectedRow = selectedIndex / maxPerRow;
        int selectionY = GRID_START_Y + selectedRow * GRID_ITEM_HEIGHT;
        int visibleHeight = this.height - 60; // Leave space for text
        
        // Target scroll to keep selection visible
        int targetScroll = scrollOffset;
        
        // If selection is below visible area
        if (selectionY + GRID_ITEM_HEIGHT + scrollOffset > visibleHeight) {
            targetScroll = visibleHeight - (selectionY + GRID_ITEM_HEIGHT);
        }
        // If selection is above visible area
        else if (selectionY + scrollOffset < GRID_START_Y) {
            targetScroll = GRID_START_Y - selectionY;
        }
        
        // Clamp scroll: don't scroll past top
        if (targetScroll > 0) targetScroll = 0;
        
        scrollOffset = targetScroll;

        // Check for mouse hover if mouse moved
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            for (int i = 0; i < count; i++) {
                int row = i / maxPerRow;
                int col = i % maxPerRow;

                int x = GRID_START_X + col * GRID_ITEM_WIDTH;
                int y = GRID_START_Y + row * GRID_ITEM_HEIGHT + scrollOffset;

                if (mouseX >= x && mouseX < x + GRID_ITEM_WIDTH &&
                    mouseY >= y && mouseY < y + GRID_ITEM_HEIGHT) {
                    selectedIndex = i;
                    break;
                }
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        // Render icons (First pass: non-selected)
        for (int i = 0; i < count; i++) {
            if (i == selectedIndex) continue;
            renderGridItem(guiGraphics, i, maxPerRow, false, partialTick, scrollOffset);
        }

        // Render selected icon (Second pass: selected, on top)
        if (selectedIndex >= 0 && selectedIndex < count) {
            renderGridItem(guiGraphics, selectedIndex, maxPerRow, true, partialTick, scrollOffset);
        }

        // Render center info (Bottom middle)
        if (selectedIndex >= 0 && selectedIndex < count) {
            ITransformation transformation = TransformationManager.getTransformation(selectedIndex);
            if (transformation != null) {
                Component name = Component.translatable("transformation.chen_mod." + transformation.getId());
                int centerX = this.width / 2;
                int textY = this.height - 30; // Adjusted for new layout
                
                int textWidth = this.font.width(name);
                // Background for text
                guiGraphics.fill(centerX - textWidth / 2 - 5, textY - 6, centerX + textWidth / 2 + 5, textY + 6, 0x80000000);
                guiGraphics.drawCenteredString(this.font, name, centerX, textY - 4, 0xFFD700); // Gold text
            }
        }
    }

    private void renderGridItem(GuiGraphics guiGraphics, int index, int maxPerRow, boolean isSelected, float partialTick, int yOffset) {
        int row = index / maxPerRow;
        int col = index % maxPerRow;

        int x = GRID_START_X + col * GRID_ITEM_WIDTH + GRID_ITEM_WIDTH / 2;
        int y = GRID_START_Y + row * GRID_ITEM_HEIGHT + GRID_ITEM_HEIGHT / 2 + yOffset;

        // Optimization: Don't render if out of screen
        if (y + GRID_ITEM_HEIGHT/2 < 0 || y - GRID_ITEM_HEIGHT/2 > this.height) return;

        // Scale: Normal = 15, Selected = 25 (Reduced further)
        int scale = isSelected ? 25 : 15;
        
        renderEntity(guiGraphics, x, y, scale, index, isSelected, partialTick);
    }

    private void renderEntity(GuiGraphics guiGraphics, int x, int y, int scale, int id, boolean isSelected, float partialTick) {
        LivingEntity entity = getDummyEntity(id);
        if (entity != null) {
            // Rotation logic
            float rotationOffset = 150.0f; // Base rotation to face roughly towards camera/center
            if (isSelected) {
                // Spin slowly when selected
                entity.yBodyRot = (entityRotation + partialTick) * 2;
                entity.setYRot(entity.yBodyRot);
                entity.yHeadRot = entity.getYRot();
                entity.yHeadRotO = entity.getYRot();
            } else {
                 entity.yBodyRot = rotationOffset;
                 entity.setYRot(rotationOffset);
                 entity.yHeadRot = entity.getYRot();
                 entity.yHeadRotO = entity.getYRot();
            }

            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf cameraOrientation = new Quaternionf().rotateX((float) (-15 * Math.PI / 180));
            pose.mul(cameraOrientation);

            InventoryScreen.renderEntityInInventory(guiGraphics, x, y, scale, new Vector3f(), pose, null, entity);
        }
    }

    private LivingEntity getDummyEntity(int id) {
        if (dummyEntities.containsKey(id)) {
            return dummyEntities.get(id);
        }

        ITransformation transformation = TransformationManager.getTransformation(id);
        if (transformation != null) {
            EntityType<? extends LivingEntity> type = transformation.getEntityType();
            if (type == EntityType.PLAYER) {
                 if (Minecraft.getInstance().level != null) {
                    // Use NIL_UUID for consistent Steve appearance
                    LivingEntity player = new net.minecraft.client.player.RemotePlayer(Minecraft.getInstance().level, new com.mojang.authlib.GameProfile(net.minecraft.Util.NIL_UUID, " "));
                    player.setCustomNameVisible(false);
                    dummyEntities.put(id, player);
                    return player;
                }
                return Minecraft.getInstance().player;
            }
            if (type != null) {
                LivingEntity entity = type.create(Minecraft.getInstance().level);
                if (entity != null) {
                    dummyEntities.put(id, entity);
                    return entity;
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int count = TransformationManager.getTransformationCount();
            if (count > 0) {
                int maxPerRow = (this.width - 2 * GRID_START_X) / GRID_ITEM_WIDTH;
                if (maxPerRow < 1) maxPerRow = 1;

                for (int i = 0; i < count; i++) {
                    int row = i / maxPerRow;
                    int col = i % maxPerRow;

                    int x = GRID_START_X + col * GRID_ITEM_WIDTH;
                    int y = GRID_START_Y + row * GRID_ITEM_HEIGHT + scrollOffset;

                    if (mouseX >= x && mouseX < x + GRID_ITEM_WIDTH &&
                        mouseY >= y && mouseY < y + GRID_ITEM_HEIGHT) {
                        selectedIndex = i;
                        onClose();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (selectedIndex >= 0) {
            PacketDistributor.sendToServer(new TransformationSelectionPayload(selectedIndex));
        }
        super.onClose();
    }
}
