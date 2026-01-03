package com.example.examplemod;

import com.example.examplemod.talisman.HorseTalismanItem;
import com.example.examplemod.talisman.OxTalismanItem;
import com.example.examplemod.talisman.RabbitTalismanItem;
import com.example.examplemod.talisman.SnackTalismanItem;
import com.example.examplemod.talisman.DogTalismanItem;
import com.example.examplemod.talisman.RoosterTalismanItem;
import com.example.examplemod.talisman.MonkeyTalismanItem;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.effect.MobEffect;
import com.example.examplemod.magic.RabbitPowerMagic;
import com.example.examplemod.magic.HorsePowerMagic;
import com.example.examplemod.magic.OxPowerMagic;
import com.example.examplemod.magic.SnackPowerMagic;
import com.example.examplemod.magic.DogPowerMagic;
import com.example.examplemod.magic.RoosterPowerMagic;
import com.example.examplemod.magic.MonkeyPowerMagic;
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

    // Mod 类的构造函数是 Mod 加载时运行的第一段代码。
    public ChenMod(IEventBus modEventBus, ModContainer modContainer) {

        // 注册 commonSetup 方法以进行 Mod 加载
        modEventBus.addListener(this::commonSetup);

        // 将 Deferred Register 注册到 Mod 事件总线，以便注册物品
        ITEMS.register(modEventBus);
        MOB_EFFECTS.register(modEventBus);

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

    // 将物品添加到创造模式标签页
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 将羊符咒添加到战斗（武器）标签页
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(HORSE_TALISMAN);
            event.accept(OX_TALISMAN);
            event.accept(RABBIT_TALISMAN);
            event.accept(SNACK_TALISMAN);
            event.accept(DOG_TALISMAN);
            event.accept(ROOSTER_TALISMAN);
            event.accept(MONKEY_TALISMAN);
        }
    }

    // 您可以使用 SubscribeEvent 让事件总线发现要调用的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 在服务器启动时执行某些操作
        LOGGER.info("Server starting with ChenMod loaded");
    }
}
