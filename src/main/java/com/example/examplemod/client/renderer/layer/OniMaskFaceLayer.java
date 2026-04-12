package com.example.examplemod.client.renderer.layer;

import com.example.examplemod.ChenMod;
import com.example.examplemod.item.OniMaskItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class OniMaskFaceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "textures/entity/oni_mask_face.png");

    public OniMaskFaceLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!OniMaskItem.isOniMask(headStack) || player.isInvisible()) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().getHead().translateAndRotate(poseStack);
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

        // Use a much larger plane so the full oni mask texture can sit over the whole face.
        addVertex(consumer, pose, -7.5F, -11.5F, -4.7F, 0.0F, 0.0F, packedLight, overlay);
        addVertex(consumer, pose, -7.5F, 5.0F, -4.7F, 0.0F, 1.0F, packedLight, overlay);
        addVertex(consumer, pose, 7.5F, 5.0F, -4.7F, 1.0F, 1.0F, packedLight, overlay);
        addVertex(consumer, pose, 7.5F, -11.5F, -4.7F, 1.0F, 0.0F, packedLight, overlay);

        poseStack.popPose();
    }

    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedLight,
            int overlay
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
