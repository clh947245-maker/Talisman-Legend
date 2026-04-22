package com.example.examplemod.world;

import com.example.examplemod.ChenMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class VillageFusionTrackerSavedData extends SavedData {
    private static final String DATA_NAME = ChenMod.MODID + "_village_fusion_tracker";
    private static final String PROCESSED_VILLAGE_STARTS_TAG = "ProcessedVillageStarts";
    private static final Factory<VillageFusionTrackerSavedData> FACTORY = new Factory<>(
            VillageFusionTrackerSavedData::new,
            VillageFusionTrackerSavedData::load
    );

    private final LongSet processedVillageStarts;

    public VillageFusionTrackerSavedData() {
        this(new LongOpenHashSet());
    }

    private VillageFusionTrackerSavedData(LongSet processedVillageStarts) {
        this.processedVillageStarts = processedVillageStarts;
    }

    public static VillageFusionTrackerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static VillageFusionTrackerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new VillageFusionTrackerSavedData(new LongOpenHashSet(tag.getLongArray(PROCESSED_VILLAGE_STARTS_TAG)));
    }

    public boolean isProcessed(long villageStartChunk) {
        return this.processedVillageStarts.contains(villageStartChunk);
    }

    public void markProcessed(long villageStartChunk) {
        if (this.processedVillageStarts.add(villageStartChunk)) {
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray(PROCESSED_VILLAGE_STARTS_TAG, this.processedVillageStarts.toLongArray());
        return tag;
    }
}
