package com.deveisia.enceladus.entityshredder.jei;

import com.deveisia.enceladus.entityshredder.ShredderTier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ShredderJeiRecipe(ShredderTier tier, List<EntityView> entities) {
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public record EntityView(ResourceLocation id, String name, List<ItemStack> drops) {}
}
