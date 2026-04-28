package com.example.examplemod.event;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.SheepBodyEntity;
import com.example.examplemod.magic.transformation.ITransformation;
import com.example.examplemod.magic.transformation.TransformationManager;
import com.example.examplemod.network.packet.TransformationRestorePayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import com.example.examplemod.network.ModNetwork;

@EventBusSubscriber(modid = ChenMod.MODID)
public class TransformationEventHandler {

    private static final ResourceLocation MONKEY_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "monkey_transformation_health");

    /**
     * 处理实体尺寸变化事件
     * 当玩家拥有猴符咒效果时，修改其碰撞箱尺寸
     */
    @SubscribeEvent
    public static void onMobEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect().value() == ChenMod.MONKEY_POWER.get()) {
             event.getEntity().refreshDimensions();
             updateHealthAttribute(event.getEntity(), event.getEffectInstance().getAmplifier());
        }
    }

    @SubscribeEvent
    public static void onMobEffectRemove(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        handleEffectRemove(event.getEntity(), event.getEffectInstance());
    }

    @SubscribeEvent
    public static void onMobEffectExpired(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) {
        handleEffectRemove(event.getEntity(), event.getEffectInstance());
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewTarget();
        // 检查新目标是否拥有猴符咒效果
        if (newTarget != null && newTarget.hasEffect(ChenMod.MONKEY_POWER.getHolder().orElseThrow())) {
            MobEffectInstance effect = newTarget.getEffect(ChenMod.MONKEY_POWER.getHolder().orElseThrow());
            if (effect != null) {
                int id = effect.getAmplifier();
                // 只要不是变回原形 (Revert)，且不是主动攻击导致的仇恨，就取消目标
                if (id != TransformationManager.ID_REVERT) {
                     // 检查是否是复仇目标 (Revenge Target)
                     // 如果怪物最后一次受击是由该实体造成的，则允许攻击
                     if (event.getEntity() instanceof net.minecraft.world.entity.Mob mob) {
                         if (mob.getLastHurtByMob() != newTarget) {
                             event.setCanceled(true);
                         }
                     } else if (event.getEntity().getLastHurtByMob() != newTarget) {
                         event.setCanceled(true);
                     }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingFall(net.minecraftforge.event.entity.living.LivingFallEvent event) {
        if (event.getEntity().hasEffect(ChenMod.MONKEY_POWER.getHolder().orElseThrow())) {
            MobEffectInstance effect = event.getEntity().getEffect(ChenMod.MONKEY_POWER.getHolder().orElseThrow());
            if (effect != null && effect.getAmplifier() == TransformationManager.ID_CAT) {
                event.setDistance(0);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingEntityUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            // Check if item is cookie
            if (event.getItem().is(Items.COOKIE)) {
                // Check if player has Monkey Power
                MobEffectInstance effect = player.getEffect(ChenMod.MONKEY_POWER.getHolder().orElseThrow());
                if (effect != null) {
                    // Check if transformation is Parrot
                    if (effect.getAmplifier() == TransformationManager.ID_PARROT) {
                        // Kill the player (Parrot dies from cookie)
                        player.hurt(player.damageSources().magic(), 999.0f);
                    }
                }
            }
        }
    }

    private static void handleEffectRemove(net.minecraft.world.entity.LivingEntity entity, MobEffectInstance instance) {
        if (instance != null && instance.getEffect().value() == ChenMod.MONKEY_POWER.get()) {
             // 记录移除修饰符前的生命值百分比
             float currentHealth = entity.getHealth();
             float currentMax = entity.getMaxHealth();
             float healthPercentage = (currentMax > 0) ? currentHealth / currentMax : 1.0f;
             if (healthPercentage > 1.0f) healthPercentage = 1.0f;

             // 移除生命值修饰符
             AttributeInstance healthAttribute = entity.getAttribute(Attributes.MAX_HEALTH);
             if (healthAttribute != null) {
                 healthAttribute.removeModifier(MONKEY_HEALTH_ID);
                 
                 // 必须重新获取 MaxHealth，因为 removeModifier 后它变了
                 float newMax = entity.getMaxHealth();
                 float newHealth = newMax * healthPercentage;
                 
                 entity.setHealth(newHealth);
                 
                 if (!entity.level().isClientSide) {
                     System.out.println("[MonkeyTalisman] Remove Effect: " + currentHealth + "/" + currentMax + " -> " + newHealth + "/" + newMax + " (" + healthPercentage + ")");
                 }
             }

             // 1. 调用变身结束逻辑
             int transformationId = instance.getAmplifier();
             ITransformation transformation = TransformationManager.getTransformation(transformationId);
             if (transformation != null) {
                 transformation.onRemove(entity);
             }

             if (entity instanceof Player player && !player.level().isClientSide) {
                 // Schedule refresh for next tick to ensure effect is gone
                 player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 1, () -> {
                     player.refreshDimensions();
                     player.setPose(net.minecraft.world.entity.Pose.STANDING);
                     
                     // 2. 发送数据包给客户端强制刷新
                     if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                         ModNetwork.sendToPlayer(serverPlayer, new TransformationRestorePayload());
                     }
                 }));
             }
        }
    }

    private static void updateHealthAttribute(LivingEntity entity, int transformationId) {
        ITransformation transformation = TransformationManager.getTransformation(transformationId);
        if (transformation != null) {
            float targetHealth = transformation.getHealth();
            AttributeInstance healthAttribute = entity.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttribute != null) {
                // 记录添加修饰符前的生命值百分比
                float healthPercentage = entity.getHealth() / entity.getMaxHealth();

                // 移除旧的修饰符
                healthAttribute.removeModifier(MONKEY_HEALTH_ID);
                
                // 计算差值 (假设基础生命值为 20)
                // 注意：这里我们假设玩家的基础生命值是 20。如果受到其他修饰符影响，可能需要更复杂的计算。
                // 但通常我们希望的是“最终生命值 = 形态生命值”。
                // 由于 AttributeModifier 是叠加的，我们需要 careful。
                // 简单做法：amount = targetHealth - 20.0
                double amount = targetHealth - 20.0;
                
                healthAttribute.addPermanentModifier(new AttributeModifier(MONKEY_HEALTH_ID, amount, AttributeModifier.Operation.ADD_VALUE));
                
                // 恢复生命值百分比
                // 此时 getMaxHealth 已经是添加修饰符后的值 (例如变身后的生命值)
                entity.setHealth(entity.getMaxHealth() * healthPercentage);
            }
        }
    }
}
