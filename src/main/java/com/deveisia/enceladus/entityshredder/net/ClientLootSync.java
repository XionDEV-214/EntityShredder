package com.deveisia.enceladus.entityshredder.net;

import com.deveisia.enceladus.entityshredder.data.EntityLoot;
import com.deveisia.enceladus.entityshredder.data.LootCache;
import com.deveisia.enceladus.entityshredder.jei.ShredderJeiPlugin;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientLootSync {
    private ClientLootSync() {}

    public static void apply(List<EntityLoot> entities) {
        Map<ResourceLocation, EntityLoot> map = new LinkedHashMap<>();
        for (EntityLoot e : entities) map.put(e.entityId(), e);
        LootCache.setClient(map);
        if (ModList.get().isLoaded("jei")) ShredderJeiPlugin.refresh();
    }
}
