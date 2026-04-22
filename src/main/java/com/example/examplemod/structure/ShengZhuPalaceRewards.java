package com.example.examplemod.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class ShengZhuPalaceRewards {
    private static final BlockPos[] FIRST_FLOOR_PRIMARY_CHEST_CANDIDATES = new BlockPos[] {
            new BlockPos(40, 5, 25),
            new BlockPos(36, 5, 25),
            new BlockPos(44, 5, 25),
            new BlockPos(33, 5, 43),
            new BlockPos(41, 5, 43),
            new BlockPos(37, 5, 49),
            new BlockPos(45, 5, 49),
            new BlockPos(33, 5, 55),
            new BlockPos(41, 5, 55),
            new BlockPos(37, 5, 55),
            new BlockPos(45, 5, 55),
            new BlockPos(35, 5, 80),
            new BlockPos(41, 5, 80),
            new BlockPos(35, 5, 84),
            new BlockPos(41, 5, 84)
    };
    private static final BlockPos[] FIRST_FLOOR_SECONDARY_CHEST_CANDIDATES = new BlockPos[] {
            new BlockPos(32, 5, 21),
            new BlockPos(38, 5, 21),
            new BlockPos(44, 5, 21),
            new BlockPos(48, 5, 21),
            new BlockPos(30, 5, 25),
            new BlockPos(50, 5, 25),
            new BlockPos(31, 5, 31),
            new BlockPos(49, 5, 31),
            new BlockPos(47, 5, 84)
    };
    private static final BlockPos[] SECOND_FLOOR_PRIMARY_CHEST_CANDIDATES = new BlockPos[] {
            new BlockPos(36, 11, 100),
            new BlockPos(40, 11, 100),
            new BlockPos(44, 11, 100),
            new BlockPos(40, 11, 102),
            new BlockPos(34, 16, 60),
            new BlockPos(38, 16, 60),
            new BlockPos(44, 16, 60),
            new BlockPos(48, 16, 60),
            new BlockPos(34, 16, 68),
            new BlockPos(38, 16, 68),
            new BlockPos(44, 16, 68),
            new BlockPos(48, 16, 68),
            new BlockPos(34, 16, 76),
            new BlockPos(38, 16, 76),
            new BlockPos(44, 16, 76),
            new BlockPos(48, 16, 76)
    };
    private static final BlockPos[] SECOND_FLOOR_SECONDARY_CHEST_CANDIDATES = new BlockPos[] {
            new BlockPos(36, 11, 102),
            new BlockPos(44, 11, 102),
            new BlockPos(30, 16, 60),
            new BlockPos(52, 16, 60),
            new BlockPos(30, 16, 68),
            new BlockPos(52, 16, 68),
            new BlockPos(30, 16, 76),
            new BlockPos(52, 16, 76)
    };
    private static final int MIN_TOTAL_CHESTS = 20;
    private static final int MAX_TOTAL_CHESTS = 40;
    private static final int MIN_FIRST_FLOOR_CHESTS = 10;
    private static final int MIN_SECOND_FLOOR_CHESTS = 8;
    private static final int MIN_LOCAL_DISTANCE = 5;
    private static final int MAX_CEILING_DISTANCE = 24;
    private static final Item[] DIAMOND_WEAPONS = new Item[] {
            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE
    };
    private static final Item[] DIAMOND_ARMOR = new Item[] {
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS
    };
    private static final EnchantedBookSpec[] ENCHANTED_BOOKS = new EnchantedBookSpec[] {
            new EnchantedBookSpec(Enchantments.SHARPNESS, 5),
            new EnchantedBookSpec(Enchantments.PROTECTION, 4),
            new EnchantedBookSpec(Enchantments.UNBREAKING, 3),
            new EnchantedBookSpec(Enchantments.LOOTING, 3),
            new EnchantedBookSpec(Enchantments.EFFICIENCY, 5),
            new EnchantedBookSpec(Enchantments.FORTUNE, 3),
            new EnchantedBookSpec(Enchantments.SILK_TOUCH, 1),
            new EnchantedBookSpec(Enchantments.POWER, 5),
            new EnchantedBookSpec(Enchantments.FIRE_ASPECT, 2),
            new EnchantedBookSpec(Enchantments.MENDING, 1),
            new EnchantedBookSpec(Enchantments.FEATHER_FALLING, 4),
            new EnchantedBookSpec(Enchantments.RESPIRATION, 3),
            new EnchantedBookSpec(Enchantments.THORNS, 3)
    };

    private ShengZhuPalaceRewards() {
    }

    public static void placeRewardChests(ServerLevelAccessor level, BlockPos origin, Rotation rotation, BoundingBox chunkBox) {
        long palaceSeed = computePalaceSeed(level, origin, rotation);
        RandomSource palaceRandom = RandomSource.create(palaceSeed);
        int totalChestCount = MIN_TOTAL_CHESTS + palaceRandom.nextInt(MAX_TOTAL_CHESTS - MIN_TOTAL_CHESTS + 1);
        int maxSecondFloorChests = Math.min(
                SECOND_FLOOR_PRIMARY_CHEST_CANDIDATES.length + SECOND_FLOOR_SECONDARY_CHEST_CANDIDATES.length,
                totalChestCount - MIN_FIRST_FLOOR_CHESTS
        );
        int secondFloorChestCount = MIN_SECOND_FLOOR_CHESTS + palaceRandom.nextInt(maxSecondFloorChests - MIN_SECOND_FLOOR_CHESTS + 1);
        int firstFloorChestCount = totalChestCount - secondFloorChestCount;

        List<BlockPos> selectedLocalPositions = new ArrayList<>(totalChestCount);
        selectedLocalPositions.addAll(selectCandidatesForFloor(
                firstFloorChestCount,
                palaceRandom,
                FIRST_FLOOR_PRIMARY_CHEST_CANDIDATES,
                FIRST_FLOOR_SECONDARY_CHEST_CANDIDATES
        ));
        selectedLocalPositions.addAll(selectCandidatesForFloor(
                secondFloorChestCount,
                palaceRandom,
                SECOND_FLOOR_PRIMARY_CHEST_CANDIDATES,
                SECOND_FLOOR_SECONDARY_CHEST_CANDIDATES
        ));

        for (BlockPos localPos : selectedLocalPositions) {
            BlockPos worldPos = transformToWorld(origin, localPos, rotation);
            if (chunkBox != null && !chunkBox.isInside(worldPos)) {
                continue;
            }

            if (!level.getBlockState(worldPos).isAir()
                    || !level.getBlockState(worldPos.above()).isAir()
                    || level.getBlockState(worldPos.below()).isAir()
                    || !hasCeiling(level, worldPos)) {
                continue;
            }

            Direction facing = pickChestFacing(level, worldPos, palaceRandom);
            BlockState chestState = Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, facing);
            level.setBlock(worldPos, chestState, 3);

            if (level.getBlockEntity(worldPos) instanceof ChestBlockEntity chestBlockEntity) {
                RandomSource chestRandom = RandomSource.create(palaceSeed ^ worldPos.asLong() ^ 0xD1B54A32D192ED03L);
                fillChest(chestBlockEntity, level, chestRandom);
                chestBlockEntity.setChanged();
            }
        }
    }

    private static void fillChest(ChestBlockEntity chest, ServerLevelAccessor level, RandomSource random) {
        List<Integer> slots = createShuffledSlots(chest.getContainerSize(), random);
        int slotIndex = 0;

        chest.setItem(slots.get(slotIndex++), createMajorReward(level, random));
        if (random.nextFloat() < 0.55F && slotIndex < slots.size()) {
            chest.setItem(slots.get(slotIndex++), createMajorReward(level, random));
        }

        int bonusRolls = 3 + random.nextInt(4);
        for (int roll = 0; roll < bonusRolls && slotIndex < slots.size(); roll++) {
            chest.setItem(slots.get(slotIndex++), createBonusReward(level, random));
        }
    }

    private static ItemStack createMajorReward(ServerLevelAccessor level, RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 34) {
            return createAdvancedEnchantedBook(level, random);
        }
        if (roll < 67) {
            return createDiamondEquipment(level, random, DIAMOND_WEAPONS, 0.75F);
        }
        return createDiamondEquipment(level, random, DIAMOND_ARMOR, 0.7F);
    }

    private static ItemStack createBonusReward(ServerLevelAccessor level, RandomSource random) {
        return switch (random.nextInt(10)) {
            case 0 -> new ItemStack(Items.GOLD_INGOT, 4 + random.nextInt(9));
            case 1 -> new ItemStack(Items.GOLD_BLOCK, 1 + random.nextInt(2));
            case 2 -> new ItemStack(Items.DIAMOND, 2 + random.nextInt(5));
            case 3 -> new ItemStack(Items.DIAMOND_BLOCK, 1 + random.nextInt(2));
            case 4 -> new ItemStack(Items.IRON_INGOT, 6 + random.nextInt(11));
            case 5 -> new ItemStack(Items.EMERALD, 4 + random.nextInt(9));
            case 6 -> new ItemStack(Items.FIRE_CHARGE, 4 + random.nextInt(9));
            case 7 -> createAdvancedEnchantedBook(level, random);
            case 8 -> createDiamondEquipment(level, random, DIAMOND_WEAPONS, 0.45F);
            default -> createDiamondEquipment(level, random, DIAMOND_ARMOR, 0.45F);
        };
    }

    private static ItemStack createAdvancedEnchantedBook(ServerLevelAccessor level, RandomSource random) {
        EnchantedBookSpec spec = ENCHANTED_BOOKS[random.nextInt(ENCHANTED_BOOKS.length)];
        HolderLookup.RegistryLookup<Enchantment> enchantments = level.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> enchantment = enchantments.getOrThrow(spec.enchantment());
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment, spec.level()));
    }

    private static ItemStack createDiamondEquipment(ServerLevelAccessor level, RandomSource random, Item[] options, float enchantChance) {
        ItemStack stack = new ItemStack(options[random.nextInt(options.length)]);
        if (random.nextFloat() < enchantChance) {
            EnchantmentHelper.enchantItem(
                    random,
                    stack,
                    24 + random.nextInt(10),
                    level.getLevel().registryAccess(),
                    Optional.empty()
            );
        }
        return stack;
    }

    private static void addSelectedCandidates(List<BlockPos> selectedLocalPositions, BlockPos[] candidates, int targetCount, RandomSource random) {
        List<BlockPos> shuffledCandidates = new ArrayList<>(List.of(candidates));
        shuffle(shuffledCandidates, random);

        int selectedForFloor = 0;
        for (BlockPos candidate : shuffledCandidates) {
            if (selectedForFloor >= targetCount) {
                return;
            }
            if (isFarEnough(selectedLocalPositions, candidate)) {
                selectedLocalPositions.add(candidate);
                selectedForFloor++;
            }
        }

        for (BlockPos candidate : shuffledCandidates) {
            if (selectedForFloor >= targetCount) {
                return;
            }
            if (!selectedLocalPositions.contains(candidate)) {
                selectedLocalPositions.add(candidate);
                selectedForFloor++;
            }
        }
    }

    @SafeVarargs
    private static List<BlockPos> selectCandidatesForFloor(int targetCount, RandomSource random, BlockPos[]... candidateGroups) {
        List<BlockPos> selected = new ArrayList<>(targetCount);
        for (BlockPos[] candidateGroup : candidateGroups) {
            if (selected.size() >= targetCount) {
                break;
            }
            addSelectedCandidates(selected, candidateGroup, targetCount - selected.size(), random);
        }
        return selected;
    }

    private static boolean isFarEnough(List<BlockPos> selectedLocalPositions, BlockPos candidate) {
        for (BlockPos selected : selectedLocalPositions) {
            if (selected.distManhattan(candidate) < MIN_LOCAL_DISTANCE) {
                return false;
            }
        }
        return true;
    }

    private static Direction pickChestFacing(ServerLevelAccessor level, BlockPos worldPos, RandomSource random) {
        List<Direction> availableDirections = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos frontPos = worldPos.relative(direction);
            if (level.getBlockState(frontPos).isAir() && level.getBlockState(frontPos.above()).isAir()) {
                availableDirections.add(direction);
            }
        }

        if (availableDirections.isEmpty()) {
            return Direction.SOUTH;
        }
        return availableDirections.get(random.nextInt(availableDirections.size()));
    }

    private static boolean hasCeiling(ServerLevelAccessor level, BlockPos worldPos) {
        for (int offset = 2; offset <= MAX_CEILING_DISTANCE; offset++) {
            if (!level.getBlockState(worldPos.above(offset)).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos transformToWorld(BlockPos origin, BlockPos localPos, Rotation rotation) {
        return origin.offset(StructureTemplate.transform(localPos, Mirror.NONE, rotation, BlockPos.ZERO));
    }

    private static long computePalaceSeed(ServerLevelAccessor level, BlockPos origin, Rotation rotation) {
        long seed = level.getLevel().getSeed();
        seed ^= origin.asLong() * 0x9E3779B97F4A7C15L;
        seed ^= ((long) rotation.ordinal() + 1L) * 0x94D049BB133111EBL;
        return seed;
    }

    private static List<Integer> createShuffledSlots(int size, RandomSource random) {
        List<Integer> slots = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            slots.add(slot);
        }
        shuffle(slots, random);
        return slots;
    }

    private static <T> void shuffle(List<T> values, RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            T temp = values.get(index);
            values.set(index, values.get(swapIndex));
            values.set(swapIndex, temp);
        }
    }

    private record EnchantedBookSpec(ResourceKey<Enchantment> enchantment, int level) {
    }
}
