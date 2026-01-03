package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * 狗符咒不死逻辑处理器
 * <p>
 * 监听生物死亡事件，如果拥有狗符咒效果，则阻止死亡并恢复生命值。
 * </p>
 */
@EventBusSubscriber(modid = ChenMod.MODID)
public class DogImmortalityHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 调用 DogPowerMagic 中的逻辑
        com.example.examplemod.magic.DogPowerMagic.onDeath(event);
    }
}
