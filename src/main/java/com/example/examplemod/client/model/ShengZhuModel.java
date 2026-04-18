package com.example.examplemod.client.model;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.ShengZhuEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ShengZhuModel extends GeoModel<ShengZhuEntity> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "geo/dragon_brutel.geo.json");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "textures/entity/dragon_brutel.png");

    @Override
    public ResourceLocation getModelResource(ShengZhuEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ShengZhuEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShengZhuEntity animatable) {
        return null;
    }
}
