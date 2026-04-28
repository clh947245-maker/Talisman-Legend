package com.example.examplemod.world;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.AiboMoDiCaiFusionEntity;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;

@EventBusSubscriber(modid = ChenMod.MODID)
public final class VillageFusionSpawnHandler {
    private static final Map<ServerLevel, LongSet> PENDING_VILLAGE_STARTS = new IdentityHashMap<>();
    private static final int MAX_VILLAGES_PER_TICK = 4;
    private static final int VILLAGE_ENTITY_SCAN_PADDING = 24;
    private static final float VILLAGE_FUSION_SPAWN_CHANCE = 0.30F;

    private VillageFusionSpawnHandler() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        if (!(chunk instanceof LevelChunk)) {
            return;
        }

        Registry<Structure> registry = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE);
        LongSet pendingStarts = PENDING_VILLAGE_STARTS.computeIfAbsent(serverLevel, ignored -> new LongOpenHashSet());
        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            StructureStart start = entry.getValue();
            if (!start.isValid() || !registry.wrapAsHolder(entry.getKey()).is(StructureTags.VILLAGE)) {
                continue;
            }
            pendingStarts.add(start.getChunkPos().toLong());
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        LongSet pendingStarts = PENDING_VILLAGE_STARTS.get(serverLevel);
        if (pendingStarts == null || pendingStarts.isEmpty()) {
            return;
        }

        VillageFusionTrackerSavedData tracker = VillageFusionTrackerSavedData.get(serverLevel);
        int processedThisTick = 0;
        LongIterator iterator = pendingStarts.iterator();
        while (iterator.hasNext() && processedThisTick < MAX_VILLAGES_PER_TICK) {
            long villageStartChunk = iterator.nextLong();
            if (tracker.isProcessed(villageStartChunk)) {
                iterator.remove();
                continue;
            }

            if (thisTickTrySpawnVillageFusion(serverLevel, villageStartChunk, tracker)) {
                iterator.remove();
            }
            processedThisTick++;
        }

        if (pendingStarts.isEmpty()) {
            PENDING_VILLAGE_STARTS.remove(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            PENDING_VILLAGE_STARTS.remove(serverLevel);
        }
    }

    private static boolean thisTickTrySpawnVillageFusion(
            ServerLevel level,
            long villageStartChunk,
            VillageFusionTrackerSavedData tracker) {
        ChunkPos startChunkPos = new ChunkPos(villageStartChunk);
        LevelChunk startChunk = level.getChunkSource().getChunkNow(startChunkPos.x, startChunkPos.z);
        if (startChunk == null) {
            return false;
        }

        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<StructureStart> villageStart = startChunk.getAllStarts().entrySet().stream()
                .filter(entry -> registry.wrapAsHolder(entry.getKey()).is(StructureTags.VILLAGE))
                .map(Map.Entry::getValue)
                .filter(StructureStart::isValid)
                .filter(start -> start.getChunkPos().toLong() == villageStartChunk)
                .findFirst();
        if (villageStart.isEmpty()) {
            tracker.markProcessed(villageStartChunk);
            return true;
        }

        if (!shouldVillageSpawnFusion(level, villageStartChunk)) {
            tracker.markProcessed(villageStartChunk);
            return true;
        }

        BoundingBox villageBounds = villageStart.get().getBoundingBox();
        if (hasExistingVillageFusion(level, villageBounds)) {
            tracker.markProcessed(villageStartChunk);
            return true;
        }

        BlockPos spawnPos = findVillageSpawnPos(level, villageBounds, level.random);
        if (spawnPos == null) {
            return false;
        }

        AiboMoDiCaiFusionEntity fusion = ChenMod.AIBO_MO_DI_CAI_FUSION.get().create(level);
        if (fusion == null) {
            tracker.markProcessed(villageStartChunk);
            return true;
        }

        fusion.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        if (!fusion.checkSpawnObstruction(level)) {
            return false;
        }

        fusion.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(fusion);
        tracker.markProcessed(villageStartChunk);
        return true;
    }

    private static boolean hasExistingVillageFusion(ServerLevel level, BoundingBox villageBounds) {
        AABB scanBox = new AABB(
                villageBounds.minX() - VILLAGE_ENTITY_SCAN_PADDING,
                level.getMinBuildHeight(),
                villageBounds.minZ() - VILLAGE_ENTITY_SCAN_PADDING,
                villageBounds.maxX() + VILLAGE_ENTITY_SCAN_PADDING + 1.0D,
                level.getMaxBuildHeight(),
                villageBounds.maxZ() + VILLAGE_ENTITY_SCAN_PADDING + 1.0D);
        return !level.getEntitiesOfClass(AiboMoDiCaiFusionEntity.class, scanBox).isEmpty();
    }

    private static boolean shouldVillageSpawnFusion(ServerLevel level, long villageStartChunk) {
        long mixedSeed = level.getSeed() ^ (villageStartChunk * 0x9E3779B97F4A7C15L);
        return RandomSource.create(mixedSeed).nextFloat() < VILLAGE_FUSION_SPAWN_CHANCE;
    }

    private static BlockPos findVillageSpawnPos(ServerLevel level, BoundingBox villageBounds, RandomSource random) {
        BlockPos center = villageBounds.getCenter();
        BlockPos centerSurface = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(center.getX(), center.getY(), center.getZ()));
        if (isValidVillageSpawnPos(level, centerSurface, random)) {
            return centerSurface;
        }

        int minX = villageBounds.minX();
        int maxX = villageBounds.maxX();
        int minZ = villageBounds.minZ();
        int maxZ = villageBounds.maxZ();
        for (int attempt = 0; attempt < 32; attempt++) {
            int sampleX = Mth.nextInt(random, minX, maxX);
            int sampleZ = Mth.nextInt(random, minZ, maxZ);
            BlockPos surfacePos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(sampleX, center.getY(), sampleZ));
            if (isValidVillageSpawnPos(level, surfacePos, random)) {
                return surfacePos;
            }
        }
        return null;
    }

    private static boolean isValidVillageSpawnPos(ServerLevel level, BlockPos pos, RandomSource random) {
        return level.isVillage(pos)
                && SpawnPlacements.isSpawnPositionOk(ChenMod.AIBO_MO_DI_CAI_FUSION.get(), level, pos)
                && SpawnPlacements.checkSpawnRules(ChenMod.AIBO_MO_DI_CAI_FUSION.get(), level, MobSpawnType.STRUCTURE, pos, random);
    }
}
