package com.example.examplemod.structure;

import com.example.examplemod.ChenMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

public class BuildingConstructorItem extends Item {
    private static final double REMOTE_PLACE_REACH = 64.0D;
    private static final int FALLBACK_PLACE_DISTANCE = 12;

    private final BuildingVariant variant;

    public BuildingConstructorItem(BuildingVariant variant) {
        super(new Item.Properties().stacksTo(1));
        this.variant = variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        BlockPos base = context.getClickedPos().relative(context.getClickedFace());
        return placeStructure(serverLevel, base, context.getPlayer())
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        BlockPos base = findRemoteBase(level, player);
        boolean placed = placeStructure(serverLevel, base, player);
        return placed ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    private boolean placeStructure(ServerLevel serverLevel, BlockPos base, Player player) {
        Optional<StructureTemplate> template = serverLevel.getStructureManager().get(variant.structureId());
        if (template.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable("message.chen_mod.building_constructor.missing", variant.structureId().toString()),
                        true
                );
            }
            return false;
        }

        StructureTemplate structure = template.get();
        Vec3i size = structure.getSize();
        BlockPos origin = base.offset(-size.getX() / 2, 0, -size.getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(false)
                .setKnownShape(true);

        boolean placed = structure.placeInWorld(serverLevel, origin, origin, settings, serverLevel.getRandom(), 2);
        if (placed) {
            PalaceRewardChestPlacer.placeTemplateRewardChests(serverLevel, structure, origin, variant, serverLevel.getRandom());
        }
        if (player != null) {
            Component message = placed
                    ? Component.translatable("message.chen_mod.building_constructor.placed", Component.translatable(variant.translationKey()))
                    : Component.translatable("message.chen_mod.building_constructor.failed", Component.translatable(variant.translationKey()));
            player.displayClientMessage(message, true);
        }

        return placed;
    }

    private static BlockPos findRemoteBase(Level level, Player player) {
        HitResult hit = player.pick(REMOTE_PLACE_REACH, 0.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos().relative(blockHit.getDirection());
        }

        Vec3 look = player.getLookAngle();
        int x = (int)Math.floor(player.getX() + look.x * FALLBACK_PLACE_DISTANCE);
        int z = (int)Math.floor(player.getZ() + look.z * FALLBACK_PLACE_DISTANCE);
        return new BlockPos(x, player.blockPosition().getY(), z);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.chen_mod.building_constructor.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.chen_mod.building_constructor.structure", variant.structureId().toString()).withStyle(ChatFormatting.DARK_GRAY));
    }

    public enum BuildingVariant {
        CHENGTIAN_HALL("chengtian_hall_constructor", "chengtian_hall"),
        QIYUE_PALACE("qiyue_palace_constructor", "qiyue_palace"),
        LINGXIAO_TOWER("lingxiao_tower_constructor", "lingxiao_tower"),
        TINGFENG_PAVILION("tingfeng_pavilion_constructor", "tingfeng_pavilion"),
        TINGYU_PAVILION("tingyu_pavilion_constructor", "tingyu_pavilion"),
        LINGYUN_TERRACE("lingyun_terrace_constructor", "lingyun_terrace"),
        YINGXIA_WATERSIDE("yingxia_waterside_constructor", "yingxia_waterside"),
        HUIFENG_CORRIDOR("huifeng_corridor_constructor", "huifeng_corridor"),
        FUGUANG_BOAT("fuguang_boat_constructor", "fuguang_boat"),
        FENGMING_GATE_TOWER("fengming_gate_tower_constructor", "fengming_gate_tower"),
        CHONGHUA_GATE("chonghua_gate_constructor", "chonghua_gate"),
        HANXIANG_COURTYARD("hanxiang_courtyard_constructor", "hanxiang_courtyard"),
        MINGDE_HALL("mingde_hall_constructor", "mingde_hall"),
        TINGZHU_STUDIO("tingzhu_studio_constructor", "tingzhu_studio");

        private final String itemId;
        private final String structureId;

        BuildingVariant(String itemId, String structureId) {
            this.itemId = itemId;
            this.structureId = structureId;
        }

        public String itemId() {
            return itemId;
        }

        public ResourceLocation structureId() {
            return ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "debug_buildings/" + structureId);
        }

        public static Optional<BuildingVariant> fromStructureId(ResourceLocation structureId) {
            for (BuildingVariant variant : values()) {
                if (variant.structureId().equals(structureId)) {
                    return Optional.of(variant);
                }
            }
            return Optional.empty();
        }

        public String translationKey() {
            return "item." + ChenMod.MODID + "." + itemId;
        }
    }
}
