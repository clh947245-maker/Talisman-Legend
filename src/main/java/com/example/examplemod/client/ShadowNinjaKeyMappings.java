package com.example.examplemod.client;

import com.example.examplemod.ChenMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ShadowNinjaKeyMappings {
    public static final String CATEGORY = "key.categories." + ChenMod.MODID;
    public static final KeyMapping SUMMON = new KeyMapping(
            "key." + ChenMod.MODID + ".shadow_ninja_summon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );
    public static final KeyMapping DISMISS = new KeyMapping(
            "key." + ChenMod.MODID + ".shadow_ninja_dismiss",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );
    public static final KeyMapping KNEEL = new KeyMapping(
            "key." + ChenMod.MODID + ".shadow_ninja_kneel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    private ShadowNinjaKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SUMMON);
        event.register(DISMISS);
        event.register(KNEEL);
    }
}
