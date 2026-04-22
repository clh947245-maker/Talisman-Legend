package com.example.examplemod.loot;

import com.example.examplemod.ChenMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Tracks which loot sources have already consumed their mouse talisman roll.
 *
 * <p>The stored marker is usually a dungeon spawner position. When a loot table
 * does not have a nearby spawner, the chest position is used instead.</p>
 */
public class DungeonLootTrackerSavedData extends SavedData {
    private static final String DATA_NAME = ChenMod.MODID + "_dungeon_loot_tracker";
    private static final String PROCESSED_LOCATIONS_TAG = "ProcessedLocations";
    private static final String LEGACY_PROCESSED_SPAWNERS_TAG = "ProcessedSpawners";
    private static final Factory<DungeonLootTrackerSavedData> FACTORY = new Factory<>(
            DungeonLootTrackerSavedData::new,
            DungeonLootTrackerSavedData::load
    );

    private final LongSet processedLocations;

    public DungeonLootTrackerSavedData() {
        this(new LongOpenHashSet());
    }

    private DungeonLootTrackerSavedData(LongSet processedLocations) {
        this.processedLocations = processedLocations;
    }

    public static DungeonLootTrackerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static DungeonLootTrackerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LongSet processedLocations = new LongOpenHashSet(tag.getLongArray(PROCESSED_LOCATIONS_TAG));
        for (long legacyPos : tag.getLongArray(LEGACY_PROCESSED_SPAWNERS_TAG)) {
            processedLocations.add(legacyPos);
        }
        return new DungeonLootTrackerSavedData(processedLocations);
    }

    public boolean isProcessed(BlockPos markerPos) {
        return this.processedLocations.contains(markerPos.asLong());
    }

    public void markProcessed(BlockPos markerPos) {
        if (this.processedLocations.add(markerPos.asLong())) {
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray(PROCESSED_LOCATIONS_TAG, this.processedLocations.toLongArray());
        return tag;
    }
}
