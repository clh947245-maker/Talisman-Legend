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
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Adds the rabbit talisman to turtle drops.
 */
public class RabbitTurtleLootModifier extends LootModifier {
    private static final float DEFAULT_CHANCE = 0.8F;
    private static final String DEFAULT_CONFIG_KEY = ChenModLootConfig.RABBIT_TURTLE;

    public static final MapCodec<RabbitTurtleLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(RabbitTurtleLootModifier::chance))
            .and(Codec.STRING.optionalFieldOf("config_key", DEFAULT_CONFIG_KEY).forGetter(RabbitTurtleLootModifier::configKey))
            .apply(instance, RabbitTurtleLootModifier::new));

    private final float chance;
    private final String configKey;

    public RabbitTurtleLootModifier(LootItemCondition[] conditions, float chance, String configKey) {
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
            generatedLoot.add(new ItemStack(ChenMod.RABBIT_TALISMAN.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ChenMod.RABBIT_TURTLE_LOOT_MODIFIER.get();
    }
}
