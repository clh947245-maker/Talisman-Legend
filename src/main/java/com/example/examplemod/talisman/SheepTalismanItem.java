package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.PlayerDecoyEntity;
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
 * 羊符咒物品类 (SheepTalismanItem) — NeoForge 1.21
 *
 * 右键使用后：
 *   1. 在玩家当前位置生成分身（PlayerDecoyEntity），
 *      复制外观、血量、装备、药水效果（排除羊符咒自身）。
 *   2. 玩家本体获得羊符咒魔法效果。
 *   3. 分身存活时间与符咒持续时间同步（MAGIC_DURATION ticks）。
 */
public class SheepTalismanItem extends Item {

    /**
     * 魔法效果 + 分身存活时间（ticks）；200 ticks = 10 秒
     */
    public static final int MAGIC_DURATION = 200;

    /**
     * 使用冷却时间（ticks）；20 ticks = 1 秒
     */
    public static final int COOLDOWN_TICKS = 20;

    public SheepTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            // ① 在原地生成分身，存活时间与符咒持续时间一致
            PlayerDecoyEntity.spawnFor(player, MAGIC_DURATION);

            // ② 给予玩家羊符咒效果（禁用粒子效果）
            player.addEffect(new MobEffectInstance(
                    ChenMod.SHEEP_POWER,
                    MAGIC_DURATION,
                    0,
                    false,
                    false,
                    true
            ));

            // ③ 冷却
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
