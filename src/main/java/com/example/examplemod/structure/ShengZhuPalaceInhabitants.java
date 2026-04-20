package com.example.examplemod.structure;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.ShadowNinjaEntity;
import com.example.examplemod.entity.ShengZhuEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public final class ShengZhuPalaceInhabitants {
    private static final int MAX_CEILING_DISTANCE = 24;
    private static final BlockPos PRIMARY_BOSS_CANDIDATE = new BlockPos(40, 5, 84);
    private static final BlockPos[] BOSS_CANDIDATES = new BlockPos[] {
            PRIMARY_BOSS_CANDIDATE,
            new BlockPos(36, 5, 84),
            new BlockPos(44, 5, 84),
            new BlockPos(40, 5, 80)
    };
    private static final BlockPos[] NINJA_GUARD_CANDIDATES = new BlockPos[] {
            new BlockPos(32, 5, 21),
            new BlockPos(44, 5, 21),
            new BlockPos(30, 5, 25),
            new BlockPos(50, 5, 25),
            new BlockPos(33, 5, 43),
            new BlockPos(41, 5, 43),
            new BlockPos(33, 5, 55),
            new BlockPos(41, 5, 55),
            new BlockPos(36, 11, 100),
            new BlockPos(44, 11, 100),
            new BlockPos(34, 16, 60),
            new BlockPos(48, 16, 60),
            new BlockPos(34, 16, 76),
            new BlockPos(48, 16, 76)
    };

    private ShengZhuPalaceInhabitants() {
    }

    public static void populatePalace(ServerLevel level, BlockPos origin, Rotation rotation) {
        long palaceSeed = computePalaceSeed(level, origin, rotation);
        RandomSource random = RandomSource.create(palaceSeed ^ 0x6A09E667F3BCC909L);
        spawnBossIfMissing(level, origin, rotation, random);
        spawnGuardNinjas(level, origin, rotation, random);
    }

    public static void spawnBossIfMissing(ServerLevel level, BlockPos origin, Rotation rotation) {
        long palaceSeed = computePalaceSeed(level, origin, rotation);
        RandomSource random = RandomSource.create(palaceSeed ^ 0x6A09E667F3BCC909L);
        spawnBossIfMissing(level, origin, rotation, random);
    }

    public static ChunkPos getBossChunkPos(BlockPos origin, Rotation rotation) {
        return new ChunkPos(getPrimaryBossPos(origin, rotation));
    }

    public static boolean canShadowNinjaSpawn(EntityType<ShadowNinjaEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (isInsideShengZhuPalace(level, pos) && hasCeiling(level, pos)) {
            return Monster.checkAnyLightMonsterSpawnRules(entityType, level, spawnType, pos, random);
        }
        return Monster.checkMonsterSpawnRules(entityType, level, spawnType, pos, random);
    }

    public static boolean isInsideShengZhuPalace(ServerLevelAccessor level, BlockPos pos) {
        StructureStart structureStart = level.getLevel().structureManager().getStructureWithPieceAt(
                pos,
                holder -> holder.value().type() == ModStructures.SHENGZHU_PALACE_STRUCTURE.get()
        );
        return structureStart.isValid();
    }

    private static void spawnBossIfMissing(ServerLevel level, BlockPos origin, Rotation rotation, RandomSource random) {
        if (hasExistingBoss(level, origin, rotation)) {
            return;
        }

        ShengZhuEntity boss = ChenMod.SHENG_ZHU.get().create(level);
        if (boss == null) {
            return;
        }

        boss.setPersistenceRequired();
        BlockPos spawnPos = findBossSpawnPosition(boss, level, origin, rotation, random);
        if (spawnPos == null) {
            return;
        }

        boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(boss);
    }

    private static void spawnGuardNinjas(ServerLevel level, BlockPos origin, Rotation rotation, RandomSource random) {
        List<BlockPos> candidates = new ArrayList<>(List.of(NINJA_GUARD_CANDIDATES));
        shuffle(candidates, random);

        int guardCount = 6 + random.nextInt(3);
        for (int index = 0; index < guardCount && index < candidates.size(); index++) {
            ShadowNinjaEntity ninja = ChenMod.SHADOW_NINJA.get().create(level);
            if (ninja == null) {
                continue;
            }

            if (tryPlaceMob(ninja, level, origin, rotation, candidates.subList(index, candidates.size()), random, true)) {
                ninja.finalizeSpawn(level, level.getCurrentDifficultyAt(ninja.blockPosition()), MobSpawnType.STRUCTURE, null);
                level.addFreshEntity(ninja);
            }
        }
    }

    @Nullable
    private static BlockPos findBossSpawnPosition(Mob boss, ServerLevel level, BlockPos origin, Rotation rotation, RandomSource random) {
        for (BlockPos localPos : BOSS_CANDIDATES) {
            BlockPos worldPos = transformToWorld(origin, localPos, rotation);
            boss.moveTo(worldPos.getX() + 0.5D, worldPos.getY(), worldPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            if (canPlaceMob(level, boss, worldPos, false)) {
                return worldPos;
            }
        }

        BlockPos primaryBossPos = getPrimaryBossPos(origin, rotation);
        prepareBossSpawnArea(level, primaryBossPos, boss);
        boss.moveTo(primaryBossPos.getX() + 0.5D, primaryBossPos.getY(), primaryBossPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        return canPlaceMob(level, boss, primaryBossPos, false) ? primaryBossPos : null;
    }

    private static boolean tryPlaceMob(Mob mob, ServerLevel level, BlockPos origin, Rotation rotation, List<BlockPos> localCandidates, RandomSource random, boolean requireCeiling) {
        for (BlockPos localPos : localCandidates) {
            BlockPos worldPos = transformToWorld(origin, localPos, rotation);
            mob.moveTo(worldPos.getX() + 0.5D, worldPos.getY(), worldPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            if (canPlaceMob(level, mob, worldPos, requireCeiling)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExistingBoss(ServerLevel level, BlockPos origin, Rotation rotation) {
        BlockPos primaryBossPos = getPrimaryBossPos(origin, rotation);
        AABB searchBox = new AABB(primaryBossPos).inflate(24.0D, 12.0D, 24.0D);
        return !level.getEntitiesOfClass(
                ShengZhuEntity.class,
                searchBox,
                entity -> entity.isAlive() && isInsideShengZhuPalace(level, entity.blockPosition())
        ).isEmpty();
    }

    private static boolean canPlaceMob(ServerLevel level, Mob mob, BlockPos worldPos, boolean requireCeiling) {
        if (!level.getBlockState(worldPos.below()).blocksMotion()) {
            return false;
        }
        if (!level.getBlockState(worldPos).isAir()) {
            return false;
        }
        if (requireCeiling && !hasCeiling(level, worldPos)) {
            return false;
        }
        return level.noCollision(mob);
    }

    private static void prepareBossSpawnArea(ServerLevel level, BlockPos center, Mob boss) {
        int clearHeight = Math.max(6, (int) Math.ceil(boss.getBbHeight()) + 1);
        for (BlockPos clearPos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, clearHeight - 1, 1))) {
            if (!level.getBlockState(clearPos).isAir()) {
                level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos sidePos = center.relative(direction);
            if (!level.getBlockState(sidePos).blocksMotion()) {
                continue;
            }
            for (int offsetY = 1; offsetY <= 2; offsetY++) {
                BlockPos clearPos = sidePos.above(offsetY);
                if (!level.getBlockState(clearPos).isAir()) {
                    level.setBlock(clearPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean hasCeiling(ServerLevelAccessor level, BlockPos pos) {
        for (int offset = 2; offset <= MAX_CEILING_DISTANCE; offset++) {
            if (!level.getBlockState(pos.above(offset)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos transformToWorld(BlockPos origin, BlockPos localPos, Rotation rotation) {
        return origin.offset(StructureTemplate.transform(localPos, Mirror.NONE, rotation, BlockPos.ZERO));
    }

    private static BlockPos getPrimaryBossPos(BlockPos origin, Rotation rotation) {
        return transformToWorld(origin, PRIMARY_BOSS_CANDIDATE, rotation);
    }

    private static long computePalaceSeed(ServerLevelAccessor level, BlockPos origin, Rotation rotation) {
        long seed = level.getLevel().getSeed();
        seed ^= origin.asLong() * 0x9E3779B97F4A7C15L;
        seed ^= ((long) rotation.ordinal() + 1L) * 0x94D049BB133111EBL;
        return seed;
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            T temp = values.get(index);
            values.set(index, values.get(swapIndex));
            values.set(swapIndex, temp);
        }
    }
}
