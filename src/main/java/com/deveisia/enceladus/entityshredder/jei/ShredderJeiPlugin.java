package com.deveisia.enceladus.entityshredder.jei;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.ShredderTier;
import com.deveisia.enceladus.entityshredder.data.EntityLoot;
import com.deveisia.enceladus.entityshredder.data.EntityUtil;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@JeiPlugin
public class ShredderJeiPlugin implements IModPlugin {
    @Nullable
    private static IJeiRuntime runtime;
    private static final Map<ShredderTier, List<ShredderJeiRecipe>> registered = new EnumMap<>(ShredderTier.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID, "jei_plugin");
    }

    @Nullable
    private static Item machineItem(ShredderTier tier) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID,
                tier.key));
        return item == null || item == Items.AIR ? null : item;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        for (ShredderTier tier : ShredderTier.values()) {
            Item machine = machineItem(tier);
            ItemStack icon = machine != null ? new ItemStack(machine) : new ItemStack(Items.ZOMBIE_HEAD);
            registration.addRecipeCategories(new ShredderJeiCategory(helpers, tier, icon));
        }
    }

    @Nullable
    private static ItemStack spawnEgg(ResourceLocation id) {
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) return null;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return null;
        SpawnEggItem egg = ForgeSpawnEggItem.fromEntityType(type);
        return egg == null ? null : new ItemStack(egg);
    }

    private static Map<ShredderTier, List<ShredderJeiRecipe>> buildRecipes() {
        Map<ShredderTier, List<ShredderJeiRecipe>> out = new EnumMap<>(ShredderTier.class);
        for (ShredderTier tier : ShredderTier.values()) out.put(tier, new ArrayList<>());

        for (ResourceLocation id : EntityUtil.ids(true)) {
            EntityLoot loot = EntityUtil.get(id, true);
            if (loot == null) continue;
            List<ItemStack> stacks = new ArrayList<>();
            for (EntityLoot.Drop drop : loot.drops()) {
                Item item = ForgeRegistries.ITEMS.getValue(drop.itemId());
                if (item != null && item != Items.AIR) stacks.add(new ItemStack(item, Math.max(1, drop.count())));
            }
            if (stacks.isEmpty()) continue;
            List<ItemStack> drops = List.copyOf(stacks);
            String name = EntityUtil.displayName(id);
            ItemStack egg = spawnEgg(id);
            // Separate instances per tier: hide/unhide in refresh() works on identity.
            for (ShredderTier tier : ShredderTier.values()) {
                out.get(tier).add(new ShredderJeiRecipe(tier, id, name, drops, egg));
            }
        }
        return out;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registered.clear();
        registered.putAll(buildRecipes());
        for (var e : registered.entrySet()) {
            registration.addRecipes(ShredderJeiCategory.type(e.getKey()), e.getValue());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ShredderTier tier : ShredderTier.values()) {
            Item item = machineItem(tier);
            if (item != null) registration.addRecipeCatalyst(new ItemStack(item), ShredderJeiCategory.type(tier));
        }
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        registered.clear();
    }

    /** Called when the server sends fresh loot data; swaps every page without reloading JEI. */
    public static void refresh() {
        Minecraft.getInstance().execute(() -> {
            if (runtime == null) return;
            var manager = runtime.getRecipeManager();
            Map<ShredderTier, List<ShredderJeiRecipe>> fresh = buildRecipes();
            for (ShredderTier tier : ShredderTier.values()) {
                List<ShredderJeiRecipe> old = registered.getOrDefault(tier, List.of());
                if (!old.isEmpty()) manager.hideRecipes(ShredderJeiCategory.type(tier), old);
                List<ShredderJeiRecipe> now = fresh.getOrDefault(tier, List.of());
                if (!now.isEmpty()) manager.addRecipes(ShredderJeiCategory.type(tier), now);
            }
            registered.clear();
            registered.putAll(fresh);
        });
    }
}
