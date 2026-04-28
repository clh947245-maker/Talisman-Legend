package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.client.gui.MonkeyRadialMenu;
import com.example.examplemod.talisman.MonkeyTalismanItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class MonkeyInputEventHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Check for Tab key press (Player List key)
        if (event.getKey() == GLFW.GLFW_KEY_TAB && event.getAction() == GLFW.GLFW_PRESS) {
            // Check if holding Monkey Talisman
            ItemStack mainHand = mc.player.getMainHandItem();
            ItemStack offHand = mc.player.getOffhandItem();
            
            boolean holdingTalisman = mainHand.getItem() instanceof MonkeyTalismanItem || 
                                      offHand.getItem() instanceof MonkeyTalismanItem;

            if (holdingTalisman) {
                // Open Radial Menu
                // Only open if no other screen is open (to avoid interrupting chat or inventory)
                if (mc.screen == null) {
                    mc.setScreen(new MonkeyRadialMenu());
                }
            }
        }
    }
}
