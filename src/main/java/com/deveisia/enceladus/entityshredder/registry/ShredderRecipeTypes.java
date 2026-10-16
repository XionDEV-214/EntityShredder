package com.deveisia.enceladus.entityshredder.registry;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;

public final class ShredderRecipeTypes {
    private ShredderRecipeTypes() {}

    public static GTRecipeType ENTITY_SHREDDER;
    public static GTRecipeType ADVANCED_ENTITY_SHREDDER;

    public static void init() {
        ENTITY_SHREDDER = create("entity_shredder");
        ADVANCED_ENTITY_SHREDDER = create("advanced_entity_shredder");
    }

    private static GTRecipeType create(String name) {
        return GTRecipeTypes.register(name, GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(0, 16, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_MACERATE, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.MACERATOR)
                .setXEIVisible(false);
    }
}
