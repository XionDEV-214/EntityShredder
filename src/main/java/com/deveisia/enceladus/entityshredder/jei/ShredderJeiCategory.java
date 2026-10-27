package com.deveisia.enceladus.entityshredder.jei;

import com.deveisia.enceladus.entityshredder.EntityShredder;
import com.deveisia.enceladus.entityshredder.ShredderStats;
import com.deveisia.enceladus.entityshredder.ShredderTier;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One JEI tab per shredder tier; every page is a single entity:
 *
 * <pre>
 * +----------------------------------------------+
 * |                                              |
 * |            (mob, fitted to the box,          |   ~1/3 of the page
 * |             slowly spinning)                 |
 * +----------------------------------------------+
 * Zombie                                3 kinds
 * minecraft:zombie
 * [d][d][d][d][d][d][d][d][d]
 * [d][d][d][d][d][d][d][d][d]                        up to 36 outputs (9 x 4)
 * [d][d][d][d][d][d][d][d][d]
 * [d][d][d][d][d][d][d][d][d]
 * ------------------------------------------------
 * IV 8,192 EU/t (per entity)
 * Base cycle: 20 s
 * Base success rate: 28%
 * N entities selected = N x EU/t
 * </pre>
 *
 * Hovering the preview box turns the mob to follow the mouse; otherwise it slowly spins.
 */
public class ShredderJeiCategory implements IRecipeCategory<ShredderJeiRecipe> {
    private static final Map<ShredderTier, RecipeType<ShredderJeiRecipe>> TYPES = new EnumMap<>(ShredderTier.class);

    static {
        for (ShredderTier tier : ShredderTier.values()) {
            TYPES.put(tier, RecipeType.create(EntityShredder.MOD_ID, tier.key, ShredderJeiRecipe.class));
        }
    }

    public static RecipeType<ShredderJeiRecipe> type(ShredderTier tier) {
        return TYPES.get(tier);
    }

    private static final int WIDTH = 176;

    private static final int BOX_X = 4;
    private static final int BOX_Y = 4;
    private static final int BOX_W = WIDTH - 8;
    private static final int BOX_H = 68;

    private static final int NAME_Y = BOX_Y + BOX_H + 4;
    private static final int ID_Y = NAME_Y + 10;

    private static final int COLS = 9;
    private static final int ROWS = 4;
    private static final int CAPACITY = COLS * ROWS;
    /** Slot x of the first column; the 9x18 grid (incl. 1px slot border) is centered on the page. */
    private static final int GRID_X = (WIDTH - COLS * 18) / 2 + 1;
    private static final int GRID_Y = ID_Y + 13;

    private static final int FOOTER = GRID_Y + ROWS * 18 + 5;
    private static final int HEIGHT = FOOTER + 42;

    private static final int TEXT = 0xFF404040;
    private static final int MUTED = 0xFF808080;
    private static final long SPIN_MS = 10_000L;

    private final ShredderTier tier;
    private final IJeiHelpers helpers;
    private final IDrawable icon;

    public ShredderJeiCategory(IJeiHelpers helpers, ShredderTier tier, ItemStack iconStack) {
        this.tier = tier;
        this.helpers = helpers;
        this.icon = helpers.getGuiHelper().createDrawableItemStack(iconStack);
    }

    @Override
    public @NotNull RecipeType<ShredderJeiRecipe> getRecipeType() {
        return type(tier);
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable(tier.langKey());
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
    public @Nullable ResourceLocation getRegistryName(ShredderJeiRecipe recipe) {
        ResourceLocation id = recipe.entityId();
        return ResourceLocation.fromNamespaceAndPath(EntityShredder.MOD_ID,
                tier.key + "/" + id.getNamespace() + "/" + id.getPath());
    }

    private static double successPercent() {
        return ShredderStats.BASE_CHANCE_THIRDS_OF_PERCENT * 100.0 / ShredderStats.FULL_CHANCE_THIRDS_OF_PERCENT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShredderJeiRecipe recipe, IFocusGroup focuses) {
        CompactItemRenderer renderer = new CompactItemRenderer(helpers, 16, true);
        List<ItemStack> drops = recipe.drops();
        String chance = String.format(Locale.ROOT, "%.0f", successPercent());

        int direct = drops.size() > CAPACITY ? CAPACITY - 1 : drops.size();
        for (int i = 0; i < direct; i++) {
            ItemStack drop = drops.get(i);
            int count = drop.getCount();
            builder.addSlot(RecipeIngredientRole.OUTPUT, GRID_X + (i % COLS) * 18, GRID_Y + (i / COLS) * 18)
                    .setStandardSlotBackground()
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, renderer)
                    .addItemStack(drop)
                    .addRichTooltipCallback((view, tooltip) -> addDropTooltip(tooltip, count, chance));
        }
        if (drops.size() > CAPACITY) {
            // Last cell cycles through everything that did not fit, so nothing is hidden.
            List<ItemStack> rest = new ArrayList<>(drops.subList(direct, drops.size()));
            int last = CAPACITY - 1;
            int more = rest.size();
            builder.addSlot(RecipeIngredientRole.OUTPUT, GRID_X + (last % COLS) * 18, GRID_Y + (last / COLS) * 18)
                    .setStandardSlotBackground()
                    .setCustomRenderer(VanillaTypes.ITEM_STACK, renderer)
                    .addItemStacks(rest)
                    .addRichTooltipCallback((view, tooltip) -> {
                        view.getDisplayedItemStack().ifPresent(s -> addDropTooltip(tooltip, s.getCount(), chance));
                        tooltip.add(Component.translatable("entityshredder.jei.cycling", more)
                                .withStyle(ChatFormatting.DARK_GRAY));
                    });
        }

        if (recipe.spawnEgg() != null && !recipe.spawnEgg().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.spawnEgg());
        }
    }

    private void addDropTooltip(ITooltipBuilder tooltip, int count, String chance) {
        tooltip.add(Component.translatable("entityshredder.jei.per_cycle", FormattingUtil.formatNumbers(count))
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("entityshredder.jei.success", chance).withStyle(ChatFormatting.GRAY));
        if (tier.modules) {
            tooltip.add(Component.translatable("entityshredder.jei.output_note").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static boolean inBox(double mx, double my) {
        return mx >= BOX_X && mx < BOX_X + BOX_W && my >= BOX_Y && my < BOX_Y + BOX_H;
    }

    @Override
    public void draw(ShredderJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gg, double mouseX,
                     double mouseY) {
        Font font = Minecraft.getInstance().font;

        // Preview box, vanilla slot style (dark top-left, light bottom-right).
        gg.fill(BOX_X, BOX_Y, BOX_X + BOX_W, BOX_Y + BOX_H, 0xFF373737);
        gg.fill(BOX_X + 1, BOX_Y + 1, BOX_X + BOX_W, BOX_Y + BOX_H, 0xFFFFFFFF);
        gg.fill(BOX_X + 1, BOX_Y + 1, BOX_X + BOX_W - 1, BOX_Y + BOX_H - 1, 0xFF8B8B8B);

        float yaw;
        float pitch;
        if (inBox(mouseX, mouseY)) {
            float cx = BOX_X + BOX_W / 2f;
            float cy = BOX_Y + BOX_H / 2f;
            yaw = (float) Math.toDegrees(Math.atan((cx - mouseX) / 40.0)) * 0.7f;
            pitch = (float) Math.toDegrees(Math.atan((cy - mouseY) / 40.0)) * 0.35f;
            pitch = Math.max(-EntityPreview.MAX_PITCH, Math.min(EntityPreview.MAX_PITCH, pitch));
        } else {
            yaw = (System.currentTimeMillis() % SPIN_MS) * 360f / SPIN_MS;
            pitch = 0;
        }
        boolean drawn = EntityPreview.render(gg, recipe.entityId(), BOX_X + 1, BOX_Y + 1, BOX_W - 2, BOX_H - 2,
                yaw, pitch);
        if (!drawn) drawFallback(gg, font, recipe);

        String kinds = Component.translatable("entityshredder.jei.drop_kinds", recipe.drops().size()).getString();
        int kindsW = font.width(kinds);
        gg.drawString(font, fit(font, recipe.name(), WIDTH - 8 - kindsW - 6), 4, NAME_Y, TEXT, false);
        gg.drawString(font, kinds, WIDTH - 4 - kindsW, NAME_Y, MUTED, false);
        gg.drawString(font, fit(font, recipe.entityId().toString(), WIDTH - 8), 4, ID_Y, MUTED, false);

        gg.fill(4, FOOTER - 3, WIDTH - 4, FOOTER - 2, 0x30000000);
        int w = WIDTH - 8;
        String power = GTValues.VNF[tier.voltageTier] + ChatFormatting.RESET + " " +
                Component.translatable("entityshredder.jei.power", FormattingUtil.formatNumbers(tier.voltage()))
                        .getString();
        gg.drawString(font, fit(font, power, w), 4, FOOTER, TEXT, false);
        gg.drawString(font, fit(font, Component.translatable("entityshredder.jei.duration",
                FormattingUtil.formatNumbers(tier.baseDuration / 20.0)).getString(), w), 4, FOOTER + 10, TEXT, false);
        gg.drawString(font, fit(font, Component.translatable("entityshredder.jei.success",
                String.format(Locale.ROOT, "%.0f", successPercent())).getString(), w), 4, FOOTER + 20, TEXT, false);
        gg.drawString(font, fit(font, Component.translatable(tier.modules ? "entityshredder.jei.modules" :
                "entityshredder.jei.multi_select").getString(), w), 4, FOOTER + 30, MUTED, false);
    }

    private static void drawFallback(GuiGraphics gg, Font font, ShredderJeiRecipe recipe) {
        int cx = BOX_X + BOX_W / 2;
        int cy = BOX_Y + BOX_H / 2;
        ItemStack egg = recipe.spawnEgg();
        if (egg != null && !egg.isEmpty()) {
            gg.pose().pushPose();
            gg.pose().translate(cx - 16, cy - 16, 0);
            gg.pose().scale(2f, 2f, 1f);
            gg.renderItem(egg, 0, 0);
            gg.pose().popPose();
        } else {
            String q = "?";
            gg.drawString(font, q, cx - font.width(q) / 2, cy - 4, 0xFF404040, false);
        }
        String s = Component.translatable("entityshredder.jei.no_preview").getString();
        String f = fit(font, s, BOX_W - 4);
        gg.drawString(font, f, cx - font.width(f) / 2, cy + 20, 0xFF505050, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ShredderJeiRecipe recipe,
                           IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (!inBox(mouseX, mouseY)) return;
        tooltip.add(Component.literal(recipe.name()));
        tooltip.add(Component.literal(recipe.entityId().toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("entityshredder.jei.drop_kinds", recipe.drops().size())
                .withStyle(ChatFormatting.GRAY));
    }

    private static String fit(Font font, String s, int width) {
        if (font.width(s) <= width) return s;
        String ell = "...";
        return font.plainSubstrByWidth(s, Math.max(0, width - font.width(ell))) + ell;
    }
}
