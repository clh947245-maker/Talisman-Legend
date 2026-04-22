package com.example.examplemod.structure;

import com.example.examplemod.ChenMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class ShengZhuPalacePiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE_ID = ResourceLocation.fromNamespaceAndPath(ChenMod.MODID, "shengzhu_palace");

    public ShengZhuPalacePiece(StructureTemplateManager structureTemplateManager, BlockPos origin, Rotation rotation) {
        super(
                ModStructures.SHENGZHU_PALACE_PIECE.get(),
                0,
                structureTemplateManager,
                TEMPLATE_ID,
                TEMPLATE_ID.toString(),
                makeSettings(rotation),
                origin
        );
    }

    public ShengZhuPalacePiece(StructureTemplateManager structureTemplateManager, CompoundTag tag) {
        super(
                ModStructures.SHENGZHU_PALACE_PIECE.get(),
                tag,
                structureTemplateManager,
                ignored -> makeSettings(Rotation.valueOf(tag.getString("Rot")))
        );
    }

    private static StructurePlaceSettings makeSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setKnownShape(true);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rot", this.placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        box.encapsulate(this.template.getBoundingBox(this.placeSettings, this.templatePosition));
        super.postProcess(level, structureManager, chunkGenerator, random, box, chunkPos, pivot);
        ShengZhuPalaceRewards.placeRewardChests(level, this.templatePosition, this.placeSettings.getRotation(), box);
    }
}
