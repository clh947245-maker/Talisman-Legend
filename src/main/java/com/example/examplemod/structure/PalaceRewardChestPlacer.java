package com.example.examplemod.structure;

import com.example.examplemod.ChenMod;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;

final class PalaceRewardChestPlacer {
    private static final ResourceKey<LootTable> BUILDING_REWARD_LOOT = lootTable("chests/palace_building_reward");
    private static final ResourceKey<LootTable> LINGXIAO_REWARD_LOOT = lootTable("chests/lingxiao_tower_reward");
    private static final ResourceKey<LootTable> AUXILIARY_LOOT = lootTable("chests/palace_auxiliary_common");
    private static final int LINGXIAO_FLOOR_HEIGHT = 7;

    private PalaceRewardChestPlacer() {
    }

    static void placeTemplateRewardChests(
            ServerLevelAccessor level,
            StructureTemplate template,
            BlockPos origin,
            BuildingConstructorItem.BuildingVariant variant,
            RandomSource random) {
        placeTemplateRewardChests(level, template, origin, variant, random, null);
    }

    static void placeTemplateRewardChests(
            ServerLevelAccessor level,
            StructureTemplate template,
            BlockPos origin,
            BuildingConstructorItem.BuildingVariant variant,
            RandomSource random,
            BoundingBox chunkBox) {
        if (variant == null) {
            return;
        }

        Vec3i size = template.getSize();
        BoundingBox area = templateBox(origin, size);
        if (usesTemplateRewardChests(variant)) {
            clearTemplateContainers(level, area, chunkBox);
        }

        List<Candidate> candidates = collectTemplateCandidates(level, template, origin, size);
        if (candidates.isEmpty()) {
            return;
        }

        int count;
        ResourceKey<LootTable> lootTable;
        if (variant == BuildingConstructorItem.BuildingVariant.LINGXIAO_TOWER) {
            count = 2 + random.nextInt(4);
            lootTable = LINGXIAO_REWARD_LOOT;
        } else {
            count = 1 + random.nextInt(2);
            lootTable = BUILDING_REWARD_LOOT;
        }

        RandomSource selectionRandom = RandomSource.create(selectionSeed(origin, variant));
        placeSelectedChests(
                level,
                candidates,
                count,
                lootTable,
                random,
                selectionRandom,
                variant == BuildingConstructorItem.BuildingVariant.LINGXIAO_TOWER,
                chunkBox
        );
    }

    static void placeAuxiliaryCommonChest(ServerLevelAccessor level, BoundingBox area, RandomSource random) {
        if (random.nextBoolean()) {
            return;
        }

        List<Candidate> candidates = collectBoxCandidates(level, area);
        if (candidates.isEmpty()) {
            return;
        }

        placeSelectedChests(level, candidates, 1, AUXILIARY_LOOT, random, random, false, null);
    }

    private static List<Candidate> collectTemplateCandidates(
            ServerLevelAccessor level,
            StructureTemplate template,
            BlockPos origin,
            Vec3i size) {
        Map<BlockPos, BlockState> templateBlocks = readTemplateBlocks(level, template);
        List<Candidate> candidates = new ArrayList<>();
        int edgePaddingX = Math.max(1, size.getX() / 10);
        int edgePaddingZ = Math.max(1, size.getZ() / 10);
        int minX = edgePaddingX;
        int maxX = size.getX() - 1 - edgePaddingX;
        int minZ = edgePaddingZ;
        int maxZ = size.getZ() - 1 - edgePaddingZ;
        if (minX > maxX || minZ > maxZ) {
            minX = 0;
            maxX = size.getX() - 1;
            minZ = 0;
            maxZ = size.getZ() - 1;
        }

        for (int y = 1; y < size.getY() - 1; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    BlockState state = templateBlocks.getOrDefault(local, Blocks.AIR.defaultBlockState());
                    BlockState above = templateBlocks.getOrDefault(local.above(), Blocks.AIR.defaultBlockState());
                    BlockState below = templateBlocks.getOrDefault(local.below(), Blocks.AIR.defaultBlockState());
                    if (isTemplateChestBase(state, above, below)) {
                        candidates.add(new Candidate(origin.offset(local), y, interiorPriority(templateBlocks, local, size)));
                    }
                }
            }
        }
        return candidates;
    }

    private static BoundingBox templateBox(BlockPos origin, Vec3i size) {
        return new BoundingBox(
                origin.getX(),
                origin.getY(),
                origin.getZ(),
                origin.getX() + Math.max(0, size.getX() - 1),
                origin.getY() + Math.max(0, size.getY() - 1),
                origin.getZ() + Math.max(0, size.getZ() - 1)
        );
    }

    private static List<Candidate> collectBoxCandidates(ServerLevelAccessor level, BoundingBox area) {
        return collectCandidates(level, area, 1, 1);
    }

    private static List<Candidate> collectCandidates(ServerLevelAccessor level, BoundingBox area, int edgePaddingX, int edgePaddingZ) {
        List<Candidate> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = area.minX() + edgePaddingX;
        int maxX = area.maxX() - edgePaddingX;
        int minZ = area.minZ() + edgePaddingZ;
        int maxZ = area.maxZ() - edgePaddingZ;
        if (minX > maxX || minZ > maxZ) {
            minX = area.minX();
            maxX = area.maxX();
            minZ = area.minZ();
            maxZ = area.maxZ();
        }

        for (int y = area.minY() + 1; y < area.maxY(); y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (canPlaceChestAt(level, pos)) {
                        candidates.add(new Candidate(pos.immutable(), y - area.minY(), 0));
                    }
                }
            }
        }
        return candidates;
    }

    private static boolean canPlaceChestAt(ServerLevelAccessor level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        return !floor.isAir() && floor.isFaceSturdy(level, below, Direction.UP);
    }

    private static void placeSelectedChests(
            ServerLevelAccessor level,
            List<Candidate> candidates,
            int count,
            ResourceKey<LootTable> lootTable,
            RandomSource random,
            RandomSource selectionRandom,
            boolean onePerFloor,
            BoundingBox chunkBox) {
        shuffle(candidates, selectionRandom);
        candidates.sort(Comparator.comparingInt(Candidate::priority).reversed());
        Set<Integer> usedFloors = new HashSet<>();
        int placed = 0;
        for (Candidate candidate : candidates) {
            if (placed >= count) {
                return;
            }

            int floor = candidate.localY() / LINGXIAO_FLOOR_HEIGHT;
            if (onePerFloor && !usedFloors.add(floor)) {
                continue;
            }

            if (chunkBox != null && !chunkBox.isInside(candidate.pos())) {
                placed++;
                continue;
            }

            if (placeChest(level, candidate.pos(), lootTable, random)) {
                placed++;
            } else if (onePerFloor) {
                usedFloors.remove(floor);
            }
        }
    }

    private static boolean placeChest(ServerLevelAccessor level, BlockPos pos, ResourceKey<LootTable> lootTable, RandomSource random) {
        if (!canPlaceChestAt(level, pos)) {
            return false;
        }

        BlockState chest = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, chooseFacing(level, pos));
        level.setBlock(pos, chest, 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, pos, lootTable);
        return true;
    }

    private static boolean isTemplateChestBase(BlockState state, BlockState above, BlockState below) {
        return state.isAir()
                && above.isAir()
                && !below.isAir()
                && below.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, Direction.UP);
    }

    private static int interiorPriority(Map<BlockPos, BlockState> templateBlocks, BlockPos local, Vec3i size) {
        boolean roofed = hasRoofAbove(templateBlocks, local, size);
        int enclosure = enclosureScore(templateBlocks, local, size);
        if (roofed && enclosure >= 2) {
            return 3;
        }
        if (roofed && enclosure >= 1) {
            return 2;
        }
        if (roofed) {
            return 1;
        }
        return 0;
    }

    private static boolean hasRoofAbove(Map<BlockPos, BlockState> templateBlocks, BlockPos local, Vec3i size) {
        int maxY = Math.min(size.getY() - 1, local.getY() + 12);
        for (int y = local.getY() + 2; y <= maxY; y++) {
            BlockState state = templateBlocks.getOrDefault(new BlockPos(local.getX(), y, local.getZ()), Blocks.AIR.defaultBlockState());
            if (!state.isAir()) {
                return true;
            }
        }
        return false;
    }

    private static int enclosureScore(Map<BlockPos, BlockState> templateBlocks, BlockPos local, Vec3i size) {
        int score = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (hasSideStructure(templateBlocks, local, size, direction)) {
                score++;
            }
        }
        return score;
    }

    private static boolean hasSideStructure(Map<BlockPos, BlockState> templateBlocks, BlockPos local, Vec3i size, Direction direction) {
        for (int distance = 1; distance <= 6; distance++) {
            int x = local.getX() + direction.getStepX() * distance;
            int z = local.getZ() + direction.getStepZ() * distance;
            if (x < 0 || x >= size.getX() || z < 0 || z >= size.getZ()) {
                return false;
            }

            for (int dy = 0; dy <= 2; dy++) {
                BlockState state = templateBlocks.getOrDefault(new BlockPos(x, local.getY() + dy, z), Blocks.AIR.defaultBlockState());
                if (!state.isAir()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<BlockPos, BlockState> readTemplateBlocks(ServerLevelAccessor level, StructureTemplate template) {
        CompoundTag saved = template.save(new CompoundTag());
        ListTag palette = saved.getList("palette", 10);
        ListTag blocks = saved.getList("blocks", 10);
        HolderGetter<Block> blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        List<BlockState> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            states.add(NbtUtils.readBlockState(blockLookup, palette.getCompound(i)));
        }

        Map<BlockPos, BlockState> templateBlocks = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockTag = blocks.getCompound(i);
            ListTag posTag = blockTag.getList("pos", 3);
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= states.size()) {
                continue;
            }

            templateBlocks.put(
                    new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2)),
                    states.get(stateIndex)
            );
        }
        return templateBlocks;
    }

    private static long selectionSeed(BlockPos origin, BuildingConstructorItem.BuildingVariant variant) {
        long seed = origin.asLong();
        seed ^= (long)variant.ordinal() * 0x9E3779B97F4A7C15L;
        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        return seed;
    }

    private static boolean usesTemplateRewardChests(BuildingConstructorItem.BuildingVariant variant) {
        return variant == BuildingConstructorItem.BuildingVariant.CHONGHUA_GATE
                || variant == BuildingConstructorItem.BuildingVariant.FENGMING_GATE_TOWER;
    }

    private static void clearTemplateContainers(ServerLevelAccessor level, BoundingBox area, BoundingBox chunkBox) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunkBox == null ? area.minX() : Math.max(area.minX(), chunkBox.minX());
        int maxX = chunkBox == null ? area.maxX() : Math.min(area.maxX(), chunkBox.maxX());
        int minY = chunkBox == null ? area.minY() : Math.max(area.minY(), chunkBox.minY());
        int maxY = chunkBox == null ? area.maxY() : Math.min(area.maxY(), chunkBox.maxY());
        int minZ = chunkBox == null ? area.minZ() : Math.max(area.minZ(), chunkBox.minZ());
        int maxZ = chunkBox == null ? area.maxZ() : Math.min(area.maxZ(), chunkBox.maxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof ChestBlock
                            || state.getBlock() instanceof TrappedChestBlock
                            || state.getBlock() instanceof BarrelBlock) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static Direction chooseFacing(ServerLevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return direction.getOpposite();
            }
        }
        return Direction.NORTH;
    }

    private static void shuffle(List<Candidate> candidates, RandomSource random) {
        for (int i = candidates.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            Candidate current = candidates.get(i);
            candidates.set(i, candidates.get(swapIndex));
            candidates.set(swapIndex, current);
        }
    }

    private static ResourceKey<LootTable> lootTable(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, path));
    }

    private record Candidate(BlockPos pos, int localY, int priority) {
    }
}
