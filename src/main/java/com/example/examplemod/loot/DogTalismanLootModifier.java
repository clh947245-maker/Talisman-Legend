package com.example.examplemod.loot;

import com.example.examplemod.ChenMod;
import com.example.examplemod.config.ChenModLootConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Handles dog talisman drops from evokers and tamed wolves.
 */
public class DogTalismanLootModifier extends LootModifier {
    private static final float DEFAULT_CHANCE = 0.8F;
    private static final String DEFAULT_CONFIG_KEY = ChenModLootConfig.DOG_EVOKER;

    public static final MapCodec<DogTalismanLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(DogTalismanLootModifier::chance))
            .and(Codec.STRING.optionalFieldOf("config_key", DEFAULT_CONFIG_KEY).forGetter(DogTalismanLootModifier::configKey))
            .and(Codec.BOOL.optionalFieldOf("replace_totem", false).forGetter(DogTalismanLootModifier::replaceTotem))
            .and(Codec.BOOL.optionalFieldOf("require_tamed", false).forGetter(DogTalismanLootModifier::requireTamed))
            .apply(instance, DogTalismanLootModifier::new));

    private final float chance;
    private final String configKey;
    private final boolean replaceTotem;
    private final boolean requireTamed;

    public DogTalismanLootModifier(LootItemCondition[] conditions, float chance, String configKey, boolean replaceTotem, boolean requireTamed) {
        super(conditions);
        this.chance = chance;
        this.configKey = configKey;
        this.replaceTotem = replaceTotem;
        this.requireTamed = requireTamed;
    }

    private float chance() {
        return this.chance;
    }

    private String configKey() {
        return this.configKey;
    }

    private boolean replaceTotem() {
        return this.replaceTotem;
    }

    private boolean requireTamed() {
        return this.requireTamed;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (this.requireTamed && !isTamedDeath(context)) {
            return generatedLoot;
        }

        float configuredChance = ChenModLootConfig.getLootChance(this.configKey, this.chance);
        if (context.getRandom().nextFloat() >= configuredChance) {
            return generatedLoot;
        }

        ItemStack dogTalisman = new ItemStack(ChenMod.DOG_TALISMAN.get());
        if (this.replaceTotem) {
            replaceOneTotem(generatedLoot, dogTalisman);
            return generatedLoot;
        }

        generatedLoot.add(dogTalisman);
        return generatedLoot;
    }

    private static boolean isTamedDeath(LootContext context) {
        if (!context.hasParam(LootContextParams.THIS_ENTITY)) {
            return false;
        }

        Entity entity = context.getParam(LootContextParams.THIS_ENTITY);
        return entity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame();
    }

    private static boolean replaceOneTotem(ObjectArrayList<ItemStack> generatedLoot, ItemStack replacement) {
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                if (stack.getCount() <= 1) {
                    generatedLoot.set(i, replacement);
                } else {
                    stack.shrink(1);
                    generatedLoot.add(replacement);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ChenMod.DOG_TALISMAN_LOOT_MODIFIER.get();
    }
}
