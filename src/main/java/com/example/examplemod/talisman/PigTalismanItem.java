package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.PigLaserEntity;
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

public class PigTalismanItem extends Item {

    public static final int COOLDOWN_TICKS = 30; // 1.5秒冷却

    public PigTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            Vec3 lookVec = player.getLookAngle();
            double speed = 2.5;
            
            // 计算从玩家眼睛两侧发射的位置（模拟双眼）
            Vec3 right = new Vec3(-lookVec.z, 0, lookVec.x).normalize();
            double separation = 0.15; // 两道激光的间距
            
            // 左眼激光
            double leftX = player.getX() + right.x * separation;
            double leftZ = player.getZ() + right.z * separation;
            spawnLaser(level, player, lookVec, speed, leftX, leftZ);
            
            // 右眼激光
            double rightX = player.getX() - right.x * separation;
            double rightZ = player.getZ() - right.z * separation;
            spawnLaser(level, player, lookVec, speed, rightX, rightZ);

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(itemStack);
    }

    private void spawnLaser(Level level, Player player, Vec3 lookVec, double speed, double offsetX, double offsetZ) {
        double startX = offsetX + lookVec.x * 0.5;
        double startY = player.getEyeY() + lookVec.y * 0.5 - 0.1;
        double startZ = offsetZ + lookVec.z * 0.5;

        double accelX = lookVec.x * speed * 0.1;
        double accelY = lookVec.y * speed * 0.1;
        double accelZ = lookVec.z * speed * 0.1;

        PigLaserEntity laser = new PigLaserEntity(level, player, startX, startY, startZ, accelX, accelY, accelZ);
        level.addFreshEntity(laser);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.pig_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
