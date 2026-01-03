package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;

/**
 * 魔法效果："鸡的力量" (Rooster Power)
 * 
 * 核心能力：漂浮 (Levitation) 与 念力 (Telekinesis - 待实现)。
 * 目前主要实现了漂浮飞行能力。
 * 
 * 交互逻辑：
 * - 单独使用 (Solo): 获得类似创造模式飞行的体验，但受惯性影响。
 *     - 如果不按 Shift：给予持续的向上速度 (缓慢上升)。
 *     - 如果按住 Shift：给予缓慢的向下速度 (可控降落)。
 * 
 * - 组合使用 (Combo - with Rabbit Power): 获得极速飞行体验 (High Speed Flight)。
 *     - 按住 Space (跳跃)：高速上升。
 *     - 按住 Shift (潜行)：高速下降。
 *     - 不按任何键：空中悬停 (Hover)，垂直速度归零。
 */
public class RoosterPowerMagic extends MobEffect {

    // 用于通过反射获取实体跳跃状态的字段
    // Minecraft 原生逻辑中，LivingEntity.jumping 字段记录了实体是否按下了跳跃键
    private static Field jumpingField;
    // 用于获取玩家的移动输入 (MojMap: xxa = strafe, zza = forward)
    private static Field xxaField;
    private static Field zzaField;

    static {
        try {
            // 获取 LivingEntity 的 jumping 字段
            // 注意：在混淆环境下 (如生产环境)，字段名可能不同，通常需要使用 AccessTransformer 或 MojMap 映射
            // 这里假设开发环境使用 MojMap，字段名为 "jumping"
            jumpingField = LivingEntity.class.getDeclaredField("jumping");
            jumpingField.setAccessible(true); // 暴力访问私有字段
            
            // 获取输入字段
            try {
                xxaField = LivingEntity.class.getDeclaredField("xxa");
                xxaField.setAccessible(true);
                zzaField = LivingEntity.class.getDeclaredField("zza");
                zzaField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                 ChenMod.LOGGER.error("Failed to access 'xxa'/'zza' fields in RoosterPowerMagic.", e);
            }
        } catch (NoSuchFieldException e) {
            ChenMod.LOGGER.error("Failed to access 'jumping' field in RoosterPowerMagic. Flight controls may be limited.", e);
        }
    }

    /**
     * API: 赋予实体鸡力量效果
     *
     * @param entity   目标实体
     * @param duration 持续时间 (ticks)
     */
    public static void grantRoosterPower(LivingEntity entity, int duration) {
        if (entity == null) return;
        
        // 添加 MobEffectInstance
        // amplifier: 0 (等级 1)
        // ambient: true (环境效果，粒子更透明)
        // visible: true (显示粒子)
        // showIcon: true (显示右上角图标)
        entity.addEffect(new MobEffectInstance(
            ChenMod.ROOSTER_POWER, 
            duration, 
            0, 
            false, 
            false, 
            false
        ));
    }

    public RoosterPowerMagic() {
        // Category: BENEFICIAL (有益效果)
        // Color: 0xFFFFFF (白色，对应鸡符咒的颜色)
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    /**
     * 每一 tick 是否应该执行 applyEffectTick。
     * 对于持续性飞行动作，我们需要每 tick 更新实体的速度，所以返回 true。
     */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    /**
     * 魔法效果的核心逻辑每一 tick 触发一次。
     * 这里控制实体的垂直运动 (Y轴速度)。
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 1. 重置掉落距离
        // 只要拥有鸡符咒效果，玩家就不应受到摔落伤害。
        // 将 fallDistance 设为 0 可以欺骗游戏系统，使其认为玩家一直在地面或刚起跳。
        entity.fallDistance = 0;
        
        // 2. 获取当前运动向量 (Velocity)
        // DeltaMovement 代表实体在这一 tick 的位移量 (即速度)
        Vec3 currentMovement = entity.getDeltaMovement();
        
        // 3. 获取按键与状态信息
        // isShiftKeyDown(): 玩家是否按下了潜行键 (Shift)
        boolean isShiftKeyDown = entity.isShiftKeyDown();
        // 检查是否同时拥有兔符咒 (Rabbit Power) - 用于触发组合技
        boolean hasRabbitPower = entity.hasEffect(ChenMod.RABBIT_POWER);

        if (hasRabbitPower) {
            // ==================================================
            // 组合技：鸡 + 兔 (Rooster + Rabbit) -> 极速飞行模式
            // ==================================================
            // 兔符咒提供极速，鸡符咒提供悬浮。结合后允许玩家高速且精确地控制飞行位置。
            
            // 通过反射获取 "跳跃键" (Space) 的状态
            // 正常的 entity.isJumping() 在某些上下文中可能不准确或受服务端限制，反射字段更直接
            boolean isJumping = false;
            try {
                if (jumpingField != null) {
                    isJumping = jumpingField.getBoolean(entity);
                }
            } catch (IllegalAccessException e) {
                // 如果反射失败，默认为 false，不抛出异常以免崩溃游戏
            }

            // 鸡+兔组合：水平速度增强 (无惯性版)
            // 用户要求：像地面一样，没有加速时间和惯性。这意味着我们需要直接根据输入设置速度。
            
            float strafe = 0;
            float forward = 0;
            
            try {
                if (xxaField != null && zzaField != null) {
                    strafe = xxaField.getFloat(entity);
                    forward = zzaField.getFloat(entity);
                }
            } catch (IllegalAccessException e) {
                // ignore
            }

            double targetX = 0;
            double targetZ = 0;

            // 只有当有输入时才计算新的水平速度
            if (Math.abs(strafe) > 1.0E-5 || Math.abs(forward) > 1.0E-5) {
                // 基础飞行速度：兔符咒应该很快
                // 创造模式飞行速度约为 0.1 (每tick位移)，冲刺飞行更快。
                // 兔符咒给予 4.0 的速度倍率，我们可以设定一个较高的基数。
                double flySpeed = 1.5; 

                // 计算相对于玩家朝向的位移向量
                // 算法参考 LivingEntity.travelAround
                Vec3 inputVec = new Vec3(strafe, 0, forward);
                
                // 将输入向量转换为长度为 1 的归一化向量 (处理斜向移动速度过快问题)
                if (inputVec.lengthSqr() > 1.0E-5) {
                    inputVec = inputVec.normalize();
                }
                
                // 旋转向量以匹配玩家朝向 (YRot)
                // Vec3.yRot() 方法接受的是弧度，且正负方向可能需要调整
                // 更好的方法是使用 standard Minecraft math
                float yRot = entity.getYRot();
                float f = yRot * ((float)Math.PI / 180F);
                
                // standard rotation logic for strafe(x) and forward(z)
                // forward (+z input) 应该沿着视线方向
                // strafe (+x input) 应该沿着视线左侧
                // 注意：Minecraft 的坐标系中，+Z 是南，+X 是东。Yaw 0 是南。
                // 公式：
                // x = strafe * cos - forward * sin
                // z = forward * cos + strafe * sin
                // 让我们使用 Vec3 的 yRot 方法，它绕 Y 轴旋转。
                // Vec3(x, y, z).yRot(angle) -> 逆时针旋转
                // Minecraft 的 YRot 是顺时针 (0=S, 90=W, 180=N, 270=E)
                // 所以我们需要取反
                
                Vec3 moveVec = new Vec3(inputVec.x, 0, inputVec.z).yRot(-f);
                
                // 应用速度
                targetX = moveVec.x * flySpeed;
                targetZ = moveVec.z * flySpeed;
            } else {
                // 无输入 -> 立即停止水平移动 (无惯性)
                targetX = 0;
                targetZ = 0;
            }

            // 更新垂直速度 (保持之前的逻辑)
            double targetY = 0;
            if (isShiftKeyDown) {
                // [下降] 按住 Shift -> 快速下降
                targetY = -0.4;
            } else if (isJumping) {
                // [上升] 按住 Space -> 快速上升
                targetY = 0.4;
            } else {
                // [悬停] 无按键 -> 空中悬停
                targetY = 0.0;
            }
            
            // 应用最终速度
            entity.setDeltaMovement(targetX, targetY, targetZ);

        } else {
            // ==================================================
            // 单一效果：仅鸡符咒 (Rooster Only) -> 漂浮模式
            // ==================================================
            // 经典的 "念力悬浮" 体验。
            
            if (isShiftKeyDown) {
                // [下降] 按住 Shift -> 缓慢下降
                // 速度 -0.15 比组合技慢很多，提供一种"飘落"的感觉
                entity.setDeltaMovement(currentMovement.x, -0.15, currentMovement.z);
            } else {
                // [上升] 未按 Shift -> 自动缓慢上升
                // 模拟失去重力束缚，身体不由自主地向上飘
                
                // 计算上升速度：基础速度 0.05，随等级 (amplifier) 增加
                double ascentSpeed = 0.05 * (amplifier + 1);
                
                // 限制最大上升速度，防止速度无限叠加或过快
                // 只有当当前上升速度小于目标速度时才应用，允许玩家利用外部弹射装置获得更高速度而不被立即减速
                if (currentMovement.y < ascentSpeed) {
                     entity.setDeltaMovement(currentMovement.x, ascentSpeed, currentMovement.z);
                }
            }
        }

        return true;
    }
}
