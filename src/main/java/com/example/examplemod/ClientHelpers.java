package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

/**
 * 客户端辅助类 (ClientHelpers)
 * <p>
 * 该类用于存放仅在客户端（Client）环境下运行的辅助方法。
 * </p>
 * <p>
 * <strong>重要提示：</strong>
 * 此类中引用的 {@link Minecraft} 等类属于客户端专用类。
 * 如果在服务端（Server）环境下直接加载或调用此类，会导致 {@code NoClassDefFoundError} 崩溃。
 * 因此，调用此类方法前必须先检查 {@code Dist.CLIENT}。
 * </p>
 */
public class ClientHelpers {

    /**
     * 检查指定物品是否处于冷却状态
     * <p>
     * 该方法通过访问客户端唯一的 {@link Minecraft} 实例来获取当前玩家对象，
     * 并查询其冷却时间追踪器。
     * </p>
     *
     * @param item 需要检查冷却状态的物品
     * @return 如果玩家存在且物品处于冷却中，返回 {@code true}；否则返回 {@code false}
     */
    public static boolean isCooldown(Item item) {
        // 确保玩家对象不为空（例如在进入世界前可能为空）
        if (Minecraft.getInstance().player != null) {
            return Minecraft.getInstance().player.getCooldowns().isOnCooldown(item);
        }
        return false;
    }

    /**
     * 检查物品堆栈是否被当前玩家拿在手中（主手或副手）
     */
    public static boolean isHeld(net.minecraft.world.item.ItemStack stack) {
        if (Minecraft.getInstance().player != null) {
            net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
            return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        }
        return false;
    }

    /**
     * 检查当前玩家是否在移动
     * 使用位置差值判断，比速度向量更准确
     */
    public static boolean isMoving() {
        if (Minecraft.getInstance().player != null) {
            var player = Minecraft.getInstance().player;
            double dx = player.getX() - player.xo;
            double dz = player.getZ() - player.zo;
            return dx * dx + dz * dz > 0.0001;
        }
        return false;
    }

    /**
     * 处理变身恢复逻辑 (强制刷新尺寸)
     */
    public static void handleRestore() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.refreshDimensions();
            Minecraft.getInstance().player.setPose(net.minecraft.world.entity.Pose.STANDING);
        }
    }
}
