package com.example.examplemod.mixin;

import com.example.examplemod.item.OniMaskItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
public abstract class ArmorSlotMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void chenMod$preventMagicBindingRemoval(Player player, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = ((Slot) (Object) this).getItem();
        if (OniMaskItem.isOniMask(stack) && !OniMaskItem.canRemove(player, stack)) {
            cir.setReturnValue(false);
        }
    }
}
