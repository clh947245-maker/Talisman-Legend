package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import com.example.examplemod.entity.TigerCloneEntity;
import com.example.examplemod.magic.TigerPowerMagic;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class TigerTalismanHalfItem extends Item {
    private static final String TAG_LINKED_CLONE_UUID = "TigerCloneUUID";
    private static final String TAG_LINKED_OWNER_UUID = "TigerOwnerUUID";
    private static final double FUSION_DISTANCE = 5.0D;

    private final String descriptionKey;

    private TigerTalismanHalfItem(String descriptionKey) {
        super(new Item.Properties().stacksTo(1));
        this.descriptionKey = descriptionKey;
    }

    public static TigerTalismanHalfItem newLeftHalf() {
        return new TigerTalismanHalfItem("item.chen_mod.tiger_talisman_left_half.desc");
    }

    public static TigerTalismanHalfItem newRightHalf() {
        return new TigerTalismanHalfItem("item.chen_mod.tiger_talisman_right_half.desc");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        TigerCloneEntity clone = TigerPowerMagic.getTrackedClone(player.getServer(), getLinkedCloneUUID(stack));
        if (clone != null
                && clone.level() == level
                && clone.distanceToSqr(player) <= FUSION_DISTANCE * FUSION_DISTANCE
                && player.hasEffect(ChenMod.TIGER_POWER)) {
            TigerTalismanItem.mergeBack(level, player, usedHand, stack, clone);
        } else {
            Component tracker = buildTrackerMessage(player.getServer(), player, stack);
            if (tracker != null) {
                player.displayClientMessage(tracker, true);
            } else {
                ItemStack restored = restoreFullTalisman(stack);
                player.setItemInHand(usedHand, restored);
                player.displayClientMessage(Component.translatable("message.chen_mod.tiger_restored"), true);
            }
        }

        player.getCooldowns().addCooldown(this, TigerTalismanItem.COOLDOWN_TICKS);
        return InteractionResultHolder.success(player.getItemInHand(usedHand));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(this.descriptionKey));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    public static boolean isTigerHalf(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.is(ChenMod.TIGER_TALISMAN_LEFT_HALF.get()) || stack.is(ChenMod.TIGER_TALISMAN_RIGHT_HALF.get());
    }

    @Nullable
    public static ItemStack getHeldTigerHalf(net.minecraft.world.entity.player.Player player) {
        if (player == null) {
            return null;
        }
        if (isTigerHalf(player.getMainHandItem())) {
            return player.getMainHandItem();
        }
        if (isTigerHalf(player.getOffhandItem())) {
            return player.getOffhandItem();
        }
        return null;
    }

    public static void showTracker(net.minecraft.world.entity.player.Player player) {
        ItemStack heldHalf = getHeldTigerHalf(player);
        if (heldHalf == null || heldHalf.isEmpty()) {
            return;
        }

        Component tracker = buildTrackerMessage(player.getServer(), player, heldHalf);
        if (tracker != null) {
            player.displayClientMessage(tracker, true);
        }
    }

    public static ItemStack createLinkedHalf(ItemStack source, Item halfItem, TigerCloneEntity clone, UUID ownerUUID) {
        ItemStack half = source.transmuteCopy(halfItem);
        setLinkedCloneUUID(half, clone.getUUID());
        setLinkedOwnerUUID(half, ownerUUID);
        return half;
    }

    public static ItemStack restoreFullTalisman(ItemStack source) {
        ItemStack restored = source.transmuteCopy(ChenMod.TIGER_TALISMAN.get());
        clearTigerLink(restored);
        return restored;
    }

    public static boolean restoreLinkedHalf(Player player, UUID cloneUUID) {
        if (player == null || cloneUUID == null) {
            return false;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isTigerHalf(stack) && cloneUUID.equals(getLinkedCloneUUID(stack))) {
                player.getInventory().setItem(slot, restoreFullTalisman(stack));
                return true;
            }
        }

        return false;
    }

    @Nullable
    public static UUID getLinkedCloneUUID(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.hasUUID(TAG_LINKED_CLONE_UUID) ? tag.getUUID(TAG_LINKED_CLONE_UUID) : null;
    }

    @Nullable
    public static UUID getLinkedOwnerUUID(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        return tag.hasUUID(TAG_LINKED_OWNER_UUID) ? tag.getUUID(TAG_LINKED_OWNER_UUID) : null;
    }

    private static void setLinkedCloneUUID(ItemStack stack, UUID cloneUUID) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.putUUID(TAG_LINKED_CLONE_UUID, cloneUUID);
        writeCustomDataTag(stack, tag);
    }

    private static void setLinkedOwnerUUID(ItemStack stack, UUID ownerUUID) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.putUUID(TAG_LINKED_OWNER_UUID, ownerUUID);
        writeCustomDataTag(stack, tag);
    }

    private static void clearTigerLink(ItemStack stack) {
        CompoundTag tag = getCustomDataTag(stack);
        tag.remove(TAG_LINKED_CLONE_UUID);
        tag.remove(TAG_LINKED_OWNER_UUID);
        writeCustomDataTag(stack, tag);
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customData.copyTag();
    }

    private static void writeCustomDataTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Nullable
    private static Component buildTrackerMessage(@Nullable MinecraftServer server,
                                                 net.minecraft.world.entity.player.Player player,
                                                 ItemStack stack) {
        UUID linkedCloneUUID = getLinkedCloneUUID(stack);
        if (linkedCloneUUID == null) {
            return null;
        }

        TigerCloneEntity clone = TigerPowerMagic.getTrackedClone(server, linkedCloneUUID);
        TigerPowerMagic.CloneTrackerSnapshot snapshot = clone != null
                ? new TigerPowerMagic.CloneTrackerSnapshot(clone.position(), clone.level().dimension())
                : TigerPowerMagic.getCloneSnapshot(linkedCloneUUID);

        if (snapshot == null) {
            return null;
        }

        int cloneX = Mth.floor(snapshot.position().x);
        int cloneY = Mth.floor(snapshot.position().y);
        int cloneZ = Mth.floor(snapshot.position().z);

        if (!player.level().dimension().equals(snapshot.dimension())) {
            return Component.translatable(
                    "message.chen_mod.tiger_tracker_other_dimension",
                    cloneX,
                    cloneY,
                    cloneZ
            );
        }

        double dx = snapshot.position().x - player.getX();
        double dz = snapshot.position().z - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        return Component.translatable(
                "message.chen_mod.tiger_tracker",
                Component.translatable(getDirectionKey(dx, dz)),
                String.format(Locale.ROOT, "%.1f", distance),
                cloneX,
                cloneY,
                cloneZ
        );
    }

    private static String getDirectionKey(double dx, double dz) {
        if (Math.abs(dx) < 1.0D && Math.abs(dz) < 1.0D) {
            return "direction.chen_mod.here";
        }

        double angle = Math.toDegrees(Math.atan2(dz, dx));
        int index = Mth.floor((angle + 22.5D) / 45.0D) & 7;
        return switch (index) {
            case 0 -> "direction.chen_mod.east";
            case 1 -> "direction.chen_mod.south_east";
            case 2 -> "direction.chen_mod.south";
            case 3 -> "direction.chen_mod.south_west";
            case 4 -> "direction.chen_mod.west";
            case 5 -> "direction.chen_mod.north_west";
            case 6 -> "direction.chen_mod.north";
            default -> "direction.chen_mod.north_east";
        };
    }
}
