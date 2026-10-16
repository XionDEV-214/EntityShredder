package com.deveisia.enceladus.entityshredder.registry;

import com.deveisia.enceladus.entityshredder.EntityShredder;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

public final class ShredderPatterns {
    private ShredderPatterns() {}

    private static Block gt(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gtceu", path);
        if (!ForgeRegistries.BLOCKS.containsKey(id)) {
            EntityShredder.LOGGER.error("[{}] required block {} is not registered", EntityShredder.MOD_ID, id);
            return Blocks.BARRIER;
        }
        return ForgeRegistries.BLOCKS.getValue(id);
    }

    public static Block casing() {
        return gt("robust_machine_casing");
    }

    private static FactoryBlockPattern common(FactoryBlockPattern builder, MultiblockMachineDefinition definition) {
        TraceabilityPredicate casing = Predicates.blocks(casing())
                .or(Predicates.autoAbilities(definition.getRecipeTypes()))
                .or(Predicates.abilities(PartAbility.MAINTENANCE).setExactLimit(1))
                .or(Predicates.abilities(PartAbility.PARALLEL_HATCH).setMaxGlobalLimited(1));
        return builder
                .where('K', Predicates.controller(Predicates.blocks(definition.getBlock())))
                .where('N', casing)
                .where('P', Predicates.blocks(gt("tungstensteel_gearbox")))
                .where('T', Predicates.blocks(gt("tungstensteel_firebox_casing")))
                .where('Y', Predicates.blocks(gt("tungstensteel_pipe_casing")))
                .where('Z', Predicates.blocks(gt("tungsten_steel_frame")))
                .where('G', Predicates.blocks(gt("laminated_glass")))
                .where('C', Predicates.heatingCoils())
                .where(' ', Predicates.any());
    }

    public static BlockPattern basic(MultiblockMachineDefinition definition) {
        FactoryBlockPattern builder = FactoryBlockPattern.start();
        for (String[] aisle : PatternData.basic()) builder.aisle(aisle);
        return common(builder, definition).build();
    }

    public static BlockPattern advanced(MultiblockMachineDefinition definition) {
        FactoryBlockPattern builder = FactoryBlockPattern.start();
        for (String[] aisle : PatternData.advanced()) builder.aisle(aisle);
        return common(builder, definition)
                .where('Q', ModuleType.SPACETIME.predicate())
                .where('R', ModuleType.TIME.predicate())
                .where('S', ModuleType.STABILISATION.predicate())
                .build();
    }
}
