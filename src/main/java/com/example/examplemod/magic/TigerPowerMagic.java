package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 魔法效果："虎的力量"
 * <p>
 * 生成一个具有攻击性的分身（阴暗面）。
 * 分身会复制玩家的装备，并攻击一切生物。
 * </p>
 */
public class TigerPowerMagic extends MobEffect {

    public static final String CLONE_TAG = "chen_mod.tiger_clone";
    public static final String OWNER_UUID_TAG = "OwnerUUID";

    public TigerPowerMagic() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
    }

    /**
     * API: 赋予实体虎力量效果
     */
    public static void grantTigerPower(LivingEntity entity, int duration) {
        if (entity == null) return;
        
        entity.addEffect(new MobEffectInstance(
            ChenMod.TIGER_POWER, 
            duration, 
            0, 
            false, 
            false, 
            true
        ));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide && entity instanceof Player player) {
            ServerLevel serverLevel = (ServerLevel) entity.level();
            
            // 检查玩家 NBT 中记录的分身 UUID
            CompoundTag playerData = player.getPersistentData();
            boolean needSpawn = true;
            
            if (playerData.contains("TigerCloneUUID")) {
                UUID cloneUUID = playerData.getUUID("TigerCloneUUID");
                if (serverLevel.getEntity(cloneUUID) instanceof LivingEntity clone && clone.isAlive()) {
                    needSpawn = false;
                }
            }
            
            if (needSpawn) {
                spawnClone(player, serverLevel);
            }
        }
        return true;
    }

    private void spawnClone(Player player, ServerLevel level) {
        Zombie clone = new Zombie(level);
        
        // 设置属性
        clone.setBaby(false);
        clone.setCustomName(Component.translatable("entity.chen_mod.tiger_clone", player.getName()));
        clone.setCustomNameVisible(true);
        clone.setPersistenceRequired(); // 防止被刷掉
        
        // 复制装备
        clone.setItemSlot(EquipmentSlot.MAINHAND, player.getItemBySlot(EquipmentSlot.MAINHAND).copy());
        clone.setItemSlot(EquipmentSlot.OFFHAND, player.getItemBySlot(EquipmentSlot.OFFHAND).copy());
        clone.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD).copy());
        clone.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        clone.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        clone.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());
        
        // 强化属性以匹配玩家（简单模拟）
        if (clone.getAttribute(Attributes.MAX_HEALTH) != null) {
            clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getMaxHealth());
        }
        clone.setHealth(player.getHealth());
        
        // 标记为分身
        clone.addTag(CLONE_TAG);
        clone.getPersistentData().putUUID(OWNER_UUID_TAG, player.getUUID());
        
        // 设置位置（在玩家位置）
        clone.setPos(player.getX(), player.getY(), player.getZ());
        
        level.addFreshEntity(clone);
        
        // 记录 UUID 到玩家
        player.getPersistentData().putUUID("TigerCloneUUID", clone.getUUID());
    }
}
