package com.deveisia.enceladus.entityshredder.jei;

import com.deveisia.enceladus.entityshredder.ShredderTier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One JEI page = one shredder tier x one entity.
 *
 * @param drops    every item the entity's loot table can yield, with its base count per successful cycle
 * @param spawnEgg the entity's spawn egg, if it has one (lets "U" on a spawn egg open this page)
 */
public record ShredderJeiRecipe(ShredderTier tier, ResourceLocation entityId, String name, List<ItemStack> drops,
                                @Nullable ItemStack spawnEgg) {
    // Identity semantics: refresh() hides exactly the instances it registered earlier.
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
