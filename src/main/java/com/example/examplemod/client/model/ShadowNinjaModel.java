package com.example.examplemod.client.model;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.ShadowNinjaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShadowNinjaModel extends GeoModel<ShadowNinjaEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "geo/shadow_ninja.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "textures/entity/shadow_ninja.png");
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "animations/shadow_ninja.animation.json");

    @Override
    public ResourceLocation getModelResource(ShadowNinjaEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ShadowNinjaEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShadowNinjaEntity animatable) {
        return ANIMATION;
    }
}