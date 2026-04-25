package com.example.examplemod;

import com.example.examplemod.talisman.HorseTalismanItem;
import com.example.examplemod.talisman.OxTalismanItem;
import com.example.examplemod.talisman.RabbitTalismanItem;
import com.example.examplemod.talisman.SnackTalismanItem;
import com.example.examplemod.talisman.DogTalismanItem;
import com.example.examplemod.talisman.RoosterTalismanItem;
import com.example.examplemod.talisman.MonkeyTalismanItem;
import com.example.examplemod.talisman.TigerTalismanItem;
import com.example.examplemod.talisman.TigerTalismanHalfItem;
import com.example.examplemod.talisman.DragonTalismanItem;
import com.example.examplemod.talisman.MouseTalismanItem;
import com.example.examplemod.talisman.PigTalismanItem;
import com.example.examplemod.talisman.SheepTalismanItem;
import com.example.examplemod.item.OniMaskItem;
import com.example.examplemod.config.ChenModLootConfig;
import com.example.examplemod.structure.BuildingConstructorItem;
import com.example.examplemod.structure.ShengZhuPalaceStructure;
import com.example.examplemod.loot.HorseFishingLootModifier;
import com.example.examplemod.loot.MonkeyOceanRuinLootModifier;
import com.example.examplemod.loot.MouseDungeonLootModifier;
import com.example.examplemod.loot.OxBastionLootModifier;
import com.example.examplemod.loot.RabbitTurtleLootModifier;
import com.example.examplemod.loot.SheepEndCityLootModifier;
import com.example.examplemod.loot.SnakeAncientCityLootModifier;
import com.example.examplemod.loot.TigerNetherFortressLootModifier;
import com.example.examplemod.entity.DragonFireballEntity;
import com.example.examplemod.entity.LivingBlockEntity;
import com.example.examplemod.entity.MouseBeamEntity;
import com.example.examplemod.entity.PigLaserEntity;
import com.example.examplemod.entity.PufferfishLaserEntity;
import com.example.examplemod.entity.SheepBodyEntity;
import com.example.examplemod.entity.ShengZhuEntity;
import com.example.examplemod.entity.ShadowNinjaEntity;
import com.example.examplemod.entity.AiboEntity;
import com.example.examplemod.entity.MoDiCaiEntity;
import com.example.examplemod.entity.AiboMoDiCaiFusionEntity;
import com.example.examplemod.item.PufferfishWeaponItem;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import com.example.examplemod.magic.MaskReleaseEffect;
import com.example.examplemod.magic.RabbitPowerMagic;
import com.example.examplemod.magic.HorsePowerMagic;
import com.example.examplemod.magic.OxPowerMagic;
import com.example.examplemod.magic.SnackPowerMagic;
import com.example.examplemod.magic.DogPowerMagic;
import com.example.examplemod.magic.RoosterPowerMagic;
import com.example.examplemod.magic.MonkeyPowerMagic;
import com.example.examplemod.magic.TigerPowerMagic;
import com.example.examplemod.magic.SheepPowerMagic;
import com.example.examplemod.magic.ShadowGeneralBlessingEffect;
import com.example.examplemod.entity.TigerCloneEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import com.example.examplemod.network.ServerPayloadHandler;
import com.example.examplemod.network.packet.SheepBodyTrackerPayload;
import com.example.examplemod.network.packet.ShadowNinjaCommandPayload;
import com.example.examplemod.network.packet.SheepDisguisePayload;
import com.example.examplemod.network.packet.SheepReturnPayload;
import com.example.examplemod.network.packet.SheepSuicidePayload;
import com.example.examplemod.network.packet.PufferfishWeaponAttackPayload;
import com.example.examplemod.network.packet.TransformationSelectionPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.EnumMap;
import java.util.List;

/**
 * ChenMod 主类
 *
 * 这是模组的主入口类，负责注册所有游戏内容（物品、效果、实体等）
 * 并处理模组生命周期事件。类路径和 MODID 必须与 META-INF/neoforge.mods.toml 中的配置匹配。
 */
@Mod(ChenMod.MODID)
public class ChenMod {

    /**
     * 模组唯一标识符
     *
     * 用于资源路径、注册表名称等，必须与 neoforge.mods.toml 中的 modId 一致
     */
    public static final String MODID = "chen_mod";

    /**
     * 日志记录器
     *
     * 使用 slf4j 框架记录模组运行日志
     */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 物品注册器
     *
     * 使用 Deferred Register 模式延迟注册所有模组物品
     */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CHEN_MOD_TAB = CREATIVE_MODE_TABS.register("chen_mod", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(ChenMod::createCreativeTabIcon)
                    .displayItems((parameters, output) -> addModCreativeTabContents(output))
                    .build()
    );
    /**
     * 全局战利品修饰器序列化器注册表
     *
     * 用于注册类似“地牢箱子额外注入鼠符咒”这类基于 LootContext 动态处理的掉落逻辑。
     */
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<ShengZhuPalaceStructure>> SHENG_ZHU_PALACE_STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("sheng_zhu_palace", () -> () -> ShengZhuPalaceStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHENG_ZHU_PALACE_BUILDING_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "sheng_zhu_palace_building",
                    () -> (StructurePieceType.StructureTemplateType) ShengZhuPalaceStructure.BuildingPiece::new
            );
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHENG_ZHU_PALACE_SPAWN_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "sheng_zhu_palace_spawn",
                    () -> (StructurePieceType.ContextlessType) ShengZhuPalaceStructure.ShengZhuSpawnPiece::new
            );
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHENG_ZHU_PALACE_GROUNDS_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "sheng_zhu_palace_grounds",
                    () -> (StructurePieceType.ContextlessType) ShengZhuPalaceStructure.GroundsPiece::new
            );
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHENG_ZHU_PALACE_AUXILIARY_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "sheng_zhu_palace_auxiliary",
                    () -> (StructurePieceType.ContextlessType) ShengZhuPalaceStructure.AuxiliaryBuildingPiece::new
            );

    /**
     * 盔甲材料注册器
     *
     * 用于注册自定义盔甲材料（如鬼影面具）
     */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    /**
     * 魔法绑定附魔的资源键
     *
     * 用于标识和引用魔法绑定附魔
     */
    public static final ResourceKey<Enchantment> MAGIC_BINDING_ENCHANTMENT =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, "magic_binding"));

    // ==================== 符咒物品注册 ====================

    /**
     * 马符咒
     *
     * 赋予玩家治疗能力，右键使用触发效果
     */
    public static final DeferredItem<HorseTalismanItem> HORSE_TALISMAN = ITEMS.register("horse_talisman", HorseTalismanItem::new);

    /**
     * 牛符咒
     *
     * 赋予玩家力量增强，右键使用触发效果
     */
    public static final DeferredItem<OxTalismanItem> OX_TALISMAN = ITEMS.register("ox_talisman", OxTalismanItem::new);

    /**
     * 兔符咒
     *
     * 赋予玩家速度提升，右键使用触发效果
     */
    public static final DeferredItem<RabbitTalismanItem> RABBIT_TALISMAN = ITEMS.register("rabbit_talisman", RabbitTalismanItem::new);

    /**
     * 蛇符咒
     *
     * 赋予玩家隐身能力，右键使用触发效果
     */
    public static final DeferredItem<SnackTalismanItem> SNACK_TALISMAN = ITEMS.register("snack_talisman", SnackTalismanItem::new);

    /**
     * 狗符咒
     *
     * 赋予玩家不死能力，右键使用触发效果
     */
    public static final DeferredItem<DogTalismanItem> DOG_TALISMAN = ITEMS.register("dog_talisman", DogTalismanItem::new);

    /**
     * 鸡符咒
     *
     * 赋予玩家漂浮能力，右键使用触发效果
     */
    public static final DeferredItem<RoosterTalismanItem> ROOSTER_TALISMAN = ITEMS.register("rooster_talisman", RoosterTalismanItem::new);

    /**
     * 猴符咒
     *
     * 赋予玩家变形能力，右键使用触发效果
     */
    public static final DeferredItem<MonkeyTalismanItem> MONKEY_TALISMAN = ITEMS.register("monkey_talisman", MonkeyTalismanItem::new);

    /**
     * 虎符咒
     *
     * 赋予玩家分身能力，右键使用触发效果
     */
    public static final DeferredItem<TigerTalismanItem> TIGER_TALISMAN = ITEMS.register("tiger_talisman", TigerTalismanItem::new);
    public static final DeferredItem<TigerTalismanHalfItem> TIGER_TALISMAN_LEFT_HALF =
            ITEMS.register("tiger_talisman_left_half", TigerTalismanHalfItem::newLeftHalf);
    public static final DeferredItem<TigerTalismanHalfItem> TIGER_TALISMAN_RIGHT_HALF =
            ITEMS.register("tiger_talisman_right_half", TigerTalismanHalfItem::newRightHalf);

    /**
     * 龙符咒
     *
     * 赋予玩家火焰爆破能力，右键使用触发效果
     */
    public static final DeferredItem<DragonTalismanItem> DRAGON_TALISMAN = ITEMS.register("dragon_talisman", DragonTalismanItem::new);

    /**
     * 鼠符咒
     *
     * 赋予玩家化静为动能力，右键使用触发效果
     */
    public static final DeferredItem<MouseTalismanItem> MOUSE_TALISMAN = ITEMS.register("mouse_talisman", MouseTalismanItem::new);

    /**
     * 猪符咒
     *
     * 赋予玩家激光眼能力，右键使用触发效果
     */
    public static final DeferredItem<PigTalismanItem> PIG_TALISMAN = ITEMS.register("pig_talisman", PigTalismanItem::new);

    /**
     * 羊符咒
     *
     * 赋予玩家灵魂出窍能力，右键使用触发效果
     */
    public static final DeferredItem<SheepTalismanItem> SHEEP_TALISMAN = ITEMS.register("sheep_talisman", SheepTalismanItem::new);

    /**
     * 鬼影面具盔甲材料
     *
     * 用于创建鬼影面具装备，提供基础防御属性
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ONI_MASK_MATERIAL = ARMOR_MATERIALS.register("oni_mask", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, 0);
        }

        return new ArmorMaterial(
                defense,
                0,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "oni_mask"))),
                0.0F,
                0.0F
        );
    });

    /**
     * 鬼影面具物品
     *
     * 特殊装备，佩戴后可召唤黑影兵团
     */
    public static final DeferredItem<OniMaskItem> ONI_MASK = ITEMS.register("oni_mask", () -> new OniMaskItem(ONI_MASK_MATERIAL));

    public static final DeferredItem<BuildingConstructorItem> CHENGTIAN_HALL_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.CHENGTIAN_HALL);
    public static final DeferredItem<BuildingConstructorItem> QIYUE_PALACE_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.QIYUE_PALACE);
    public static final DeferredItem<BuildingConstructorItem> LINGXIAO_TOWER_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.LINGXIAO_TOWER);
    public static final DeferredItem<BuildingConstructorItem> TINGFENG_PAVILION_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.TINGFENG_PAVILION);
    public static final DeferredItem<BuildingConstructorItem> TINGYU_PAVILION_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.TINGYU_PAVILION);
    public static final DeferredItem<BuildingConstructorItem> LINGYUN_TERRACE_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.LINGYUN_TERRACE);
    public static final DeferredItem<BuildingConstructorItem> YINGXIA_WATERSIDE_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.YINGXIA_WATERSIDE);
    public static final DeferredItem<BuildingConstructorItem> HUIFENG_CORRIDOR_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.HUIFENG_CORRIDOR);
    public static final DeferredItem<BuildingConstructorItem> FUGUANG_BOAT_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.FUGUANG_BOAT);
    public static final DeferredItem<BuildingConstructorItem> FENGMING_GATE_TOWER_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.FENGMING_GATE_TOWER);
    public static final DeferredItem<BuildingConstructorItem> CHONGHUA_GATE_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.CHONGHUA_GATE);
    public static final DeferredItem<BuildingConstructorItem> HANXIANG_COURTYARD_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.HANXIANG_COURTYARD);
    public static final DeferredItem<BuildingConstructorItem> MINGDE_HALL_CONSTRUCTOR = registerBuildingConstructor(BuildingConstructorItem.BuildingVariant.MINGDE_HALL);

    // ==================== 魔法效果注册 ====================

    /**
     * 魔法效果注册器
     *
     * 用于注册所有符咒对应的 MobEffect 效果
     */
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(net.minecraft.core.registries.Registries.MOB_EFFECT, MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, MODID);

    /**
     * 兔符咒魔法效果
     *
     * 提供速度加成
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, RabbitPowerMagic> RABBIT_POWER = MOB_EFFECTS.register("rabbit_power", RabbitPowerMagic::new);

    /**
     * 牛符咒魔法效果
     *
     * 提供力量加成
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, OxPowerMagic> OX_POWER = MOB_EFFECTS.register("ox_power", OxPowerMagic::new);

    /**
     * 马符咒魔法效果
     *
     * 提供治疗效果
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, HorsePowerMagic> HORSE_POWER = MOB_EFFECTS.register("horse_power", HorsePowerMagic::new);

    /**
     * 蛇符咒魔法效果
     *
     * 提供隐身效果
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, SnackPowerMagic> SNACK_POWER = MOB_EFFECTS.register("snack_power", SnackPowerMagic::new);

    /**
     * 狗符咒魔法效果
     *
     * 提供不死效果
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, DogPowerMagic> DOG_POWER = MOB_EFFECTS.register("dog_power", DogPowerMagic::new);

    /**
     * 鸡符咒魔法效果
     *
     * 提供漂浮效果
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, RoosterPowerMagic> ROOSTER_POWER = MOB_EFFECTS.register("rooster_power", RoosterPowerMagic::new);

    /**
     * 猴符咒魔法效果
     *
     * 提供变形能力
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, MonkeyPowerMagic> MONKEY_POWER = MOB_EFFECTS.register("monkey_power", MonkeyPowerMagic::new);

    /**
     * 虎符咒魔法效果
     *
     * 提供分身能力
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, TigerPowerMagic> TIGER_POWER = MOB_EFFECTS.register("tiger_power", TigerPowerMagic::new);

    /**
     * 羊符咒魔法效果
     *
     * 提供灵魂出窍能力
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, SheepPowerMagic> SHEEP_POWER = MOB_EFFECTS.register("sheep_power", SheepPowerMagic::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, MaskReleaseEffect> MASK_RELEASE =
            MOB_EFFECTS.register("mask_release", MaskReleaseEffect::new);

    /**
     * 鬼影将军祝福效果
     * 召唤黑影兵团的增益效果
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, ShadowGeneralBlessingEffect> SHADOW_GENERAL_BLESSING =
            MOB_EFFECTS.register("shadow_general_blessing", ShadowGeneralBlessingEffect::new);
    public static final DeferredHolder<Potion, Potion> MASK_RELEASE_POTION = POTIONS.register(
            "mask_release",
            () -> new Potion("mask_release", new MobEffectInstance(MASK_RELEASE, 3600))
    );

    // ==================== 实体注册 ====================

    /**
     * 实体类型注册器
     * 用于注册所有模组自定义实体
     */
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, MODID);

    /**
     * 虎分身实体
     * 由虎符咒召唤的分身实体
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<TigerCloneEntity>> TIGER_CLONE = ENTITIES.register("tiger_clone", () ->
            EntityType.Builder.of(TigerCloneEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .build("tiger_clone"));

    /**
     * 羊符咒躯体实体
     *
     * 灵魂出窍时留下的玩家躯体
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<SheepBodyEntity>> SHEEP_BODY = ENTITIES.register("sheep_body", () ->
            EntityType.Builder.<SheepBodyEntity>of(SheepBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("sheep_body"));

    /**
     * 龙火球实体
     *
     * 龙符咒发射的火焰弹
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<DragonFireballEntity>> DRAGON_FIREBALL = ENTITIES.register("dragon_fireball", () ->
            EntityType.Builder.<DragonFireballEntity>of(DragonFireballEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("dragon_fireball"));

    /**
     * 鼠光束实体
     *
     * 鼠符咒发射的活化光束
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<MouseBeamEntity>> MOUSE_BEAM = ENTITIES.register("mouse_beam", () ->
            EntityType.Builder.<MouseBeamEntity>of(MouseBeamEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("mouse_beam"));

    /**
     * 活化方块实体
     *
     * 被鼠符咒活化的方块实体
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<LivingBlockEntity>> LIVING_BLOCK = ENTITIES.register("living_block", () ->
            EntityType.Builder.<LivingBlockEntity>of(LivingBlockEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.0F)
                    .clientTrackingRange(10)
                    .build("living_block"));

    /**
     * 猪激光实体
     *
     * 猪符咒发射的激光
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<PigLaserEntity>> PIG_LASER = ENTITIES.register("pig_laser", () ->
            EntityType.Builder.<PigLaserEntity>of(PigLaserEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("pig_laser"));

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<PufferfishLaserEntity>> PUFFERFISH_LASER = ENTITIES.register("pufferfish_laser", () ->
            EntityType.Builder.<PufferfishLaserEntity>of(PufferfishLaserEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("pufferfish_laser"));

    /**
     * 黑影忍者实体
     *
     * 鬼影面具召唤的黑影兵团单位
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<ShadowNinjaEntity>> SHADOW_NINJA = ENTITIES.register("shadow_ninja", () ->
            EntityType.Builder.<ShadowNinjaEntity>of(ShadowNinjaEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.0F)
                    .clientTrackingRange(10)
                    .build("shadow_ninja"));

    /**
     * 黑影忍者刷怪蛋
     *
     * 用于召唤黑影忍者实体的刷怪蛋
     */
    public static final DeferredItem<DeferredSpawnEggItem> SHADOW_NINJA_SPAWN_EGG = ITEMS.register(
            "shadow_ninja_spawn_egg",
            () -> new DeferredSpawnEggItem(SHADOW_NINJA, 0x14161F, 0xC9D7DE, new Item.Properties())
    );

    /**
     * 圣主实体
     *
     * 使用 dragon_brutel 模型的敌对大型生物。
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<ShengZhuEntity>> SHENG_ZHU = ENTITIES.register("sheng_zhu", () ->
            EntityType.Builder.<ShengZhuEntity>of(ShengZhuEntity::new, MobCategory.MONSTER)
                    .fireImmune()
                    .sized(1.9F, 5.0F)
                    .clientTrackingRange(12)
                    .build("sheng_zhu"));

    /**
     * 圣主刷怪蛋
     *
     * 用于生成圣主实体。
     */
    public static final DeferredItem<DeferredSpawnEggItem> SHENG_ZHU_SPAWN_EGG = ITEMS.register(
            "sheng_zhu_spawn_egg",
            () -> new DeferredSpawnEggItem(SHENG_ZHU, 0x6A2417, 0xD9AF58, new Item.Properties())
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<AiboEntity>> AIBO = ENTITIES.register("aibo", () ->
            EntityType.Builder.<AiboEntity>of(AiboEntity::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.7F)
                    .clientTrackingRange(10)
                    .build("aibo"));

    public static final DeferredItem<DeferredSpawnEggItem> AIBO_SPAWN_EGG = ITEMS.register(
            "aibo_spawn_egg",
            () -> new DeferredSpawnEggItem(AIBO, 0xF3D28C, 0x75B3E5, new Item.Properties())
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<MoDiCaiEntity>> MO_DI_CAI = ENTITIES.register("mo_di_cai", () ->
            EntityType.Builder.<MoDiCaiEntity>of(MoDiCaiEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(10)
                    .build("mo_di_cai"));

    public static final DeferredItem<DeferredSpawnEggItem> MO_DI_CAI_SPAWN_EGG = ITEMS.register(
            "mo_di_cai_spawn_egg",
            () -> new DeferredSpawnEggItem(MO_DI_CAI, 0xE9A8A2, 0x8C4B3F, new Item.Properties())
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<AiboMoDiCaiFusionEntity>> AIBO_MO_DI_CAI_FUSION = ENTITIES.register("aibo_mo_di_cai_fusion", () ->
            EntityType.Builder.<AiboMoDiCaiFusionEntity>of(AiboMoDiCaiFusionEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 1.0F)
                    .clientTrackingRange(10)
                    .build("aibo_mo_di_cai_fusion"));

    public static final DeferredItem<DeferredSpawnEggItem> AIBO_MO_DI_CAI_FUSION_SPAWN_EGG = ITEMS.register(
            "aibo_mo_di_cai_fusion_spawn_egg",
            () -> new DeferredSpawnEggItem(AIBO_MO_DI_CAI_FUSION, 0xE7B18B, 0x5F466E, new Item.Properties())
    );

    public static final DeferredItem<PufferfishWeaponItem> PUFFERFISH_WEAPON = ITEMS.register(
            "pufferfish_weapon",
            PufferfishWeaponItem::new
    );

    private static DeferredItem<BuildingConstructorItem> registerBuildingConstructor(BuildingConstructorItem.BuildingVariant variant) {
        return ITEMS.register(variant.itemId(), () -> new BuildingConstructorItem(variant));
    }

    /**
     * 鼠符咒地牢掉落修饰器
     *
     * 负责在原版地牢箱子结算战利品时，以低概率向结果中注入鼠符咒，
     * 并配合存档判重保证单个地牢最多出现一个。
     */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<MouseDungeonLootModifier>> MOUSE_DUNGEON_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("mouse_dungeon_loot", () -> MouseDungeonLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<HorseFishingLootModifier>> HORSE_FISHING_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("horse_fishing_loot", () -> HorseFishingLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<MonkeyOceanRuinLootModifier>> MONKEY_OCEAN_RUIN_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("monkey_ocean_ruin_loot", () -> MonkeyOceanRuinLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<OxBastionLootModifier>> OX_BASTION_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("ox_bastion_loot", () -> OxBastionLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<RabbitTurtleLootModifier>> RABBIT_TURTLE_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("rabbit_turtle_loot", () -> RabbitTurtleLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SheepEndCityLootModifier>> SHEEP_END_CITY_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("sheep_end_city_loot", () -> SheepEndCityLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SnakeAncientCityLootModifier>> SNAKE_ANCIENT_CITY_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("snake_ancient_city_loot", () -> SnakeAncientCityLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<TigerNetherFortressLootModifier>> TIGER_NETHER_FORTRESS_LOOT_MODIFIER =
            LOOT_MODIFIER_SERIALIZERS.register("tiger_nether_fortress_loot", () -> TigerNetherFortressLootModifier.CODEC);

    // ==================== 构造函数 ====================

    /**
     * 模组主构造函数
     *
     * 由 NeoForge 在模组加载时调用，负责初始化所有模组内容
     *
     * @param modEventBus 模组事件总线，用于注册事件监听器
     * @param modContainer 模组容器，提供模组元数据
     */
    public ChenMod(IEventBus modEventBus, ModContainer modContainer) {

        // 注册通用设置事件监听器
        modEventBus.addListener(this::commonSetup);
        // 注册实体属性创建事件监听器
        modEventBus.addListener(this::addEntityAttributes);

        // 将所有 Deferred Register 注册到模组事件总线
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        // 供 data/chen_mod/loot_modifiers/*.json 引用自定义全局战利品修饰器类型。
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECE_TYPES.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
        ENTITIES.register(modEventBus);
        // 将当前类注册到 NeoForge 全局事件总线，用于接收游戏内事件
        NeoForge.EVENT_BUS.register(this);

        // 注册创造模式物品栏填充事件监听器
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerSpawnPlacements);

        // 注册网络数据包处理器
        modEventBus.addListener(this::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.COMMON, ChenModLootConfig.SPEC);
    }

    /**
     * 注册网络数据包
     *
     * 配置模组使用的所有网络通信数据包，包括服务器到客户端和客户端到服务器的通信
     *
     * @param event 数据包注册事件
     */
    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // 注册变形选择数据包（客户端到服务器）
        registrar.playToServer(
            TransformationSelectionPayload.TYPE,
            TransformationSelectionPayload.STREAM_CODEC,
            ServerPayloadHandler::handleTransformationSelection
        );

        // 注册羊符咒返回数据包（客户端到服务器）
        registrar.playToServer(
            SheepReturnPayload.TYPE,
            SheepReturnPayload.STREAM_CODEC,
            ServerPayloadHandler::handleSheepReturn
        );

        // 注册羊符咒自杀数据包（客户端到服务器）
        registrar.playToServer(
            SheepSuicidePayload.TYPE,
            SheepSuicidePayload.STREAM_CODEC,
            ServerPayloadHandler::handleSheepSuicide
        );

        // 注册黑影忍者命令数据包（客户端到服务器）
        registrar.playToServer(
            ShadowNinjaCommandPayload.TYPE,
            ShadowNinjaCommandPayload.STREAM_CODEC,
            ServerPayloadHandler::handleShadowNinjaCommand
        );

        registrar.playToServer(
            PufferfishWeaponAttackPayload.TYPE,
            PufferfishWeaponAttackPayload.STREAM_CODEC,
            ServerPayloadHandler::handlePufferfishWeaponAttack
        );

        // 注册羊躯体追踪数据包（服务器到客户端）
        registrar.playToClient(
            SheepBodyTrackerPayload.TYPE,
            SheepBodyTrackerPayload.STREAM_CODEC,
            com.example.examplemod.network.ClientPayloadHandler::handleSheepBodyTracker
        );

        // 注册羊伪装数据包（服务器到客户端）
        registrar.playToClient(
            SheepDisguisePayload.TYPE,
            SheepDisguisePayload.STREAM_CODEC,
            com.example.examplemod.network.ClientPayloadHandler::handleSheepDisguise
        );

        // 注册变形恢复数据包（服务器到客户端）
        registrar.playToClient(
            com.example.examplemod.network.packet.TransformationRestorePayload.TYPE,
            com.example.examplemod.network.packet.TransformationRestorePayload.STREAM_CODEC,
            com.example.examplemod.network.ClientPayloadHandler::handleTransformationRestore
        );
    }

    /**
     * 通用设置
     *
     * 在模组加载时执行一次性初始化操作
     *
     * @param event 通用设置事件
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("ChenMod common setup complete.");
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                SHADOW_NINJA.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ShadowNinjaEntity::checkShadowNinjaSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        event.register(
                AIBO_MO_DI_CAI_FUSION.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AiboMoDiCaiFusionEntity::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    @SubscribeEvent
    public void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, Items.SALMON, MASK_RELEASE_POTION);
    }

    /**
     * 添加实体属性
     *
     * 为模组自定义实体注册属性（如生命值、攻击力等）
     *
     * @param event 实体属性创建事件
     */
    private void addEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(TIGER_CLONE.get(), TigerCloneEntity.createAttributes().build());
        event.put(SHEEP_BODY.get(), SheepBodyEntity.createAttributes().build());
        event.put(LIVING_BLOCK.get(), LivingBlockEntity.createAttributes().build());
        event.put(SHADOW_NINJA.get(), ShadowNinjaEntity.createAttributes().build());
        event.put(SHENG_ZHU.get(), ShengZhuEntity.createAttributes().build());
        event.put(AIBO.get(), AiboEntity.createAttributes().build());
        event.put(MO_DI_CAI.get(), MoDiCaiEntity.createAttributes().build());
        event.put(AIBO_MO_DI_CAI_FUSION.get(), AiboMoDiCaiFusionEntity.createAttributes().build());
    }

    /**
     * 添加创造模式物品栏内容
     *
     * 将模组物品添加到指定的创造模式物品栏中
     *
     * @param event 创造模式物品栏构建事件
     */
    private static void addModCreativeTabContents(CreativeModeTab.Output output) {
        output.accept(HORSE_TALISMAN);
        output.accept(OX_TALISMAN);
        output.accept(RABBIT_TALISMAN);
        output.accept(SNACK_TALISMAN);
        output.accept(DOG_TALISMAN);
        output.accept(ROOSTER_TALISMAN);
        output.accept(MONKEY_TALISMAN);
        output.accept(TIGER_TALISMAN);
        output.accept(DRAGON_TALISMAN);
        output.accept(MOUSE_TALISMAN);
        output.accept(PIG_TALISMAN);
        output.accept(SHEEP_TALISMAN);
        output.accept(ONI_MASK);
        output.accept(PUFFERFISH_WEAPON);
        output.accept(CHENGTIAN_HALL_CONSTRUCTOR);
        output.accept(QIYUE_PALACE_CONSTRUCTOR);
        output.accept(LINGXIAO_TOWER_CONSTRUCTOR);
        output.accept(TINGFENG_PAVILION_CONSTRUCTOR);
        output.accept(TINGYU_PAVILION_CONSTRUCTOR);
        output.accept(LINGYUN_TERRACE_CONSTRUCTOR);
        output.accept(YINGXIA_WATERSIDE_CONSTRUCTOR);
        output.accept(HUIFENG_CORRIDOR_CONSTRUCTOR);
        output.accept(FUGUANG_BOAT_CONSTRUCTOR);
        output.accept(FENGMING_GATE_TOWER_CONSTRUCTOR);
        output.accept(CHONGHUA_GATE_CONSTRUCTOR);
        output.accept(HANXIANG_COURTYARD_CONSTRUCTOR);
        output.accept(MINGDE_HALL_CONSTRUCTOR);
        output.accept(createMaskReleasePotionStack());
        output.accept(createSplashMaskReleasePotionStack());
        output.accept(createLingeringMaskReleasePotionStack());
        output.accept(SHADOW_NINJA_SPAWN_EGG);
        output.accept(SHENG_ZHU_SPAWN_EGG);
        output.accept(AIBO_SPAWN_EGG);
        output.accept(MO_DI_CAI_SPAWN_EGG);
        output.accept(AIBO_MO_DI_CAI_FUSION_SPAWN_EGG);
    }

    private static ItemStack createMaskReleasePotionStack() {
        return PotionContents.createItemStack(Items.POTION, MASK_RELEASE_POTION);
    }

    private static ItemStack createSplashMaskReleasePotionStack() {
        return PotionContents.createItemStack(Items.SPLASH_POTION, MASK_RELEASE_POTION);
    }

    private static ItemStack createLingeringMaskReleasePotionStack() {
        return PotionContents.createItemStack(Items.LINGERING_POTION, MASK_RELEASE_POTION);
    }

    private static ItemStack createCreativeTabIcon() {
        return new ItemStack(DRAGON_TALISMAN.get());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 将所有符咒和鬼影面具添加到战斗物品栏
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(HORSE_TALISMAN);
            event.accept(OX_TALISMAN);
            event.accept(RABBIT_TALISMAN);
            event.accept(SNACK_TALISMAN);
            event.accept(DOG_TALISMAN);
            event.accept(ROOSTER_TALISMAN);
            event.accept(MONKEY_TALISMAN);
            event.accept(TIGER_TALISMAN);
            event.accept(DRAGON_TALISMAN);
            event.accept(MOUSE_TALISMAN);
            event.accept(PIG_TALISMAN);
            event.accept(SHEEP_TALISMAN);
            event.accept(ONI_MASK);
        }

        // 将黑影忍者刷怪蛋添加到刷怪蛋物品栏
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(SHADOW_NINJA_SPAWN_EGG);
            event.accept(SHENG_ZHU_SPAWN_EGG);
            event.accept(AIBO_SPAWN_EGG);
            event.accept(MO_DI_CAI_SPAWN_EGG);
            event.accept(AIBO_MO_DI_CAI_FUSION_SPAWN_EGG);
        }
    }

    /**
     * 服务器启动事件处理
     *
     * 当服务器开始启动时触发，可用于执行服务器级别的初始化
     *
     * @param event 服务器启动事件
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("ChenMod server starting...");
    }

}
