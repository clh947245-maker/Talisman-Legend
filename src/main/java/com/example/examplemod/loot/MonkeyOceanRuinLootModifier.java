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
 * Adds the monkey talisman to warm ocean ruin archaeology loot.
 */
public class MonkeyOceanRuinLootModifier extends LootModifier {
    private static final float DEFAULT_CHANCE = 0.2F;
    private static final String DEFAULT_CONFIG_KEY = ChenModLootConfig.MONKEY_OCEAN_RUIN_WARM;

    public static final MapCodec<MonkeyOceanRuinLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(MonkeyOceanRuinLootModifier::chance))
            .and(Codec.STRING.optionalFieldOf("config_key", DEFAULT_CONFIG_KEY).forGetter(MonkeyOceanRuinLootModifier::configKey))
            .apply(instance, MonkeyOceanRuinLootModifier::new));

    private final float chance;
    private final String configKey;

    public MonkeyOceanRuinLootModifier(LootItemCondition[] conditions, float chance, String configKey) {
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
            // Archaeology loot must resolve to a single item, so replace the
            // original brush result instead of appending an extra drop.
            generatedLoot.clear();
            generatedLoot.add(new ItemStack(ChenMod.MONKEY_TALISMAN.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ChenMod.MONKEY_OCEAN_RUIN_LOOT_MODIFIER.get();
    }
}
