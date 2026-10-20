package neofontrender.addons.compat;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.Loader;
import neofontrender.api.config.NfrMixinConfig;
import zone.rong.mixinbooter.Context;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/**
 * Queues optional integrations after Forge adds mod jars to the classpath. This class deliberately
 * lives outside the configured Mixin package, whose classes LaunchWrapper is forbidden to load.
 */
public final class UiEnhancementsCompatMixinLoader implements ILateMixinLoader {
    static final String PREGEN_CONFIG = "mixins.neofontrender_ui_enhancements_pregenerator.json";
    static final String HEI_CONFIG = "mixins.neofontrender_ui_enhancements_hei.json";
    static final String LEGENDARY_TOOLTIPS_CONFIG =
            "mixins.neofontrender_ui_enhancements_legendary_tooltips.json";
    static final String OBSCURE_TOOLTIPS_CONFIG =
            "mixins.neofontrender_ui_enhancements_obscure_tooltips.json";
    static final String QUARK_CONFIG =
            "mixins.neofontrender_ui_enhancements_quark.json";
    static final String SHOULDER_SURFING_CONFIG =
            "mixins.neofontrender_ui_enhancements_shouldersurfing.json";
    static final String SHOULDER_SURFING_TCONSTRUCT_CONFIG =
            "mixins.neofontrender_ui_enhancements_shouldersurfing_tconstruct.json";
    static final String SHOULDER_SURFING_MATTER_OVERDRIVE_CONFIG =
            "mixins.neofontrender_ui_enhancements_shouldersurfing_matteroverdrive.json";
    static final String BETTER_COMBAT_CONFIG =
            "mixins.neofontrender_ui_enhancements_bettercombat.json";
    static final String THAUMCRAFT_CONFIG =
            "mixins.neofontrender_ui_enhancements_thaumcraft.json";
    static final String CLEANROOM_COMMAND_SUGGESTIONS_CONFIG =
            "mixins.neofontrender_ui_enhancements_cleanroom_command_suggestions.json";
    static final String DRAGONCORE_CONFIG =
            "mixins.neofontrender_ui_enhancements_dragoncore.json";
    static final String RPGHUDLITE_CONFIG =
            "mixins.neofontrender_ui_enhancements_rpghudlite.json";

    /**
     * Returns the optional compatibility patches that are available in the running client.
     * This is deliberately calculated on demand: mod discovery and the transformed Minecraft
     * classes are only reliable after the client has finished bootstrapping.
     */
    public static List<CompatibilityPatchStatus> patchStatuses() {
        List<CompatibilityPatchStatus> statuses = new ArrayList<>();
        addStatus(statuses, "DragonCore Tabby/渲染状态", "dragoncore", true,
                () -> dragonCorePresent(),
                "neofontrender.addons.mixin.compat.MixinDragonCoreTabbyChat",
                "neofontrender.addons.mixin.compat.MixinDragonCoreEntityViewState",
                "neofontrender.addons.mixin.compat.MixinDragonCoreLivingRenderState",
                "neofontrender.addons.mixin.compat.MixinDragonCoreSpecialsState");
        addStatus(statuses, "RPGHUD Lite 界面状态", "rpghudlite", true,
                () -> modAndClassPresent("rpghudlite", "com/sao/rpghudlite/CompactHud.class"),
                "neofontrender.addons.mixin.compat.MixinRpgHudLiteGuiState");
        addStatus(statuses, "JEI/HEI 工具提示", "jei", false,
                () -> jeiPresent(),
                "neofontrender.addons.mixin.compat.MixinJeiIngredientRendererHover",
                "neofontrender.addons.mixin.compat.MixinHeiCollapsedGroupHover",
                "neofontrender.addons.mixin.compat.MixinJeiIngredientGridHover",
                "neofontrender.addons.mixin.compat.MixinHeiIngredientPreviewHover",
                "neofontrender.addons.mixin.compat.MixinHeiCollapsedGroupTooltip",
                "neofontrender.addons.mixin.compat.MixinHeiTooltipRenderer",
                "neofontrender.addons.mixin.compat.MixinHeiScrollBar");
        addStatus(statuses, "Legendary Tooltips", "legendarytooltips", false,
                () -> modAndClassPresent("legendarytooltips",
                        "com/anthonyhilyard/legendarytooltips/render/TooltipDecor.class"),
                "neofontrender.addons.mixin.compat.MixinLegendaryTooltipEvents",
                "neofontrender.addons.mixin.compat.MixinLegendaryTooltipDecor");
        addStatus(statuses, "Obscure Tooltips", "obscure_tooltips", false,
                () -> modAndClassPresent("obscure_tooltips",
                        "dev/obscuria/tooltips/client/TooltipEventHandler.class"),
                "neofontrender.addons.mixin.compat.MixinObscureTooltipEventHandler");
        addStatus(statuses, "Quark 工具提示", "quark", false,
                () -> modAndClassPresent("quark", "vazkii/quark/client/feature/MapTooltip.class"),
                "neofontrender.addons.mixin.compat.MixinQuarkEnchantedBooksShowItems",
                "neofontrender.addons.mixin.compat.MixinQuarkFoodTooltip",
                "neofontrender.addons.mixin.compat.MixinQuarkMapTooltip",
                "neofontrender.addons.mixin.compat.MixinQuarkVisualStatDisplay");
        addStatus(statuses, "Shoulder Surfing", "shouldersurfing", false,
                () -> modAndClassPresent("shouldersurfing",
                        "com/teamderpy/shouldersurfing/client/ShoulderRenderer.class"),
                "neofontrender.addons.mixin.compat.MixinShoulderSurfingCrosshairMatrix",
                "neofontrender.addons.mixin.compat.MixinEntityRendererShoulderSurfingMouseOver");
        addStatus(statuses, "TConstruct 准星", "tconstruct", false,
                () -> modAndClassPresent("tconstruct",
                        "slimeknights/tconstruct/library/client/crosshair/CrosshairRenderEvents.class"),
                "neofontrender.addons.mixin.compat.MixinTConstructCrosshairOffset");
        addStatus(statuses, "Matter Overdrive 准星", "matteroverdrive", false,
                () -> modAndClassPresent("matteroverdrive", "matteroverdrive/gui/GuiAndroidHud.class"),
                "neofontrender.addons.mixin.compat.MixinMatterOverdriveCrosshairOffset");
        addStatus(statuses, "Better Combat 准星", "bettercombat", false,
                () -> !safeModPresent("shouldersurfing")
                        && !classResourcePresent("com/teamderpy/shouldersurfing/client/ShoulderRenderer.class")
                        && classResourcePresent("bettercombat/mod/client/gui/GuiCrosshairsBC.class"),
                "neofontrender.addons.mixin.compat.MixinBetterCombatShoulderCrosshair");
        addStatus(statuses, "Thaumcraft 工具提示", "thaumcraft", false,
                () -> thaumcraftPresent(),
                "neofontrender.addons.mixin.compat.MixinThaumcraftResearchBrowserBackground",
                "neofontrender.addons.mixin.compat.MixinThaumcraftCustomTooltip",
                "neofontrender.addons.mixin.compat.MixinThaumcraftAspectTooltip");
        addStatus(statuses, "Chunk Pregenerator", "chunkpregenerator", false,
                () -> safeModPresent("chunkpregenerator")
                        && classResourcePresent("pregenerator/impl/client/ClientHandler.class")
                        && classResourcePresent("pregenerator/impl/client/gui/chat/ChatScreen$Completor.class")
                        && classResourcePresent("carbonconfiglib/config/ConfigEntry$BoolValue.class"),
                "neofontrender.addons.mixin.compat.MixinPregenClientHandler");
        addStatus(statuses, "Cleanroom 命令建议", "cleanroom-command-suggestions", false,
                () -> classResourcePresent("com/cleanroommc/client/chat/suggestion/SuggestionUpdater.class")
                        && classResourcePresent("com/cleanroommc/client/chat/suggestion/SuggestionList.class"),
                "neofontrender.addons.mixin.compat.MixinCleanroomSuggestionList",
                "neofontrender.addons.mixin.compat.MixinCleanroomSuggestionUpdater");
        return Collections.unmodifiableList(statuses);
    }

    private static void addStatus(List<CompatibilityPatchStatus> statuses, String name, String id,
                                  boolean configIsSwitchable, BooleanSupplier available,
                                  String... mixins) {
        boolean present;
        try {
            present = available.getAsBoolean();
        } catch (Throwable ignored) {
            // A diagnostic page must remain usable when an optional mod exposes an incomplete
            // class set or a loader implementation is not fully initialized yet.
            present = false;
        }
        int enabled = 0;
        if (present) {
            for (String mixin : mixins) {
                if (!configIsSwitchable || NfrMixinConfig.isEnabled(mixin)) enabled++;
            }
        }
        statuses.add(new CompatibilityPatchStatus(id, name, present, enabled, mixins.length));
    }

    private static boolean dragonCorePresent() {
        return safeModPresent("dragoncore")
                && classResourcePresent("eos/moe/dragoncore/xg.class")
                && classResourcePresent("eos/moe/dragoncore/nh.class")
                && classResourcePresent("eos/moe/dragoncore/uha.class")
                && classResourcePresent("eos/moe/dragoncore/np.class");
    }

    private static boolean jeiPresent() {
        return safeModPresent("jei")
                && classResourcePresent("mezz/jei/gui/TooltipRenderer.class")
                && classResourcePresent("mezz/jei/gui/overlay/IngredientGrid.class")
                && classResourcePresent("mezz/jei/render/IngredientRenderer.class");
    }

    private static boolean thaumcraftPresent() {
        return safeModPresent("thaumcraft")
                && classResourcePresent("thaumcraft/client/gui/GuiResearchBrowser.class")
                && classResourcePresent("thaumcraft/client/lib/UtilsFX.class")
                && classResourcePresent("thaumcraft/client/lib/events/RenderEventHandler.class")
                && classResourcePresent("thaumcraft/client/lib/events/HudHandler.class");
    }

    private static boolean modAndClassPresent(String modId, String resource) {
        return safeModPresent(modId) && classResourcePresent(resource);
    }

    private static boolean safeModPresent(String modId) {
        try {
            return Loader.isModLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean classResourcePresent(String resource) {
        if (Launch.classLoader != null && Launch.classLoader.getResource(resource) != null) return true;
        ClassLoader loader = UiEnhancementsCompatMixinLoader.class.getClassLoader();
        return loader != null && loader.getResource(resource) != null;
    }

    /** A point-in-time status row used by the diagnostics page. */
    public static final class CompatibilityPatchStatus {
        private final String id;
        private final String name;
        private final boolean available;
        private final int enabledMixins;
        private final int totalMixins;

        private CompatibilityPatchStatus(String id, String name, boolean available,
                                         int enabledMixins, int totalMixins) {
            this.id = id;
            this.name = name;
            this.available = available;
            this.enabledMixins = enabledMixins;
            this.totalMixins = totalMixins;
        }

        public String id() { return id; }
        public String name() { return name; }
        public boolean available() { return available; }
        public int enabledMixins() { return enabledMixins; }
        public int totalMixins() { return totalMixins; }
        public boolean enabled() { return available && enabledMixins > 0; }
        public boolean partial() { return enabled() && enabledMixins < totalMixins; }
    }

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(PREGEN_CONFIG, HEI_CONFIG, OBSCURE_TOOLTIPS_CONFIG, LEGENDARY_TOOLTIPS_CONFIG, QUARK_CONFIG,
                SHOULDER_SURFING_CONFIG, SHOULDER_SURFING_TCONSTRUCT_CONFIG,
                SHOULDER_SURFING_MATTER_OVERDRIVE_CONFIG, BETTER_COMBAT_CONFIG,
                THAUMCRAFT_CONFIG, CLEANROOM_COMMAND_SUGGESTIONS_CONFIG,
                DRAGONCORE_CONFIG, RPGHUDLITE_CONFIG);
    }

    @Override
    public boolean shouldMixinConfigQueue(Context context) {
        String config = context.mixinConfig();
        if (PREGEN_CONFIG.equals(config)) {
            return context.isModPresent("chunkpregenerator")
                    && classResourcePresent("pregenerator/impl/client/ClientHandler.class")
                    && classResourcePresent("pregenerator/impl/client/gui/chat/ChatScreen$Completor.class")
                    && classResourcePresent("carbonconfiglib/config/ConfigEntry$BoolValue.class");
        }
        if (LEGENDARY_TOOLTIPS_CONFIG.equals(config)) {
            return context.isModPresent("legendarytooltips")
                    && classResourcePresent("com/anthonyhilyard/legendarytooltips/render/TooltipDecor.class");
        }
        if (HEI_CONFIG.equals(config)) {
            return context.isModPresent("jei")
                    && classResourcePresent("mezz/jei/gui/TooltipRenderer.class")
                    && classResourcePresent("mezz/jei/gui/overlay/IngredientGrid.class")
                    && classResourcePresent("mezz/jei/render/IngredientRenderer.class");
        }
        if (OBSCURE_TOOLTIPS_CONFIG.equals(config)) {
            return context.isModPresent("obscure_tooltips")
                    && classResourcePresent("dev/obscuria/tooltips/client/TooltipEventHandler.class");
        }
        if (QUARK_CONFIG.equals(config)) {
            return context.isModPresent("quark")
                    && classResourcePresent("vazkii/quark/client/feature/MapTooltip.class");
        }
        if (SHOULDER_SURFING_CONFIG.equals(config)) {
            return context.isModPresent("shouldersurfing")
                    && classResourcePresent(
                            "com/teamderpy/shouldersurfing/client/ShoulderRenderer.class");
        }
        if (SHOULDER_SURFING_TCONSTRUCT_CONFIG.equals(config)) {
            return context.isModPresent("tconstruct")
                    && classResourcePresent(
                            "slimeknights/tconstruct/library/client/crosshair/CrosshairRenderEvents.class");
        }
        if (SHOULDER_SURFING_MATTER_OVERDRIVE_CONFIG.equals(config)) {
            return context.isModPresent("matteroverdrive")
                    && classResourcePresent("matteroverdrive/gui/GuiAndroidHud.class");
        }
        if (BETTER_COMBAT_CONFIG.equals(config)) {
            return !context.isModPresent("shouldersurfing")
                    && !classResourcePresent(
                            "com/teamderpy/shouldersurfing/client/ShoulderRenderer.class")
                    && classResourcePresent("bettercombat/mod/client/gui/GuiCrosshairsBC.class");
        }
        if (THAUMCRAFT_CONFIG.equals(config)) {
            return context.isModPresent("thaumcraft")
                    && classResourcePresent(
                            "thaumcraft/client/gui/GuiResearchBrowser.class")
                    && classResourcePresent("thaumcraft/client/lib/UtilsFX.class")
                    && classResourcePresent(
                            "thaumcraft/client/lib/events/RenderEventHandler.class")
                    && classResourcePresent(
                            "thaumcraft/client/lib/events/HudHandler.class");
        }
        if (CLEANROOM_COMMAND_SUGGESTIONS_CONFIG.equals(config)) {
            return classResourcePresent(
                    "com/cleanroommc/client/chat/suggestion/SuggestionUpdater.class")
                    && classResourcePresent(
                    "com/cleanroommc/client/chat/suggestion/SuggestionList.class");
        }
        if (DRAGONCORE_CONFIG.equals(config)) {
            // DragonCore is optional. Gate this entire config before Mixin resolves any of
            // its obfuscated target classes. The class checks also reject partial/incompatible
            // jars before the compatibility mixins are considered.
            return context.isModPresent("dragoncore")
                    && classResourcePresent("eos/moe/dragoncore/xg.class")
                    && classResourcePresent("eos/moe/dragoncore/nh.class")
                    && classResourcePresent("eos/moe/dragoncore/uha.class")
                    && classResourcePresent("eos/moe/dragoncore/np.class");
        }
        if (RPGHUDLITE_CONFIG.equals(config)) {
            return context.isModPresent("rpghudlite")
                    && classResourcePresent("com/sao/rpghudlite/CompactHud.class");
        }
        return false;
    }

}
