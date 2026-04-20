package com.example.examplemod.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class ShengZhuPalaceStructure extends Structure {
    public static final MapCodec<ShengZhuPalaceStructure> CODEC = simpleCodec(ShengZhuPalaceStructure::new);
    private static final int SIZE_X = 82;
    private static final int SIZE_Z = 115;
    private static final int MAX_HEIGHT_DELTA = 18;

    public ShengZhuPalaceStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int originX = context.chunkPos().getMiddleBlockX() - (SIZE_X / 2);
        int originZ = context.chunkPos().getMiddleBlockZ() - (SIZE_Z / 2);

        int northWest = surfaceY(context, originX, originZ);
        int northEast = surfaceY(context, originX + SIZE_X - 1, originZ);
        int southWest = surfaceY(context, originX, originZ + SIZE_Z - 1);
        int southEast = surfaceY(context, originX + SIZE_X - 1, originZ + SIZE_Z - 1);

        int minY = Math.min(Math.min(northWest, northEast), Math.min(southWest, southEast));
        int maxY = Math.max(Math.max(northWest, northEast), Math.max(southWest, southEast));
        if (minY < context.chunkGenerator().getSeaLevel() || (maxY - minY) > MAX_HEIGHT_DELTA) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(originX, minY, originZ);
        return Optional.of(new GenerationStub(origin, builder -> this.generatePieces(builder, origin, context)));
    }

    private static int surfaceY(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(
                x,
                z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
    }

    private void generatePieces(StructurePiecesBuilder builder, BlockPos origin, GenerationContext context) {
        builder.addPiece(new ShengZhuPalacePiece(context.structureTemplateManager(), origin, net.minecraft.world.level.block.Rotation.NONE));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SHENGZHU_PALACE_STRUCTURE.get();
    }
}
