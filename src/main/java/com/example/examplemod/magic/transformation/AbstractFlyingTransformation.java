package com.example.examplemod.magic.transformation;

import com.example.examplemod.ChenMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;

/**
 * 飞行形态的抽象基类
 * <p>
 * 为所有具有飞行能力的变身形态提供统一的飞行逻辑。
 * 包括自动赋予飞行能力，以及单键起飞功能。
 * </p>
 */
public abstract class AbstractFlyingTransformation implements ITransformation {

    private static final ResourceLocation CREATIVE_FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "transformation_flight");

    // 反射获取 LivingEntity 的 jumping 字段
    private static Field jumpingField;

    static {
        try {
            // 通过反射访问 LivingEntity 的 'jumping' 字段
            // 这允许我们在服务器端检测玩家是否按下了跳跃键
            jumpingField = LivingEntity.class.getDeclaredField("jumping");
            jumpingField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            ChenMod.LOGGER.error("在 AbstractFlyingTransformation 中无法访问 'jumping' 字段。", e);
        }
    }

    @Override
    public void onTick(LivingEntity entity) {
        if (entity instanceof Player player) {
            // 1. 始终赋予飞行能力 (类似创造模式飞行)// 2. "单键起飞" 逻辑 (按下空格键直接飞行)
            boolean isJumping = false;
            try {
                if (jumpingField != null) {
                    isJumping = jumpingField.getBoolean(player);
                }
            } catch (IllegalAccessException e) {
                // 忽略反射异常
            }

            // 如果玩家在半空中按下跳跃键 (空格)，立即激活飞行模式
            // 这样避免了原版需要双击空格才能飞行的繁琐操作
            if (isJumping && !player.onGround() && !player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }
        }
    }

    @Override
    public void onRemove(LivingEntity entity) {
        if (entity instanceof Player player) {
            // 如果玩家不是创造模式或旁观者模式，移除飞行能力
            if (!player.isCreative() && !player.isSpectator()) {player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }
}
