package com.example.examplemod.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {
    @Shadow
    private void openWorldLoadBundledResourcePack(
        LevelStorageSource.LevelStorageAccess levelStorageAccess,
        WorldStem worldStem,
        PackRepository packRepository,
        Runnable callback
    ) {
    }

    @Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true)
    private static void chenMod$skipModdedWorldgenCreationWarning(
        Minecraft minecraft,
        CreateWorldScreen screen,
        Lifecycle lifecycle,
        Runnable confirmedAction,
        boolean skipWarning,
        CallbackInfo ci
    ) {
        if (!skipWarning && lifecycle == Lifecycle.experimental()) {
            confirmedAction.run();
            ci.cancel();
        }
    }

    @Inject(method = "openWorldCheckWorldStemCompatibility", at = @At("HEAD"), cancellable = true)
    private void chenMod$skipModdedWorldgenOpenWarning(
        LevelStorageSource.LevelStorageAccess levelStorageAccess,
        WorldStem worldStem,
        PackRepository packRepository,
        Runnable callback,
        CallbackInfo ci
    ) {
        WorldData worldData = worldStem.worldData();
        if (!worldData.worldGenOptions().isOldCustomizedWorld()
            && worldData.worldGenSettingsLifecycle() == Lifecycle.experimental()
            && !FeatureFlags.isExperimental(worldData.enabledFeatures())) {
            this.openWorldLoadBundledResourcePack(levelStorageAccess, worldStem, packRepository, callback);
            ci.cancel();
        }
    }
}
