package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.TigerPowerMagic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.util.UUID;

@EventBusSubscriber(modid = ChenMod.MODID)
public class TigerCloneHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Zombie zombie && !event.getLevel().isClientSide) {
            if (zombie.getTags().contains(TigerPowerMagic.CLONE_TAG)) {
                // 清除默认目标
                // zombie.targetSelector.getAvailableGoals().clear(); // 不建议直接清除所有，可能会破坏基本逻辑
                
                // 添加攻击一切生物的目标 (包括玩家)
                // 优先级设为 1 (最高)
                zombie.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(zombie, LivingEntity.class, true));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.getTags().contains(TigerPowerMagic.CLONE_TAG)) {
            // 检查主人状态
            if (entity.getPersistentData().contains(TigerPowerMagic.OWNER_UUID_TAG)) {
                UUID ownerUUID = entity.getPersistentData().getUUID(TigerPowerMagic.OWNER_UUID_TAG);
                ServerLevel serverLevel = (ServerLevel) entity.level();
                
                boolean shouldDie = true;
                
                // 查找主人
                // 注意：getPlayerByUUID 只能找到在线玩家。如果玩家离线，分身也应该消失。
                Player owner = serverLevel.getPlayerByUUID(ownerUUID);
                
                if (owner != null && owner.hasEffect(ChenMod.TIGER_POWER)) {
                    shouldDie = false;
                }
                
                if (shouldDie) {
                    entity.kill(); // 或者 discard()
                    // 如果使用 kill() 会有死亡动画和掉落物。
                    // 符咒分身可能不应该掉落物品（因为是复制出来的）。
                    // 如果要避免掉落物品，可以使用 remove(RemovalReason.DISCARDED)
                    entity.remove(LivingEntity.RemovalReason.DISCARDED);
                }
            }
        }
    }
}
