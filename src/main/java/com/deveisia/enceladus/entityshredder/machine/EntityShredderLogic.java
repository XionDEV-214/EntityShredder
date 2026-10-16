package com.deveisia.enceladus.entityshredder.machine;

import com.deveisia.enceladus.entityshredder.ShredderStats;
import com.deveisia.enceladus.entityshredder.data.EntityLoot;
import com.deveisia.enceladus.entityshredder.data.EntityUtil;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityShredderLogic extends RecipeLogic {
    private Map<ResourceLocation, Integer> pendingCounts;

    private record Plan(int entityCount, Map<ResourceLocation, Integer> counts) {}

    public EntityShredderLogic(EntityShredderMachine machine) {
        super(machine);
    }

    @Override
    public EntityShredderMachine getMachine() {
        return (EntityShredderMachine) super.getMachine();
    }

    @Override
    public void findAndHandleRecipe() {
        if (getMachine().isRemote()) return;
        lastFailedMatches = null;
        failureReasonMap.clear();
        lastRecipe = null;
        lastOriginRecipe = null;
        GTRecipe origin = buildRecipe();
        if (origin != null) start(origin);
        recipeDirty = false;
    }

    private boolean start(GTRecipe origin) {
        if (!getMachine().hasRequiredVoltage()) {
            putFailureReason(this, origin, Component.translatable("gtceu.recipe_modifier.insufficient_voltage"));
            return false;
        }

        GTRecipe modified = machine.fullModifyRecipe(origin);
        if (modified == null) {
            if (failureReasonMap.isEmpty()) {
                putFailureReason(this, origin, Component.translatable("gtceu.recipe_modifier.default_fail"));
            }
            return false;
        }

        ActionResult check = checkRecipe(modified);
        if (!check.isSuccess()) {
            putFailureReason(this, origin, check.reason());
            return false;
        }

        setupRecipe(modified);
        if (lastRecipe == null || !isWorking()) return false;
        lastOriginRecipe = origin;
        return true;
    }

    private Plan plan(ShredderStats stats, int parallel) {
        Map<ResourceLocation, Integer> perItem = new LinkedHashMap<>();
        int entityCount = 0;
        for (ResourceLocation id : getMachine().getSelectedEntitySet()) {
            EntityLoot loot = EntityUtil.get(id, false);
            if (loot == null || loot.drops().isEmpty()) continue;
            entityCount++;
            for (EntityLoot.Drop drop : loot.drops()) perItem.merge(drop.itemId(), drop.count(), Integer::sum);
        }
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> e : perItem.entrySet()) {
            counts.put(e.getKey(), stats.scaleOutput(e.getValue(), parallel));
        }
        return new Plan(entityCount, counts);
    }

    @Nullable
    private GTRecipe buildRecipe() {
        EntityShredderMachine m = getMachine();
        ShredderStats stats = m.stats();
        int parallel = m.parallels();

        Plan plan = plan(stats, parallel);
        if (plan.entityCount() == 0) return null;

        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw()
                .duration(stats.durationTicks())
                .EUt(stats.powerPerTick(plan.entityCount(), parallel));

        boolean any = false;
        for (Map.Entry<ResourceLocation, Integer> e : plan.counts().entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(e.getKey());
            if (item == null || item == Items.AIR) continue;
            builder.outputItems(new ItemStack(item, e.getValue()));
            any = true;
        }
        if (!any) return null;
        pendingCounts = plan.counts();
        return builder.buildRawRecipe();
    }

    private GTRecipe partialRecipe(Map<ResourceLocation, Integer> counts, int successes) {
        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw().duration(1);
        for (Map.Entry<ResourceLocation, Integer> e : counts.entrySet()) {
            Item item = ForgeRegistries.ITEMS.getValue(e.getKey());
            if (item == null || item == Items.AIR) continue;
            long total = Math.min(Integer.MAX_VALUE, (long) e.getValue() * successes);
            builder.outputItems(new ItemStack(item, (int) total));
        }
        return builder.buildRawRecipe();
    }

    private void fizzle() {
        EntityShredderMachine m = getMachine();
        if (!(m.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = m.getPos();
        Direction front = m.getFrontFacing();
        double x = pos.getX() + 0.5 + front.getStepX() * 0.6;
        double y = pos.getY() + 0.5 + front.getStepY() * 0.6;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.6;
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 12, 0.25, 0.25, 0.25, 0.05);
    }

    @Override
    public void onRecipeFinish() {
        machine.afterWorking();
        if (lastRecipe != null) {
            runAttempt = 0;
            runDelay = 0;
            consecutiveRecipes++;
            ShredderStats stats = getMachine().stats();
            int batch = Math.max(1, lastRecipe.batchParallels);
            int successes = 0;
            for (int i = 0; i < batch; i++) {
                if (stats.roll(getMachine().getLevel().getRandom())) successes++;
            }
            if (successes == batch) {
                handleRecipeIO(lastRecipe, IO.OUT);
            } else {
                if (successes > 0) {
                    Map<ResourceLocation, Integer> counts = pendingCounts != null ? pendingCounts :
                            plan(stats, getMachine().parallels()).counts();
                    handleRecipeIO(partialRecipe(counts, successes), IO.OUT);
                }
                fizzle();
            }
        }
        pendingCounts = null;

        lastRecipe = null;
        lastOriginRecipe = null;
        progress = 0;
        duration = 0;
        isActive = false;

        if (suspendAfterFinish) {
            consecutiveRecipes = 0;
            setStatus(Status.SUSPEND);
            return;
        }

        lastFailedMatches = null;
        failureReasonMap.clear();
        GTRecipe next = buildRecipe();
        boolean started = next != null && start(next);
        recipeDirty = false;
        if (!started) {
            consecutiveRecipes = 0;
            setStatus(Status.IDLE);
        }
    }
}
