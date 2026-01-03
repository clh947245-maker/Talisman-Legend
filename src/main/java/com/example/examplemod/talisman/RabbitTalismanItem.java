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
 * 兔符咒 (Rabbit Talisman) 物品类
 * <p>
 * 右键使用后给予玩家长时间的“兔子的力量”效果（移动速度提升）。
 * </p>
 */
public class RabbitTalismanItem extends Item {

    // 魔法效果持续时间常量 (半分钟)
    public static final int MAGIC_DURATION = 600;
    // 冷却时间常量 (两分钟)
    public static final int COOLDOWN_TICKS = 20;

    public RabbitTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /**
     * 处理物品的右键点击交互逻辑
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            // 给予兔符咒效果
            player.addEffect(new MobEffectInstance(ChenMod.RABBIT_POWER, MAGIC_DURATION, 0, true, true, true));
            // 添加冷却时间
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.rabbit_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
