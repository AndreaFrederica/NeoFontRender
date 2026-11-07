package neofontrender.addons.tooltips;

import net.minecraftforge.fml.common.Loader;

/** Selects a single tooltip renderer when Obscure Tooltips is installed. */
public final class ObscureTooltipCompat {
    private ObscureTooltipCompat() {}

    /** Either UIE preview option transfers the entire tooltip to UIE. */
    public static boolean shouldBypassObscure() {
        return selectsNfrRenderer(TooltipConfig.enabled, Arc3DRuntimeSupport.isAvailable(),
                TooltipConfig.itemPreviewEnabled, TooltipConfig.armorPreviewEnabled,
                TooltipConfig.headerIconEnabled);
    }

    public static boolean shouldYieldToObscure() {
        return Loader.isModLoaded("obscure_tooltips") && !shouldBypassObscure();
    }

    static boolean selectsNfrRenderer(boolean modernEnabled, boolean runtimeAvailable,
                                      boolean itemPreview, boolean armorPreview) {
        return selectsNfrRenderer(modernEnabled, runtimeAvailable, itemPreview, armorPreview, false);
    }

    static boolean selectsNfrRenderer(boolean modernEnabled, boolean runtimeAvailable,
                                      boolean itemPreview, boolean armorPreview,
                                      boolean headerIcon) {
        return modernEnabled && runtimeAvailable && (itemPreview || armorPreview || headerIcon);
    }
}
