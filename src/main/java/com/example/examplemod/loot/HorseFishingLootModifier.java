package com.example.examplemod.loot;

import com.example.examplemod.ChenMod;
import com.example.examplemod.config.ChenModLootConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Replaces fishing loot with the horse talisman.
 */
public class HorseFishingLootModifier extends LootModifier {
    private static final float DEFAULT_CHANCE = 0.1F;
    private static final String DEFAULT_CONFIG_KEY = ChenModLootConfig.HORSE_FISHING;

    public static final MapCodec<HorseFishingLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(HorseFishingLootModifier::chance))
            .and(Codec.STRING.optionalFieldOf("config_key", DEFAULT_CONFIG_KEY).forGetter(HorseFishingLootModifier::configKey))
            .apply(instance, HorseFishingLootModifier::new));

    private final float chance;
    private final String configKey;

    public HorseFishingLootModifier(LootItemCondition[] conditions, float chance, String configKey) {
        super(conditions);
        this.chance = chance;
        this.configKey = configKey;
    }

    private float chance() {
        return this.chance;
    }

    private String configKey() {
        return this.configKey;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        float configuredChance = ChenModLootConfig.getLootChance(this.configKey, this.chance);
        if (context.getRandom().nextFloat() < configuredChance) {
            generatedLoot.clear();
            generatedLoot.add(new ItemStack(ChenMod.HORSE_TALISMAN.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ChenMod.HORSE_FISHING_LOOT_MODIFIER.get();
    }
}
