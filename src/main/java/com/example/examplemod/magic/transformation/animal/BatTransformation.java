package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractFlyingTransformation;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import java.lang.reflect.Field;

/**
 * 蝙蝠形态
 */
public class BatTransformation extends AbstractFlyingTransformation {

    private static Field batFlyAnimField;
    private static Field batRestAnimField;

    static {
        try {
            batFlyAnimField = Bat.class.getDeclaredField("flyAnimationState");
            batFlyAnimField.setAccessible(true);
            batRestAnimField = Bat.class.getDeclaredField("restAnimationState");
            batRestAnimField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String getId() {
        return "bat";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.BAT;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.BAT.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.6F; // Bat eye height is low
    }

    @Override
    public float getHealth() {
        return 6.0f;
    }

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Bat bat) {
            // 蝙蝠飞行逻辑
            bat.setResting(false);

            // 必须手动启动动画状态，因为 dummy 实体不会执行 tick()
            try {
                if (batRestAnimField != null && batFlyAnimField != null) {
                    AnimationState restAnim = (AnimationState) batRestAnimField.get(bat);
                    AnimationState flyAnim = (AnimationState) batFlyAnimField.get(bat);
                    
                    restAnim.stop();
                    flyAnim.startIfStopped(player.tickCount);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
