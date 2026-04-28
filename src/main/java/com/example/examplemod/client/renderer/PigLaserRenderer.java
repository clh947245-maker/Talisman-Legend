package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.PigLaserEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class PigLaserRenderer extends EntityRenderer<PigLaserEntity> {

    private static final ResourceLocation BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final float TEXTURE_SCROLL_SPEED = 0.35F;

    public PigLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PigLaserEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(PigLaserEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderBeam(entity, entity.getVisibleLeftStart(partialTicks), entity.getVisibleLeftEnd(partialTicks), partialTicks, poseStack, buffer);
        renderBeam(entity, entity.getVisibleRightStart(partialTicks), entity.getVisibleRightEnd(partialTicks), partialTicks, poseStack, buffer);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderBeam(PigLaserEntity entity, Vec3 start, Vec3 end, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        Vec3 diff = end.subtract(start);
        float length = (float) diff.length();
        if (length < 0.05F) {
            return;
        }

        Vec3 entityPos = entity.getPosition(partialTicks);
        Vec3 localStart = start.subtract(entityPos);
        double horizontalDistance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yRot = (float) (Mth.atan2(diff.x, diff.z) * (180.0F / (float) Math.PI));
        float xRot = (float) (Mth.atan2(diff.y, horizontalDistance) * (180.0F / (float) Math.PI));
        float textureOffset = -(entity.tickCount + partialTicks) * TEXTURE_SCROLL_SPEED;
        float roll = 0.0F;

        poseStack.pushPose();
        poseStack.translate(localStart.x, localStart.y, localStart.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));

        VertexConsumer coreConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_LOCATION, true));
        renderCylinder(poseStack, coreConsumer, 1.0F, 0.95F, 0.2F, 1.0F, length, 0.05F, textureOffset, textureOffset + length * 0.9F, roll);

        VertexConsumer glowConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_LOCATION, false));
        renderCylinder(poseStack, glowConsumer, 1.0F, 0.9F, 0.25F, 0.45F, length, 0.11F, textureOffset, textureOffset + length * 0.9F, roll);
        poseStack.popPose();
    }

    private static void renderCylinder(PoseStack poseStack, VertexConsumer consumer, float red, float green, float blue, float alpha,
                                       float length, float radius, float minV, float maxV, float rotation) {
        PoseStack.Pose pose = poseStack.last();
        int segments = 12;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (Math.PI * 2 * i / segments) + rotation;
            float angle2 = (float) (Math.PI * 2 * (i + 1) / segments) + rotation;

            float x1 = Mth.cos(angle1) * radius;
            float y1 = Mth.sin(angle1) * radius;
            float x2 = Mth.cos(angle2) * radius;
            float y2 = Mth.sin(angle2) * radius;

            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;

            float nx = (x1 + x2) * 0.5F;
            float ny = (y1 + y2) * 0.5F;
            float normalLength = Mth.sqrt(nx * nx + ny * ny);
            if (normalLength > 0.0F) {
                nx /= normalLength;
                ny /= normalLength;
            }

            addQuad(pose, consumer, red, green, blue, alpha,
                    x1, y1, 0.0F, u0, minV,
                    x2, y2, 0.0F, u1, minV,
                    x2, y2, length, u1, maxV,
                    x1, y1, length, u0, maxV,
                    nx, ny, 0.0F);

            addQuad(pose, consumer, red, green, blue, alpha,
                    x1, y1, length, u0, maxV,
                    x2, y2, length, u1, maxV,
                    x2, y2, 0.0F, u1, minV,
                    x1, y1, 0.0F, u0, minV,
                    -nx, -ny, 0.0F);
        }
    }

    private static void addQuad(PoseStack.Pose pose, VertexConsumer consumer, float r, float g, float b, float a,
                                float x0, float y0, float z0, float u0, float v0,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float nx, float ny, float nz) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(PigLaserEntity entity) {
        return BEAM_LOCATION;
    }
}
