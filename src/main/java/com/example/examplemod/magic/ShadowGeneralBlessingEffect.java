package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ShadowGeneralBlessingEffect extends MobEffect {
    private static final double DOUBLE_STAT_AMOUNT = 1.0D;

    public ShadowGeneralBlessingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4A0F1F);

        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shadow_general_blessing_speed"),
                DOUBLE_STAT_AMOUNT,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shadow_general_blessing_attack_damage"),
                DOUBLE_STAT_AMOUNT,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
                Attributes.ATTACK_SPEED,
                ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shadow_general_blessing_attack_speed"),
                DOUBLE_STAT_AMOUNT,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
                Attributes.ARMOR,
                ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shadow_general_blessing_armor"),
                DOUBLE_STAT_AMOUNT,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        this.addAttributeModifier(
                Attributes.ARMOR_TOUGHNESS,
                ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shadow_general_blessing_armor_toughness"),
                DOUBLE_STAT_AMOUNT,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
