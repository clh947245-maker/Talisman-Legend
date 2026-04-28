package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.MonkeyPowerMagic;
import com.example.examplemod.magic.transformation.TransformationManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 猴符咒物品类 (Monkey Talisman Item)
 * <p>
 * 该符咒赋予玩家变形的能力（基于《成龙历险记》中的猴符咒）。
 * 玩家可以通过使用该符咒在不同的生物形态之间进行切换。
 * </p>
 */
public class MonkeyTalismanItem extends Item {

    /**
     * 魔法持续时间：6000 ticks (5分钟)
     * <p>变身效果通常需要持续较长时间，以便玩家体验。</p>
     */
    public static final int MAGIC_DURATION = -1;

    /**
     * 冷却时间：20 ticks (1秒)
     * <p>防止玩家过快连续点击。</p>
     */
    public static final int COOLDOWN_TICKS = 20;

    /**
     * 构造函数
     * <p>设置物品属性，最大堆叠数量为 1 (不可堆叠)。</p>
     */
    public MonkeyTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /**
     * 当玩家右键使用物品时触发
     *
     * @param level    当前世界等级
     * @param player   使用物品的玩家
     * @param usedHand 玩家使用的手
     * @return 交互结果
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        // 仅在服务端执行逻辑，确保数据同步
        // if (!level.isClientSide) {
            // 获取当前符咒物品上存储的形态ID
            int selectedId = getSelectedTransformation(itemStack);
            // 获取对应的形态ID字符串（用于翻译）
            String transformationId = TransformationManager.getTransformation(selectedId).getId();

            // 如果选择的是“恢复原形” (ID_REVERT)
            if (selectedId == TransformationManager.ID_REVERT) {
                // 移除猴符咒的魔法效果 (变回原形)
                player.removeEffect(ChenMod.MONKEY_POWER.getHolder().orElseThrow());
                // 发送ActionBar消息提示玩家已变回原形
                player.displayClientMessage(Component.translatable("message.chen_mod.monkey_revert"), true);
            } else {
                // 赋予玩家猴符咒魔法效果，并指定形态ID
                MonkeyPowerMagic.grantMonkeyPower(player, MAGIC_DURATION, selectedId);
                // 发送ActionBar消息提示玩家已变身，并显示变身的目标形态
                player.displayClientMessage(Component.translatable("message.chen_mod.monkey_transform", Component.translatable("transformation.chen_mod." + transformationId)), true);
            }

            // 为物品添加冷却时间，防止连续快速使用
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        // }

        // 返回成功结果，表示物品已被使用
        return InteractionResultHolder.success(itemStack);
    }

    /**
     * 添加鼠标悬停提示信息 (Tooltip)
     *
     * @param stack             物品堆
     * @param context           Tooltip上下文
     * @param tooltipComponents Tooltip组件列表
     * @param tooltipFlag       Tooltip标志
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // 添加基础描述信息
        tooltipComponents.add(Component.translatable("item.chen_mod.monkey_talisman.desc"));
        
        // 获取当前选中的形态ID
        int selectedId = getSelectedTransformation(stack);
        String transformationId = TransformationManager.getTransformation(selectedId).getId();
        
        // 显示当前选中的形态
        tooltipComponents.add(Component.translatable("item.chen_mod.monkey_talisman.selected", Component.translatable("transformation.chen_mod." + transformationId)));
        
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    /**
     * 设置符咒当前选中的形态ID
     * <p>
     * 将形态ID保存到物品的 NBT 数据 (CustomData) 中。
     * </p>
     *
     * @param stack 物品堆
     * @param id    形态ID
     */
    public static void setSelectedTransformation(ItemStack stack, int id) {
        // 获取或创建 CustomData
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        // 存入 TransformationId
        tag.putInt("TransformationId", id);
        
        // 更新物品的 CustomData
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 获取符咒当前选中的形态ID
     * <p>
     * 从物品的 NBT 数据 (CustomData) 中读取形态ID。
     * </p>
     *
     * @param stack 物品堆
     * @return 形态ID，默认为 0 (恢复原形)
     */
    public static int getSelectedTransformation(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        // 读取 TransformationId，如果不存在则返回 0
        return tag.getInt("TransformationId"); // Default is 0 (Revert)
    }
}
