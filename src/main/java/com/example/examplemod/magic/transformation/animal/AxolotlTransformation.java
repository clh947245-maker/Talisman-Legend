package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterSurvivalTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Field;

/**
 * 美西螈形态
 * <p>
 * 特性：在水中获得生命恢复。
 * </p>
 */
public class AxolotlTransformation extends AbstractWaterSurvivalTransformation {

    private static Field wasTouchingWaterField;

    static {
        try {
            wasTouchingWaterField = net.minecraft.world.entity.Entity.class.getDeclaredField("wasTouchingWater");
            wasTouchingWaterField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String getId() {
        return "axolotl";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.AXOLOTL;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.AXOLOTL.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.65F;
    }

    @Override
    public float getHealth() {
        return 14.0f; // 7 hearts
    }

    @Override
    public void onTick(LivingEntity entity) {
        super.onTick(entity);
        
        // 在水中获得生命恢复 (Regeneration I: 1 HP per 2.5s = 50 ticks)
        if (entity.isInWater() && entity.tickCount % 50 == 0) {
            entity.heal(1.0f);
        }
    }

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Axolotl axolotl) {
            // 美西螈动画处理
            
            // 1. 同步水中状态 (决定是游泳还是爬行)
            // AxolotlModel 会检查 entity.isInWater() 和 entity.onGround()
            // 如果在水中 -> 游泳动画
            // 如果在地面 -> 爬行动画
            try {
                if (wasTouchingWaterField != null) {
                    // 只有当玩家在游泳或者完全在水中时，才视为在水中，否则视为在地面
                    wasTouchingWaterField.setBoolean(axolotl, player.isInWater());
                }
            } catch (Exception e) {
                // ignore
            }
            
            // 2. 同步地面状态
            axolotl.setOnGround(player.onGround());
            
            // 3. 俯仰角同步 (仅游泳时)
            // 在陆地上爬行时，身体应该保持水平
            if (player.isInWater() || player.isSwimming()) {
                 axolotl.setXRot(player.getXRot());
                 axolotl.xRotO = player.xRotO;
                 // 确保在水中不使用 onGround，否则可能导致动画冲突
                 axolotl.setOnGround(false);
            } else {
                 // 陆地上重置俯仰角
                 axolotl.setXRot(0);
                 axolotl.xRotO = 0;
            }
            
            // 4. 速度同步 (已经在 syncEntityData 开头处理了 setDeltaMovement)
             // AxolotlModel 使用 speed 来计算腿部摆动
             
             // 关键修复：Axolotl 在陆地上需要 walkAnimation 的 speed > 0 才会播放爬行动画
             // 原版 Axolotl 在陆地上的动画是基于 walkAnimation.position() 和 speed()
             // 我们已经同步了 walkAnimation，但是 AxolotlModel 可能还需要 swingTime 或者其他状态
             
             // 强制更新 walkAnimation 状态，确保在陆地上有足够的 movement
             if (!player.isInWater() && player.walkAnimation.speed() > 0.01f) {
                  // 稍微放大一点 speed 以确保动画可见
                  axolotl.walkAnimation.setSpeed(player.walkAnimation.speed() * 1.5f);
              }
        }
    }
}
