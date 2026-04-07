package com.example.examplemod;

import com.example.examplemod.talisman.HorseTalismanItem;
import com.example.examplemod.talisman.OxTalismanItem;
import com.example.examplemod.talisman.RabbitTalismanItem;
import com.example.examplemod.talisman.SnackTalismanItem;
import com.example.examplemod.talisman.DogTalismanItem;
import com.example.examplemod.talisman.RoosterTalismanItem;
import com.example.examplemod.talisman.MonkeyTalismanItem;
import com.example.examplemod.talisman.TigerTalismanItem;
import com.example.examplemod.talisman.DragonTalismanItem;
import com.example.examplemod.talisman.MouseTalismanItem;
import com.example.examplemod.talisman.PigTalismanItem;
import com.example.examplemod.talisman.SheepTalismanItem;
import com.example.examplemod.entity.DragonFireballEntity;
import com.example.examplemod.entity.LivingBlockEntity;
import com.example.examplemod.entity.MouseBeamEntity;
import com.example.examplemod.entity.PigLaserEntity;
import com.example.examplemod.entity.SheepBodyEntity;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.example.examplemod.magic.RabbitPowerMagic;
import com.example.examplemod.magic.HorsePowerMagic;
import com.example.examplemod.magic.OxPowerMagic;
import com.example.examplemod.magic.SnackPowerMagic;
import com.example.examplemod.magic.DogPowerMagic;
import com.example.examplemod.magic.RoosterPowerMagic;
import com.example.examplemod.magic.MonkeyPowerMagic;
import com.example.examplemod.magic.TigerPowerMagic;
import com.example.examplemod.magic.SheepPowerMagic;
import com.example.examplemod.entity.TigerCloneEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.example.examplemod.network.ServerPayloadHandler;
import com.example.examplemod.network.packet.SheepBodyTrackerPayload;
import com.example.examplemod.network.packet.SheepDisguisePayload;
import com.example.examplemod.network.packet.SheepReturnPayload;
import com.example.examplemod.network.packet.SheepSuicidePayload;
import com.example.examplemod.network.packet.TransformationSelectionPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

// 此处的值应与 META-INF/neoforge.mods.toml 文件中的条目匹配
@Mod(ChenMod.MODID)
public class ChenMod {
    // 在公共位置定义 Mod ID，供所有引用使用
    public static final String MODID = "chen_mod";
    // 直接引用 slf4j 日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // 创建一个 Deferred Register 来保存 Item（物品），所有物品都将在 "chen_mod" 命名空间下注册
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    // 注册马符咒物品
    public static final DeferredItem<HorseTalismanItem> HORSE_TALISMAN = ITEMS.register("horse_talisman", HorseTalismanItem::new);
    // 注册牛符咒物品
    public static final DeferredItem<OxTalismanItem> OX_TALISMAN = ITEMS.register("ox_talisman", OxTalismanItem::new);
    // 注册兔符咒物品
    public static final DeferredItem<RabbitTalismanItem> RABBIT_TALISMAN = ITEMS.register("rabbit_talisman", RabbitTalismanItem::new);
    // 注册蛇符咒物品
    public static final DeferredItem<SnackTalismanItem> SNACK_TALISMAN = ITEMS.register("snack_talisman", SnackTalismanItem::new);
    // 注册狗符咒物品
    public static final DeferredItem<DogTalismanItem> DOG_TALISMAN = ITEMS.register("dog_talisman", DogTalismanItem::new);
    // 注册鸡符咒物品
    public static final DeferredItem<RoosterTalismanItem> ROOSTER_TALISMAN = ITEMS.register("rooster_talisman", RoosterTalismanItem::new);
    // 注册猴符咒物品
    public static final DeferredItem<MonkeyTalismanItem> MONKEY_TALISMAN = ITEMS.register("monkey_talisman", MonkeyTalismanItem::new);
    // 注册虎符咒物品
    public static final DeferredItem<TigerTalismanItem> TIGER_TALISMAN = ITEMS.register("tiger_talisman", TigerTalismanItem::new);
    // 注册龙符咒物品
    public static final DeferredItem<DragonTalismanItem> DRAGON_TALISMAN = ITEMS.register("dragon_talisman", DragonTalismanItem::new);
    // 注册鼠符咒物品
    public static final DeferredItem<MouseTalismanItem> MOUSE_TALISMAN = ITEMS.register("mouse_talisman", MouseTalismanItem::new);
    public static final DeferredItem<PigTalismanItem> PIG_TALISMAN = ITEMS.register("pig_talisman", PigTalismanItem::new);
    // 注册羊符咒物品
    public static final DeferredItem<SheepTalismanItem> SHEEP_TALISMAN = ITEMS.register("sheep_talisman", SheepTalismanItem::new);

    /*
        注册魔法效果
    */
    // 兔子的魔法效果
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(net.minecraft.core.registries.Registries.MOB_EFFECT, MODID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, RabbitPowerMagic> RABBIT_POWER = MOB_EFFECTS.register("rabbit_power", RabbitPowerMagic::new);
    // 牛的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, OxPowerMagic> OX_POWER = MOB_EFFECTS.register("ox_power", OxPowerMagic::new);
    // 马的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, HorsePowerMagic> HORSE_POWER = MOB_EFFECTS.register("horse_power", HorsePowerMagic::new);
    // 蛇的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, SnackPowerMagic> SNACK_POWER = MOB_EFFECTS.register("snack_power", SnackPowerMagic::new);
    // 狗的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, DogPowerMagic> DOG_POWER = MOB_EFFECTS.register("dog_power", DogPowerMagic::new);
    // 鸡的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, RoosterPowerMagic> ROOSTER_POWER = MOB_EFFECTS.register("rooster_power", RoosterPowerMagic::new);
    // 猴的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, MonkeyPowerMagic> MONKEY_POWER = MOB_EFFECTS.register("monkey_power", MonkeyPowerMagic::new);
    // 虎的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, TigerPowerMagic> TIGER_POWER = MOB_EFFECTS.register("tiger_power", TigerPowerMagic::new);
    // 羊的魔法效果
    public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, SheepPowerMagic> SHEEP_POWER = MOB_EFFECTS.register("sheep_power", SheepPowerMagic::new);

    // 实体注册
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<TigerCloneEntity>> TIGER_CLONE = ENTITIES.register("tiger_clone", () ->
            EntityType.Builder.of(TigerCloneEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .build("tiger_clone"));

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<SheepBodyEntity>> SHEEP_BODY = ENTITIES.register("sheep_body", () ->
            EntityType.Builder.<SheepBodyEntity>of(SheepBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("sheep_body"));

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<DragonFireballEntity>> DRAGON_FIREBALL = ENTITIES.register("dragon_fireball", () ->
            EntityType.Builder.<DragonFireballEntity>of(DragonFireballEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("dragon_fireball"));

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<MouseBeamEntity>> MOUSE_BEAM = ENTITIES.register("mouse_beam", () ->
            EntityType.Builder.<MouseBeamEntity>of(MouseBeamEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("mouse_beam"));

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<LivingBlockEntity>> LIVING_BLOCK = ENTITIES.register("living_block", () ->
            EntityType.Builder.<LivingBlockEntity>of(LivingBlockEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.0F)
                    .clientTrackingRange(10)
                    .build("living_block"));

    public static final net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<PigLaserEntity>> PIG_LASER = ENTITIES.register("pig_laser", () ->
            EntityType.Builder.<PigLaserEntity>of(PigLaserEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("pig_laser"));



    // Mod 类的构造函数是 Mod 加载时运行的第一段代码。
    public ChenMod(IEventBus modEventBus, ModContainer modContainer) {

        // 注册 commonSetup 方法以进行 Mod 加载
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addEntityAttributes);

        // 将 Deferred Register 注册到 Mod 事件总线，以便注册物品
        ITEMS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);
        ENTITIES.register(modEventBus);

        // 注册我们自己以关注服务器和其他感兴趣的游戏事件。
        NeoForge.EVENT_BUS.register(this);

        // 将物品注册到创造模式标签页
        modEventBus.addListener(this::addCreative);
        
        // 注册网络包
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
            TransformationSelectionPayload.TYPE,
            TransformationSelectionPayload.STREAM_CODEC,
            ServerPayloadHandler::handleTransformationSelection
        );

        registrar.playToServer(
            SheepReturnPayload.TYPE,
            SheepReturnPayload.STREAM_CODEC,
            ServerPayloadHandler::handleSheepReturn
        );

        registrar.playToServer(
            SheepSuicidePayload.TYPE,
            SheepSuicidePayload.STREAM_CODEC,
            ServerPayloadHandler::handleSheepSuicide
        );
        
        registrar.playToClient(
            SheepBodyTrackerPayload.TYPE,
            SheepBodyTrackerPayload.STREAM_CODEC,
            com.example.examplemod.network.ClientPayloadHandler::handleSheepBodyTracker
        );

        registrar.playToClient(
            SheepDisguisePayload.TYPE,
            SheepDisguisePayload.STREAM_CODEC,
            com.example.examplemod.network.ClientPayloadHandler::handleSheepDisguise
        );

        registrar.playToClient(
            com.example.examplemod.network.packet.TransformationRestorePayload.TYPE,
            com.example.examplemod.network.packet.TransformationRestorePayload.STREAM_CODEC,
            com.example.examplemod.network.ClientPayloadHandler::handleTransformationRestore
        );
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 一些通用的设置代码
        LOGGER.info("ChenMod common setup complete.");
    }

    private void addEntityAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(TIGER_CLONE.get(), TigerCloneEntity.createAttributes().build());
        event.put(SHEEP_BODY.get(), SheepBodyEntity.createAttributes().build());
        event.put(LIVING_BLOCK.get(), LivingBlockEntity.createAttributes().build());
    }

    // 将物品添加到创造模式标签页
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
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
        }
    }

    // 您可以使用 SubscribeEvent 让事件总线发现要调用的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 在服务器启动时执行某些操作
        LOGGER.info("Server starting with ChenMod loaded");
    }
}
