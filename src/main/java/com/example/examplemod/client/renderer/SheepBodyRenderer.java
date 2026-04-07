package com.example.examplemod.client.renderer;

import com.example.examplemod.client.ClientTransformationHandler;
import com.example.examplemod.entity.SheepBodyEntity;
import com.example.examplemod.magic.transformation.TransformationManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import java.util.UUID;

/**
 * 羊身体实体的渲染器类。
 *
 * 该类负责渲染羊身体实体，使用玩家模型来显示。
 * 支持根据拥有者的皮肤类型自动切换宽版/纤细版玩家模型，
 * 并渲染拥有者的皮肤纹理、手持物品和装备护甲。
 */
public class SheepBodyRenderer extends LivingEntityRenderer<SheepBodyEntity, PlayerModel<SheepBodyEntity>> {

    /**
     * 宽版玩家模型（Steve风格）。
     * 用于手臂为4像素宽的玩家皮肤。
     */
    private final PlayerModel<SheepBodyEntity> wideModel;

    /**
     * 纤细版玩家模型（Alex风格）。
     * 用于手臂为3像素宽的玩家皮肤。
     */
    private final PlayerModel<SheepBodyEntity> slimModel;

    /**
     * 构造方法，初始化渲染器。
     *
     * @param context 实体渲染器提供上下文，用于获取模型层和渲染资源
     */
    public SheepBodyRenderer(EntityRendererProvider.Context context) {
        // 调用父类构造方法，默认使用宽版玩家模型，阴影大小为0.5F
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        // 保存宽版模型引用
        this.wideModel = this.model;

        // 创建纤细版模型实例，第二个参数true表示使用纤细手臂
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // 添加手持物品渲染层，用于渲染实体手中的物品
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));

        // 添加护甲渲染层，用于渲染实体穿戴的护甲
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    /**
     * 渲染实体的主方法。
     *
     * 在渲染前根据拥有者的皮肤类型选择合适的玩家模型（宽版或纤细版），
     * 并确保模型的所有部分都可见。
     *
     * @param entity 要渲染的羊身体实体
     * @param entityYaw 实体的水平旋转角度（偏航角）
     * @param partialTick 部分游戏刻，用于平滑动画插值
     * @param poseStack 姿态堆栈，用于变换渲染位置、旋转和缩放
     * @param buffer 多重缓冲源，用于获取渲染缓冲区
     * @param packedLight 打包的光照值，包含方块光照和天空光照
     */
    @Override
    public void render(SheepBodyEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity.isSnackInvisible()) {
            return;
        }

        int transformationId = entity.getMonkeyTransformationId();
        if (transformationId != TransformationManager.ID_REVERT
                && ClientTransformationHandler.renderTransformedEntity(entity, transformationId, partialTick, poseStack, buffer, packedLight)) {
            return;
        }
        // 根据拥有者的皮肤模型类型选择使用宽版或纤细版模型
        this.model = this.usesSlimModel(entity) ? this.slimModel : this.wideModel;

        // 设置模型的所有部分可见，确保完整渲染
        this.model.setAllVisible(true);

        // 调用父类的渲染方法执行实际渲染
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * 获取实体的纹理位置（皮肤）。
     *
     * 根据实体的拥有者UUID获取对应的玩家皮肤纹理。
     * 如果拥有者不在线或UUID为空，则返回默认皮肤。
     *
     * @param entity 羊身体实体
     * @return 皮肤纹理的资源位置
     */
    @Override
    public ResourceLocation getTextureLocation(SheepBodyEntity entity) {
        // 获取拥有者的UUID
        UUID ownerUUID = entity.resolveAppearanceUUID();

        // 如果UUID为空，返回默认皮肤纹理
        if (ownerUUID == null) {
            return DefaultPlayerSkin.getDefaultTexture();
        }

        // 从客户端连接中获取玩家信息
        PlayerInfo playerInfo = Minecraft.getInstance().getConnection() == null ? null : Minecraft.getInstance().getConnection().getPlayerInfo(ownerUUID);

        // 如果获取到玩家信息，返回该玩家的皮肤纹理
        if (playerInfo != null) {
            return playerInfo.getSkin().texture();
        }

        // 如果玩家不在线，根据UUID获取对应的默认皮肤（可能基于UUID生成特定默认皮肤）
        return DefaultPlayerSkin.get(ownerUUID).texture();
    }

    /**
     * 判断应该使用纤细版模型还是宽版模型。
     *
     * 通过检查拥有者的皮肤信息来确定模型类型。
     * Alex皮肤使用纤细版（3像素手臂），Steve皮肤使用宽版（4像素手臂）。
     *
     * @param entity 羊身体实体
     * @return 如果应该使用纤细版模型返回true，否则返回false
     */
    private boolean usesSlimModel(SheepBodyEntity entity) {
        // 获取拥有者的UUID
        UUID ownerUUID = entity.resolveAppearanceUUID();

        // 如果UUID为空或客户端连接为空，默认使用宽版模型
        if (ownerUUID == null || Minecraft.getInstance().getConnection() == null) {
            return false;
        }

        // 获取玩家信息
        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(ownerUUID);

        // 如果无法获取玩家信息，默认使用宽版模型
        if (playerInfo == null) {
            return false;
        }

        // 检查玩家皮肤的模型类型是否为纤细版（SLIM）
        return playerInfo.getSkin().model() == PlayerSkin.Model.SLIM;
    }
}
