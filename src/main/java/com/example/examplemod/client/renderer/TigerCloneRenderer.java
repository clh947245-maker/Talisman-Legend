package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.TigerCloneEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * 虎分身渲染器
 * <p>
 * 使用玩家模型渲染分身。
 * 尝试获取主人皮肤，如果获取失败则使用默认皮肤。
 * </p>
 */
public class TigerCloneRenderer extends LivingEntityRenderer<TigerCloneEntity, PlayerModel<TigerCloneEntity>> {

    public TigerCloneRenderer(EntityRendererProvider.Context context) {
        // 使用标准的玩家模型（不带纤细手臂的默认 Steve 模型）
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        
        // 添加手持物品渲染层（武器、工具等）
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        
        // 添加盔甲渲染层（内层和外层）
        this.addLayer(new HumanoidArmorLayer<>(
            this, 
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), 
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), 
            context.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(TigerCloneEntity entity) {
        // 尝试获取主人的 UUID
        UUID ownerUUID = entity.getOwnerUUID();
        
        if (ownerUUID != null) {
            // 获取对应 UUID 的皮肤
            // 注意：DefaultPlayerSkin.get(UUID) 会返回默认皮肤（Steve 或 Alex）
            // 如果要获取真实皮肤，需要 SkinManager，但这通常只对 AbstractClientPlayer 有效
            // 对于非玩家实体，直接获取皮肤比较复杂，这里暂时使用 DefaultPlayerSkin 作为基础
            // 改进方案：如果需要显示玩家真实皮肤，需要手动请求 SkinManager 加载纹理，或者让 Entity 伪装成 AbstractClientPlayer（非常复杂）
            // 目前 1.21 NeoForge 环境下，最简单的方式是使用 DefaultPlayerSkin 确保不紫黑丢失
            return DefaultPlayerSkin.get(ownerUUID).texture();
        }
        
        // 默认返回 Steve 皮肤
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
