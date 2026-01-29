package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.DragonFireballEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DragonTalismanItem extends Item {

    public static final int COOLDOWN_TICKS = 20; // 1秒冷却

    public DragonTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            // 创建龙爆破实体
            Vec3 lookVec = player.getLookAngle();
            
            // 初始位置在玩家头部前方
            double startX = player.getX() + lookVec.x * 1.5;
            double startY = player.getEyeY() + lookVec.y * 1.5;
            double startZ = player.getZ() + lookVec.z * 1.5;

            // 加速度向量 (控制飞行方向和速度)
            double accelX = lookVec.x * 0.1;
            double accelY = lookVec.y * 0.1;
            double accelZ = lookVec.z * 0.1;

            DragonFireballEntity fireball = new DragonFireballEntity(level, player, accelX, accelY, accelZ);
            fireball.setPos(startX, startY, startZ);
            
            // 添加到世界
            level.addFreshEntity(fireball);

            // 添加冷却时间
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.dragon_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
