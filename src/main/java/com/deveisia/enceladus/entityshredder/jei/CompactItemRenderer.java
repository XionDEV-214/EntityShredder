package com.deveisia.enceladus.entityshredder.jei;

import com.mojang.blaze3d.vertex.PoseStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.IIngredientRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * Item renderer for JEI slots that
 * <ul>
 * <li>can be larger than 16x16 (used for the big ore-block preview), and</li>
 * <li>shows huge stack sizes compactly (1024000 -> 1.0M) instead of overflowing the slot.</li>
 * </ul>
 * Tooltips are delegated to JEI's own item renderer so they look exactly like normal slots.
 */
public final class CompactItemRenderer implements IIngredientRenderer<ItemStack> {
    private final IJeiHelpers helpers;
    private final int size;
    private final boolean showCount;

    public CompactItemRenderer(IJeiHelpers helpers, int size, boolean showCount) {
        this.helpers = helpers;
        this.size = size;
        this.showCount = showCount;
    }

    private IIngredientRenderer<ItemStack> base() {
        return helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gg, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        PoseStack pose = gg.pose();
        pose.pushPose();
        if (size != 16) {
            float s = size / 16f;
            pose.scale(s, s, s);
        }
        gg.renderItem(stack, 0, 0);
        if (showCount && stack.getCount() > 1) {
            gg.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0, compact(stack.getCount()));
        }
        pose.popPose();
    }

    @Override
    @SuppressWarnings("removal")
    public List<Component> getTooltip(ItemStack stack, TooltipFlag flag) {
        return base().getTooltip(stack, flag);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ItemStack stack, TooltipFlag flag) {
        base().getTooltip(tooltip, stack, flag);
    }

    @Override
    public int getWidth() {
        return size;
    }

    @Override
    public int getHeight() {
        return size;
    }

    public static String compact(long n) {
        if (n >= 1_000_000_000L) return trim(n / 1_000_000_000.0) + "G";
        if (n >= 1_000_000L) return trim(n / 1_000_000.0) + "M";
        if (n >= 10_000L) return trim(n / 1_000.0) + "K";
        return Long.toString(n);
    }

    private static String trim(double v) {
        String s = v >= 100 ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
