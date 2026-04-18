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
 * 记录哪些地牢已经完成过鼠符咒生成判定。
 *
 * <p>这里保存的是刷怪笼坐标而不是箱子坐标，因为一个原版地牢可能有多个箱子，
 * 但通常只有一个核心刷怪笼。以刷怪笼为键能更稳定地表达“一个地牢最多一个鼠符咒”。</p>
 */
public class DungeonLootTrackerSavedData extends SavedData {
    private static final String DATA_NAME = ChenMod.MODID + "_dungeon_loot_tracker";
    private static final String PROCESSED_SPAWNERS_TAG = "ProcessedSpawners";
    private static final Factory<DungeonLootTrackerSavedData> FACTORY = new Factory<>(
            DungeonLootTrackerSavedData::new,
            DungeonLootTrackerSavedData::load
    );

    private final LongSet processedSpawners;

    public DungeonLootTrackerSavedData() {
        this(new LongOpenHashSet());
    }

    private DungeonLootTrackerSavedData(LongSet processedSpawners) {
        this.processedSpawners = processedSpawners;
    }

    /** 获取当前维度对应的地牢判重存档。 */
    public static DungeonLootTrackerSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private static DungeonLootTrackerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new DungeonLootTrackerSavedData(new LongOpenHashSet(tag.getLongArray(PROCESSED_SPAWNERS_TAG)));
    }

    /** 判断这个刷怪笼所属地牢是否已经做过一次鼠符咒判定。 */
    public boolean isProcessed(BlockPos spawnerPos) {
        return this.processedSpawners.contains(spawnerPos.asLong());
    }

    /** 标记该地牢已处理，并通知存档系统后续保存。 */
    public void markProcessed(BlockPos spawnerPos) {
        if (this.processedSpawners.add(spawnerPos.asLong())) {
            this.setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // 直接保存 BlockPos 的 long 表示，读写都比较轻量。
        tag.putLongArray(PROCESSED_SPAWNERS_TAG, this.processedSpawners.toLongArray());
        return tag;
    }
}
