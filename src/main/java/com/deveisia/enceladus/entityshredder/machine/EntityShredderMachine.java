package com.deveisia.enceladus.entityshredder.machine;

import com.deveisia.enceladus.entityshredder.ShredderStats;
import com.deveisia.enceladus.entityshredder.ShredderTier;
import com.deveisia.enceladus.entityshredder.data.EntityUtil;
import com.deveisia.enceladus.entityshredder.registry.ModuleType;
import com.deveisia.enceladus.entityshredder.ui.EntitySelectorFancyConfigurator;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.CoilWorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EntityShredderMachine extends CoilWorkableElectricMultiblockMachine {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            EntityShredderMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private final ShredderTier shredderTier;

    @Persisted
    @DescSynced
    private String selectedEntities = "";

    private int stabSum;
    private int stabCount;
    private int spaceSum;
    private int spaceCount;
    private int timeSum;
    private int timeCount;

    public EntityShredderMachine(IMachineBlockEntity holder, ShredderTier tier) {
        super(holder);
        this.shredderTier = tier;
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new EntityShredderLogic(this);
    }

    @Override
    public @NotNull EntityShredderLogic getRecipeLogic() {
        return (EntityShredderLogic) super.getRecipeLogic();
    }

    public ShredderTier getShredderTier() {
        return shredderTier;
    }

    @Override
    public boolean keepSubscribing() {
        return true;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        if (shredderTier.modules) {
            var ctx = getMultiblockState().getMatchContext();
            stabSum = ctx.getInt(ModuleType.STABILISATION.sumKey());
            stabCount = ctx.getInt(ModuleType.STABILISATION.countKey());
            spaceSum = ctx.getInt(ModuleType.SPACETIME.sumKey());
            spaceCount = ctx.getInt(ModuleType.SPACETIME.countKey());
            timeSum = ctx.getInt(ModuleType.TIME.sumKey());
            timeCount = ctx.getInt(ModuleType.TIME.countKey());
        } else {
            resetModules();
        }
        if (!isRemote()) {
            getRecipeLogic().markLastRecipeDirty();
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        resetModules();
    }

    private void resetModules() {
        stabSum = stabCount = spaceSum = spaceCount = timeSum = timeCount = 0;
    }

    public ShredderStats stats() {
        return new ShredderStats(shredderTier, getCoilTier(), stabSum, stabCount, spaceSum, spaceCount, timeSum,
                timeCount);
    }

    public int parallels() {
        if (!isFormed()) return 1;
        return getParallelHatch().map(IParallelHatch::getCurrentParallel).map(p -> Math.max(1, p)).orElse(1);
    }

    public boolean hasRequiredVoltage() {
        return getMaxVoltage() >= shredderTier.nominalVoltage();
    }

    public Set<ResourceLocation> getSelectedEntitySet() {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        if (selectedEntities.isEmpty()) return out;
        for (String s : selectedEntities.split(",")) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) out.add(id);
        }
        return out;
    }

    public boolean isEntitySelected(ResourceLocation id) {
        return getSelectedEntitySet().contains(id);
    }

    public void toggleEntity(ResourceLocation id) {
        Set<ResourceLocation> set = getSelectedEntitySet();
        if (!set.remove(id)) set.add(id);
        applySelection(set);
    }

    public void clearEntities() {
        applySelection(new LinkedHashSet<>());
    }

    private void applySelection(Set<ResourceLocation> set) {
        List<String> parts = new ArrayList<>();
        for (ResourceLocation id : set) parts.add(id.toString());
        this.selectedEntities = String.join(",", parts);
        if (!isRemote()) {
            getRecipeLogic().markLastRecipeDirty();
            getRecipeLogic().updateTickSubscription();
        }
    }

    public int validSelectedCount() {
        return EntityUtil.countValid(getSelectedEntitySet(), isRemote());
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        sideTabs.attachSubTab(new EntitySelectorFancyConfigurator(this));
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;

        ShredderStats s = stats();
        int n = validSelectedCount();
        textList.add(Component.translatable("entityshredder.multiblock.coil", s.coilLevel(),
                fmt(s.coilTimeFactor() * 100.0)).withStyle(ChatFormatting.GRAY));

        if (shredderTier.modules) {
            textList.add(Component.translatable("entityshredder.multiblock.stabilisation", s.stabCount(), s.stabSum(),
                    fmt(s.stabSum() / 3.0)).withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable("entityshredder.multiblock.spacetime", s.spaceCount(), s.spaceSum(),
                    fmt(s.spacetimeBonus() * 100.0)).withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable("entityshredder.multiblock.time_acceleration", s.timeCount(),
                    s.timeSum(), fmt(s.timeAccelerationBonus() * 100.0)).withStyle(ChatFormatting.GRAY));
        }

        textList.add(Component.translatable("entityshredder.multiblock.success", fmt(s.successPercent()))
                .withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("entityshredder.multiblock.duration", fmt(s.exactDuration() / 20.0),
                s.durationTicks()).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("entityshredder.multiblock.output_multiplier",
                fmt(s.outputMultiplier())).withStyle(ChatFormatting.GRAY));

        if (!hasRequiredVoltage()) {
            textList.add(Component.translatable("entityshredder.multiblock.voltage_required",
                    com.gregtechceu.gtceu.api.GTValues.VNF[shredderTier.voltageTier])
                    .withStyle(ChatFormatting.RED));
        }
        if (n == 0) {
            textList.add(Component.translatable("entityshredder.multiblock.no_selection")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        textList.add(Component.translatable("entityshredder.multiblock.selected", n)
                .withStyle(ChatFormatting.GRAY));
        GTRecipe active = getRecipeLogic().isActive() ? getRecipeLogic().getLastRecipe() : null;
        long power = active != null ? RecipeHelper.getRealEUt(active).getTotalEU() :
                s.powerPerTick(n, parallels());
        textList.add(Component.translatable("entityshredder.multiblock.power_per_tick",
                com.gregtechceu.gtceu.utils.FormattingUtil.formatNumbers(power)).withStyle(ChatFormatting.GRAY));
    }
}
