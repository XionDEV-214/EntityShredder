package com.deveisia.enceladus.entityshredder;

import com.gregtechceu.gtceu.api.GTValues;

import org.jetbrains.annotations.Nullable;

public enum ShredderTier {
    BASIC("entity_shredder", GTValues.IV, 400, false),
    ADVANCED("advanced_entity_shredder", GTValues.LuV, 200, true);

    public final String key;
    public final int voltageTier;
    public final int baseDuration;
    public final boolean modules;

    ShredderTier(String key, int voltageTier, int baseDuration, boolean modules) {
        this.key = key;
        this.voltageTier = voltageTier;
        this.baseDuration = baseDuration;
        this.modules = modules;
    }

    public long voltage() {
        return GTValues.VA[voltageTier];
    }

    public long nominalVoltage() {
        return GTValues.V[voltageTier];
    }

    public String langKey() {
        return EntityShredder.MOD_ID + ".tier." + key;
    }

    @Nullable
    public static ShredderTier byKey(String key) {
        for (ShredderTier t : values()) {
            if (t.key.equals(key)) return t;
        }
        return null;
    }
}
