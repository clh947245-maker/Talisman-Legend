package com.example.examplemod.client.renderer;

import com.example.examplemod.entity.PlayerDecoyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * PlayerDecoyRenderer — NeoForge 1.21
 *
 * 把 PlayerDecoyEntity 渲染为与原玩家完全一致的外观：
 *   - Steve / Alex 双模型，根据 entity.isSlim() 切换
 *   - 皮肤贴图：从当前在线玩家缓存获取；离线时回退 Steve
 *   - 装备渲染层：盔甲、手持物品、斗篷、鞘翅、箭矢、蜜蜂刺、鹦鹉
 *
 * 注册方式（在 @Mod.EventBusSubscriber(bus=MOD, value=CLIENT) 类中）：
 *   @SubscribeEvent
 *   public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
 *       event.registerEntityRenderer(ChenMod.PLAYER_DECOY.get(), PlayerDecoyRenderer::new);
 *   }
 */
public class PlayerDecoyRenderer extends LivingEntityRenderer<PlayerDecoyEntity, PlayerModel<PlayerDecoyEntity>> {

    private final PlayerModel<PlayerDecoyEntity> steveModel;
    private final PlayerModel<PlayerDecoyEntity> alexModel;

    public PlayerDecoyRenderer(EntityRendererProvider.Context context) {
        // 父类使用 Steve 宽手臂模型初始化
        super(context,
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false),
                0.5f);

        this.steveModel = this.getModel();
        this.alexModel  = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);

        // ── 盔甲层 ──
        this.addLayer(new HumanoidArmorLayer<>(
            this, 
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), 
            new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), 
            context.getModelManager()
        ));

        // ── 手持物品层 ──
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));

        // ── 鞘翅层 ──
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()));

        // ── 蜜蜂刺层 ──
        this.addLayer(new BeeStingerLayer<>(this));

        // ── 箭矢层 ──
        this.addLayer(new ArrowLayer<>(context, this));
    }

    // ── 根据 isSlim() 在渲染时切换模型 ──────────────────────────────────────────
    @Override
    public void render(PlayerDecoyEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.model = entity.isSlim() ? alexModel : steveModel;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // ── 皮肤贴图 ─────────────────────────────────────────────────────────────────
    /**
     * 皮肤获取策略：
     *  1. 从当前 ClientLevel 的在线玩家列表中按名字匹配，直接用其皮肤贴图（零延迟）。
     *  2. 玩家不在线时，回退到原版 Steve 皮肤。
     *
     * NeoForge 1.21：AbstractClientPlayer.getSkin() 返回 PlayerSkin，
     *   .texture() 返回 ResourceLocation，与原版一致。
     */
    @Override
    public ResourceLocation getTextureLocation(PlayerDecoyEntity entity) {
        String ownerName = entity.getOwnerName();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (AbstractClientPlayer onlinePlayer : mc.level.players()) {
                if (onlinePlayer.getGameProfile().getName().equalsIgnoreCase(ownerName)) {
                    // PlayerSkin.texture() — NeoForge 1.21 / MC 1.21 API
                    return onlinePlayer.getSkin().texture();
                }
            }
        }

        // 离线或找不到时回退 Steve
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }
}