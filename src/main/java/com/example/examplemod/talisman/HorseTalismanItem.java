package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 马符咒物品类 (HorseTalismanItem)
 * <p>
 * 该类定义了“马符咒”这一特殊物品的行为逻辑。
 * </p>
 * <p>
 * 主要功能点：
 * <ul>
 * <li>右键使用：获得持续的马符咒魔法效果（生命恢复 + 净化）。</li>
 * <li>持续时间：常量定义（如 5 分钟）。</li>
 * <li>背包检测：若背包中移除该符咒，效果时间缩短为 0.5 秒。</li>
 * </ul>
 * </p>
 */
public class HorseTalismanItem extends Item {

    // 魔法效果持续时间常量 (tick)
    // 半分钟 = 30 * 20 = 600 ticks
    public static final int MAGIC_DURATION = 600;
    // 冷却时间常量 (tick)
    // 20 ticks
    public static final int COOLDOWN_TICKS = 20;

    /**
     * 构造函数
     */
    public HorseTalismanItem() {
        super(new Item.Properties()
                .stacksTo(1) // 设置最大堆叠数量为 1
        );
    }

    /**
     * 处理物品的右键点击交互逻辑
     * <p>
     * 当玩家手持该物品并按下右键时触发。
     * 给予玩家马符咒魔法效果。
     * </p>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            // 给予马符咒效果
            // 使用 .get() 从 DeferredHolder 获取 MobEffect 实例，或者直接传入如果支持
            // 参考 SnackPowerMagic 使用方式
            player.addEffect(new MobEffectInstance(ChenMod.HORSE_POWER, MAGIC_DURATION,
                    0,
                    true,
                    true, true));

            // 添加冷却时间，防止连续刷
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(itemStack);
    }

    /**
     * 添加物品的工具提示信息
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.horse_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
