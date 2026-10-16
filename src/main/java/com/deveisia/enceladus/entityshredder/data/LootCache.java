package com.deveisia.enceladus.entityshredder.data;

import com.deveisia.enceladus.entityshredder.EntityShredder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.Map;

public final class LootCache {
    private LootCache() {}

    private static volatile Map<ResourceLocation, EntityLoot> server = Map.of();
    private static volatile Map<ResourceLocation, EntityLoot> client = Map.of();

    public static Map<ResourceLocation, EntityLoot> get(boolean clientSide) {
        return clientSide ? client : server;
    }

    public static void rebuildServer(MinecraftServer minecraftServer) {
        server = LootScanner.scan(minecraftServer);
        EntityShredder.LOGGER.info("[{}] collected loot for {} entity types", EntityShredder.MOD_ID, server.size());
    }

    public static void clearServer() {
        server = Map.of();
    }

    public static void setClient(Map<ResourceLocation, EntityLoot> data) {
        client = data;
    }

    public static void clearClient() {
        client = Map.of();
    }
}
