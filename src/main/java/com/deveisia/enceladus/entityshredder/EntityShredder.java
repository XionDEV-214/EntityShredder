package com.deveisia.enceladus.entityshredder;

import com.deveisia.enceladus.entityshredder.net.ShredderNetwork;
import com.deveisia.enceladus.entityshredder.registry.ShredderMachines;
import com.deveisia.enceladus.entityshredder.registry.ShredderRecipeTypes;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTCreativeModeTabs;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EntityShredder.MOD_ID)
public class EntityShredder {
    public static final String MOD_ID = "entityshredder";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(MOD_ID);

    public static final RegistryEntry<CreativeModeTab> TAB = REGISTRATE.defaultCreativeTab("main",
            builder -> builder
                    .displayItems(new GTCreativeModeTabs.RegistrateDisplayItemsGenerator("main", REGISTRATE))
                    .icon(() -> ShredderMachines.ADVANCED_ENTITY_SHREDDER.asStack())
                    .title(Component.translatable("itemGroup." + MOD_ID + ".main"))
                    .build())
            .register();

    public EntityShredder(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        REGISTRATE.registerRegistrate();
        ShredderNetwork.init();
        modBus.addGenericListener(GTRecipeType.class, EntityShredder::registerRecipeTypes);
        modBus.addGenericListener(MachineDefinition.class, EntityShredder::registerMachines);
        LOGGER.info("[{}] loaded", MOD_ID);
    }

    private static void registerRecipeTypes(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
        ShredderRecipeTypes.init();
    }

    private static void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        ShredderMachines.init();
    }
}
