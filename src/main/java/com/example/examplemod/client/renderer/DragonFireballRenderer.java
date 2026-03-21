package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.DragonFireballEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class DragonFireballRenderer extends EntityRenderer<DragonFireballEntity> {
    private static final ResourceLocation BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public DragonFireballRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DragonFireballEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        // 渲染光束
        renderBeam(entity, poseStack, buffer, partialTicks);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderBeam(DragonFireballEntity entity, PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
        // 如果实体有所有者，尝试连接到所有者
        // 使用 getOwnerForRender 以支持客户端获取 Owner ID
        net.minecraft.world.entity.Entity owner = entity.getOwnerForRender();
        
        if (owner != null) {
            // 获取实体的插值位置
            net.minecraft.world.phys.Vec3 entityPos = entity.getPosition(partialTicks);
            // 获取所有者的插值位置 (眼部位置)
            net.minecraft.world.phys.Vec3 ownerPos = owner.getEyePosition(partialTicks).subtract(0, 0.5, 0); // 稍微下移一点，看起来像是从胸口或手发出的

            // 计算从实体指向所有者的向量
            net.minecraft.world.phys.Vec3 diff = ownerPos.subtract(entityPos);
            float length = (float) diff.length();
            
            // 如果距离太近，不渲染或渲染极短
            if (length < 0.1F) return;

            // 计算旋转以对齐光束到所有者
            // 默认光束是沿 Z 轴正向延伸的 (0 到 length)
            // 我们需要让 Z 轴对齐 diff 向量
            
            poseStack.pushPose();
            
            // 计算 Yaw 和 Pitch
            double d0 = diff.horizontalDistance();
            float yRot = (float)(Mth.atan2(diff.x, diff.z) * (double)(180F / (float)Math.PI));
            float xRot = (float)(Mth.atan2(diff.y, d0) * (double)(180F / (float)Math.PI));
            
            // 应用旋转
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot)); // 注意：这里可能需要调整正负号
            
            // 实际渲染
            float radius = 0.3F; // 光束半径
            long gameTime = entity.level().getGameTime();
            float textureOffset = -gameTime * 0.2F - partialTicks * 0.2F;
            float rotation = (float)Math.toRadians(entity.tickCount * 2);
            
            // 颜色
            float r = 1.0F; float g = 0.3F; float b = 0.0F; float alpha = 1.0F;
            
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_LOCATION, true));
            renderCylinder(poseStack, vertexConsumer, r, g, b, alpha, length, radius, textureOffset, textureOffset + length * 0.5F, rotation);
            
            VertexConsumer glowConsumer = buffer.getBuffer(RenderType.beaconBeam(BEAM_LOCATION, false));
            renderCylinder(poseStack, glowConsumer, r, g, b, 0.4F, length, radius * 1.5F, textureOffset, textureOffset + length * 0.5F, rotation);
            
            poseStack.popPose();
        } else {
            // 没有所有者时，不渲染光束，避免出现“发射出去的圆柱体”
            // 或者可以渲染一个小火球/核心，表示它是一个能量体
        }
    }

    private static void renderCylinder(PoseStack poseStack, VertexConsumer consumer, float red, float green, float blue, float alpha, float length, float radius, float minV, float maxV, float rotation) {
        PoseStack.Pose pose = poseStack.last();
        int segments = 16; // 16边形近似圆柱
        
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (Math.PI * 2 * i / segments) + rotation;
            float angle2 = (float) (Math.PI * 2 * (i + 1) / segments) + rotation;
            
            float x1 = Mth.cos(angle1) * radius;
            float y1 = Mth.sin(angle1) * radius;
            float x2 = Mth.cos(angle2) * radius;
            float y2 = Mth.sin(angle2) * radius;
            
            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;
            
            // 法线指向圆心外侧
            float nx = (x1 + x2) / 2.0F;
            float ny = (y1 + y2) / 2.0F;
            float len = Mth.sqrt(nx * nx + ny * ny);
            nx /= len;
            ny /= len;
            
            // 绘制侧面
            addQuad(pose, consumer, red, green, blue, alpha,
                x1, y1, 0,      u0, minV,
                x2, y2, 0,      u1, minV,
                x2, y2, length, u1, maxV,
                x1, y1, length, u0, maxV,
                nx, ny, 0
            );
            
            // 内部面 (如果需要双面可见)
            addQuad(pose, consumer, red, green, blue, alpha,
                x1, y1, length, u0, maxV,
                x2, y2, length, u1, maxV,
                x2, y2, 0,      u1, minV,
                x1, y1, 0,      u0, minV,
                -nx, -ny, 0
            );
        }
    }
    
    private static void addQuad(PoseStack.Pose pose, VertexConsumer consumer, float r, float g, float b, float a, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2, float x3, float y3, float z3, float u3, float v3, float nx, float ny, float nz) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(u3, v3).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(DragonFireballEntity entity) {
        return BEAM_LOCATION;
    }
}
