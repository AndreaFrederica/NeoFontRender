package neofontrender.addons.hud;

/** Captures HUD runtime state and supplies transaction-safe restore and persistence operations. */
final class HudBarsConfigSnapshot {
    private final Runnable persistence;
    private final boolean[] booleans = {
            HudBarsConfig.enabled, HudBarsConfig.yieldToClassicBar, HudBarsConfig.health,
            HudBarsConfig.absorption, HudBarsConfig.armor, HudBarsConfig.food, HudBarsConfig.air,
            HudBarsConfig.mountHealth, HudBarsConfig.showNumbers, HudBarsConfig.smoothValues,
            HudBarsConfig.rounded
    };
    private final int width = HudBarsConfig.width;
    private final int height = HudBarsConfig.height;
    private final int gap = HudBarsConfig.gap;
    private final String theme = HudBarsConfig.theme;
    private final int[] colors = {
            HudBarsConfig.background, HudBarsConfig.border, HudBarsConfig.healthColor,
            HudBarsConfig.healthyColor, HudBarsConfig.absorptionColor, HudBarsConfig.armorColor,
            HudBarsConfig.foodColor, HudBarsConfig.saturationColor, HudBarsConfig.airColor,
            HudBarsConfig.mountColor
    };

    private HudBarsConfigSnapshot(Runnable persistence) {
        if (persistence == null) throw new IllegalArgumentException("persistence must not be null");
        this.persistence = persistence;
    }

    static HudBarsConfigSnapshot capture() {
        return new HudBarsConfigSnapshot(HudBarsConfig::save);
    }

    static HudBarsConfigSnapshot capture(Runnable persistence) {
        return new HudBarsConfigSnapshot(persistence);
    }

    void restoreRuntime() {
        HudBarsConfig.enabled = booleans[0];
        HudBarsConfig.yieldToClassicBar = booleans[1];
        HudBarsConfig.health = booleans[2];
        HudBarsConfig.absorption = booleans[3];
        HudBarsConfig.armor = booleans[4];
        HudBarsConfig.food = booleans[5];
        HudBarsConfig.air = booleans[6];
        HudBarsConfig.mountHealth = booleans[7];
        HudBarsConfig.showNumbers = booleans[8];
        HudBarsConfig.smoothValues = booleans[9];
        HudBarsConfig.rounded = booleans[10];
        HudBarsConfig.width = width;
        HudBarsConfig.height = height;
        HudBarsConfig.gap = gap;
        HudBarsConfig.theme = theme;
        HudBarsConfig.background = colors[0];
        HudBarsConfig.border = colors[1];
        HudBarsConfig.healthColor = colors[2];
        HudBarsConfig.healthyColor = colors[3];
        HudBarsConfig.absorptionColor = colors[4];
        HudBarsConfig.armorColor = colors[5];
        HudBarsConfig.foodColor = colors[6];
        HudBarsConfig.saturationColor = colors[7];
        HudBarsConfig.airColor = colors[8];
        HudBarsConfig.mountColor = colors[9];
    }

    void restoreAndPersist() {
        restoreRuntime();
        persistence.run();
    }
}
