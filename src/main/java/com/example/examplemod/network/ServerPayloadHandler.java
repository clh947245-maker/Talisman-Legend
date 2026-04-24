package com.example.examplemod.network;

import com.example.examplemod.ChenMod;
import com.example.examplemod.magic.SheepPowerMagic;
import com.example.examplemod.magic.ShadowNinjaSquadManager;
import com.example.examplemod.magic.transformation.TransformationManager;
import com.example.examplemod.item.PufferfishWeaponItem;
import com.example.examplemod.network.packet.SheepReturnPayload;
import com.example.examplemod.network.packet.SheepSuicidePayload;
import com.example.examplemod.network.packet.ShadowNinjaCommandPayload;
import com.example.examplemod.network.packet.TransformationSelectionPayload;
import com.example.examplemod.item.OniMaskItem;
import com.example.examplemod.talisman.MonkeyTalismanItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 处理客户端发送到服务端的自定义网络包。
 */
public class ServerPayloadHandler {

    public static void handleTransformationSelection(final TransformationSelectionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            ItemStack stack = player.getMainHandItem();
            // 优先检查主手，主手不是猴符咒时再回退到副手。
            if (!(stack.getItem() instanceof MonkeyTalismanItem)) {
                stack = player.getOffhandItem();
            }

            if (stack.getItem() instanceof MonkeyTalismanItem) {
                // 只接受合法变形 ID，避免越界或伪造数据写入物品状态。
                if (payload.transformationId() >= 0 && payload.transformationId() < TransformationManager.getTransformationCount()) {
                    // 将选择结果写回物品数据，供后续实际变形时读取。
                    MonkeyTalismanItem.setSelectedTransformation(stack, payload.transformationId());
                }
            }
        });
    }

    public static void handleSheepReturn(final SheepReturnPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.hasEffect(ChenMod.SHEEP_POWER)) {
                return;
            }

            var targetBody = SheepPowerMagic.getNearestReturnableBody(player);
            if (targetBody == null) {
                return;
            }

            // 先记录准备回归的身体，再移除效果触发后续回归流程。
            SheepPowerMagic.setPendingReturnBody(player, targetBody);
            player.removeEffect(ChenMod.SHEEP_POWER);
        });
    }

    public static void handleSheepSuicide(final SheepSuicidePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.hasEffect(ChenMod.SHEEP_POWER)) {
                return;
            }

            // 自尽分支会跳过回魂，直接清掉灵魂绑定和追踪身体数据。
            SheepPowerMagic.markSkipRestore(player);
            SheepPowerMagic.discardTrackedBody(player);
            SheepPowerMagic.clearSoulState(player);
            player.removeEffect(ChenMod.SHEEP_POWER);
            player.hurt(player.damageSources().magic(), 999999.0F);
        });
    }

    public static void handleShadowNinjaCommand(final ShadowNinjaCommandPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }

            if (OniMaskItem.getMaskAnchor(player) == null) {
                return;
            }

            // 根据动作常量执行召唤或遣散影忍小队。
            if (payload.action() == ShadowNinjaCommandPayload.ACTION_SUMMON) {
                ShadowNinjaSquadManager.summonSquad(player);
            } else if (payload.action() == ShadowNinjaCommandPayload.ACTION_DISMISS) {
                ShadowNinjaSquadManager.dismissAll(player);
            }
        });
    }

    public static void handlePufferfishWeaponAttack(final com.example.examplemod.network.packet.PufferfishWeaponAttackPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> PufferfishWeaponItem.fireFromMainHand(context.player()));
    }
}
