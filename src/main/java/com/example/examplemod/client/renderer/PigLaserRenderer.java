package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.PigLaserEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class PigLaserRenderer extends ArrowRenderer<PigLaserEntity> {

    private static final ResourceLocation ARROW_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/arrow.png");

    public PigLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PigLaserEntity entity) {
        return ARROW_LOCATION;
    }
}
