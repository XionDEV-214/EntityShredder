package com.deveisia.enceladus.entityshredder.jei;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.ShredderStats;
import com.deveisia.enceladus.entityshredder.ShredderTier;

import com.gregtechceu.gtceu.utils.FormattingUtil;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShredderJeiCategory implements IRecipeCategory<ShredderJeiRecipe> {
    public static final RecipeType<ShredderJeiRecipe> TYPE = RecipeType.create(EntityShredder.MOD_ID,
            "entity_shredder", ShredderJeiRecipe.class);

    private static final int WIDTH = 176;
    private static final int HEIGHT = 140;
    private static final long CYCLE_MS = 1000L;
    private static final int COLS = 8;
    private static final int TEXT = 0xFF404040;

    private final IDrawable icon;

    public ShredderJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.ZOMBIE_HEAD));
    }

    @Override
    public @NotNull RecipeType<ShredderJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("entityshredder.jei.category");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShredderJeiRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> all = new ArrayList<>();
        for (var entity : recipe.entities()) all.addAll(entity.drops());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(all);
    }

    private static String compact(int n) {
        if (n >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format(Locale.ROOT, "%.1fK", n / 1_000.0);
        return Integer.toString(n);
    }

    @Override
    public void draw(ShredderJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gg, double mouseX,
                     double mouseY) {
        Font font = Minecraft.getInstance().font;
        ShredderTier tier = recipe.tier();

        gg.drawString(font, Component.translatable(tier.langKey()), 6, 4, TEXT, false);

        List<ShredderJeiRecipe.EntityView> entities = recipe.entities();
        if (!entities.isEmpty()) {
            int idx = (int) ((System.currentTimeMillis() / CYCLE_MS) % entities.size());
            var entity = entities.get(idx);
            String counter = (idx + 1) + " / " + entities.size();
            gg.drawString(font, counter, WIDTH - 6 - font.width(counter), 4, TEXT, false);
            gg.drawString(font, entity.name(), 6, 16, TEXT, false);

            List<ItemStack> drops = entity.drops();
            for (int i = 0; i < drops.size() && i < COLS * 3; i++) {
                int x = 6 + (i % COLS) * 18;
                int y = 28 + (i / COLS) * 18;
                gg.renderItem(drops.get(i), x, y);
                if (drops.get(i).getCount() > 1) {
                    gg.renderItemDecorations(font, drops.get(i), x, y, compact(drops.get(i).getCount()));
                }
            }
        } else {
            gg.drawString(font, Component.translatable("entityshredder.jei.no_data"), 6, 16, 0xFF808080, false);
        }

        int y = 86;
        gg.drawString(font, Component.translatable("entityshredder.jei.power",
                FormattingUtil.formatNumbers(tier.voltage())), 6, y, TEXT, false);
        gg.drawString(font, Component.translatable("entityshredder.jei.duration",
                FormattingUtil.formatNumbers(tier.baseDuration / 20.0)), 6, y + 10, TEXT, false);
        gg.drawString(font, Component.translatable("entityshredder.jei.success",
                String.format(Locale.ROOT, "%.0f", ShredderStats.BASE_CHANCE_THIRDS_OF_PERCENT / 3.0)), 6, y + 20,
                TEXT, false);
        gg.drawString(font, Component.translatable("entityshredder.jei.coil"), 6, y + 30, TEXT, false);
        gg.drawString(font, Component.translatable(tier.modules ? "entityshredder.jei.modules" :
                "entityshredder.jei.multi_select"), 6, y + 40, 0xFF808080, false);
    }
}
