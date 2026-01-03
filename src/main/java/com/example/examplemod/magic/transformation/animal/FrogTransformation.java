package com.example.examplemod.magic.transformation.animal;

import com.example.examplemod.magic.transformation.AbstractWaterSurvivalTransformation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.AnimationState;
import java.lang.reflect.Field;

/**
 * 青蛙形态
 * <p>
 * 特性：跳得高。
 * </p>
 */
public class FrogTransformation extends AbstractWaterSurvivalTransformation {

    private static Field frogSwimAnimField;
    private static Field frogJumpAnimField;
    private static Field frogCroakAnimField;
    private static Field frogTongueAnimField;

    static {
        try {
            frogSwimAnimField = Frog.class.getDeclaredField("swimAnimationState");
            frogSwimAnimField.setAccessible(true);
            frogJumpAnimField = Frog.class.getDeclaredField("jumpAnimationState");
            frogJumpAnimField.setAccessible(true);
            frogCroakAnimField = Frog.class.getDeclaredField("croakAnimationState");
            frogCroakAnimField.setAccessible(true);
            frogTongueAnimField = Frog.class.getDeclaredField("tongueAnimationState");
            frogTongueAnimField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public String getId() {
        return "frog";
    }

    @Override
    public EntityType<? extends LivingEntity> getEntityType() {
        return EntityType.FROG;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose, EntityDimensions original) {
        return EntityType.FROG.getDimensions();
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height() * 0.65F;
    }

    @Override
    public float getHealth() {
        return 10.0f;
    }

    @Override
    public void onTick(LivingEntity entity) {
        super.onTick(entity);
    }

    @Override
    public void onJump(LivingEntity entity) {
        // Frog Jump (Jump Boost III approx)
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.3, 0));
    }

    @Override
    public void syncAnimation(Player player, LivingEntity dummy, boolean isNewTick) {
        if (dummy instanceof Frog frog) {
            // 青蛙跳跃/游泳动画
            try {
                if (frogSwimAnimField != null && frogJumpAnimField != null) {
                    AnimationState swimAnim = (AnimationState) frogSwimAnimField.get(frog);
                    AnimationState jumpAnim = (AnimationState) frogJumpAnimField.get(frog);
                    AnimationState croakAnim = (frogCroakAnimField != null)
                            ? (AnimationState) frogCroakAnimField.get(frog)
                            : null;
                    AnimationState tongueAnim = (frogTongueAnimField != null)
                            ? (AnimationState) frogTongueAnimField.get(frog)
                            : null;

                    if (player.isSwimming()) {
                        swimAnim.startIfStopped(player.tickCount);
                        jumpAnim.stop();
                        if (croakAnim != null)
                            croakAnim.stop();
                        if (tongueAnim != null)
                            tongueAnim.stop();
                    } else if (!player.onGround() && player.getDeltaMovement().y > 0) {
                        jumpAnim.startIfStopped(player.tickCount);
                        swimAnim.stop();
                    } else {
                        swimAnim.stop();
                        jumpAnim.stop();
                    }
                }
            } catch (Exception e) {
                // ignore reflection errors
            }
        }
    }
}