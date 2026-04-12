package com.example.examplemod.client.renderer;

import com.example.examplemod.client.model.ShengZhuModel;
import com.example.examplemod.entity.ShengZhuEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ShengZhuRenderer extends GeoEntityRenderer<ShengZhuEntity> {
    public ShengZhuRenderer(EntityRendererProvider.Context context) {
        super(context, new ShengZhuModel());
        this.shadowRadius = 0.9F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
