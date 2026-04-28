package com.example.examplemod.mixin;

import com.example.examplemod.event.SheepClientEventHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class SheepSoulHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void chenMod$renderSheepSoulHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SheepClientEventHandler.renderSoulHud(graphics);
    }
}
