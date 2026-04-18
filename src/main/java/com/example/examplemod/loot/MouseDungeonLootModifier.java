package com.example.examplemod.loot;

import com.example.examplemod.ChenMod;
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
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 鼠符咒地牢战利品修饰器。
 *
 * <p>当原版地牢箱子的战利品被真正结算时，这个修饰器会尝试向结果中追加鼠符咒。
 * 为了保证“一个地牢最多一个鼠符咒”，这里不会直接按箱子判重，而是改为查找箱子附近的刷怪笼，
 * 把刷怪笼位置当作该地牢的唯一标识。</p>
 */
public class MouseDungeonLootModifier extends LootModifier {
    /** 默认低概率：5%。 */
    private static final float DEFAULT_CHANCE = 0.05F;
    /** 搜索附近刷怪笼时使用的水平范围。 */
    private static final int DEFAULT_HORIZONTAL_RANGE = 8;
    /** 搜索附近刷怪笼时使用的垂直范围。 */
    private static final int DEFAULT_VERTICAL_RANGE = 5;

    /** 供数据包读取的序列化定义。 */
    public static final MapCodec<MouseDungeonLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(Codec.FLOAT.optionalFieldOf("chance", DEFAULT_CHANCE).forGetter(MouseDungeonLootModifier::chance))
            .and(Codec.INT.optionalFieldOf("horizontal_range", DEFAULT_HORIZONTAL_RANGE).forGetter(MouseDungeonLootModifier::horizontalRange))
            .and(Codec.INT.optionalFieldOf("vertical_range", DEFAULT_VERTICAL_RANGE).forGetter(MouseDungeonLootModifier::verticalRange))
            .apply(instance, MouseDungeonLootModifier::new));

    private final float chance;
    private final int horizontalRange;
    private final int verticalRange;

    public MouseDungeonLootModifier(LootItemCondition[] conditions, float chance, int horizontalRange, int verticalRange) {
        super(conditions);
        this.chance = chance;
        this.horizontalRange = Math.max(1, horizontalRange);
        this.verticalRange = Math.max(1, verticalRange);
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

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // 缺少 ORIGIN 时无法可靠定位箱子位置，直接跳过本次附加逻辑。
        if (!context.hasParam(LootContextParams.ORIGIN)) {
            return generatedLoot;
        }

        BlockPos chestPos = BlockPos.containing(context.getParam(LootContextParams.ORIGIN));
        // 用附近刷怪笼代表“这个地牢”，避免同一地牢多个箱子分别重复抽取。
        BlockPos spawnerPos = BlockPos.findClosestMatch(
                chestPos,
                this.horizontalRange,
                this.verticalRange,
                candidate -> context.getLevel().getBlockState(candidate).is(Blocks.SPAWNER)
        ).orElse(null);

        if (spawnerPos == null) {
            return generatedLoot;
        }

        DungeonLootTrackerSavedData tracker = DungeonLootTrackerSavedData.get(context.getLevel());
        // 同一个地牢只允许进行一次鼠符咒判定。
        if (tracker.isProcessed(spawnerPos)) {
            return generatedLoot;
        }

        // 先记录已处理，再掷概率；这样即使没出符咒，后续箱子也不会重复尝试。
        tracker.markProcessed(spawnerPos);
        if (context.getRandom().nextFloat() < this.chance) {
            generatedLoot.add(new ItemStack(ChenMod.MOUSE_TALISMAN.get()));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ChenMod.MOUSE_DUNGEON_LOOT_MODIFIER.get();
    }
}
