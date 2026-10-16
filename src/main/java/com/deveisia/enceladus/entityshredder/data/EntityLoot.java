package com.deveisia.enceladus.entityshredder.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record EntityLoot(ResourceLocation entityId, List<Drop> drops) {

    public record Drop(ResourceLocation itemId, int count) {}

    public Set<String> itemKeys() {
        Set<String> out = new HashSet<>();
        for (Drop d : drops) out.add(d.itemId().toString());
        return out;
    }
}
