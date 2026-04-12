package com.example.examplemod.client.renderer;

import com.example.examplemod.client.model.ShadowNinjaModel;
import com.example.examplemod.entity.ShadowNinjaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ShadowNinjaRenderer extends GeoEntityRenderer<ShadowNinjaEntity> {
    private static final ResourceLocation SHADOW_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/shadow.png");
    private static final float TRANSITION_SHADOW_Y = 0.02F;
    private static final float TRANSITION_SHADOW_BASE_RADIUS = 0.62F;
    private static final float TRANSITION_SHADOW_MAX_RADIUS = 0.78F;
    private static final float SUMMON_SHADOW_APPEAR_END = 0.22F;
    private static final float DISMISS_SHADOW_FADE_START = 0.68F;

    public ShadowNinjaRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowNinjaModel());
        this.shadowRadius = 0.5F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(ShadowNinjaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float previousRadius = this.shadowRadius;
        float previousStrength = this.shadowStrength;
        this.shadowRadius = entity.getShadowRadiusScale();
        this.shadowStrength = entity.getShadowStrengthScale();
        this.renderTransitionShadow(entity, partialTick, poseStack, bufferSource);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        this.shadowRadius = previousRadius;
        this.shadowStrength = previousStrength;
    }

    private void renderTransitionShadow(ShadowNinjaEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!entity.isTransitioning()) {
            return;
        }

        float progress = entity.getTransitionVisualProgress();
        float yOffset = entity.getTransitionShadowOffsetY(partialTick);
        boolean emergingFromShadow = entity.isSummoning() || entity.isShadowRushRising();
        boolean sinkingIntoShadow = entity.isDismissing() || entity.isShadowRushSinking();
        float radius;
        float alpha;
        if (emergingFromShadow) {
            float appearProgress = Mth.clamp(progress / SUMMON_SHADOW_APPEAR_END, 0.0F, 1.0F);
            float emergeProgress = Mth.clamp((progress - SUMMON_SHADOW_APPEAR_END) / (1.0F - SUMMON_SHADOW_APPEAR_END), 0.0F, 1.0F);
            radius = Mth.lerp(appearProgress, 0.0F, TRANSITION_SHADOW_BASE_RADIUS);
            alpha = Mth.lerp(appearProgress, 0.0F, 0.82F);
            radius = Mth.lerp(emergeProgress, radius, 0.56F);
            alpha = Mth.lerp(emergeProgress, alpha, 0.62F);
        } else if (sinkingIntoShadow) {
            float sinkProgress = Mth.clamp(progress / DISMISS_SHADOW_FADE_START, 0.0F, 1.0F);
            float fadeProgress = Mth.clamp((progress - DISMISS_SHADOW_FADE_START) / (1.0F - DISMISS_SHADOW_FADE_START), 0.0F, 1.0F);
            radius = Mth.lerp(sinkProgress, 0.56F, TRANSITION_SHADOW_BASE_RADIUS);
            alpha = Mth.lerp(sinkProgress, 0.62F, 0.86F);
            radius = Mth.lerp(fadeProgress, radius, TRANSITION_SHADOW_MAX_RADIUS);
            alpha = Mth.lerp(fadeProgress, alpha, 0.0F);
        } else {
            return;
        }

        if (radius <= 0.01F || alpha <= 0.01F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, yOffset, 0.0D);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityShadow(SHADOW_TEXTURE));
        this.addShadowVertex(consumer, pose, -radius, TRANSITION_SHADOW_Y, -radius, 0.0F, 0.0F, alpha);
        this.addShadowVertex(consumer, pose, -radius, TRANSITION_SHADOW_Y, radius, 0.0F, 1.0F, alpha);
        this.addShadowVertex(consumer, pose, radius, TRANSITION_SHADOW_Y, radius, 1.0F, 1.0F, alpha);
        this.addShadowVertex(consumer, pose, radius, TRANSITION_SHADOW_Y, -radius, 1.0F, 0.0F, alpha);
        poseStack.popPose();
    }

    private void addShadowVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float alpha) {
        int color = FastColor.ARGB32.color(Mth.floor(alpha * 255.0F), 255, 255, 255);
        Vector3f transformed = pose.pose().transformPosition(x, y, z, new Vector3f());
        consumer.addVertex(
                transformed.x(),
                transformed.y(),
                transformed.z(),
                color,
                u,
                v,
                OverlayTexture.NO_OVERLAY,
                15728880,
                0.0F,
                1.0F,
                0.0F
        );
    }
}
