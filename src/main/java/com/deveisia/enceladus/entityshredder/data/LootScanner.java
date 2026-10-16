package com.deveisia.enceladus.entityshredder.data;

import com.deveisia.enceladus.entityshredder.EntityShredder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LootScanner {
    private LootScanner() {}

    private static final int MAX_DEPTH = 8;

    public static Map<ResourceLocation, EntityLoot> scan(MinecraftServer server) {
        Gson gson = Deserializers.createLootTableSerializer().create();
        List<ResourceLocation> ids = new ArrayList<>(ForgeRegistries.ENTITY_TYPES.getKeys());
        ids.sort(Comparator.comparing(ResourceLocation::toString));

        Map<ResourceLocation, EntityLoot> out = new LinkedHashMap<>();
        for (ResourceLocation id : ids) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type == null) continue;
            ResourceLocation tableId = type.getDefaultLootTable();
            if (tableId == null || tableId.equals(BuiltInLootTables.EMPTY)) continue;
            try {
                Map<ResourceLocation, Integer> acc = new LinkedHashMap<>();
                collectTable(server, gson, tableId, acc, new HashSet<>(), 0);
                if (acc.isEmpty()) continue;
                List<EntityLoot.Drop> drops = new ArrayList<>();
                acc.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                        .forEach(e -> drops.add(new EntityLoot.Drop(e.getKey(), e.getValue())));
                out.put(id, new EntityLoot(id, List.copyOf(drops)));
            } catch (Exception ex) {
                EntityShredder.LOGGER.warn("[{}] failed to read loot table {} of {}: {}", EntityShredder.MOD_ID,
                        tableId, id, ex.toString());
            }
        }
        return out;
    }

    private static void collectTable(MinecraftServer server, Gson gson, ResourceLocation tableId,
                                     Map<ResourceLocation, Integer> acc, Set<ResourceLocation> visiting, int depth) {
        if (depth > MAX_DEPTH || !visiting.add(tableId)) return;
        try {
            LootTable table = server.getLootData().getLootTable(tableId);
            if (table == LootTable.EMPTY) return;
            JsonElement root = gson.toJsonTree(table);
            if (!root.isJsonObject()) return;
            JsonObject tableJson = root.getAsJsonObject();
            int tableCount = countFromFunctions(tableJson.get("functions"), 1);
            JsonArray pools = array(tableJson, "pools");
            for (JsonElement poolEl : pools) {
                if (!poolEl.isJsonObject()) continue;
                JsonObject pool = poolEl.getAsJsonObject();
                int poolCount = countFromFunctions(pool.get("functions"), tableCount);
                for (JsonElement entryEl : array(pool, "entries")) {
                    collectEntry(server, gson, entryEl, poolCount, acc, visiting, depth);
                }
            }
        } finally {
            visiting.remove(tableId);
        }
    }

    private static void collectEntry(MinecraftServer server, Gson gson, JsonElement element, int inheritedCount,
                                     Map<ResourceLocation, Integer> acc, Set<ResourceLocation> visiting, int depth) {
        if (!element.isJsonObject()) return;
        JsonObject entry = element.getAsJsonObject();
        String type = string(entry, "type");
        int count = countFromFunctions(entry.get("functions"), inheritedCount);

        switch (type) {
            case "minecraft:item" -> {
                ResourceLocation id = id(entry, "name");
                if (id != null) addItem(acc, id, count);
            }
            case "minecraft:tag" -> {
                ResourceLocation id = id(entry, "name");
                if (id == null) return;
                var tag = ForgeRegistries.ITEMS.tags().getTag(TagKey.create(Registries.ITEM, id));
                for (Item item : tag) {
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
                    if (itemId != null) addItem(acc, itemId, count);
                }
            }
            case "minecraft:loot_table" -> {
                ResourceLocation id = id(entry, "name");
                if (id != null) collectTable(server, gson, id, acc, visiting, depth + 1);
            }
            case "minecraft:dynamic", "minecraft:empty" -> {}
            default -> {
                for (JsonElement child : array(entry, "children")) {
                    collectEntry(server, gson, child, count, acc, visiting, depth);
                }
            }
        }
    }

    private static void addItem(Map<ResourceLocation, Integer> acc, ResourceLocation id, int count) {
        if (!ForgeRegistries.ITEMS.containsKey(id)) return;
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) return;
        if (new ItemStack(item).getMaxStackSize() <= 1) return;
        acc.merge(id, Math.max(1, count), Math::max);
    }

    private static int countFromFunctions(JsonElement functions, int fallback) {
        if (functions == null || !functions.isJsonArray()) return fallback;
        for (JsonElement fn : functions.getAsJsonArray()) {
            if (!fn.isJsonObject()) continue;
            JsonObject obj = fn.getAsJsonObject();
            if (!"minecraft:set_count".equals(string(obj, "function"))) continue;
            JsonElement c = obj.get("count");
            if (c != null) return Math.max(1, maxOf(c));
        }
        return fallback;
    }

    private static int maxOf(JsonElement el) {
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return (int) Math.floor(el.getAsDouble());
        }
        if (!el.isJsonObject()) return 1;
        JsonObject o = el.getAsJsonObject();
        String type = string(o, "type");
        if (type.equals("minecraft:binomial") || type.equals("binomial")) {
            return o.has("n") ? maxOf(o.get("n")) : 1;
        }
        if (type.equals("minecraft:constant") || type.equals("constant")) {
            return o.has("value") ? maxOf(o.get("value")) : 1;
        }
        int best = 1;
        if (o.has("max")) best = Math.max(best, maxOf(o.get("max")));
        if (o.has("min")) best = Math.max(best, maxOf(o.get("min")));
        return best;
    }

    private static JsonArray array(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonArray() ? el.getAsJsonArray() : new JsonArray();
    }

    private static String string(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : "";
    }

    private static ResourceLocation id(JsonObject obj, String key) {
        String s = string(obj, key);
        return s.isEmpty() ? null : ResourceLocation.tryParse(s);
    }
}
