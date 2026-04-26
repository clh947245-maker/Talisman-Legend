package com.example.examplemod.structure;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.ShengZhuEntity;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class ShengZhuPalaceStructure extends Structure {
    public static final MapCodec<ShengZhuPalaceStructure> CODEC = simpleCodec(ShengZhuPalaceStructure::new);
    private static final int PALACE_RADIUS_BLOCKS = 136;
    private static final int WALL_HALF_SIZE = 128;
    private static final int MIN_SURFACE_Y = 56;
    private static final long LAYOUT_SEED_SALT = 0x71E8D15C0FFEE12L;
    private static final int BUILDING_MIN_RADIUS = 56;
    private static final int BUILDING_RADIUS_VARIANCE = 47;
    private static final int MIN_AUXILIARY_BUILDINGS = 24;
    private static final int AUXILIARY_BUILDING_VARIANCE = 9;
    private static final int BUILDING_MIN_GAP = 10;
    private static final int AUXILIARY_BUILDING_MIN_GAP = 6;
    private static final int MAIN_BUILDING_FOOTPRINT_PADDING = 4;
    private static final int MAIN_BUILDING_WALL_GAP = 15;
    private static final int CENTER_CLEAR_HALF_SIZE = 40;
    private static final int MIN_GROUNDS_CLEAR_HEIGHT = 64;
    private static final int GROUNDS_MAX_CLEAR_HEIGHT = 128;
    private static final int COLUMN_CLEAR_PADDING = 6;
    private static final int SITE_SAMPLE_RADIUS = 96;

    public ShengZhuPalaceStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        BlockPos center = new BlockPos(chunkPos.getMiddleBlockX(), 0, chunkPos.getMiddleBlockZ());
        int centerY = getSurfaceY(context, center.getX(), center.getZ());
        if (!isGoodPalaceSite(context, center, centerY)) {
            return Optional.empty();
        }

        BlockPos centerSurface = new BlockPos(center.getX(), centerY, center.getZ());
        return Optional.of(new GenerationStub(centerSurface, builder -> generatePieces(builder, context, centerSurface)));
    }

    private static void generatePieces(StructurePiecesBuilder builder, GenerationContext context, BlockPos center) {
        List<BuildingConstructorItem.BuildingVariant> variants = choosePalaceBuildings(context.seed(), context.chunkPos());
        List<PalacePlacement> placements = choosePalacePlacements(context.seed(), context.chunkPos(), context.structureTemplateManager(), variants);
        List<PalaceFootprint> footprints = placements.stream().map(PalacePlacement::footprint).toList();
        builder.addPiece(new GroundsPiece(
                center.offset(-WALL_HALF_SIZE, -1, -WALL_HALF_SIZE),
                center.offset(WALL_HALF_SIZE, GROUNDS_MAX_CLEAR_HEIGHT, WALL_HALF_SIZE),
                footprints
        ));
        for (PalacePlacement placement : placements) {
            BuildingConstructorItem.BuildingVariant variant = placement.variant();
            PalaceSlot slot = placement.footprint().slot();
            BlockPos slotCenter = center.offset(slot.xOffset(), 0, slot.zOffset());
            StructureTemplate template = context.structureTemplateManager().getOrCreate(variant.structureId());
            Vec3i size = template.getSize();
            BlockPos origin = new BlockPos(slotCenter.getX() - size.getX() / 2, center.getY(), slotCenter.getZ() - size.getZ() / 2);
            builder.addPiece(new BuildingPiece(context.structureTemplateManager(), variant.structureId(), origin));
        }

        for (PalaceFootprint footprint : chooseAuxiliaryFootprints(context.seed(), context.chunkPos(), footprints)) {
            PalaceSlot slot = footprint.slot();
            BlockPos slotCenter = center.offset(slot.xOffset(), 0, slot.zOffset());
            int style = auxiliaryStyle(slot);
            builder.addPiece(new AuxiliaryBuildingPiece(new BlockPos(slotCenter.getX(), center.getY(), slotCenter.getZ()), style));
        }

        builder.addPiece(new ShengZhuSpawnPiece(center.above()));
    }

    private static boolean isGoodPalaceSite(GenerationContext context, BlockPos center, int centerY) {
        if (centerY < MIN_SURFACE_Y) {
            return false;
        }

        int[][] sampleOffsets = {
                {-SITE_SAMPLE_RADIUS, -SITE_SAMPLE_RADIUS},
                {0, -SITE_SAMPLE_RADIUS},
                {SITE_SAMPLE_RADIUS, -SITE_SAMPLE_RADIUS},
                {-SITE_SAMPLE_RADIUS, 0},
                {0, 0},
                {SITE_SAMPLE_RADIUS, 0},
                {-SITE_SAMPLE_RADIUS, SITE_SAMPLE_RADIUS},
                {0, SITE_SAMPLE_RADIUS},
                {SITE_SAMPLE_RADIUS, SITE_SAMPLE_RADIUS}
        };
        for (int[] offset : sampleOffsets) {
            int sampleX = center.getX() + offset[0];
            int sampleZ = center.getZ() + offset[1];
            int sampleY = getSurfaceY(context, sampleX, sampleZ);
            if (Math.abs(sampleY - centerY) > 12) {
                return false;
            }
            if (isWaterSurface(context, sampleX, sampleY, sampleZ) || isTreeDenseOrWaterBiome(context, sampleX, sampleY, sampleZ)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWaterSurface(GenerationContext context, int x, int surfaceY, int z) {
        NoiseColumn column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        return !column.getBlock(surfaceY).getFluidState().isEmpty()
                || !column.getBlock(surfaceY - 1).getFluidState().isEmpty();
    }

    private static boolean isTreeDenseOrWaterBiome(GenerationContext context, int x, int y, int z) {
        var biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),
                context.randomState().sampler()
        );
        return biome.is(BiomeTags.IS_FOREST)
                || biome.is(BiomeTags.IS_JUNGLE)
                || biome.is(BiomeTags.IS_TAIGA)
                || biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_DEEP_OCEAN)
                || biome.is(BiomeTags.IS_RIVER)
                || !isAllowedPalaceBiome(context, x, y, z);
    }

    private static boolean isAllowedPalaceBiome(GenerationContext context, int x, int y, int z) {
        var biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),
                context.randomState().sampler()
        );
        return biome.is(Biomes.DESERT)
                || biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS);
    }

    private static int getSurfaceY(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(
                x,
                z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
    }

    private static List<BuildingConstructorItem.BuildingVariant> choosePalaceBuildings(long levelSeed, ChunkPos chunkPos) {
        BuildingConstructorItem.BuildingVariant[] all = BuildingConstructorItem.BuildingVariant.values();
        List<BuildingConstructorItem.BuildingVariant> shuffled = new ArrayList<>(List.of(all));
        RandomSource random = RandomSource.create(mixSeed(levelSeed, chunkPos.x, chunkPos.z, LAYOUT_SEED_SALT));
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            BuildingConstructorItem.BuildingVariant current = shuffled.get(i);
            shuffled.set(i, shuffled.get(swapIndex));
            shuffled.set(swapIndex, current);
        }
        return shuffled;
    }

    private static List<PalacePlacement> choosePalacePlacements(
            long levelSeed,
            ChunkPos chunkPos,
            StructureTemplateManager templateManager,
            List<BuildingConstructorItem.BuildingVariant> variants) {
        RandomSource random = RandomSource.create(mixSeed(levelSeed, chunkPos.x, chunkPos.z, LAYOUT_SEED_SALT ^ 0x51A7E5L));
        double angleOffset = random.nextDouble() * Math.PI * 2.0D;
        List<PalacePlacement> placements = new ArrayList<>();
        List<PalaceFootprint> footprints = new ArrayList<>();
        for (int i = 0; i < variants.size(); i++) {
            BuildingConstructorItem.BuildingVariant variant = variants.get(i);
            Vec3i size = templateManager.getOrCreate(variant.structureId()).getSize();
            int halfX = footprintHalfSize(size.getX(), MAIN_BUILDING_FOOTPRINT_PADDING);
            int halfZ = footprintHalfSize(size.getZ(), MAIN_BUILDING_FOOTPRINT_PADDING);
            PalaceFootprint placed = null;

            for (int attempt = 0; attempt < 96 && placed == null; attempt++) {
                double baseAngle = angleOffset + (Math.PI * 2.0D * i / variants.size());
                double angle = baseAngle + (random.nextDouble() - 0.5D) * 1.2D;
                int radius = BUILDING_MIN_RADIUS + random.nextInt(BUILDING_RADIUS_VARIANCE + 22);
                PalaceFootprint candidate = new PalaceFootprint(
                        new PalaceSlot(
                                (int)Math.round(Math.cos(angle) * radius) + random.nextInt(23) - 11,
                                (int)Math.round(Math.sin(angle) * radius) + random.nextInt(23) - 11
                        ),
                        halfX,
                        halfZ
                );
                if (canPlaceMainBuilding(candidate, footprints)) {
                    placed = candidate;
                }
            }

            if (placed == null) {
                placed = findFallbackFootprint(random, halfX, halfZ, footprints);
            }
            if (placed == null) {
                continue;
            }
            placements.add(new PalacePlacement(variant, placed));
            footprints.add(placed);
        }
        return placements;
    }

    private static PalaceFootprint findFallbackFootprint(RandomSource random, int halfX, int halfZ, List<PalaceFootprint> occupied) {
        List<PalaceSlot> candidates = new ArrayList<>();
        for (int x = -92; x <= 92; x += 34) {
            for (int z = -92; z <= 92; z += 34) {
                if (Math.abs(x) < CENTER_CLEAR_HALF_SIZE && Math.abs(z) < CENTER_CLEAR_HALF_SIZE) {
                    continue;
                }
                candidates.add(new PalaceSlot(x + random.nextInt(11) - 5, z + random.nextInt(11) - 5));
            }
        }
        for (int i = candidates.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            PalaceSlot current = candidates.get(i);
            candidates.set(i, candidates.get(swapIndex));
            candidates.set(swapIndex, current);
        }
        for (PalaceSlot slot : candidates) {
            PalaceFootprint candidate = new PalaceFootprint(slot, halfX, halfZ);
            if (canPlaceMainBuilding(candidate, occupied)) {
                return candidate;
            }
        }
        int maxX = WALL_HALF_SIZE - MAIN_BUILDING_WALL_GAP - halfX;
        int maxZ = WALL_HALF_SIZE - MAIN_BUILDING_WALL_GAP - halfZ;
        for (int radius = BUILDING_MIN_RADIUS; radius <= Math.max(maxX, maxZ); radius += 4) {
            for (int x = -radius; x <= radius; x += 4) {
                PalaceFootprint north = new PalaceFootprint(new PalaceSlot(x, -radius), halfX, halfZ);
                if (canPlaceMainBuilding(north, occupied)) {
                    return north;
                }
                PalaceFootprint south = new PalaceFootprint(new PalaceSlot(x, radius), halfX, halfZ);
                if (canPlaceMainBuilding(south, occupied)) {
                    return south;
                }
            }
            for (int z = -radius + 4; z <= radius - 4; z += 4) {
                PalaceFootprint west = new PalaceFootprint(new PalaceSlot(-radius, z), halfX, halfZ);
                if (canPlaceMainBuilding(west, occupied)) {
                    return west;
                }
                PalaceFootprint east = new PalaceFootprint(new PalaceSlot(radius, z), halfX, halfZ);
                if (canPlaceMainBuilding(east, occupied)) {
                    return east;
                }
            }
        }
        return null;
    }

    private static boolean canPlaceMainBuilding(PalaceFootprint candidate, List<PalaceFootprint> occupied) {
        PalaceSlot slot = candidate.slot();
        if (Math.abs(slot.xOffset()) < CENTER_CLEAR_HALF_SIZE && Math.abs(slot.zOffset()) < CENTER_CLEAR_HALF_SIZE) {
            return false;
        }
        if (Math.abs(slot.xOffset()) + candidate.halfX() > WALL_HALF_SIZE - MAIN_BUILDING_WALL_GAP) {
            return false;
        }
        if (Math.abs(slot.zOffset()) + candidate.halfZ() > WALL_HALF_SIZE - MAIN_BUILDING_WALL_GAP) {
            return false;
        }
        for (PalaceFootprint other : occupied) {
            if (rectDistance(candidate, other) < BUILDING_MIN_GAP) {
                return false;
            }
        }
        return true;
    }

    private static int rectDistance(PalaceFootprint first, PalaceFootprint second) {
        int dx = Math.max(0, Math.abs(first.slot().xOffset() - second.slot().xOffset()) - first.halfX() - second.halfX());
        int dz = Math.max(0, Math.abs(first.slot().zOffset() - second.slot().zOffset()) - first.halfZ() - second.halfZ());
        return Math.min(dx, dz) == 0 ? Math.max(dx, dz) : (int)Math.sqrt(dx * dx + dz * dz);
    }

    private static int footprintHalfSize(int templateSize, int padding) {
        return Math.max(1, templateSize / 2 + padding);
    }

    private static List<PalaceFootprint> chooseAuxiliaryFootprints(long levelSeed, ChunkPos chunkPos, List<PalaceFootprint> mainFootprints) {
        RandomSource random = RandomSource.create(mixSeed(levelSeed, chunkPos.x, chunkPos.z, LAYOUT_SEED_SALT ^ 0xA11CEB01L));
        int targetCount = MIN_AUXILIARY_BUILDINGS + random.nextInt(AUXILIARY_BUILDING_VARIANCE);
        List<PalaceFootprint> footprints = new ArrayList<>();
        List<PalaceFootprint> occupied = new ArrayList<>(mainFootprints);
        for (int attempt = 0; attempt < 560 && footprints.size() < targetCount; attempt++) {
            int x = random.nextInt(WALL_HALF_SIZE * 2 - 44) - WALL_HALF_SIZE + 22;
            int z = random.nextInt(WALL_HALF_SIZE * 2 - 44) - WALL_HALF_SIZE + 22;
            if (Math.abs(x) < CENTER_CLEAR_HALF_SIZE && Math.abs(z) < CENTER_CLEAR_HALF_SIZE) {
                continue;
            }

            PalaceSlot candidate = new PalaceSlot(x, z);
            PalaceFootprint footprint = auxiliaryFootprint(candidate);
            if (canPlaceAuxiliaryBuilding(footprint, occupied)) {
                footprints.add(footprint);
                occupied.add(footprint);
            }
        }
        addSymmetricDecorativeFootprints(random, footprints, occupied, targetCount);
        return footprints;
    }

    private static void addSymmetricDecorativeFootprints(
            RandomSource random,
            List<PalaceFootprint> footprints,
            List<PalaceFootprint> occupied,
            int targetCount) {
        int[][] anchors = {
                {-108, -108}, {108, -108}, {-108, 108}, {108, 108},
                {-88, -32}, {88, -32}, {-88, 32}, {88, 32},
                {-32, -88}, {32, -88}, {-32, 88}, {32, 88},
                {-112, 0}, {112, 0}, {0, -112}, {0, 112}
        };
        int offsetRange = 7;
        for (int[] anchor : anchors) {
            if (footprints.size() >= targetCount) {
                return;
            }

            PalaceSlot slot = new PalaceSlot(
                    anchor[0] + random.nextInt(offsetRange * 2 + 1) - offsetRange,
                    anchor[1] + random.nextInt(offsetRange * 2 + 1) - offsetRange);
            PalaceFootprint footprint = decorativeFootprint(slot);
            if (canPlaceAuxiliaryBuilding(footprint, occupied)) {
                footprints.add(footprint);
                occupied.add(footprint);
            }
        }
    }

    private static boolean canPlaceAuxiliaryBuilding(PalaceFootprint candidate, List<PalaceFootprint> occupied) {
        PalaceSlot slot = candidate.slot();
        if (Math.abs(slot.xOffset()) < CENTER_CLEAR_HALF_SIZE && Math.abs(slot.zOffset()) < CENTER_CLEAR_HALF_SIZE) {
            return false;
        }
        if (Math.abs(slot.xOffset()) + candidate.halfX() > WALL_HALF_SIZE - MAIN_BUILDING_WALL_GAP) {
            return false;
        }
        if (Math.abs(slot.zOffset()) + candidate.halfZ() > WALL_HALF_SIZE - MAIN_BUILDING_WALL_GAP) {
            return false;
        }
        for (PalaceFootprint other : occupied) {
            if (rectDistance(candidate, other) < AUXILIARY_BUILDING_MIN_GAP) {
                return false;
            }
        }
        return true;
    }

    private static PalaceFootprint auxiliaryFootprint(PalaceSlot slot) {
        int style = auxiliaryStyle(slot);
        int halfX = auxiliaryHalfX(style);
        int halfZ = auxiliaryHalfZ(style);
        return new PalaceFootprint(slot, halfX, halfZ);
    }

    private static PalaceFootprint decorativeFootprint(PalaceSlot slot) {
        int style = auxiliaryStyle(slot);
        return new PalaceFootprint(slot, auxiliaryHalfX(style), auxiliaryHalfZ(style));
    }

    private static int auxiliaryHalfX(int style) {
        return switch (style) {
            case 1 -> 8;
            case 3, 4, 5 -> 4;
            default -> 6;
        };
    }

    private static int auxiliaryHalfZ(int style) {
        return switch (style) {
            case 2 -> 8;
            case 3, 4, 5 -> 4;
            default -> 6;
        };
    }

    private static int auxiliaryHeight(int style) {
        return switch (style) {
            case 3 -> 13;
            case 4 -> 7;
            case 5 -> 5;
            default -> 9;
        };
    }

    private static boolean isDecorativeAuxiliaryStyle(int style) {
        return style >= 3;
    }

    private static int auxiliaryStyle(PalaceSlot slot) {
        return Math.floorMod(slot.xOffset() * 31 + slot.zOffset() * 17, 6);
    }

    private static long mixSeed(long levelSeed, int chunkX, int chunkZ, long salt) {
        long mixed = levelSeed ^ salt;
        mixed ^= (long)chunkX * 0x9E3779B97F4A7C15L;
        mixed ^= (long)chunkZ * 0xC2B2AE3D27D4EB4FL;
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= mixed >>> 33;
        return mixed;
    }

    @Override
    public StructureType<?> type() {
        return ChenMod.SHENG_ZHU_PALACE_STRUCTURE_TYPE.get();
    }

    public static class BuildingPiece extends TemplateStructurePiece {
        private final BuildingConstructorItem.BuildingVariant variant;

        public BuildingPiece(StructureTemplateManager templateManager, ResourceLocation templateId, BlockPos pos) {
            super(
                    ChenMod.SHENG_ZHU_PALACE_BUILDING_PIECE.get(),
                    0,
                    templateManager,
                    templateId,
                    templateId.toString(),
                    makeSettings(),
                    pos
            );
            this.variant = BuildingConstructorItem.BuildingVariant.fromStructureId(templateId).orElse(null);
        }

        public BuildingPiece(StructureTemplateManager templateManager, CompoundTag tag) {
            super(ChenMod.SHENG_ZHU_PALACE_BUILDING_PIECE.get(), tag, templateManager, ignored -> makeSettings());
            this.variant = BuildingConstructorItem.BuildingVariant.fromStructureId(this.makeTemplateLocation()).orElse(null);
        }

        private static StructurePlaceSettings makeSettings() {
            return new StructurePlaceSettings()
                    .setMirror(Mirror.NONE)
                    .setRotation(Rotation.NONE)
                    .setIgnoreEntities(false)
                    .setKnownShape(true);
        }

        @Override
        protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        }

        @Override
        public void postProcess(
                WorldGenLevel level,
                StructureManager structureManager,
                ChunkGenerator chunkGenerator,
                RandomSource random,
                BoundingBox box,
                ChunkPos chunkPos,
            BlockPos pivot) {
            super.postProcess(level, structureManager, chunkGenerator, random, box, chunkPos, pivot);
            PalaceRewardChestPlacer.placeTemplateRewardChests(level, this.template(), this.templatePosition(), this.variant, random, box);
        }
    }

    public static class ShengZhuSpawnPiece extends StructurePiece {
        public ShengZhuSpawnPiece(BlockPos pos) {
            super(ChenMod.SHENG_ZHU_PALACE_SPAWN_PIECE.get(), 0, new BoundingBox(pos));
        }

        public ShengZhuSpawnPiece(CompoundTag tag) {
            super(ChenMod.SHENG_ZHU_PALACE_SPAWN_PIECE.get(), tag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        }

        @Override
        public void postProcess(
                WorldGenLevel level,
                StructureManager structureManager,
                ChunkGenerator chunkGenerator,
                RandomSource random,
                BoundingBox box,
                ChunkPos chunkPos,
                BlockPos pivot) {
            BlockPos spawnPos = this.boundingBox.getCenter();
            if (!box.isInside(spawnPos) || !(level.getLevel() instanceof ServerLevel serverLevel)) {
                return;
            }

            ShengZhuEntity shengZhu = ChenMod.SHENG_ZHU.get().create(serverLevel);
            if (shengZhu == null) {
                return;
            }

            shengZhu.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            shengZhu.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(shengZhu);
        }
    }

    public static class GroundsPiece extends StructurePiece {
        private static final BlockState AIR = Blocks.AIR.defaultBlockState();
        private static final BlockState FOUNDATION = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        private static final BlockState FLOOR = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        private static final BlockState FLOOR_TRIM = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        private static final BlockState ROAD = Blocks.POLISHED_ANDESITE.defaultBlockState();
        private static final BlockState ROAD_CENTER = Blocks.POLISHED_DIORITE.defaultBlockState();
        private static final BlockState WALL = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        private static final BlockState WALL_CAP = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();

        private final List<PalaceFootprint> protectedFootprints;

        public GroundsPiece(BlockPos min, BlockPos max, List<PalaceFootprint> protectedFootprints) {
            super(ChenMod.SHENG_ZHU_PALACE_GROUNDS_PIECE.get(), 0, BoundingBox.fromCorners(min, max));
            this.protectedFootprints = List.copyOf(protectedFootprints);
        }

        public GroundsPiece(CompoundTag tag) {
            super(ChenMod.SHENG_ZHU_PALACE_GROUNDS_PIECE.get(), tag);
            this.protectedFootprints = readProtectedFootprints(tag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            ListTag footprintsTag = new ListTag();
            for (PalaceFootprint footprint : this.protectedFootprints) {
                CompoundTag footprintTag = new CompoundTag();
                footprintTag.putInt("X", footprint.slot().xOffset());
                footprintTag.putInt("Z", footprint.slot().zOffset());
                footprintTag.putInt("HX", footprint.halfX());
                footprintTag.putInt("HZ", footprint.halfZ());
                footprintsTag.add(footprintTag);
            }
            tag.put("ProtectedFootprints", footprintsTag);
        }

        @Override
        public void postProcess(
                WorldGenLevel level,
                StructureManager structureManager,
                ChunkGenerator chunkGenerator,
                RandomSource random,
                BoundingBox box,
                ChunkPos chunkPos,
                BlockPos pivot) {
            BoundingBox area = this.boundingBox;
            int floorY = area.minY();
            int minX = Math.max(area.minX(), box.minX());
            int maxX = Math.min(area.maxX(), box.maxX());
            int minZ = Math.max(area.minZ(), box.minZ());
            int maxZ = Math.min(area.maxZ(), box.maxZ());
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            clearPalaceArea(level, box, floorY, minX, maxX, minZ, maxZ, pos);

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = floorY - 4; y < floorY; y++) {
                        pos.set(x, y, z);
                        if (box.isInside(pos)) {
                            level.setBlock(pos, FOUNDATION, 2);
                        }
                    }

                    pos.set(x, floorY, z);
                    if (isInsideProtectedFootprint(area, x, z, this.protectedFootprints)) {
                        level.setBlock(pos, FLOOR, 2);
                        continue;
                    }

                    boolean nearFloorEdge = x <= area.minX() + 4 || x >= area.maxX() - 4 || z <= area.minZ() + 4 || z >= area.maxZ() - 4;
                    level.setBlock(pos, chooseGroundBlock(area, x, z, nearFloorEdge), 2);

                    int clearTopY = getColumnClearTop(level, x, z, floorY);
                    if (isWallColumn(area, x, z)) {
                        for (int y = floorY + 1; y <= floorY + 16; y++) {
                            pos.set(x, y, z);
                            level.setBlock(pos, y == floorY + 16 ? WALL_CAP : WALL, 2);
                        }
                        for (int y = floorY + 17; y <= clearTopY; y++) {
                            pos.set(x, y, z);
                            if (box.isInside(pos)) {
                                level.setBlock(pos, AIR, 2);
                            }
                        }
                    } else {
                        for (int y = floorY + 1; y <= clearTopY; y++) {
                            pos.set(x, y, z);
                            if (box.isInside(pos)) {
                                level.setBlock(pos, AIR, 2);
                            }
                        }
                    }
                }
            }
        }

        private static void clearPalaceArea(
                WorldGenLevel level,
                BoundingBox box,
                int floorY,
                int minX,
                int maxX,
                int minZ,
                int maxZ,
                BlockPos.MutableBlockPos pos) {
            int clearBottomY = Math.max(floorY - 4, level.getMinBuildHeight());
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int clearTopY = getColumnClearTop(level, x, z, floorY);
                    for (int y = clearBottomY; y <= clearTopY; y++) {
                        pos.set(x, y, z);
                        if (box.isInside(pos)) {
                            level.setBlock(pos, AIR, 2);
                        }
                    }
                }
            }
        }

        private static int getColumnClearTop(WorldGenLevel level, int x, int z, int floorY) {
            int maxScanY = Math.min(floorY + GROUNDS_MAX_CLEAR_HEIGHT, level.getMaxBuildHeight() - 1);
            int highestBlockY = floorY;
            BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos(x, maxScanY, z);
            for (int y = maxScanY; y > floorY; y--) {
                scanPos.setY(y);
                if (!level.getBlockState(scanPos).isAir()) {
                    highestBlockY = y;
                    break;
                }
            }
            int clearTopY = Math.max(floorY + MIN_GROUNDS_CLEAR_HEIGHT, highestBlockY + COLUMN_CLEAR_PADDING);
            return Math.min(clearTopY, maxScanY);
        }

        private static List<PalaceFootprint> readProtectedFootprints(CompoundTag tag) {
            List<PalaceFootprint> footprints = new ArrayList<>();
            ListTag footprintsTag = tag.getList("ProtectedFootprints", 10);
            for (int i = 0; i < footprintsTag.size(); i++) {
                CompoundTag footprintTag = footprintsTag.getCompound(i);
                footprints.add(new PalaceFootprint(
                        new PalaceSlot(footprintTag.getInt("X"), footprintTag.getInt("Z")),
                        footprintTag.getInt("HX"),
                        footprintTag.getInt("HZ")
                ));
            }
            return footprints;
        }

        private static boolean isInsideProtectedFootprint(BoundingBox area, int x, int z, List<PalaceFootprint> footprints) {
            int centerX = (area.minX() + area.maxX()) / 2;
            int centerZ = (area.minZ() + area.maxZ()) / 2;
            int localX = x - centerX;
            int localZ = z - centerZ;
            for (PalaceFootprint footprint : footprints) {
                if (Math.abs(localX - footprint.slot().xOffset()) <= footprint.halfX() + 2
                        && Math.abs(localZ - footprint.slot().zOffset()) <= footprint.halfZ() + 2) {
                    return true;
                }
            }
            return false;
        }

        private static BlockState chooseGroundBlock(BoundingBox area, int x, int z, boolean nearFloorEdge) {
            int centerX = (area.minX() + area.maxX()) / 2;
            int centerZ = (area.minZ() + area.maxZ()) / 2;
            int dx = x - centerX;
            int dz = z - centerZ;
            boolean centralPlaza = Math.abs(dx) <= 13 && Math.abs(dz) <= 13;
            boolean mainRoad = Math.abs(dx) <= 4 || Math.abs(dz) <= 4;
            boolean ringRoad = (Math.abs(Math.abs(dx) - 64) <= 3 && Math.abs(dz) <= 96)
                    || (Math.abs(Math.abs(dz) - 64) <= 3 && Math.abs(dx) <= 96);
            boolean outerRoad = (Math.abs(Math.abs(dx) - 104) <= 2 && Math.abs(dz) <= 112)
                    || (Math.abs(Math.abs(dz) - 104) <= 2 && Math.abs(dx) <= 112);
            if (centralPlaza) {
                return ROAD_CENTER;
            }
            if (mainRoad || ringRoad || outerRoad) {
                return ROAD;
            }
            return nearFloorEdge ? FLOOR_TRIM : FLOOR;
        }

        private static boolean isWallColumn(BoundingBox area, int x, int z) {
            boolean north = z == area.minZ();
            boolean south = z == area.maxZ();
            boolean west = x == area.minX();
            boolean east = x == area.maxX();
            if (!(north || south || west || east)) {
                return false;
            }

            int centerX = (area.minX() + area.maxX()) / 2;
            int centerZ = (area.minZ() + area.maxZ()) / 2;
            boolean gateNorthSouth = Math.abs(x - centerX) <= 5 && (north || south);
            boolean gateEastWest = Math.abs(z - centerZ) <= 5 && (west || east);
            return !gateNorthSouth && !gateEastWest;
        }
    }

    public static class AuxiliaryBuildingPiece extends StructurePiece {
        private static final BlockState FOUNDATION = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        private static final BlockState WALL = Blocks.DEEPSLATE_TILES.defaultBlockState();
        private static final BlockState PILLAR = Blocks.POLISHED_BASALT.defaultBlockState();
        private static final BlockState ROOF = Blocks.DARK_PRISMARINE.defaultBlockState();
        private static final BlockState TRIM = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        private static final BlockState ACCENT = Blocks.RED_NETHER_BRICKS.defaultBlockState();
        private static final BlockState LAMP = Blocks.SEA_LANTERN.defaultBlockState();
        private static final BlockState AIR = Blocks.AIR.defaultBlockState();
        private final int style;

        public AuxiliaryBuildingPiece(BlockPos center, int style) {
            super(ChenMod.SHENG_ZHU_PALACE_AUXILIARY_PIECE.get(), 0, createBox(center, style));
            this.style = style;
        }

        public AuxiliaryBuildingPiece(CompoundTag tag) {
            super(ChenMod.SHENG_ZHU_PALACE_AUXILIARY_PIECE.get(), tag);
            this.style = tag.getInt("Style");
        }

        private static BoundingBox createBox(BlockPos center, int style) {
            int halfX = auxiliaryHalfX(style);
            int halfZ = auxiliaryHalfZ(style);
            int height = auxiliaryHeight(style);
            return new BoundingBox(
                    center.getX() - halfX,
                    center.getY(),
                    center.getZ() - halfZ,
                    center.getX() + halfX,
                    center.getY() + height,
                    center.getZ() + halfZ
            );
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("Style", this.style);
        }

        @Override
        public void postProcess(
                WorldGenLevel level,
                StructureManager structureManager,
                ChunkGenerator chunkGenerator,
                RandomSource random,
                BoundingBox box,
                ChunkPos chunkPos,
                BlockPos pivot) {
            BoundingBox area = this.boundingBox;
            int minX = Math.max(area.minX(), box.minX());
            int maxX = Math.min(area.maxX(), box.maxX());
            int minZ = Math.max(area.minZ(), box.minZ());
            int maxZ = Math.min(area.maxZ(), box.maxZ());
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = area.minY(); y <= area.maxY(); y++) {
                        if (y < box.minY() || y > box.maxY()) {
                            continue;
                        }
                        BlockState state = stateFor(area, this.style, x, y, z);
                        if (state != AIR) {
                            pos.set(x, y, z);
                            level.setBlock(pos, state, 2);
                        }
                    }
                }
            }

            if (box.isInside(area.getCenter())) {
                PalaceRewardChestPlacer.placeAuxiliaryCommonChest(level, area, random);
            }
        }

        private static BlockState stateFor(BoundingBox area, int style, int x, int y, int z) {
            if (isDecorativeAuxiliaryStyle(style)) {
                return decorativeStateFor(area, style, x, y, z);
            }

            boolean edgeX = x == area.minX() || x == area.maxX();
            boolean edgeZ = z == area.minZ() || z == area.maxZ();
            boolean corner = edgeX && edgeZ;
            if (y == area.minY()) {
                return FOUNDATION;
            }
            if (y >= area.maxY() - 2) {
                return ROOF;
            }
            if (corner) {
                return PILLAR;
            }
            if (y <= area.minY() + 4 && (edgeX || edgeZ)) {
                int centerX = (area.minX() + area.maxX()) / 2;
                int centerZ = (area.minZ() + area.maxZ()) / 2;
                boolean doorway = (z == area.minZ() && Math.abs(x - centerX) <= 1)
                        || (x == area.minX() && Math.abs(z - centerZ) <= 1);
                return doorway ? AIR : WALL;
            }
            return AIR;
        }

        private static BlockState decorativeStateFor(BoundingBox area, int style, int x, int y, int z) {
            int centerX = (area.minX() + area.maxX()) / 2;
            int centerZ = (area.minZ() + area.maxZ()) / 2;
            int dx = Math.abs(x - centerX);
            int dz = Math.abs(z - centerZ);
            int localY = y - area.minY();
            boolean centerLine = x == centerX || z == centerZ;
            boolean corner = (x == area.minX() || x == area.maxX()) && (z == area.minZ() || z == area.maxZ());

            if (style == 3) {
                if (localY == 0) {
                    return FOUNDATION;
                }
                if (corner && localY <= 10) {
                    return PILLAR;
                }
                if (dx <= 1 && dz <= 1 && localY <= 12) {
                    return localY == 12 ? LAMP : TRIM;
                }
                if (localY >= 10 && dx <= 4 && dz <= 4) {
                    return localY == area.maxY() - area.minY() ? ROOF : ACCENT;
                }
                return AIR;
            }

            if (style == 4) {
                if (localY == 0 || localY == 1 && (dx <= 3 || dz <= 3)) {
                    return FOUNDATION;
                }
                if (corner && localY <= 5) {
                    return PILLAR;
                }
                if (localY == 6 || localY == 7) {
                    return dx <= 4 && dz <= 4 ? ROOF : AIR;
                }
                return centerLine && dx <= 4 && dz <= 4 ? TRIM : AIR;
            }

            if (localY == 0) {
                return FOUNDATION;
            }
            if (localY == 1 && (dx == 4 || dz == 4 || dx <= 1 && dz <= 1)) {
                return TRIM;
            }
            if (localY == 2 && dx <= 2 && dz <= 2) {
                return ACCENT;
            }
            if (localY == 3 && dx <= 1 && dz <= 1) {
                return LAMP;
            }
            if (localY >= 4) {
                return dx <= 3 && dz <= 3 ? ROOF : AIR;
            }
            return AIR;
        }
    }

    private record PalaceSlot(int xOffset, int zOffset) {
    }

    private record PalaceFootprint(PalaceSlot slot, int halfX, int halfZ) {
    }

    private record PalacePlacement(BuildingConstructorItem.BuildingVariant variant, PalaceFootprint footprint) {
    }
}
