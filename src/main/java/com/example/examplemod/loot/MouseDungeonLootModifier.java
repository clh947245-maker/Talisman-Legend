package com.example.examplemod.loot;

import com.example.examplemod.ChenMod;
import com.example.examplemod.config.ChenModLootConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Adds the mouse talisman to selected chest loot tables.
 *
 * <p>Vanilla dungeon chests often belong to the same room and should only be
 * evaluated once, so nearby spawners are used as the stable marker when one is
 * present. For other chest types such as mineshafts and strongholds, the chest
 * position itself is used as the marker so those loot tables can participate
 * too.</p>
 */
public class MouseDungeonLootModifier extends LootModifier {
    private static final float DEFAULT_CHANCE = 0.05F;
    private static final int DEFAULT_HORIZONTAL_RANGE = 8;
    private static final int DEFAULT_VERTICAL_RANGE = 5;
    private static final String DEFAULT_CONFIG_KEY = ChenModLootConfig.MOUSE_SIMPLE_DUNGEON;

    public static final MapCodec<MouseDungeonLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(MouseDungeonLootModifier::chance))
            .and(Codec.INT.optionalFieldOf("horizontal_range", DEFAULT_HORIZONTAL_RANGE).forGetter(MouseDungeonLootModifier::horizontalRange))
            .and(Codec.INT.optionalFieldOf("vertical_range", DEFAULT_VERTICAL_RANGE).forGetter(MouseDungeonLootModifier::verticalRange))
            .and(Codec.STRING.optionalFieldOf("config_key", DEFAULT_CONFIG_KEY).forGetter(MouseDungeonLootModifier::configKey))
            .apply(instance, MouseDungeonLootModifier::new));

    private final float chance;
    private final int horizontalRange;
    private final int verticalRange;
    private final String configKey;

    public MouseDungeonLootModifier(LootItemCondition[] conditions, float chance, int horizontalRange, int verticalRange, String configKey) {
        super(conditions);
        this.chance = chance;
        this.horizontalRange = Math.max(1, horizontalRange);
        this.verticalRange = Math.max(1, verticalRange);
        this.configKey = configKey;
    }

    private float chance() {
        return this.chance;
    }

    private int horizontalRange() {
        return this.horizontalRange;
    }

    private int verticalRange() {
        return this.verticalRange;
    }

    private String configKey() {
        return this.configKey;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!context.hasParam(LootContextParams.ORIGIN)) {
            return generatedLoot;
        }

        BlockPos chestPos = BlockPos.containing(context.getParam(LootContextParams.ORIGIN));
        BlockPos spawnerPos = BlockPos.findClosestMatch(
                chestPos,
                this.horizontalRange,
                this.verticalRange,
                candidate -> context.getLevel().getBlockState(candidate).is(Blocks.SPAWNER)
        ).orElse(null);

        BlockPos lootMarkerPos = spawnerPos != null ? spawnerPos : chestPos;
        DungeonLootTrackerSavedData tracker = DungeonLootTrackerSavedData.get(context.getLevel());
        if (tracker.isProcessed(lootMarkerPos)) {
            return generatedLoot;
        }

        tracker.markProcessed(lootMarkerPos);
        float configuredChance = ChenModLootConfig.getLootChance(this.configKey, this.chance);
        if (context.getRandom().nextFloat() < configuredChance) {
            generatedLoot.add(new ItemStack(ChenMod.MOUSE_TALISMAN.get()));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ChenMod.MOUSE_DUNGEON_LOOT_MODIFIER.get();
    }
}
