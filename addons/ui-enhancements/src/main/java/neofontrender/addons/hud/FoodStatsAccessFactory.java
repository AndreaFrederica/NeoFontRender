package neofontrender.addons.hud;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import neofontrender.addons.ui.NfrUiEnhancements;

/** Selects the food adapter while keeping AppleCore-only bytecode behind an optional method boundary. */
final class FoodStatsAccessFactory {
    private FoodStatsAccessFactory() {}

    static FoodStatsAccess create() {
        if (!Loader.isModLoaded("AppleCore")) return VanillaFoodStatsAccess.INSTANCE;
        NfrUiEnhancements.LOGGER.info("AppleCore detected; enabling exhaustion and modded-food HUD previews");
        return createAppleCoreAccess();
    }

    @Optional.Method(modid = "AppleCore")
    private static FoodStatsAccess createAppleCoreAccess() {
        return new AppleCoreFoodStatsAccess();
    }
}
