package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.transformation.TransformationManager;
import com.example.examplemod.network.packet.TransformationSelectionPayload;
import com.example.examplemod.talisman.MonkeyTalismanItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.InputEvent;
import com.example.examplemod.network.ModNetwork;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class MonkeyClientEventHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Check if TAB is held down
        long window = mc.getWindow().getWindow();
        boolean isTabDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_TAB);

        if (isTabDown) {
            Player player = mc.player;
            // Check Main Hand
            ItemStack stack = player.getMainHandItem();
            boolean isMonkeyTalisman = stack.getItem() == ChenMod.MONKEY_TALISMAN.get();
            
            // Check Off Hand if not in Main Hand
            if (!isMonkeyTalisman) {
                stack = player.getOffhandItem();
                isMonkeyTalisman = stack.getItem() == ChenMod.MONKEY_TALISMAN.get();
            }

            if (isMonkeyTalisman) {
                // Cancel original scroll event (e.g. hotbar switching)
                event.setCanceled(true);

                // Calculate direction
                // Actually, scrollY > 0 is UP. Let's make UP -> Next (+1), DOWN -> Prev (-1)
                // Adjust based on preference. Standard hotbar: Scroll DOWN -> Next slot (right), Scroll UP -> Prev slot (left)
                int change = (event.getDeltaY() > 0) ? -1 : 1; 

                // Get current ID
                int currentId = MonkeyTalismanItem.getSelectedTransformation(stack);
                int count = TransformationManager.getTransformationCount();
                
                // Calculate new ID
                int newId = (currentId + change) % count;
                if (newId < 0) newId += count;

                // Send Packet
                ModNetwork.sendToServer(new TransformationSelectionPayload(newId));
                
                // Update Client Side Item NBT (Optimistic update for immediate feedback, though packet handles server side)
                MonkeyTalismanItem.setSelectedTransformation(stack, newId);

                // Show Toast/Overlay Feedback
                String transformationId = TransformationManager.getTransformation(newId).getId();
                player.displayClientMessage(Component.translatable("item.chen_mod.monkey_talisman.selected", Component.translatable("transformation.chen_mod." + transformationId)), true);
            }
        }
    }
}
