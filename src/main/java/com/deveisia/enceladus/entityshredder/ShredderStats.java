package com.deveisia.enceladus.entityshredder;

public record ShredderStats(ShredderTier tier, int coilTier, int stabSum, int stabCount, int spaceSum, int spaceCount,
                            int timeSum, int timeCount) {

    public static final int MAX_COIL_LEVEL = 20;
    public static final int COIL_STEP_PERCENT = 5;
    public static final int BASE_CHANCE_THIRDS_OF_PERCENT = 84;
    public static final int FULL_CHANCE_THIRDS_OF_PERCENT = 300;
    public static final double MODULE_TIER_FACTOR = 0.25;

    public int coilLevel() {
        return Math.max(1, Math.min(MAX_COIL_LEVEL, coilTier + 1));
    }

    public double coilTimeFactor() {
        return (100 - COIL_STEP_PERCENT * (coilLevel() - 1)) / 100.0;
    }

    public double spacetimeBonus() {
        return spaceSum * MODULE_TIER_FACTOR;
    }

    public double timeAccelerationBonus() {
        return timeSum * MODULE_TIER_FACTOR;
    }

    public double durationMultiplier() {
        return coilTimeFactor() * (1.0 + spacetimeBonus());
    }

    public double powerMultiplier() {
        return 1.0 + timeAccelerationBonus();
    }

    public double outputMultiplier() {
        return 1.0 + spacetimeBonus() + timeAccelerationBonus();
    }

    public int successUnits() {
        return Math.min(FULL_CHANCE_THIRDS_OF_PERCENT, BASE_CHANCE_THIRDS_OF_PERCENT + stabSum);
    }

    public double successPercent() {
        return successUnits() * 100.0 / FULL_CHANCE_THIRDS_OF_PERCENT;
    }

    public double exactDuration() {
        return tier.baseDuration * durationMultiplier();
    }

    public int durationTicks() {
        long rounded = Math.round(exactDuration());
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, rounded));
    }

    public double exactPower(int entityCount, int parallel) {
        return tier.voltage() * (double) Math.max(1, entityCount) * powerMultiplier() * Math.max(1, parallel);
    }

    public long powerPerTick(int entityCount, int parallel) {
        return Math.max(1L, Math.round(exactPower(entityCount, parallel)));
    }

    public int scaleOutput(int baseCount, int parallel) {
        double scaled = Math.floor(Math.max(1, baseCount) * outputMultiplier());
        double total = scaled * Math.max(1, parallel);
        return (int) Math.max(1.0, Math.min(Integer.MAX_VALUE, total));
    }

    public boolean roll(net.minecraft.util.RandomSource random) {
        int units = successUnits();
        if (units >= FULL_CHANCE_THIRDS_OF_PERCENT) return true;
        return random.nextInt(FULL_CHANCE_THIRDS_OF_PERCENT) < units;
    }
}
