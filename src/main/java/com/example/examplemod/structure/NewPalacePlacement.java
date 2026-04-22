package com.example.examplemod.structure;

import com.example.examplemod.ChenMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

public final class NewPalacePlacement {
    public static final ResourceLocation STRUCTURE_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shengzhu_palace");
    public static final int SIZE_X = 82;
    public static final int SIZE_Z = 115;

    private NewPalacePlacement() {
    }

    public static boolean place(ServerLevel level, BlockPos anchorPos, Direction facing) {
        Optional<StructureTemplate> optionalTemplate = level.getStructureManager().get(STRUCTURE_ID);
        if (optionalTemplate.isEmpty()) {
            return false;
        }

        Rotation rotation = toRotation(facing);
        BlockPos origin = switch (facing) {
            case NORTH -> anchorPos.offset(SIZE_X / 2, 0, -(SIZE_Z - 1));
            case EAST -> anchorPos.offset(0, 0, -(SIZE_X / 2));
            case WEST -> anchorPos.offset(-(SIZE_Z - 1), 0, SIZE_X / 2);
            case SOUTH, UP, DOWN -> anchorPos.offset(-(SIZE_X / 2), 0, 0);
        };

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false)
                .setKnownShape(true);

        boolean placed = optionalTemplate.get().placeInWorld(level, origin, origin, settings, level.random, 2);
        if (placed) {
            ShengZhuPalaceRewards.placeRewardChests(level, origin, rotation, null);
        }
        return placed;
    }

    private static Rotation toRotation(Direction facing) {
        return switch (facing) {
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.CLOCKWISE_90;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            case SOUTH, UP, DOWN -> Rotation.NONE;
        };
    }
}
