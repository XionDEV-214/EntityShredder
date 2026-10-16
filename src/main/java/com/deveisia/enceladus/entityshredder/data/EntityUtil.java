package com.deveisia.enceladus.entityshredder.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EntityUtil {
    private EntityUtil() {}

    public static List<ResourceLocation> ids(boolean client) {
        List<ResourceLocation> out = new ArrayList<>(LootCache.get(client).keySet());
        out.sort(Comparator.comparing(ResourceLocation::toString));
        return out;
    }

    public static EntityLoot get(ResourceLocation id, boolean client) {
        return LootCache.get(client).get(id);
    }

    public static String displayName(ResourceLocation id) {
        if (ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type != null) return type.getDescription().getString();
        }
        return id.toString();
    }

    public static int countValid(Collection<ResourceLocation> ids, boolean client) {
        Map<ResourceLocation, EntityLoot> data = LootCache.get(client);
        int n = 0;
        Set<ResourceLocation> seen = new HashSet<>();
        for (ResourceLocation id : ids) if (seen.add(id) && data.containsKey(id)) n++;
        return n;
    }
}
