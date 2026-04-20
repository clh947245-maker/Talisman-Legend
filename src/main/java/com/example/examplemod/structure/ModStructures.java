package com.example.examplemod.structure;

import com.example.examplemod.ChenMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ChenMod.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, ChenMod.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<ShengZhuPalaceStructure>> SHENGZHU_PALACE_STRUCTURE =
            STRUCTURE_TYPES.register("shengzhu_palace", () -> () -> ShengZhuPalaceStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHENGZHU_PALACE_PIECE =
            STRUCTURE_PIECES.register(
                    "shengzhu_palace",
                    () -> (context, tag) -> new ShengZhuPalacePiece(context.structureTemplateManager(), tag)
            );

    private ModStructures() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
    }
}
