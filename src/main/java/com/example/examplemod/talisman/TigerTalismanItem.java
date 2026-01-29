package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.TigerCloneEntity;
import com.example.examplemod.magic.TigerPowerMagic;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TigerTalismanItem extends Item {

    public static final int MAGIC_DURATION = -1;
    public static final int COOLDOWN_TICKS = 20;

    public TigerTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            // 检查玩家是否已经拥有虎符咒效果（意味着可能已经存在分身）
            if (player.hasEffect(ChenMod.TIGER_POWER)) {
                // 定义搜索区域：玩家周围半径 5 格的立方体区域
                // inflate(5.0) 会在所有方向上扩展 5.0 的距离
                AABB searchArea = player.getBoundingBox().inflate(5.0);
                
                // 查找该区域内属于当前玩家的 TigerCloneEntity 列表
                // 使用 lambda 表达式过滤：必须有 ownerUUID 且 ownerUUID 与当前玩家一致
                List<TigerCloneEntity> clones = level.getEntitiesOfClass(TigerCloneEntity.class, searchArea, 
                    entity -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID()));

                if (!clones.isEmpty()) {
                    // 找到了分身，执行融合逻辑
                    // 获取第一个找到的分身（通常应该只有一个）
                    TigerCloneEntity clone = clones.get(0);
                    
                    // 播放融合特效和音效
                    if (level instanceof ServerLevel serverLevel) {
                        // 在分身位置生成 "POOF" 粒子（烟雾散去效果）
                        serverLevel.sendParticles(ParticleTypes.POOF, clone.getX(), clone.getY() + 1, clone.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                        // 在玩家位置生成 "POOF" 粒子
                        serverLevel.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                    // 播放灭火的声音作为融合音效
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);

                    // 移除分身实体
                    clone.discard();
                    // 移除玩家身上的魔法效果
                    player.removeEffect(ChenMod.TIGER_POWER);
                    // 向玩家发送融合成功的消息
                    player.displayClientMessage(Component.literal("§a已与分身融合！"), true);
                } else {
                    // 玩家有效果（说明应该有分身），但在范围内没找到
                    // 提示玩家分身太远，不执行任何操作（防止无限生成分身）
                    player.displayClientMessage(Component.literal("§c分身太远，无法融合！"), true);
                }
                
                // 无论融合成功与否，都添加冷却时间，防止频繁点击
                player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
                return InteractionResultHolder.success(itemStack);
            }

            // --- 以下是生成分身的逻辑 ---

            // 1. 给予玩家魔法效果，用于维持分身的存在
            // 如果此效果消失（例如持续时间结束或被清除），分身也会自动消失
            TigerPowerMagic.grantTigerPower(player, MAGIC_DURATION);

            // 2. 创建分身实体
            // 注意：必须确保 ChenMod.TIGER_CLONE 已经正确注册
            TigerCloneEntity clone = new TigerCloneEntity(ChenMod.TIGER_CLONE.get(), level);
            
            // 设置分身位置与玩家重合
            clone.setPos(player.getX(), player.getY(), player.getZ());
            // 设置分身的主人，用于后续逻辑判断（如融合、消失等）
            clone.setOwnerUUID(player.getUUID());
            // 设置分身的名字，格式为 "PlayerName's Shadow"
            clone.setCustomName(Component.literal(player.getName().getString() + "'s Shadow"));
            // 始终显示名字标签
            clone.setCustomNameVisible(true);

            // 复制属性 (攻击力和生命值)
            if (clone.getAttribute(Attributes.ATTACK_DAMAGE) != null && player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                clone.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
            if (clone.getAttribute(Attributes.MAX_HEALTH) != null && player.getAttribute(Attributes.MAX_HEALTH) != null) {
                clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getAttributeValue(Attributes.MAX_HEALTH));
                clone.setHealth(player.getHealth()); // 设置当前生命值
            }
            
            // 3. 复制玩家的所有装备到分身上，并设置不掉落
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    clone.setItemSlot(slot, stack.copy());
                    // 设置掉落概率为 0，防止刷物品
                    clone.setDropChance(slot, 0.0F);
                }
            }

            // 4. 将分身实体添加到世界中
            level.addFreshEntity(clone);

            // 5. 添加冷却时间
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.tiger_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
