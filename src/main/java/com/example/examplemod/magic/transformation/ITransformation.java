package com.example.examplemod.magic.transformation;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

/**
 * 变身接口
 * <p>
 * 所有动物形态都应实现此接口。
 * </p>
 */
public interface ITransformation {

    /**
     * 获取形态的唯一ID
     */
    String getId();

    /**
     * 获取对应的实体类型
     */
    EntityType<? extends LivingEntity> getEntityType();

    /**
     * 获取该形态下的碰撞箱尺寸
     */
    EntityDimensions getDimensions(Pose pose, EntityDimensions original);

    /**
     * 获取该形态下的眼睛高度
     */
    float getEyeHeight(Pose pose, EntityDimensions dimensions);

    /**
     * 获取该形态的生命值
     */
    default float getHealth() {
        return 20.0f;
    }

    /**
     * 每 tick 执行的逻辑 (用于实现特殊能力，如鸡的滑翔)
     */
    void onTick(LivingEntity entity);
    
    /**
     * 当实体受伤时触发
     */
    default void onHurt(LivingEntity entity) {}
    
    /**
     * 当实体跳跃时触发
     */
    default void onJump(LivingEntity entity) {}

    /**
     * 当变身结束时触发 (用于清理状态，如重置属性)
     */
    default void onRemove(LivingEntity entity) {}

    /**
     * 客户端动画同步 (每帧调用)
     * 用于处理特殊的动画逻辑，如翅膀拍打、触手摆动等
     * 
     * @param player 玩家实体
     * @param dummy 用于渲染的虚拟实体
     * @param isNewTick 是否是新的 tick (用于控制动画更新频率)
     */
    default void syncAnimation(net.minecraft.world.entity.player.Player player, LivingEntity dummy, boolean isNewTick) {}
}
