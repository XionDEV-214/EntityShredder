package com.deveisia.enceladus.entityshredder.registry;

import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public enum ModuleType {
    SPACETIME("spacetime_compression_field_generator"),
    TIME("time_acceleration_field_generator"),
    STABILISATION("stabilisation_field_generator");

    public static final int MAX_TIER = 9;

    private final String prefix;
    private volatile Block[] blocks;

    ModuleType(String prefix) {
        this.prefix = prefix;
    }

    public String sumKey() {
        return "es_" + name().toLowerCase() + "_sum";
    }

    public String countKey() {
        return "es_" + name().toLowerCase() + "_count";
    }

    private Block[] blocks() {
        Block[] local = blocks;
        if (local == null) {
            local = new Block[MAX_TIER];
            for (int i = 0; i < MAX_TIER; i++) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gtceu", prefix + "_" + (i + 1));
                local[i] = ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
            }
            blocks = local;
        }
        return local;
    }

    public int tierOf(Block block) {
        Block[] arr = blocks();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != null && arr[i] == block) return i + 1;
        }
        return 0;
    }

    public TraceabilityPredicate predicate() {
        TraceabilityPredicate module = new TraceabilityPredicate(state -> {
            int tier = tierOf(state.getBlockState().getBlock());
            if (tier <= 0) return false;
            var ctx = state.getMatchContext();
            ctx.increment(sumKey(), tier);
            ctx.increment(countKey(), 1);
            return true;
        }, () -> {
            List<BlockInfo> infos = new ArrayList<>();
            for (Block b : blocks()) if (b != null) infos.add(BlockInfo.fromBlockState(b.defaultBlockState()));
            return infos.toArray(BlockInfo[]::new);
        });
        return module.or(Predicates.air());
    }
}
