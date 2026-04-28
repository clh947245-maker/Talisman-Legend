package com.example.examplemod.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 统一管理符咒在建筑奖励箱、考古战利品和生物掉落中的概率配置。
 */
public final class ChenModLootConfig {
    public static final String HORSE_FISHING = "horse_fishing";
    public static final String MOUSE_SIMPLE_DUNGEON = "mouse_simple_dungeon";
    public static final String MOUSE_ABANDONED_MINESHAFT = "mouse_abandoned_mineshaft";
    public static final String MOUSE_STRONGHOLD_CORRIDOR = "mouse_stronghold_corridor";
    public static final String MOUSE_STRONGHOLD_CROSSING = "mouse_stronghold_crossing";
    public static final String MOUSE_STRONGHOLD_LIBRARY = "mouse_stronghold_library";
    public static final String MONKEY_OCEAN_RUIN_WARM = "monkey_ocean_ruin_warm";
    public static final String OX_BASTION = "ox_bastion";
    public static final String RABBIT_TURTLE = "rabbit_turtle";
    public static final String DOG_EVOKER = "dog_evoker";
    public static final String DOG_TAMED_WOLF = "dog_tamed_wolf";
    public static final String SHEEP_END_CITY = "sheep_end_city";
    public static final String SNAKE_ANCIENT_CITY = "snake_ancient_city";
    public static final String TIGER_NETHER_FORTRESS = "tiger_nether_fortress";

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue horseFishingChance;
    private static final ForgeConfigSpec.DoubleValue mouseSimpleDungeonChance;
    private static final ForgeConfigSpec.DoubleValue mouseAbandonedMineshaftChance;
    private static final ForgeConfigSpec.DoubleValue mouseStrongholdCorridorChance;
    private static final ForgeConfigSpec.DoubleValue mouseStrongholdCrossingChance;
    private static final ForgeConfigSpec.DoubleValue mouseStrongholdLibraryChance;
    private static final ForgeConfigSpec.DoubleValue monkeyOceanRuinWarmChance;
    private static final ForgeConfigSpec.DoubleValue oxBastionChance;
    private static final ForgeConfigSpec.DoubleValue rabbitTurtleChance;
    private static final ForgeConfigSpec.DoubleValue dogEvokerChance;
    private static final ForgeConfigSpec.DoubleValue dogTamedWolfChance;
    private static final ForgeConfigSpec.DoubleValue sheepEndCityChance;
    private static final ForgeConfigSpec.DoubleValue snakeAncientCityChance;
    private static final ForgeConfigSpec.DoubleValue tigerNetherFortressChance;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "符咒在建筑奖励箱、考古战利品和生物掉落中的概率配置。",
                "请使用 0.0 到 1.0 之间的数值。",
                "例如：0.2 = 20%，0.4 = 40%，1.0 = 100%。"
        ).push("loot_chances");

        builder.comment("Horse talisman chance to replace fishing loot.").push("horse_talisman");
        horseFishingChance = builder.defineInRange("fishing", 0.1D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("鼠符咒在地牢、废弃矿井和末地要塞宝箱中的出现概率。").push("mouse_talisman");
        mouseSimpleDungeonChance = builder.defineInRange("simple_dungeon", 0.4D, 0.0D, 1.0D);
        mouseAbandonedMineshaftChance = builder.defineInRange("abandoned_mineshaft", 0.4D, 0.0D, 1.0D);
        mouseStrongholdCorridorChance = builder.defineInRange("stronghold_corridor", 0.4D, 0.0D, 1.0D);
        mouseStrongholdCrossingChance = builder.defineInRange("stronghold_crossing", 0.4D, 0.0D, 1.0D);
        mouseStrongholdLibraryChance = builder.defineInRange("stronghold_library", 0.4D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("猴符咒在温暖和寒冷海底遗迹考古中的出现概率。").push("monkey_talisman");
        monkeyOceanRuinWarmChance = builder.defineInRange("ocean_ruin_warm_archaeology", 0.2D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("牛符咒在猪灵堡垒宝箱中的出现概率。").push("ox_talisman");
        oxBastionChance = builder.defineInRange("bastion_remnant", 0.4D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("兔符咒在乌龟死亡掉落中的出现概率。").push("rabbit_talisman");
        rabbitTurtleChance = builder.defineInRange("turtle_drop", 0.1D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("Dog talisman drop chances from evoker totems and tamed wolves.").push("dog_talisman");
        dogEvokerChance = builder.defineInRange("evoker_totem_replace", 0.05D, 0.0D, 1.0D);
        dogTamedWolfChance = builder.defineInRange("tamed_wolf_drop", 0.05D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("羊符咒在末地城宝箱中的出现概率。").push("sheep_talisman");
        sheepEndCityChance = builder.defineInRange("end_city", 0.4D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("蛇符咒在远古城市宝箱中的出现概率。").push("snake_talisman");
        snakeAncientCityChance = builder.defineInRange("ancient_city", 0.2D, 0.0D, 1.0D);
        builder.pop();

        builder.comment("虎符咒在下界要塞宝箱中的出现概率。").push("tiger_talisman");
        tigerNetherFortressChance = builder.defineInRange("nether_fortress", 0.4D, 0.0D, 1.0D);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private ChenModLootConfig() {
    }

    /**
     * 根据配置键读取对应的掉落概率。
     *
     * @param configKey 配置项键名
     * @param fallbackChance 当键名未命中时使用的默认概率
     * @return 实际生效的掉落概率
     */
    public static float getLootChance(String configKey, float fallbackChance) {
        return switch (configKey) {
            case HORSE_FISHING -> horseFishingChance.get().floatValue();
            case MOUSE_SIMPLE_DUNGEON -> mouseSimpleDungeonChance.get().floatValue();
            case MOUSE_ABANDONED_MINESHAFT -> mouseAbandonedMineshaftChance.get().floatValue();
            case MOUSE_STRONGHOLD_CORRIDOR -> mouseStrongholdCorridorChance.get().floatValue();
            case MOUSE_STRONGHOLD_CROSSING -> mouseStrongholdCrossingChance.get().floatValue();
            case MOUSE_STRONGHOLD_LIBRARY -> mouseStrongholdLibraryChance.get().floatValue();
            case MONKEY_OCEAN_RUIN_WARM -> monkeyOceanRuinWarmChance.get().floatValue();
            case OX_BASTION -> oxBastionChance.get().floatValue();
            case RABBIT_TURTLE -> rabbitTurtleChance.get().floatValue();
            case DOG_EVOKER -> dogEvokerChance.get().floatValue();
            case DOG_TAMED_WOLF -> dogTamedWolfChance.get().floatValue();
            case SHEEP_END_CITY -> sheepEndCityChance.get().floatValue();
            case SNAKE_ANCIENT_CITY -> snakeAncientCityChance.get().floatValue();
            case TIGER_NETHER_FORTRESS -> tigerNetherFortressChance.get().floatValue();
            default -> fallbackChance;
        };
    }
}
