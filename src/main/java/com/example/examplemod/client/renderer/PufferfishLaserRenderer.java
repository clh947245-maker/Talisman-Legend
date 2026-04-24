package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.PufferfishLaserEntity;
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

public class PufferfishLaserRenderer extends EntityRenderer<PufferfishLaserEntity> {

    private static final ResourceLocation BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public PufferfishLaserRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(PufferfishLaserEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(PufferfishLaserEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderBeam(entity, entity.getVisibleStart(partialTicks), entity.getVisibleEnd(partialTicks), partialTicks, poseStack, buffer);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderBeam(PufferfishLaserEntity entity, Vec3 start, Vec3 end, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
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
        float textureOffset = -(entity.tickCount + partialTicks) * 0.7F;
        float roll = 0.0F;
        float widthScale = entity.getWidthScale(partialTicks);
        if (widthScale <= 0.02F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(localStart.x, localStart.y, localStart.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));

        VertexConsumer coreConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_LOCATION, true));
        renderSquarePrism(poseStack, coreConsumer, 0.30F, 1.00F, 0.45F, widthScale, length, 0.075F * widthScale, textureOffset, textureOffset + length * 0.9F, roll);

        VertexConsumer glowConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_LOCATION, false));
        renderSquarePrism(poseStack, glowConsumer, 0.45F, 1.00F, 0.60F, 0.45F * widthScale, length, 0.16F * widthScale, textureOffset, textureOffset + length * 0.9F, roll);
        poseStack.popPose();
    }

    private static void renderSquarePrism(PoseStack poseStack, VertexConsumer consumer, float red, float green, float blue, float alpha,
                                          float length, float halfSize, float minV, float maxV, float rotation) {
        PoseStack.Pose pose = poseStack.last();
        float[] xs = {-halfSize, halfSize, halfSize, -halfSize};
        float[] ys = {-halfSize, -halfSize, halfSize, halfSize};
        float cos = Mth.cos(rotation);
        float sin = Mth.sin(rotation);

        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            float x1 = xs[i] * cos - ys[i] * sin;
            float y1 = xs[i] * sin + ys[i] * cos;
            float x2 = xs[next] * cos - ys[next] * sin;
            float y2 = xs[next] * sin + ys[next] * cos;

            float u0 = (float) i / 4.0F;
            float u1 = (float) (i + 1) / 4.0F;

            float nx = y2 - y1;
            float ny = -(x2 - x1);
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
    public ResourceLocation getTextureLocation(PufferfishLaserEntity entity) {
        return BEAM_LOCATION;
    }
}
