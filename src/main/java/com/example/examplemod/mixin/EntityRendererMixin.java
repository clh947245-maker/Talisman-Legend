package com.example.examplemod.mixin;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.SheepBodyEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实体渲染器 Mixin
 * <p>
 * 通过 Mixin 技术修改 Minecraft 底层渲染逻辑，用于处理常规事件无法覆盖的特殊渲染需求。
 * 此处用于实现蛇符咒的“完全隐身”效果，具体为消除实体脚下的阴影。
 * </p>
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    /**
     * 注入到 getShadowRadius 方法的头部
     * <p>
     * 该方法决定了实体阴影的大小（半径）。
     * 我们在方法执行前进行拦截，如果检测到实体拥有蛇符咒效果，
     * 直接强制返回 0.0f，从而使阴影完全消失。
     * </p>
     *
     * @param entity 当前正在获取阴影半径的实体
     * @param cir    回调信息，用于设置返回值并取消原方法的执行
     */
    @Inject(method = "getShadowRadius", at = @At("HEAD"), cancellable = true)
    private void onGetShadowRadius(Entity entity, CallbackInfoReturnable<Float> cir) {
        // 检查实体是否为生物（LivingEntity），因为只有生物才能拥有药水效果
        // 并且检查该生物是否拥有蛇符咒的魔法效果 (SNACK_POWER)
        if (entity instanceof SheepBodyEntity bodyEntity && bodyEntity.isSnackInvisible()) {
            cir.setReturnValue(0.0f);
            return;
        }
        if (entity instanceof LivingEntity living && living.hasEffect(ChenMod.SNACK_POWER.getHolder().orElseThrow())) {
            // 如果满足条件，将返回值设为 0.0f（无阴影）
            // 设置返回值会自动取消原方法的后续执行
            cir.setReturnValue(0.0f);
        }
    }
}
