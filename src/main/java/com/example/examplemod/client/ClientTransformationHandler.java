package com.example.examplemod.client;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.transformation.ITransformation;
import com.example.examplemod.magic.transformation.TransformationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import java.lang.reflect.Field;
import net.minecraft.world.entity.WalkAnimationState;

/**
 * 客户端渲染处理器
 *
 * 负责在客户端将玩家模型替换为对应的动物模型。
 */
@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class ClientTransformationHandler {

    // 缓存用于渲染的虚拟实体
    private static final Map<UUID, LivingEntity> DUMMY_ENTITIES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TRANSFORMATION_ID = new HashMap<>();

    // 反射字段，用于同步 walkAnimation 的 speedOld 和 position
    private static Field walkAnimSpeedOldField;
    private static Field walkAnimPositionField;

    static {
        try {
            // speedOld 字段
            walkAnimSpeedOldField = WalkAnimationState.class.getDeclaredField("speedOld");
            walkAnimSpeedOldField.setAccessible(true);

            // position 字段
            // 顺序通常是: speedOld, speed, position
            walkAnimPositionField = WalkAnimationState.class.getDeclaredField("position");
            walkAnimPositionField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // 如果找不到，尝试通过类型查找
            try {
                Field[] fields = WalkAnimationState.class.getDeclaredFields();
                int floatCount = 0;
                for (Field f : fields) {
                    if (f.getType() == float.class) {
                        f.setAccessible(true);
                        if (floatCount == 0)
                            walkAnimSpeedOldField = f; // 第一个是 speedOld
                        else if (floatCount == 2)
                            walkAnimPositionField = f; // 第三个是 position
                        floatCount++;
                    }
                }
            } catch (Exception ex) {
                ChenMod.LOGGER.error("Failed to find fields in WalkAnimationState", ex);
            }
        }
    }

    /**
     * 在玩家渲染之前触发
     * 如果玩家处于变身状态，取消原版渲染，并手动渲染对应的动物实体
     */
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        MobEffectInstance effect = player.getEffect(ChenMod.MONKEY_POWER);

        if (effect != null) {
            // 如果玩家拥有蛇符咒效果（隐身），则不仅取消原版渲染，也不渲染替代模型
            if (player.hasEffect(ChenMod.SNACK_POWER)) {
                event.setCanceled(true);
                return;
            }

            int transformationId = effect.getAmplifier();
            ITransformation transformation = TransformationManager.getTransformation(transformationId);

            if (transformation != null) {
                // 取消原版玩家渲染
                event.setCanceled(true);

                // 渲染替代模型
                renderTransformedEntity(player, transformationId, event);
            }
        }
    }

    private static void renderTransformedEntity(Player player, int transformationId, RenderPlayerEvent.Pre event) {
        // 获取或创建虚拟实体
        LivingEntity dummy = getDummyEntity(player, transformationId);
        if (dummy == null)
            return;

        // 同步玩家的数据到虚拟实体
        syncEntityData(player, dummy, transformationId);

        // 渲染虚拟实体
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        // 调整位置：玩家渲染事件的位置通常是插值后的位置
        // 我们直接使用 event.getPoseStack() 中的变换，它已经包含了相对于相机的位移

        // 保存当前的 PoseStack 状态
        event.getPoseStack().pushPose();

        // 处理渲染
        // 注意：RenderPlayerEvent 中 PoseStack 已经定位到了玩家的位置
        // 我们需要传递 dummy 实体给 dispatcher

        // 渲染偏移调整 (有些动物模型中心点可能不同)
        // event.getPoseStack().translate(0, 0, 0);

        try {
            // 使用 dispatcher 渲染实体
            // packedLight 等参数从 event 中获取比较麻烦，我们可以直接计算或使用 EntityRenderer 的默认逻辑
            // 但最简单的是调用 render
            dispatcher.render(
                    dummy,
                    0, 0, 0, // x, y, z (因为 PoseStack 已经变换过了，这里相对坐标为 0)
                    0.0f, // entityYaw (dummy 已经同步了)
                    event.getPartialTick(),
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight());
        } catch (Exception e) {
            // 防止渲染崩溃导致游戏退出
            ChenMod.LOGGER.error("Error rendering transformed entity", e);
        }

        event.getPoseStack().popPose();
    }

    private static LivingEntity getDummyEntity(Player player, int transformationId) {
        UUID uuid = player.getUUID();

        // 检查缓存是否有效
        if (DUMMY_ENTITIES.containsKey(uuid)) {
            LivingEntity existing = DUMMY_ENTITIES.get(uuid);
            // 检查变身ID是否改变，或者世界是否改变（防止跨世界引用）
            if (LAST_TRANSFORMATION_ID.getOrDefault(uuid, -1) != transformationId
                    || existing.level() != player.level()) {
                DUMMY_ENTITIES.remove(uuid);
            } else {
                return existing;
            }
        }

        // 创建新的虚拟实体
        LivingEntity entity = null;
        
        // 使用 TransformationManager 动态获取实体类型，避免硬编码
        ITransformation transformation = TransformationManager.getTransformation(transformationId);
        if (transformation != null) {
            EntityType<? extends LivingEntity> type = transformation.getEntityType();
            if (type != null && type != EntityType.PLAYER) {
                entity = type.create(player.level());
            }
        }

        if (entity != null) {
            DUMMY_ENTITIES.put(uuid, entity);
            LAST_TRANSFORMATION_ID.put(uuid, transformationId);
        }

        return entity;
    }

    private static void syncEntityData(Player player, LivingEntity dummy, int transformationId) {
        // 记录是否是新的 tick (用于控制动画更新频率)
        boolean isNewTick = dummy.tickCount != player.tickCount;
        dummy.tickCount = player.tickCount;

        // 同步位置和旋转
        dummy.setPos(player.getX(), player.getY(), player.getZ());
        dummy.xo = player.xo;
        dummy.yo = player.yo;
        dummy.zo = player.zo;

        dummy.setYRot(player.getYRot());
        dummy.yRotO = player.yRotO;
        dummy.setXRot(player.getXRot());
        dummy.xRotO = player.xRotO;

        dummy.yBodyRot = player.yBodyRot;
        dummy.yBodyRotO = player.yBodyRotO;
        dummy.yHeadRot = player.yHeadRot;
        dummy.yHeadRotO = player.yHeadRotO;

        // 同步状态
        dummy.swinging = player.swinging;
        dummy.swingTime = player.swingTime;
        dummy.hurtTime = player.hurtTime;
        dummy.deathTime = player.deathTime;
        dummy.walkAnimation.setSpeed(player.walkAnimation.speed());

        dummy.setSwimming(player.isSwimming());
        dummy.setSprinting(player.isSprinting());
        dummy.setDeltaMovement(player.getDeltaMovement());

        // 关键修复1：同步 speedOld 以防止腿部动画震颤
        // 关键修复2：同步 position 以防止腿部动画卡住
        try {
            if (walkAnimSpeedOldField != null) {
                float playerSpeedOld = walkAnimSpeedOldField.getFloat(player.walkAnimation);
                walkAnimSpeedOldField.setFloat(dummy.walkAnimation, playerSpeedOld);
            }
            if (walkAnimPositionField != null) {
                float playerPosition = walkAnimPositionField.getFloat(player.walkAnimation);
                walkAnimPositionField.setFloat(dummy.walkAnimation, playerPosition);
            }
        } catch (IllegalAccessException e) {
            // 忽略
        }

        // 潜行状态
        dummy.setPose(player.getPose());
        dummy.setShiftKeyDown(player.isShiftKeyDown());

        // 调用 Transformation 类中的动画同步逻辑
        ITransformation transformation = TransformationManager.getTransformation(transformationId);
        if (transformation != null) {
            transformation.syncAnimation(player, dummy, isNewTick);
        }
    }
}    