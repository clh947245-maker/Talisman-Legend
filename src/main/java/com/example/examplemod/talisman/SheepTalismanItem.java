package com.example.examplemod.talisman;

import com.example.examplemod.magic.SheepPowerMagic;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 羊符咒物品类 (SheepTalismanItem) — NeoForge 1.21
 *
 * 右键使用后：
 *   1. 玩家本体获得羊符咒魔法效果。
 *   2. 冷却 COOLDOWN_TICKS。
 */
public class SheepTalismanItem extends Item {

    /** 魔法效果持续时间（ticks）；200 ticks = 10 秒 */
    public static final int MAGIC_DURATION = 200;

    /** 使用冷却时间（ticks）；20 ticks = 1 秒 */
    public static final int COOLDOWN_TICKS = 20;

    public SheepTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            // 给予玩家羊符咒效果（禁用粒子效果）
            SheepPowerMagic.grantSheepPower(player, MAGIC_DURATION);

            // 冷却
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.sheep_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
