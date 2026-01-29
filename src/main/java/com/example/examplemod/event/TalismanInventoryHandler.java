package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 符咒背包检测处理器
 * <p>
 * 用于实时监测玩家背包中是否持有对应的符咒物品。
 * 如果玩家拥有符咒效果但背包中没有对应的符咒物品，则将效果时间缩短。
 * </p>
 */
@EventBusSubscriber(modid = ChenMod.MODID)
public class TalismanInventoryHandler {

    /**
     * 玩家每 tick 更新时触发
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        // 检测马符咒
        if (player.hasEffect(ChenMod.HORSE_POWER)) {
            checkTalismanInInventory(player, ChenMod.HORSE_POWER, ChenMod.HORSE_TALISMAN.get());
        }
        
        // 检测牛符咒
        if (player.hasEffect(ChenMod.OX_POWER)) {
            checkTalismanInInventory(player, ChenMod.OX_POWER, ChenMod.OX_TALISMAN.get());
        }

        // 检测兔符咒
        if (player.hasEffect(ChenMod.RABBIT_POWER)) {
            checkTalismanInInventory(player, ChenMod.RABBIT_POWER, ChenMod.RABBIT_TALISMAN.get());
        }

        // 检测蛇符咒
        if (player.hasEffect(ChenMod.SNACK_POWER)) {
            checkTalismanInInventory(player, ChenMod.SNACK_POWER, ChenMod.SNACK_TALISMAN.get());
        }

        // 检测狗符咒
        if (player.hasEffect(ChenMod.DOG_POWER)) {
            checkTalismanInInventory(player, ChenMod.DOG_POWER, ChenMod.DOG_TALISMAN.get());
        }

        // 检测鸡符咒
        if (player.hasEffect(ChenMod.ROOSTER_POWER)) {
            checkTalismanInInventory(player, ChenMod.ROOSTER_POWER, ChenMod.ROOSTER_TALISMAN.get());
        }

        // 检测猴符咒
        if (ChenMod.MONKEY_POWER.isBound() && player.hasEffect(ChenMod.MONKEY_POWER)) {
            checkTalismanInInventory(player, ChenMod.MONKEY_POWER, ChenMod.MONKEY_TALISMAN.get());
        }

        // 检测虎符咒
        if (ChenMod.TIGER_POWER.isBound() && player.hasEffect(ChenMod.TIGER_POWER)) {
            checkTalismanInInventory(player, ChenMod.TIGER_POWER, ChenMod.TIGER_TALISMAN.get());
        }
    }

    /**
     * 检查玩家背包中是否有指定符咒，如果没有，缩短对应魔法效果时间
     *
     * @param player 玩家实体
     * @param effect 魔法效果 (Holder)
     * @param item   符咒物品
     */
    private static void checkTalismanInInventory(Player player, Holder<MobEffect> effect, net.minecraft.world.item.Item item) {
        boolean hasItem = false;
        
        // 检查主背包
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                hasItem = true;
                break;
            }
        }
        
        // 检查副手
        if (!hasItem && player.getOffhandItem().getItem() == item) {
            hasItem = true;
        }

        // 如果没有物品，且效果时间还很长，则缩短时间
        if (!hasItem) {
            MobEffectInstance instance = player.getEffect(effect);
            // 10 ticks = 0.5 秒
            if (instance != null && instance.getDuration() > 10) {
                 // 通过重新添加效果来更新持续时间
                 // 保持原有的 amplifier 等属性
                 int amplifier = instance.getAmplifier();
                 boolean ambient = instance.isAmbient();
                 boolean visible = instance.isVisible();
                 boolean showIcon = instance.showIcon();
                 
                 // 移除旧效果并添加新效果（短时间）
                 player.removeEffect(effect);
                 player.addEffect(new MobEffectInstance(effect, 10, amplifier, ambient, visible, showIcon));
            }
        }
    }
}
