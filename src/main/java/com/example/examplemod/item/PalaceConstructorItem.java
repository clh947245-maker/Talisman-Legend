package com.example.examplemod.item;

import com.example.examplemod.structure.NewPalacePlacement;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class PalaceConstructorItem extends Item {

    public PalaceConstructorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player && context.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos anchorPos = context.getClickedPos().relative(context.getClickedFace());
            Direction facing = sanitizeFacing(player.getDirection());
            placePalace(serverLevel, player, anchorPos, facing);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            Direction facing = sanitizeFacing(player.getDirection());
            BlockPos anchorPos = player.blockPosition().relative(facing, 2).above();
            placePalace(serverLevel, serverPlayer, anchorPos, facing);
        }
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.palace_constructor.desc"));
        tooltipComponents.add(Component.translatable("item.chen_mod.palace_constructor.desc_2").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private static void placePalace(ServerLevel level, ServerPlayer player, BlockPos anchorPos, Direction facing) {
        boolean placed = NewPalacePlacement.place(level, anchorPos, facing);
        String key = placed
                ? "message.chen_mod.palace_constructor.placed"
                : "message.chen_mod.palace_constructor.missing";
        ChatFormatting color = placed ? ChatFormatting.GOLD : ChatFormatting.RED;
        player.displayClientMessage(Component.translatable(key).withStyle(color), false);
    }

    private static Direction sanitizeFacing(Direction direction) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return Direction.SOUTH;
        }
        return direction;
    }
}
