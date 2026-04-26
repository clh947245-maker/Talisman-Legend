package com.example.examplemod.world;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.ShadowNinjaEntity;
import com.example.examplemod.entity.ShengZhuEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = ChenMod.MODID)
public final class ShengZhuPalaceShadowNinjaSpawnHandler {
    private static final ResourceKey<Structure> SHENG_ZHU_PALACE_KEY = ResourceKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "sheng_zhu_palace"));
    private static final int SPAWN_INTERVAL_TICKS = 20 * 6;
    private static final float SPAWN_CHANCE = 0.75F;
    private static final int MAX_NINJAS_PER_PALACE = 20;
    private static final int SPAWN_ATTEMPTS = 32;
    private static final int MIN_PLAYER_DISTANCE = 18;
    private static final int MAX_PLAYER_DISTANCE = 42;
    private static final int ENTITY_SCAN_VERTICAL_PADDING = 24;

    private ShengZhuPalaceShadowNinjaSpawnHandler() {
    }

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getEntityType() != ChenMod.SHADOW_NINJA.get() && isInsidePalace(event.getLevel(), event.getPos())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity().getType() != ChenMod.SHADOW_NINJA.get()
                && isInsidePalace(event.getLevel(), BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getDifficulty() == Difficulty.PEACEFUL
                || level.getGameTime() % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }

        LongSet processedPalaces = new LongOpenHashSet();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || level.random.nextFloat() > SPAWN_CHANCE) {
                continue;
            }

            StructureStart palaceStart = getPalaceStart(level, player.blockPosition());
            if (!palaceStart.isValid() || !processedPalaces.add(palaceStart.getChunkPos().toLong())) {
                continue;
            }

            trySpawnNinja(level, player, palaceStart, level.random);
        }
    }

    private static boolean isInsidePalace(ServerLevelAccessor level, BlockPos pos) {
        return getPalaceStart(level, pos).isValid();
    }

    private static StructureStart getPalaceStart(ServerLevelAccessor level, BlockPos pos) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> palace = registry.getHolder(SHENG_ZHU_PALACE_KEY);
        return palace.map(holder -> level.getLevel().structureManager().getStructureWithPieceAt(pos, holder.value()))
                .orElse(StructureStart.INVALID_START);
    }

    private static void trySpawnNinja(ServerLevel level, ServerPlayer player, StructureStart palaceStart, RandomSource random) {
        BoundingBox bounds = palaceStart.getBoundingBox();
        AABB scanBox = new AABB(
                bounds.minX(),
                Math.max(level.getMinBuildHeight(), bounds.minY() - ENTITY_SCAN_VERTICAL_PADDING),
                bounds.minZ(),
                bounds.maxX() + 1.0D,
                Math.min(level.getMaxBuildHeight(), bounds.maxY() + ENTITY_SCAN_VERTICAL_PADDING) + 1.0D,
                bounds.maxZ() + 1.0D);
        if (level.getEntitiesOfClass(ShadowNinjaEntity.class, scanBox, ShadowNinjaEntity::isAlive).size() >= MAX_NINJAS_PER_PALACE) {
            return;
        }

        BlockPos spawnPos = findSpawnPos(level, player, bounds, random);
        if (spawnPos == null) {
            return;
        }

        ShadowNinjaEntity ninja = ChenMod.SHADOW_NINJA.get().create(level);
        if (ninja == null) {
            return;
        }

        ninja.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        if (!ninja.checkSpawnObstruction(level)) {
            return;
        }

        ShengZhuEntity palaceLord = findPalaceLord(level, scanBox);
        if (palaceLord != null) {
            ninja.assignLegionSummoner(palaceLord);
        }

        ninja.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(ninja);
    }

    private static BlockPos findSpawnPos(
            ServerLevel level,
            ServerPlayer player,
            BoundingBox bounds,
            RandomSource random) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = Mth.nextInt(random, MIN_PLAYER_DISTANCE, MAX_PLAYER_DISTANCE);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (x < bounds.minX() || x > bounds.maxX() || z < bounds.minZ() || z > bounds.maxZ()) {
                continue;
            }

            BlockPos surfacePos = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, player.blockPosition().getY(), z));
            if (isValidSpawnPos(level, surfacePos)) {
                return surfacePos;
            }
        }
        return null;
    }

    private static boolean isValidSpawnPos(ServerLevel level, BlockPos pos) {
        return getPalaceStart(level, pos).isValid()
                && SpawnPlacements.isSpawnPositionOk(ChenMod.SHADOW_NINJA.get(), level, pos)
                && level.getBlockState(pos.below()).blocksMotion()
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private static ShengZhuEntity findPalaceLord(ServerLevel level, AABB scanBox) {
        return level.getEntitiesOfClass(ShengZhuEntity.class, scanBox, ShengZhuEntity::isAlive)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
