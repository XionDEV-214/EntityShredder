package com.deveisia.enceladus.entityshredder.jei;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.ShredderTier;
import com.deveisia.enceladus.entityshredder.data.EntityLoot;
import com.deveisia.enceladus.entityshredder.data.EntityUtil;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class ShredderJeiPlugin implements IModPlugin {
    private static IJeiRuntime runtime;
    private static List<ShredderJeiRecipe> registered = List.of();

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ShredderJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    private static List<ShredderJeiRecipe> buildRecipes() {
        List<ShredderJeiRecipe.EntityView> views = new ArrayList<>();
        for (ResourceLocation id : EntityUtil.ids(true)) {
            EntityLoot loot = EntityUtil.get(id, true);
            if (loot == null) continue;
            List<ItemStack> stacks = new ArrayList<>();
            for (EntityLoot.Drop drop : loot.drops()) {
                Item item = ForgeRegistries.ITEMS.getValue(drop.itemId());
                if (item != null && item != Items.AIR) stacks.add(new ItemStack(item, drop.count()));
            }
            if (!stacks.isEmpty()) views.add(new ShredderJeiRecipe.EntityView(id, EntityUtil.displayName(id), stacks));
        }
        List<ShredderJeiRecipe> recipes = new ArrayList<>();
        for (ShredderTier tier : ShredderTier.values()) recipes.add(new ShredderJeiRecipe(tier, views));
        return recipes;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registered = buildRecipes();
        registration.addRecipes(ShredderJeiCategory.TYPE, registered);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ShredderTier tier : ShredderTier.values()) {
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID, tier.key));
            if (item != null && item != Items.AIR) {
                registration.addRecipeCatalyst(new ItemStack(item), ShredderJeiCategory.TYPE);
            }
        }
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        registered = List.of();
    }

    public static void refresh() {
        Minecraft.getInstance().execute(() -> {
            if (runtime == null) return;
            var manager = runtime.getRecipeManager();
            List<ShredderJeiRecipe> old = registered;
            List<ShredderJeiRecipe> fresh = buildRecipes();
            if (!old.isEmpty()) manager.hideRecipes(ShredderJeiCategory.TYPE, old);
            manager.addRecipes(ShredderJeiCategory.TYPE, fresh);
            registered = fresh;
        });
    }
}
