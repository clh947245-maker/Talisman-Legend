package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.LivingBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public class LivingBlockRenderer extends EntityRenderer<LivingBlockEntity> {

    public LivingBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public boolean shouldRender(LivingBlockEntity livingBlock, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingBlock, camera, camX, camY, camZ);
    }

    @Override
    public void render(LivingBlockEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BlockState material = entity.getAnimatedBlockState();
        if (material.isAir()) {
            material = Blocks.STONE.defaultBlockState();
        }
        BlockState eyeWhite = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        BlockState pupil = Blocks.BLACK_CONCRETE.defaultBlockState();

        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.clamp(Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot) - bodyYaw, -30.0F, 30.0F);
        float headPitch = Mth.clamp(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()), -20.0F, 20.0F);
        float walkAmount = Mth.clamp((float) entity.getDeltaMovement().horizontalDistance() * 12.0F, 0.0F, 1.0F);
        float walkTime = (entity.tickCount + partialTicks) * 1.25F + entity.getId() * 0.35F;
        float legSwing = Mth.cos(walkTime) * 24.0F * walkAmount;
        float armSwing = Mth.cos(walkTime + Mth.PI) * 18.0F * walkAmount;
        float bodyBob = Mth.sin(walkTime * 2.0F) * 0.02F * walkAmount;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.02D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));

        renderBlockPart(poseStack, buffer, packedLight, material, 0.0F, 0.65F + bodyBob, 0.0F, 0.48F, 0.34F, 0.34F);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.88F + bodyBob, -0.01F);
        poseStack.mulPose(Axis.YP.rotationDegrees(headYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-headPitch));
        renderBlockPartAtOrigin(poseStack, buffer, packedLight, material, 0.34F, 0.34F, 0.34F);
        renderBlockPart(poseStack, buffer, packedLight, eyeWhite, -0.08F, 0.02F, -0.19F, 0.08F, 0.08F, 0.04F);
        renderBlockPart(poseStack, buffer, packedLight, eyeWhite, 0.08F, 0.02F, -0.19F, 0.08F, 0.08F, 0.04F);
        renderBlockPart(poseStack, buffer, packedLight, pupil, -0.08F, 0.02F, -0.215F, 0.04F, 0.04F, 0.025F);
        renderBlockPart(poseStack, buffer, packedLight, pupil, 0.08F, 0.02F, -0.215F, 0.04F, 0.04F, 0.025F);
        poseStack.popPose();

        renderSwingingLimb(poseStack, buffer, packedLight, material, -0.29F, 0.64F + bodyBob, 0.0F, 0.12F, 0.34F, 0.12F, armSwing);
        renderSwingingLimb(poseStack, buffer, packedLight, material, 0.29F, 0.64F + bodyBob, 0.0F, 0.12F, 0.34F, 0.12F, -armSwing);
        renderSwingingLimb(poseStack, buffer, packedLight, material, -0.12F, 0.49F + bodyBob, 0.0F, 0.14F, 0.32F, 0.14F, -legSwing);
        renderSwingingLimb(poseStack, buffer, packedLight, material, 0.12F, 0.49F + bodyBob, 0.0F, 0.14F, 0.32F, 0.14F, legSwing);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderSwingingLimb(PoseStack poseStack, MultiBufferSource buffer, int packedLight, BlockState state,
                                    float pivotX, float pivotY, float pivotZ, float scaleX, float scaleY, float scaleZ, float xRotation) {
        poseStack.pushPose();
        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(xRotation));
        poseStack.translate(0.0F, -scaleY * 0.5F, 0.0F);
        renderBlockPartAtOrigin(poseStack, buffer, packedLight, state, scaleX, scaleY, scaleZ);
        poseStack.popPose();
    }

    private void renderBlockPart(PoseStack poseStack, MultiBufferSource buffer, int packedLight, BlockState state,
                                 float centerX, float centerY, float centerZ, float scaleX, float scaleY, float scaleZ) {
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, centerZ);
        renderBlockPartAtOrigin(poseStack, buffer, packedLight, state, scaleX, scaleY, scaleZ);
        poseStack.popPose();
    }

    private void renderBlockPartAtOrigin(PoseStack poseStack, MultiBufferSource buffer, int packedLight, BlockState state,
                                         float scaleX, float scaleY, float scaleZ) {
        poseStack.pushPose();
        poseStack.translate(-scaleX * 0.5F, -scaleY * 0.5F, -scaleZ * 0.5F);
        poseStack.scale(scaleX, scaleY, scaleZ);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(LivingBlockEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
