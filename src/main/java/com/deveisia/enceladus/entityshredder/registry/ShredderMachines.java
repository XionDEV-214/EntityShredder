package com.deveisia.enceladus.entityshredder.registry;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.ShredderTier;
import com.deveisia.enceladus.entityshredder.machine.EntityShredderMachine;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ShredderMachines {
    private ShredderMachines() {}

    public static MultiblockMachineDefinition ENTITY_SHREDDER;
    public static MultiblockMachineDefinition ADVANCED_ENTITY_SHREDDER;

    public static final ResourceLocation BASE_CASING = ResourceLocation.fromNamespaceAndPath("gtceu",
            "block/casings/solid/machine_casing_robust_tungstensteel");
    public static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath("gtceu", "block/multiblock/research_station");

    public static void init() {
        EntityShredder.REGISTRATE.creativeModeTab(() -> EntityShredder.TAB);

        ENTITY_SHREDDER = EntityShredder.REGISTRATE
                .multiblock("entity_shredder", holder -> new EntityShredderMachine(holder, ShredderTier.BASIC))
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ShredderRecipeTypes.ENTITY_SHREDDER)
                .recipeModifiers(GTRecipeModifiers.BATCH_MODE)
                .appearanceBlock(ShredderPatterns::casing)
                .pattern(ShredderPatterns::basic)
                .tooltips(Component.translatable("entityshredder.tooltip.basic"))
                .workableCasingModel(BASE_CASING, OVERLAY)
                .register();

        ADVANCED_ENTITY_SHREDDER = EntityShredder.REGISTRATE
                .multiblock("advanced_entity_shredder",
                        holder -> new EntityShredderMachine(holder, ShredderTier.ADVANCED))
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(ShredderRecipeTypes.ADVANCED_ENTITY_SHREDDER)
                .recipeModifiers(GTRecipeModifiers.BATCH_MODE)
                .appearanceBlock(ShredderPatterns::casing)
                .pattern(ShredderPatterns::advanced)
                .tooltips(Component.translatable("entityshredder.tooltip.advanced"))
                .workableCasingModel(BASE_CASING, OVERLAY)
                .register();
    }
}
