package neofontrender.addons.tooltips;

import neofontrender.api.config.NfrConfigFile;
import neofontrender.addons.ui.UiEnhancementsConfig;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;

final class TooltipConfig {
    private static final int[] DEFAULT_FILL = defaults(0xE6101018);
    private static final int[] DEFAULT_BORDER = {0xF0AADCF0, 0xF0DAD0F4, 0xF0DAD0F4, 0xF0AADCF0};
    private static NfrConfigFile config;

    static boolean enabled = true;
    static String renderStyle = "modernui";
    static boolean lowBrightnessMicaEnhancement = false;
    static boolean micaSampleUi = false;
    static boolean yieldToLegendaryTooltips = true;
    static String legendaryOwnership = "auto";
    static boolean heiCustomTooltips = true;
    static boolean quarkModernMapTooltip = false;
    static boolean modNameEnabled = true;
    static boolean modNameMoveToEnd = true;
    static String modNameFormat = "blue italic";
    static boolean advancedEnabled = true;
    static boolean advancedRequireCtrl = true;
    static boolean advancedOreDictionary = true;
    static boolean advancedRegistryName = true;
    static boolean advancedUnlocalizedName = true;
    static boolean advancedMeta = true;
    static boolean advancedNbt = true;
    static boolean advancedNbtRequireShift = true;
    static int advancedNbtCharacterLimit = 0;
    static boolean rounded = true;
    /** Title text alignment: left, center or right. */
    static String titleAlignment = "center";
    /** Ordinary body rows use the same measured text box, with a separate alignment policy. */
    static String bodyAlignment = "left";
    /** Rarity row alignment is independent; left is the least surprising default. */
    static String rarityAlignment = "left";
    /** Vertical anchor used by the title icon: full header, title block or first title row. */
    static String headerIconAlignment = "header";
    static boolean titleBreak = true;
    static boolean adaptiveBorder = true;
    static String borderShading = "gradient";
    static int borderCycleMillis = 1000;
    static float cornerRadius = 4.0F;
    static float borderWidth = 1.25F;
    static float shadowRadius = 4.0F;
    static int shadowAlpha = 72;
    static float shadowOffsetX = 0.0F;
    static float shadowOffsetY = 2.0F;
    static int shadowColor = 0xFF000000;
    static int shadowSteps = 12;
    static int cornerSegments = 12;
    static float antialiasWidth = 0.55F;
    static boolean textShadow = true;
    static boolean headerIconEnabled = true;
    static boolean headerIconFrameEnabled = true;
    static boolean headerIconFrameRarityColor = true;
    static int headerIconFrameColor = 0xD0AAB4C4;
    static boolean headerIconBackgroundEnabled = true;
    static int headerIconBackgroundColor = 0x80101018;
    static boolean headerIconRounded = true;
    static float headerIconCornerRadius = 3.0F;
    static boolean headerIconAnimationEnabled = true;
    static int headerIconAnimationMillis = 180;
    static boolean rarityEnabled = true;
    static boolean itemPreviewEnabled = true;
    static String itemPreviewScope = "tools";
    static boolean armorPreviewEnabled = true;
    static boolean previewAnimationEnabled = true;
    static boolean previewMeasureBounds = true;
    static int previewAnimationMillis = 180;
    static boolean previewEffectsEnabled = true;
    static boolean previewParticlesEnabled = true;
    static float previewEffectSpeed = 1.0F;
    static int previewParticleCount = 8;
    static boolean previewSoundEnabled = false;
    static float previewSoundVolume = 0.35F;
    static String previewSoundEvent = "ui.button.click";
    static float previewSoundPitch = 1.0F;
    static int previewSoundCooldownMillis = 250;
    static String armorPreviewModel = "armor_stand";
    static String armorPreviewMode = "single_piece";
    static boolean armorStandBasePlate = false;
    static boolean armorPlayerCopyHands = false;
    static boolean armorPlayerSneaking = false;
    static String armorPlayerPose = "idle";
    static int itemPreviewWidth = 30;
    static int armorPreviewWidth = 40;
    static List<String> previewWhitelist = Collections.emptyList();
    static List<String> previewBlacklist = Collections.emptyList();
    /** Item Zoom/Item Zoomer compatible enlarged hover overlay. */
    static boolean zoomOverlayEnabled = false;
    static String zoomOverlayScope = "tools";
    static String zoomOverlayBlockMode = "3d";
    static String zoomOverlayToolMode = "3d";
    static String zoomOverlayEquipmentMode = "3d";
    static String zoomOverlayOtherMode = "2d";
    static String zoomOverlayArmorModel = "follow";
    static String zoomOverlayArmorMode = "follow";
    static String zoomOverlayMotion = "spin";
    static String zoomOverlaySide = "auto";
    static String zoomOverlayLayer = "below_tooltip";
    static int zoomOverlaySize = 88;
    static int zoomOverlayGap = 8;
    static boolean zoomOverlayAnimation = true;
    static boolean zoomOverlayMeasureBounds = true;
    static String zoomOverlayAnimationSwitch = "continue";
    static int zoomOverlayAnimationMillis = 180;
    static boolean zoomOverlayRotation = true;
    static float zoomOverlayRotationSpeed = 18.0F;
    static boolean zoomOverlayShowStackSize = false;
    static boolean zoomOverlayShowDurability = false;
    static boolean zoomOverlayShowCooldown = false;
    static boolean zoomOverlayPanel = false;
    static boolean zoomOverlayBorder = true;
    static int zoomOverlayPanelInset = 5;
    static int zoomOverlayBackgroundColor = 0xC0101018;
    static int zoomOverlayBorderColor = 0xD0AAB4C4;
    static List<String> zoomOverlayWhitelist = Collections.emptyList();
    static List<String> zoomOverlayBlacklist = Collections.emptyList();
    static int textColor = 0xFFFFFFFF;
    static int titleColor = 0xFFFFFFFF;
    static int dividerAlpha = 176;
    static int leftPadding = 5;
    static int rightPadding = 5;
    /** Insets are applied once, outside the measured content bounds. */
    static int topPadding = 5;
    static int bottomPadding = 5;
    static int lineHeight = 10;
    /** Space between the title block and the divider line. */
    static int dividerTopMargin = 0;
    /** Space between the divider line and the first body row. */
    static int dividerBottomMargin = 3;
    static int cursorOffset = 12;
    static int maxWidth = 0;
    static int[] fillColors = DEFAULT_FILL.clone();
    static int[] borderColors = DEFAULT_BORDER.clone();
    // Complete preview selector registry; typography controls are limited by the settings page.
    private static final String[] PROFILE_IDS = {
            "vanilla", "thaumcraft", "hei", "quark"
    };
    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();

    private TooltipConfig() {}

    static void load() {
        config = UiEnhancementsConfig.file();
        defineDefaults();
        enabled = config.getBoolean("tooltip.enabled", true);
        renderStyle = normalizeStyle(config.getString("tooltip.style", "modernui"));
        lowBrightnessMicaEnhancement = config.getBoolean(
                "tooltip.lowBrightnessMicaEnhancement", false);
        micaSampleUi = config.getBoolean("tooltip.micaSampleUi", false);
        yieldToLegendaryTooltips = config.getBoolean("tooltip.yieldToLegendaryTooltips", true);
        legendaryOwnership = normalizeOwnership(config.getString("tooltip.legendaryOwnership", "auto"));
        heiCustomTooltips = config.getBoolean("tooltip.heiCustomTooltips", true);
        quarkModernMapTooltip = config.getBoolean("tooltip.quarkModernMapTooltip", false);
        modNameEnabled = config.getBoolean("tooltip.modName.enabled", true);
        modNameMoveToEnd = config.getBoolean("tooltip.modName.moveToEnd", true);
        modNameFormat = config.getString("tooltip.modName.format", "blue italic");
        advancedEnabled = config.getBoolean("tooltip.advanced.enabled", true);
        advancedRequireCtrl = config.getBoolean("tooltip.advanced.requireCtrl", true);
        advancedOreDictionary = config.getBoolean("tooltip.advanced.oreDictionary", true);
        advancedRegistryName = config.getBoolean("tooltip.advanced.registryName", true);
        advancedUnlocalizedName = config.getBoolean("tooltip.advanced.unlocalizedName", true);
        advancedMeta = config.getBoolean("tooltip.advanced.meta", true);
        advancedNbt = config.getBoolean("tooltip.advanced.nbt", true);
        advancedNbtRequireShift = config.getBoolean("tooltip.advanced.nbtRequireShift", true);
        advancedNbtCharacterLimit = config.getInt("tooltip.advanced.nbtCharacterLimit", 0, 0, 100000);
        rounded = config.getBoolean("tooltip.rounded", true);
        titleAlignment = normalizeTitleAlignment(config.getString("tooltip.titleAlignment", "center"));
        bodyAlignment = normalizeAlignment(config.getString("tooltip.bodyAlignment", "left"), "left");
        rarityAlignment = normalizeAlignment(config.getString("tooltip.rarityAlignment", "left"), "left");
        headerIconAlignment = normalizeHeaderIconAlignment(config.getString(
                "tooltip.preview.icon.alignment", "header"));
        titleBreak = config.getBoolean("tooltip.titleBreak", true);
        adaptiveBorder = config.getBoolean("tooltip.adaptiveBorder", true);
        borderShading = normalizeBorderShading(config.getString("tooltip.borderShading", "gradient"));
        borderCycleMillis = config.getInt("tooltip.borderCycleMillis", 1000, 250, 10000);
        cornerRadius = (float) config.getDouble("tooltip.cornerRadius", 4.0D, 0.0D, 16.0D);
        borderWidth = (float) config.getDouble("tooltip.borderWidth", 1.25D, 0.5D, 4.0D);
        shadowRadius = (float) config.getDouble("tooltip.shadowRadius", 4.0D, 0.0D, 12.0D);
        shadowAlpha = config.getInt("tooltip.shadowAlpha", 72, 0, 255);
        shadowOffsetX = (float) config.getDouble("tooltip.shadowOffsetX", 0.0D, -12.0D, 12.0D);
        shadowOffsetY = (float) config.getDouble("tooltip.shadowOffsetY", 2.0D, -12.0D, 12.0D);
        shadowColor = parseColor(config.getString("tooltip.shadowColor", "#FF000000"), 0xFF000000);
        shadowSteps = config.getInt("quality.shadowSteps", 12, 2, 32);
        cornerSegments = config.getInt("quality.cornerSegments", 12, 3, 32);
        antialiasWidth = (float) config.getDouble("quality.antialiasWidth", 0.55D, 0.0D, 1.5D);
        textShadow = config.getBoolean("text.shadow", true);
        headerIconEnabled = config.getBoolean("tooltip.preview.icon.enabled", true);
        headerIconFrameEnabled = config.getBoolean("tooltip.preview.icon.frame.enabled", true);
        headerIconFrameRarityColor = config.getBoolean(
                "tooltip.preview.icon.frame.rarityColor", true);
        headerIconFrameColor = parseColor(config.getString(
                "tooltip.preview.icon.frame.color", "#D0AAB4C4"), 0xD0AAB4C4);
        headerIconBackgroundEnabled = config.getBoolean(
                "tooltip.preview.icon.background.enabled", true);
        headerIconBackgroundColor = parseColor(config.getString(
                "tooltip.preview.icon.background.color", "#80101018"), 0x80101018);
        headerIconRounded = config.getBoolean("tooltip.preview.icon.rounded", true);
        headerIconCornerRadius = (float) config.getDouble(
                "tooltip.preview.icon.cornerRadius", 3.0D, 0.0D, 9.0D);
        headerIconAnimationEnabled = config.getBoolean("tooltip.preview.icon.animation.enabled", true);
        headerIconAnimationMillis = config.getInt(
                "tooltip.preview.icon.animation.durationMillis", 180, 0, 2000);
        rarityEnabled = config.getBoolean("tooltip.preview.rarity.enabled", true);
        itemPreviewEnabled = config.getBoolean("tooltip.preview.item.enabled", true);
        itemPreviewScope = normalizeItemPreviewScope(
                config.getString("tooltip.preview.item.scope", "tools"));
        armorPreviewEnabled = config.getBoolean("tooltip.preview.armor.enabled", true);
        previewAnimationEnabled = config.getBoolean("tooltip.preview.animation.enabled", true);
        previewMeasureBounds = config.getBoolean("tooltip.preview.measureBounds", true);
        previewAnimationMillis = config.getInt("tooltip.preview.animation.durationMillis", 180, 0, 2000);
        previewEffectsEnabled = config.getBoolean("tooltip.preview.effects.enabled", true);
        previewParticlesEnabled = config.getBoolean("tooltip.preview.effects.particles", true);
        previewEffectSpeed = (float) config.getDouble("tooltip.preview.effects.speed", 1.0D, 0.1D, 4.0D);
        previewParticleCount = config.getInt("tooltip.preview.effects.particleCount", 8, 0, 32);
        previewSoundEnabled = config.getBoolean("tooltip.preview.sound.enabled", false);
        previewSoundVolume = (float) config.getDouble("tooltip.preview.sound.volume", 0.35D, 0.0D, 1.0D);
        previewSoundEvent = config.getString("tooltip.preview.sound.event", "ui.button.click");
        previewSoundPitch = (float) config.getDouble("tooltip.preview.sound.pitch", 1.0D, 0.5D, 2.0D);
        previewSoundCooldownMillis = config.getInt("tooltip.preview.sound.cooldownMillis", 250, 0, 5000);
        armorPreviewModel = config.getString("tooltip.preview.armor.model", "armor_stand");
        armorPreviewMode = config.getString("tooltip.preview.armor.mode", "single_piece");
        armorStandBasePlate = config.getBoolean("tooltip.preview.armor.stand.basePlate", false);
        armorPlayerCopyHands = config.getBoolean("tooltip.preview.armor.player.copyHands", false);
        armorPlayerSneaking = config.getBoolean("tooltip.preview.armor.player.sneaking", false);
        armorPlayerPose = config.getString("tooltip.preview.armor.player.pose", "idle");
        itemPreviewWidth = config.getInt("tooltip.preview.item.width", 30, 16, 128);
        armorPreviewWidth = config.getInt("tooltip.preview.armor.width", 40, 16, 128);
        previewWhitelist = config.getStringList("tooltip.preview.whitelist", Collections.emptyList());
        previewBlacklist = config.getStringList("tooltip.preview.blacklist", Collections.emptyList());
        zoomOverlayEnabled = config.getBoolean("tooltip.zoomOverlay.enabled", false);
        zoomOverlayScope = normalizeItemPreviewScope(config.getString("tooltip.zoomOverlay.scope", "tools"));
        zoomOverlayBlockMode = normalizeZoomDisplayMode(config.getString("tooltip.zoomOverlay.block.mode", "3d"), "3d");
        zoomOverlayToolMode = normalizeZoomDisplayMode(config.getString("tooltip.zoomOverlay.tool.mode", "3d"), "3d");
        zoomOverlayEquipmentMode = normalizeZoomDisplayMode(config.getString("tooltip.zoomOverlay.equipment.mode", "3d"), "3d");
        zoomOverlayOtherMode = normalizeZoomDisplayMode(config.getString("tooltip.zoomOverlay.other.mode", "2d"), "2d");
        zoomOverlayArmorModel = normalizeZoomArmorModel(config.getString("tooltip.zoomOverlay.equipment.model", "follow"));
        zoomOverlayArmorMode = normalizeZoomArmorMode(config.getString("tooltip.zoomOverlay.equipment.outfit", "follow"));
        zoomOverlayMotion = normalizeZoomMotion(config.getString("tooltip.zoomOverlay.motion", "spin"));
        zoomOverlaySide = normalizeZoomOverlaySide(config.getString("tooltip.zoomOverlay.side", "auto"));
        zoomOverlayLayer = normalizeZoomOverlayLayer(config.getString("tooltip.zoomOverlay.layer", "below_tooltip"));
        zoomOverlaySize = config.getInt("tooltip.zoomOverlay.size", 88, ItemZoomLayout.MIN_SIZE, ItemZoomLayout.MAX_SIZE);
        zoomOverlayGap = config.getInt("tooltip.zoomOverlay.gap", 8, 2, 32);
        zoomOverlayAnimation = config.getBoolean("tooltip.zoomOverlay.animation.enabled", true);
        zoomOverlayMeasureBounds = config.getBoolean("tooltip.zoomOverlay.measureBounds", true);
        zoomOverlayAnimationSwitch = normalizeZoomAnimationSwitch(config.getString("tooltip.zoomOverlay.animation.switch", "continue"));
        zoomOverlayAnimationMillis = config.getInt("tooltip.zoomOverlay.animation.durationMillis", 180, 0, 2000);
        zoomOverlayRotation = config.getBoolean("tooltip.zoomOverlay.rotation.enabled", true);
        zoomOverlayRotationSpeed = (float) config.getDouble("tooltip.zoomOverlay.rotation.speed", 18.0D, -180.0D, 180.0D);
        zoomOverlayShowStackSize = config.getBoolean("tooltip.zoomOverlay.show.stackSize", false);
        zoomOverlayShowDurability = config.getBoolean("tooltip.zoomOverlay.show.durability", false);
        zoomOverlayShowCooldown = config.getBoolean("tooltip.zoomOverlay.show.cooldown", false);
        zoomOverlayPanel = config.getBoolean("tooltip.zoomOverlay.panel.enabled", false);
        zoomOverlayBorder = config.getBoolean("tooltip.zoomOverlay.panel.border", true);
        zoomOverlayPanelInset = config.getInt("tooltip.zoomOverlay.panel.inset", 5, 0, 24);
        zoomOverlayBackgroundColor = parseColor(config.getString("tooltip.zoomOverlay.panel.background", "#C0101018"), 0xC0101018);
        zoomOverlayBorderColor = parseColor(config.getString("tooltip.zoomOverlay.panel.borderColor", "#D0AAB4C4"), 0xD0AAB4C4);
        zoomOverlayWhitelist = config.getStringList("tooltip.zoomOverlay.whitelist", Collections.emptyList());
        zoomOverlayBlacklist = config.getStringList("tooltip.zoomOverlay.blacklist", Collections.emptyList());
        textColor = parseColor(config.getString("text.color", "#FFFFFFFF"), 0xFFFFFFFF);
        titleColor = parseColor(config.getString("text.titleColor", "#FFFFFFFF"), 0xFFFFFFFF);
        dividerAlpha = config.getInt("text.dividerAlpha", 176, 0, 255);
        leftPadding = config.getInt("layout.leftPadding", 5, 0, 24);
        rightPadding = config.getInt("layout.rightPadding", 5, 0, 24);
        topPadding = config.getInt("layout.topPadding", 5, 0, 24);
        bottomPadding = config.getInt("layout.bottomPadding", 5, 0, 24);
        lineHeight = config.getInt("layout.lineHeight", 10, 8, 24);
        dividerTopMargin = config.getInt("layout.dividerTopMargin", 0, 0, 16);
        dividerBottomMargin = config.getInt("layout.dividerBottomMargin", 3, 0, 16);
        cursorOffset = config.getInt("layout.cursorOffset", 12, 0, 32);
        maxWidth = config.getInt("layout.maxWidth", 0, 0, 1024);
        fillColors = parseColors(config.getStringList("tooltip.fillColors", colorStrings(DEFAULT_FILL)), DEFAULT_FILL);
        borderColors = parseColors(config.getStringList("tooltip.borderColors", colorStrings(DEFAULT_BORDER)), DEFAULT_BORDER);
        PROFILES.clear();
        for (String id : PROFILE_IDS) {
            Profile profile = new Profile();
            profile.textScale = (float) config.getDouble(profileKey(id, "textScale"), 1.0D, 0.5D, 2.0D);
            profile.offsetX = (float) config.getDouble(profileKey(id, "offsetX"), 0.0D, -12.0D, 12.0D);
            profile.offsetY = (float) config.getDouble(profileKey(id, "offsetY"), 0.0D, -12.0D, 12.0D);
            PROFILES.put(id, profile);
        }
        // Discard obsolete keys without transferring their values to current settings.
        config.remove("tooltip.centerTitle");
        config.remove("tooltip.suppress.immersiveEngineering");
        config.remove("tooltip.suppress.enderCore");
        config.remove("layout.titleGap");
        config.remove("layout.horizontalPadding");
        config.remove("layout.verticalPadding");
        config.remove("layout.textTopPadding");
        config.remove("layout.textBottomPadding");
        config.save();
    }

    static void save() {
        config.set("tooltip.enabled", enabled)
                .set("tooltip.style", renderStyle)
                .set("tooltip.lowBrightnessMicaEnhancement", lowBrightnessMicaEnhancement)
                .set("tooltip.micaSampleUi", micaSampleUi)
                .set("tooltip.yieldToLegendaryTooltips", yieldToLegendaryTooltips)
                .set("tooltip.legendaryOwnership", legendaryOwnership)
                .set("tooltip.heiCustomTooltips", heiCustomTooltips)
                .set("tooltip.quarkModernMapTooltip", quarkModernMapTooltip)
                .set("tooltip.modName.enabled", modNameEnabled)
                .set("tooltip.modName.moveToEnd", modNameMoveToEnd)
                .set("tooltip.modName.format", modNameFormat)
                .set("tooltip.advanced.enabled", advancedEnabled)
                .set("tooltip.advanced.requireCtrl", advancedRequireCtrl)
                .set("tooltip.advanced.oreDictionary", advancedOreDictionary)
                .set("tooltip.advanced.registryName", advancedRegistryName)
                .set("tooltip.advanced.unlocalizedName", advancedUnlocalizedName)
                .set("tooltip.advanced.meta", advancedMeta)
                .set("tooltip.advanced.nbt", advancedNbt)
                .set("tooltip.advanced.nbtRequireShift", advancedNbtRequireShift)
                .set("tooltip.advanced.nbtCharacterLimit", advancedNbtCharacterLimit)
                .set("tooltip.rounded", rounded)
                .set("tooltip.titleAlignment", titleAlignment)
                .set("tooltip.bodyAlignment", bodyAlignment)
                .set("tooltip.rarityAlignment", rarityAlignment)
                .set("tooltip.preview.icon.alignment", headerIconAlignment)
                .set("tooltip.titleBreak", titleBreak)
                .set("tooltip.adaptiveBorder", adaptiveBorder)
                .set("tooltip.borderShading", borderShading)
                .set("tooltip.borderCycleMillis", borderCycleMillis)
                .set("tooltip.cornerRadius", cornerRadius)
                .set("tooltip.borderWidth", borderWidth)
                .set("tooltip.shadowRadius", shadowRadius)
                .set("tooltip.shadowAlpha", shadowAlpha)
                .set("tooltip.shadowOffsetX", shadowOffsetX)
                .set("tooltip.shadowOffsetY", shadowOffsetY)
                .set("tooltip.shadowColor", colorString(shadowColor))
                .set("quality.shadowSteps", shadowSteps)
                .set("quality.cornerSegments", cornerSegments)
                .set("quality.antialiasWidth", antialiasWidth)
                .set("text.shadow", textShadow)
                .set("tooltip.preview.icon.enabled", headerIconEnabled)
                .set("tooltip.preview.icon.frame.enabled", headerIconFrameEnabled)
                .set("tooltip.preview.icon.frame.rarityColor", headerIconFrameRarityColor)
                .set("tooltip.preview.icon.frame.color", colorString(headerIconFrameColor))
                .set("tooltip.preview.icon.background.enabled", headerIconBackgroundEnabled)
                .set("tooltip.preview.icon.background.color", colorString(headerIconBackgroundColor))
                .set("tooltip.preview.icon.rounded", headerIconRounded)
                .set("tooltip.preview.icon.cornerRadius", headerIconCornerRadius)
                .set("tooltip.preview.icon.animation.enabled", headerIconAnimationEnabled)
                .set("tooltip.preview.icon.animation.durationMillis", headerIconAnimationMillis)
                .set("tooltip.preview.rarity.enabled", rarityEnabled)
                .set("tooltip.preview.item.enabled", itemPreviewEnabled)
                .set("tooltip.preview.item.scope", itemPreviewScope)
                .set("tooltip.preview.armor.enabled", armorPreviewEnabled)
                .set("tooltip.preview.animation.enabled", previewAnimationEnabled)
                .set("tooltip.preview.measureBounds", previewMeasureBounds)
                .set("tooltip.preview.animation.durationMillis", previewAnimationMillis)
                .set("tooltip.preview.effects.enabled", previewEffectsEnabled)
                .set("tooltip.preview.effects.particles", previewParticlesEnabled)
                .set("tooltip.preview.effects.speed", previewEffectSpeed)
                .set("tooltip.preview.effects.particleCount", previewParticleCount)
                .set("tooltip.preview.sound.enabled", previewSoundEnabled)
                .set("tooltip.preview.sound.volume", previewSoundVolume)
                .set("tooltip.preview.sound.event", previewSoundEvent)
                .set("tooltip.preview.sound.pitch", previewSoundPitch)
                .set("tooltip.preview.sound.cooldownMillis", previewSoundCooldownMillis)
                .set("tooltip.preview.armor.model", armorPreviewModel)
                .set("tooltip.preview.armor.mode", armorPreviewMode)
                .set("tooltip.preview.armor.stand.basePlate", armorStandBasePlate)
                .set("tooltip.preview.armor.player.copyHands", armorPlayerCopyHands)
                .set("tooltip.preview.armor.player.sneaking", armorPlayerSneaking)
                .set("tooltip.preview.armor.player.pose", armorPlayerPose)
                .set("tooltip.preview.item.width", itemPreviewWidth)
                .set("tooltip.preview.armor.width", armorPreviewWidth)
                .set("tooltip.preview.whitelist", previewWhitelist)
                .set("tooltip.preview.blacklist", previewBlacklist)
                .set("tooltip.zoomOverlay.enabled", zoomOverlayEnabled)
                .set("tooltip.zoomOverlay.scope", zoomOverlayScope)
                .set("tooltip.zoomOverlay.block.mode", zoomOverlayBlockMode)
                .set("tooltip.zoomOverlay.tool.mode", zoomOverlayToolMode)
                .set("tooltip.zoomOverlay.equipment.mode", zoomOverlayEquipmentMode)
                .set("tooltip.zoomOverlay.other.mode", zoomOverlayOtherMode)
                .set("tooltip.zoomOverlay.equipment.model", zoomOverlayArmorModel)
                .set("tooltip.zoomOverlay.equipment.outfit", zoomOverlayArmorMode)
                .set("tooltip.zoomOverlay.motion", zoomOverlayMotion)
                .set("tooltip.zoomOverlay.side", zoomOverlaySide)
                .set("tooltip.zoomOverlay.layer", zoomOverlayLayer)
                .set("tooltip.zoomOverlay.size", zoomOverlaySize)
                .set("tooltip.zoomOverlay.gap", zoomOverlayGap)
                .set("tooltip.zoomOverlay.animation.enabled", zoomOverlayAnimation)
                .set("tooltip.zoomOverlay.measureBounds", zoomOverlayMeasureBounds)
                .set("tooltip.zoomOverlay.animation.switch", zoomOverlayAnimationSwitch)
                .set("tooltip.zoomOverlay.animation.durationMillis", zoomOverlayAnimationMillis)
                .set("tooltip.zoomOverlay.rotation.enabled", zoomOverlayRotation)
                .set("tooltip.zoomOverlay.rotation.speed", zoomOverlayRotationSpeed)
                .set("tooltip.zoomOverlay.show.stackSize", zoomOverlayShowStackSize)
                .set("tooltip.zoomOverlay.show.durability", zoomOverlayShowDurability)
                .set("tooltip.zoomOverlay.show.cooldown", zoomOverlayShowCooldown)
                .set("tooltip.zoomOverlay.panel.enabled", zoomOverlayPanel)
                .set("tooltip.zoomOverlay.panel.border", zoomOverlayBorder)
                .set("tooltip.zoomOverlay.panel.inset", zoomOverlayPanelInset)
                .set("tooltip.zoomOverlay.panel.background", colorString(zoomOverlayBackgroundColor))
                .set("tooltip.zoomOverlay.panel.borderColor", colorString(zoomOverlayBorderColor))
                .set("tooltip.zoomOverlay.whitelist", zoomOverlayWhitelist)
                .set("tooltip.zoomOverlay.blacklist", zoomOverlayBlacklist)
                .set("text.color", colorString(textColor))
                .set("text.titleColor", colorString(titleColor))
                .set("text.dividerAlpha", dividerAlpha)
                .set("layout.leftPadding", leftPadding)
                .set("layout.rightPadding", rightPadding)
                .set("layout.topPadding", topPadding)
                .set("layout.bottomPadding", bottomPadding)
                .set("layout.lineHeight", lineHeight)
                .set("layout.dividerTopMargin", dividerTopMargin)
                .set("layout.dividerBottomMargin", dividerBottomMargin)
                .set("layout.cursorOffset", cursorOffset)
                .set("layout.maxWidth", maxWidth)
                .set("tooltip.fillColors", colorStrings(fillColors))
                .set("tooltip.borderColors", colorStrings(borderColors));
        for (String id : PROFILE_IDS) {
            Profile profile = profile(id);
            config.set(profileKey(id, "textScale"), profile.textScale)
                    .set(profileKey(id, "offsetX"), profile.offsetX)
                    .set(profileKey(id, "offsetY"), profile.offsetY);
        }
        config.save();
    }

    static Snapshot snapshot() { return new Snapshot(); }

    private static void defineDefaults() {
        config.define("tooltip.enabled", true, "Replace Forge 1.12.2 tooltip layout and background.")
                .define("tooltip.style", "modernui", "Renderer style: modernui, mica or legacy.")
                .define("tooltip.lowBrightnessMicaEnhancement", false,
                        "Use alternate Mica capture and compositing for better legibility in low-brightness scenes.")
                .define("tooltip.micaSampleUi", false,
                        "Capture already-rendered GUI content behind Mica tooltips; false samples only the world and HUD.")
                .define("tooltip.yieldToLegendaryTooltips", true, "Prefer LegendaryTooltips panel colors and decorations while keeping NFR text layout.")
                .define("tooltip.legendaryOwnership", "auto", "Legendary resource ownership: auto, uie, legendary, or resource-pack.")
                .define("tooltip.heiCustomTooltips", true, "Apply NFR's panel and frame to HEI tooltips that contain custom-rendered ingredient grids.")
                .define("tooltip.quarkModernMapTooltip", false, "Replace Quark's parchment map preview with a compact NFR modern panel.")
                .define("tooltip.modName.enabled", true, "Append the owning mod's display name to item tooltips.")
                .define("tooltip.modName.moveToEnd", true, "Place the owning mod's display name after all other tooltip lines.")
                .define("tooltip.modName.format", "blue italic", "Space-separated TextFormatting friendly names; empty means unformatted.")
                .define("tooltip.advanced.enabled", true, "Generate advanced item information in the modern tooltip.")
                .define("tooltip.advanced.requireCtrl", true, "Require Ctrl to expand advanced information.")
                .define("tooltip.advanced.oreDictionary", true, "Show deduplicated Forge OreDictionary names.")
                .define("tooltip.advanced.registryName", true, "Show the registry name.")
                .define("tooltip.advanced.unlocalizedName", true, "Show unlocalized item names.")
                .define("tooltip.advanced.meta", true, "Show item metadata.")
                .define("tooltip.advanced.nbt", true, "Show NBT information.")
                .define("tooltip.advanced.nbtRequireShift", true, "Require Shift to reveal NBT contents.")
                .define("tooltip.advanced.nbtCharacterLimit", 0, "Maximum NBT characters; zero means unlimited.")
                .define("tooltip.rounded", true, "Draw rounded antialiased corners.")
                .define("tooltip.titleAlignment", "center",
                        "Title text alignment: left, center or right.")
                .define("tooltip.bodyAlignment", "left",
                        "Ordinary tooltip text alignment: left, center or right.")
                .define("tooltip.rarityAlignment", "left",
                        "Rarity row alignment: left, center or right.")
                .define("tooltip.preview.icon.alignment", "header",
                        "Title icon vertical alignment: header, title or first_line.")
                .define("tooltip.titleBreak", true, "Draw a divider after the title.")
                .define("tooltip.adaptiveBorder", true, "Derive border colors from formatted title colors, rarity and enchantment.")
                .define("tooltip.borderShading", "gradient", "Border shading: gradient, solid, horizontal, vertical or spectrum.")
                .define("tooltip.borderCycleMillis", 1000, "Milliseconds for a spectrum color to move to the next corner.")
                .define("tooltip.cornerRadius", 4.0D, "Corner radius in GUI pixels (0-16).")
                .define("tooltip.borderWidth", 1.25D, "Border width in GUI pixels (0.5-4).")
                .define("tooltip.shadowRadius", 4.0D, "Soft shadow radius in GUI pixels (0-12).")
                .define("tooltip.shadowAlpha", 72, "Maximum shadow alpha (0-255).")
                .define("tooltip.shadowOffsetX", 0.0D, "Horizontal shadow offset in GUI pixels.")
                .define("tooltip.shadowOffsetY", 2.0D, "Vertical shadow offset in GUI pixels.")
                .define("tooltip.shadowColor", "#FF000000", "ARGB shadow color.")
                .define("quality.shadowSteps", 12, "Number of continuous shadow gradient rings (2-32).")
                .define("quality.cornerSegments", 12, "Curve segments per rounded corner (3-32).")
                .define("quality.antialiasWidth", 0.55D, "Outer edge coverage width in GUI pixels.")
                .define("text.shadow", true, "Draw Minecraft's text shadow.")
                .define("tooltip.preview.icon.enabled", true, "Show the 2D item icon in the tooltip title header.")
                .define("tooltip.preview.icon.frame.enabled", true,
                        "Draw a rarity-colored frame around the item title icon.")
                .define("tooltip.preview.icon.frame.rarityColor", true,
                        "Use the item rarity color for the title icon frame.")
                .define("tooltip.preview.icon.frame.color", "#D0AAB4C4",
                        "Custom title icon frame color; its alpha also controls rarity-frame opacity.")
                .define("tooltip.preview.icon.background.enabled", true,
                        "Draw a background behind the item title icon.")
                .define("tooltip.preview.icon.background.color", "#80101018",
                        "Title icon background color.")
                .define("tooltip.preview.icon.rounded", true,
                        "Use rounded corners for the title icon frame and background.")
                .define("tooltip.preview.icon.cornerRadius", 3.0D,
                        "Title icon frame and background corner radius.")
                .define("tooltip.preview.icon.animation.enabled", true,
                        "Animate the item title icon when a tooltip first appears.")
                .define("tooltip.preview.icon.animation.durationMillis", 180,
                        "Item title icon appearance animation duration in milliseconds.")
                .define("tooltip.preview.rarity.enabled", true, "Show the item rarity label below the tooltip title.")
                .define("tooltip.preview.item.enabled", true, "Show rotating 3D item previews.")
                .define("tooltip.preview.item.scope", "tools", "Item preview scope: tools or all.")
                .define("tooltip.preview.armor.enabled", true, "Show 3D previews for armor items.")
                .define("tooltip.preview.animation.enabled", true, "Animate preview appearance with a short fade and scale.")
                .define("tooltip.preview.measureBounds", true, "Measure full-turn model bounds for tooltip layout. Special renderers are sampled once and cached.")
                .define("tooltip.preview.animation.durationMillis", 180, "Preview appearance animation duration in milliseconds.")
                .define("tooltip.preview.effects.enabled", true, "Draw decorative preview effects selected by JSON styles.")
                .define("tooltip.preview.effects.particles", true, "Allow particle-style preview effects.")
                .define("tooltip.preview.effects.speed", 1.0D, "Preview effect animation speed multiplier.")
                .define("tooltip.preview.effects.particleCount", 8, "Maximum particles emitted by a preview effect.")
                .define("tooltip.preview.sound.enabled", false, "Play a sound when a model preview first appears.")
                .define("tooltip.preview.sound.volume", 0.35D, "Preview appearance sound volume.")
                .define("tooltip.preview.sound.event", "ui.button.click", "Sound event played when a preview appears.")
                .define("tooltip.preview.sound.pitch", 1.0D, "Pitch of the preview appearance sound.")
                .define("tooltip.preview.sound.cooldownMillis", 250, "Minimum interval between preview appearance sounds.")
                .define("tooltip.preview.armor.model", "armor_stand", "Armor model: armor_stand or player.")
                .define("tooltip.preview.armor.mode", "single_piece", "Armor contents: single_piece or full_set.")
                .define("tooltip.preview.armor.stand.basePlate", false,
                        "Show the stone base plate under armor stand previews.")
                .define("tooltip.preview.armor.player.copyHands", false, "Copy the local player's held items to the player preview.")
                .define("tooltip.preview.armor.player.sneaking", false, "Render the player preview in a crouched pose.")
                .define("tooltip.preview.armor.player.pose", "idle", "Player preview pose: idle or swing.")
                .define("tooltip.preview.item.width", 30, "Width of an item model preview.")
                .define("tooltip.preview.armor.width", 40, "Width of an armor model preview.")
                .define("tooltip.preview.whitelist", Collections.emptyList(), "Item IDs or namespace:* rules allowed to show previews.")
                .define("tooltip.preview.blacklist", Collections.emptyList(), "Item IDs or namespace:* rules blocked from showing previews.")
                .define("tooltip.zoomOverlay.enabled", false, "Show an enlarged item beside an inventory when a stack is hovered.")
                .define("tooltip.zoomOverlay.scope", "tools", "Enlarged item scope: tools or all.")
                .define("tooltip.zoomOverlay.block.mode", "3d", "Category presentation: off, 2d or 3d.")
                .define("tooltip.zoomOverlay.tool.mode", "3d", "Category presentation: off, 2d or 3d.")
                .define("tooltip.zoomOverlay.equipment.mode", "3d", "Category presentation: off, 2d or 3d.")
                .define("tooltip.zoomOverlay.other.mode", "2d", "Category presentation: off, 2d or 3d.")
                .define("tooltip.zoomOverlay.equipment.model", "follow", "Equipment model: follow, player or armor_stand.")
                .define("tooltip.zoomOverlay.equipment.outfit", "follow", "Equipment outfit: follow, single_piece or full_set.")
                .define("tooltip.zoomOverlay.motion", "spin", "Motion when enabled: spin or sway; 2D icons only sway.")
                .define("tooltip.zoomOverlay.side", "auto", "Enlarged item side: auto, left or right.")
                .define("tooltip.zoomOverlay.layer", "below_tooltip", "Overlap order: below_tooltip keeps the tooltip on top; above_tooltip keeps the enlarged item on top.")
                .define("tooltip.zoomOverlay.size", 88, "Preview size in GUI pixels (32-512), reduced only to fit the available screen space.")
                .define("tooltip.zoomOverlay.gap", 8, "Gap between the enlarged item and the container.")
                .define("tooltip.zoomOverlay.animation.enabled", true, "Animate the enlarged item when its hovered stack changes.")
                .define("tooltip.zoomOverlay.measureBounds", true, "Fit enlarged 3D models using measured rotation bounds, including equipment layers.")
                .define("tooltip.zoomOverlay.animation.switch", "continue", "Rapid item switches: restart the appearance, continue its progress, or show the new item instantly.")
                .define("tooltip.zoomOverlay.animation.durationMillis", 180, "Enlarged item appearance duration in milliseconds.")
                .define("tooltip.zoomOverlay.rotation.enabled", true, "Rotate the enlarged item while it is visible.")
                .define("tooltip.zoomOverlay.rotation.speed", 18.0D, "Enlarged item rotation speed in degrees per second.")
                .define("tooltip.zoomOverlay.show.stackSize", false, "Show the stack count on the enlarged item.")
                .define("tooltip.zoomOverlay.show.durability", false, "Show the durability bar on the enlarged item.")
                .define("tooltip.zoomOverlay.show.cooldown", false, "Show the cooldown overlay on the enlarged item.")
                .define("tooltip.zoomOverlay.panel.enabled", false, "Draw a panel behind the enlarged item.")
                .define("tooltip.zoomOverlay.panel.border", true, "Draw a border around the enlarged item panel.")
                .define("tooltip.zoomOverlay.panel.inset", 5, "Panel inset around the enlarged item.")
                .define("tooltip.zoomOverlay.panel.background", "#C0101018", "ARGB enlarged item panel background color.")
                .define("tooltip.zoomOverlay.panel.borderColor", "#D0AAB4C4", "ARGB enlarged item panel border color.")
                .define("tooltip.zoomOverlay.whitelist", Collections.emptyList(), "Item IDs or namespace:* rules allowed for the enlarged overlay.")
                .define("tooltip.zoomOverlay.blacklist", Collections.emptyList(), "Item IDs or namespace:* rules blocked from the enlarged overlay.")
                .define("text.color", "#FFFFFFFF", "ARGB body text color.")
                .define("text.titleColor", "#FFFFFFFF", "ARGB title text color.")
                .define("text.dividerAlpha", 176, "Title divider alpha (0-255).")
                .define("layout.leftPadding", 5, "Left inset from the content bounds.")
                .define("layout.rightPadding", 5, "Right inset from the content bounds.")
                .define("layout.topPadding", 5, "Top inset from the visible content bounds.")
                .define("layout.bottomPadding", 5, "Bottom inset from the visible content bounds.")
                .define("layout.lineHeight", 10, "Distance between text baselines.")
                .define("layout.dividerTopMargin", 0, "Space between title text and the divider.")
                .define("layout.dividerBottomMargin", 3, "Space between the divider and body text.")
                .define("layout.cursorOffset", 12, "Distance from the mouse cursor.")
                .define("layout.maxWidth", 0, "Maximum text width; zero uses Forge/screen limits.")
                .define("tooltip.fillColors", colorStrings(DEFAULT_FILL), "Four ARGB colors: UL, UR, LR, LL.")
                .define("tooltip.borderColors", colorStrings(DEFAULT_BORDER), "Four ARGB colors: UL, UR, LR, LL.");
        for (String id : PROFILE_IDS) {
            config.define(profileKey(id, "textScale"), 1.0D,
                    "Tooltip text scale for the " + id + " renderer.")
                    .define(profileKey(id, "offsetX"), 0.0D,
                            "Tooltip text horizontal offset for the " + id + " renderer.")
                    .define(profileKey(id, "offsetY"), 0.0D,
                            "Tooltip text vertical baseline offset for the " + id + " renderer.");
        }
    }

    static Profile profile(String id) {
        String normalized = normalizeProfile(id);
        Profile profile = PROFILES.get(normalized);
        if (profile == null) {
            profile = new Profile();
            PROFILES.put(normalized, profile);
        }
        return profile;
    }

    static String normalizeProfile(String id) {
        if (id != null) {
            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (String allowed : PROFILE_IDS) if (allowed.equals(normalized)) return normalized;
        }
        return PROFILE_IDS[0];
    }

    private static String profileKey(String id, String field) {
        return "tooltip.profiles." + id + "." + field;
    }

    static final class Profile {
        float textScale = 1.0F;
        float offsetX;
        float offsetY;

        Profile copy() {
            Profile copy = new Profile();
            copy.textScale = textScale;
            copy.offsetX = offsetX;
            copy.offsetY = offsetY;
            return copy;
        }
    }

    private static int[] parseColors(List<String> values, int[] fallback) {
        if (values == null || values.size() != 4) return fallback.clone();
        int[] parsed = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                String value = values.get(i).trim();
                if (value.startsWith("#")) value = value.substring(1);
                long raw = Long.parseLong(value, 16);
                if (value.length() <= 6) raw |= 0xFF000000L;
                parsed[i] = (int) raw;
            }
            return parsed;
        } catch (RuntimeException ignored) {
            return fallback.clone();
        }
    }

    static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        try {
            String normalized = value.trim();
            if (normalized.startsWith("#")) normalized = normalized.substring(1);
            long raw = Long.parseLong(normalized, 16);
            if (normalized.length() <= 6) raw |= 0xFF000000L;
            return (int) raw;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static List<String> colorStrings(int[] colors) {
        String[] values = new String[colors.length];
        for (int i = 0; i < colors.length; i++) values[i] = String.format("#%08X", colors[i]);
        return Arrays.asList(values);
    }

    static String colorString(int color) { return String.format("#%08X", color); }

    static String normalizeStyle(String value) {
        if ("legacy".equalsIgnoreCase(value)) return "legacy";
        if ("mica".equalsIgnoreCase(value)) return "mica";
        return "modernui";
    }

    static String normalizeBorderShading(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("solid".equals(normalized) || "horizontal".equals(normalized)
                    || "vertical".equals(normalized) || "spectrum".equals(normalized)) {
                return normalized;
            }
        }
        return "gradient";
    }

    static String normalizeItemPreviewScope(String value) {
        return "all".equalsIgnoreCase(value == null ? "" : value.trim()) ? "all" : "tools";
    }

    static String normalizeZoomDisplayMode(String value, String fallback) {
        return normalizeChoice(value, fallback, "off", "2d", "3d");
    }

    static String normalizeZoomArmorModel(String value) {
        return normalizeChoice(value, "follow", "follow", "player", "armor_stand");
    }

    static String normalizeZoomArmorMode(String value) {
        return normalizeChoice(value, "follow", "follow", "single_piece", "full_set");
    }

    static String normalizeZoomMotion(String value) {
        return normalizeChoice(value, "spin", "spin", "sway");
    }

    static String normalizeZoomOverlayLayer(String value) {
        return normalizeChoice(value, "below_tooltip", "below_tooltip", "above_tooltip");
    }

    static String normalizeZoomAnimationSwitch(String value) {
        return normalizeChoice(value, "continue", "continue", "restart", "instant");
    }

    private static String normalizeChoice(String value, String fallback, String... choices) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (String choice : choices) if (choice.equals(normalized)) return choice;
        }
        return fallback;
    }

    static String normalizeZoomOverlaySide(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("left".equals(normalized) || "right".equals(normalized)
                    || "auto".equals(normalized)) return normalized;
        }
        return "auto";
    }

    static String normalizeTitleAlignment(String value) {
        return normalizeAlignment(value, "center");
    }

    static String normalizeAlignment(String value, String fallback) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("left".equals(normalized) || "right".equals(normalized)
                    || "center".equals(normalized)) return normalized;
        }
        return fallback;
    }

    static String normalizeHeaderIconAlignment(String value) {
        if (value != null) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if ("title".equals(normalized) || "first_line".equals(normalized)
                    || "header".equals(normalized)) return normalized;
        }
        return "header";
    }

    private static int[] defaults(int color) { return new int[]{color, color, color, color}; }

    static final class Snapshot {
        private final boolean originalEnabled = enabled;
        private final String originalRenderStyle = renderStyle;
        private final boolean originalLowBrightnessMicaEnhancement = lowBrightnessMicaEnhancement;
        private final boolean originalMicaSampleUi = micaSampleUi;
        private final boolean originalYield = yieldToLegendaryTooltips;
        private final String originalLegendaryOwnership = legendaryOwnership;
        private final boolean originalHeiCustomTooltips = heiCustomTooltips;
        private final boolean originalQuarkModernMapTooltip = quarkModernMapTooltip;
        private final boolean originalModNameEnabled = modNameEnabled;
        private final boolean originalModNameMoveToEnd = modNameMoveToEnd;
        private final String originalModNameFormat = modNameFormat;
        private final boolean originalAdvancedEnabled = advancedEnabled;
        private final boolean originalAdvancedRequireCtrl = advancedRequireCtrl;
        private final boolean originalAdvancedOreDictionary = advancedOreDictionary;
        private final boolean originalAdvancedRegistryName = advancedRegistryName;
        private final boolean originalAdvancedUnlocalizedName = advancedUnlocalizedName;
        private final boolean originalAdvancedMeta = advancedMeta;
        private final boolean originalAdvancedNbt = advancedNbt;
        private final boolean originalAdvancedNbtRequireShift = advancedNbtRequireShift;
        private final int originalAdvancedNbtCharacterLimit = advancedNbtCharacterLimit;
        private final boolean originalRounded = rounded;
        private final String originalTitleAlignment = titleAlignment;
        private final String originalBodyAlignment = bodyAlignment;
        private final String originalRarityAlignment = rarityAlignment;
        private final String originalHeaderIconAlignment = headerIconAlignment;
        private final boolean originalTitleBreak = titleBreak;
        private final boolean originalAdaptive = adaptiveBorder;
        private final String originalBorderShading = borderShading;
        private final int originalBorderCycleMillis = borderCycleMillis;
        private final float originalCorner = cornerRadius;
        private final float originalBorder = borderWidth;
        private final float originalShadow = shadowRadius;
        private final int originalAlpha = shadowAlpha;
        private final float originalShadowX = shadowOffsetX;
        private final float originalShadowY = shadowOffsetY;
        private final int originalShadowColor = shadowColor;
        private final int originalShadowSteps = shadowSteps;
        private final int originalCornerSegments = cornerSegments;
        private final float originalAa = antialiasWidth;
        private final boolean originalTextShadow = textShadow;
        private final boolean originalHeaderIconEnabled = headerIconEnabled;
        private final boolean originalHeaderIconFrameEnabled = headerIconFrameEnabled;
        private final boolean originalHeaderIconFrameRarityColor = headerIconFrameRarityColor;
        private final int originalHeaderIconFrameColor = headerIconFrameColor;
        private final boolean originalHeaderIconBackgroundEnabled = headerIconBackgroundEnabled;
        private final int originalHeaderIconBackgroundColor = headerIconBackgroundColor;
        private final boolean originalHeaderIconRounded = headerIconRounded;
        private final float originalHeaderIconCornerRadius = headerIconCornerRadius;
        private final boolean originalHeaderIconAnimationEnabled = headerIconAnimationEnabled;
        private final int originalHeaderIconAnimationMillis = headerIconAnimationMillis;
        private final boolean originalRarityEnabled = rarityEnabled;
        private final boolean originalItemPreviewEnabled = itemPreviewEnabled;
        private final String originalItemPreviewScope = itemPreviewScope;
        private final boolean originalArmorPreviewEnabled = armorPreviewEnabled;
        private final int originalItemPreviewWidth = itemPreviewWidth;
        private final int originalArmorPreviewWidth = armorPreviewWidth;
        private final List<String> originalPreviewWhitelist = new java.util.ArrayList<>(previewWhitelist);
        private final List<String> originalPreviewBlacklist = new java.util.ArrayList<>(previewBlacklist);
        private final boolean originalZoomOverlayEnabled = zoomOverlayEnabled;
        private final String originalZoomOverlayScope = zoomOverlayScope;
        private final String originalZoomOverlayBlockMode = zoomOverlayBlockMode;
        private final String originalZoomOverlayToolMode = zoomOverlayToolMode;
        private final String originalZoomOverlayEquipmentMode = zoomOverlayEquipmentMode;
        private final String originalZoomOverlayOtherMode = zoomOverlayOtherMode;
        private final String originalZoomOverlayArmorModel = zoomOverlayArmorModel;
        private final String originalZoomOverlayArmorMode = zoomOverlayArmorMode;
        private final String originalZoomOverlayMotion = zoomOverlayMotion;
        private final String originalZoomOverlaySide = zoomOverlaySide;
        private final String originalZoomOverlayLayer = zoomOverlayLayer;
        private final int originalZoomOverlaySize = zoomOverlaySize;
        private final int originalZoomOverlayGap = zoomOverlayGap;
        private final boolean originalZoomOverlayAnimation = zoomOverlayAnimation;
        private final boolean originalZoomOverlayMeasureBounds = zoomOverlayMeasureBounds;
        private final String originalZoomOverlayAnimationSwitch = zoomOverlayAnimationSwitch;
        private final int originalZoomOverlayAnimationMillis = zoomOverlayAnimationMillis;
        private final boolean originalZoomOverlayRotation = zoomOverlayRotation;
        private final float originalZoomOverlayRotationSpeed = zoomOverlayRotationSpeed;
        private final boolean originalZoomOverlayStackSize = zoomOverlayShowStackSize;
        private final boolean originalZoomOverlayDurability = zoomOverlayShowDurability;
        private final boolean originalZoomOverlayCooldown = zoomOverlayShowCooldown;
        private final boolean originalZoomOverlayPanel = zoomOverlayPanel;
        private final boolean originalZoomOverlayBorder = zoomOverlayBorder;
        private final int originalZoomOverlayInset = zoomOverlayPanelInset;
        private final int originalZoomOverlayBackground = zoomOverlayBackgroundColor;
        private final int originalZoomOverlayBorderColor = zoomOverlayBorderColor;
        private final List<String> originalZoomOverlayWhitelist = new java.util.ArrayList<>(zoomOverlayWhitelist);
        private final List<String> originalZoomOverlayBlacklist = new java.util.ArrayList<>(zoomOverlayBlacklist);
        private final boolean originalPreviewAnimation = previewAnimationEnabled;
        private final boolean originalPreviewMeasureBounds = previewMeasureBounds;
        private final int originalPreviewAnimationMillis = previewAnimationMillis;
        private final boolean originalPreviewEffects = previewEffectsEnabled;
        private final boolean originalPreviewParticles = previewParticlesEnabled;
        private final float originalPreviewEffectSpeed = previewEffectSpeed;
        private final int originalPreviewParticleCount = previewParticleCount;
        private final boolean originalPreviewSound = previewSoundEnabled;
        private final float originalPreviewSoundVolume = previewSoundVolume;
        private final String originalPreviewSoundEvent = previewSoundEvent;
        private final float originalPreviewSoundPitch = previewSoundPitch;
        private final int originalPreviewSoundCooldownMillis = previewSoundCooldownMillis;
        private final String originalArmorPreviewModel = armorPreviewModel;
        private final String originalArmorPreviewMode = armorPreviewMode;
        private final boolean originalArmorStandBasePlate = armorStandBasePlate;
        private final boolean originalArmorPlayerCopyHands = armorPlayerCopyHands;
        private final boolean originalArmorPlayerSneaking = armorPlayerSneaking;
        private final String originalArmorPlayerPose = armorPlayerPose;
        private final int originalTextColor = textColor;
        private final int originalTitleColor = titleColor;
        private final int originalDividerAlpha = dividerAlpha;
        private final int originalLeftPadding = leftPadding;
        private final int originalRightPadding = rightPadding;
        private final int originalTopPadding = topPadding;
        private final int originalBottomPadding = bottomPadding;
        private final int originalLineHeight = lineHeight;
        private final int originalDividerTopMargin = dividerTopMargin;
        private final int originalDividerBottomMargin = dividerBottomMargin;
        private final int originalCursorOffset = cursorOffset;
        private final int originalMaxWidth = maxWidth;
        private final int[] originalFill = fillColors.clone();
        private final int[] originalBorderColors = borderColors.clone();
        private final Map<String, Profile> originalProfiles = copyProfiles();

        void restore() {
            enabled = originalEnabled;
            renderStyle = originalRenderStyle;
            lowBrightnessMicaEnhancement = originalLowBrightnessMicaEnhancement;
            micaSampleUi = originalMicaSampleUi;
            yieldToLegendaryTooltips = originalYield; rounded = originalRounded;
            legendaryOwnership = originalLegendaryOwnership;
            heiCustomTooltips = originalHeiCustomTooltips;
            quarkModernMapTooltip = originalQuarkModernMapTooltip;
            modNameEnabled = originalModNameEnabled; modNameMoveToEnd = originalModNameMoveToEnd;
            modNameFormat = originalModNameFormat;
            advancedEnabled = originalAdvancedEnabled;
            advancedRequireCtrl = originalAdvancedRequireCtrl;
            advancedOreDictionary = originalAdvancedOreDictionary;
            advancedRegistryName = originalAdvancedRegistryName;
            advancedUnlocalizedName = originalAdvancedUnlocalizedName;
            advancedMeta = originalAdvancedMeta;
            advancedNbt = originalAdvancedNbt;
            advancedNbtRequireShift = originalAdvancedNbtRequireShift;
            advancedNbtCharacterLimit = originalAdvancedNbtCharacterLimit;
            titleBreak = originalTitleBreak; adaptiveBorder = originalAdaptive;
            titleAlignment = originalTitleAlignment;
            bodyAlignment = originalBodyAlignment;
            rarityAlignment = originalRarityAlignment;
            headerIconAlignment = originalHeaderIconAlignment;
            borderShading = originalBorderShading; borderCycleMillis = originalBorderCycleMillis;
            cornerRadius = originalCorner; borderWidth = originalBorder; shadowRadius = originalShadow;
            shadowAlpha = originalAlpha;
            shadowOffsetX = originalShadowX; shadowOffsetY = originalShadowY; shadowColor = originalShadowColor;
            shadowSteps = originalShadowSteps; cornerSegments = originalCornerSegments; antialiasWidth = originalAa;
            textShadow = originalTextShadow; headerIconEnabled = originalHeaderIconEnabled;
            headerIconFrameEnabled = originalHeaderIconFrameEnabled;
            headerIconFrameRarityColor = originalHeaderIconFrameRarityColor;
            headerIconFrameColor = originalHeaderIconFrameColor;
            headerIconBackgroundEnabled = originalHeaderIconBackgroundEnabled;
            headerIconBackgroundColor = originalHeaderIconBackgroundColor;
            headerIconRounded = originalHeaderIconRounded;
            headerIconCornerRadius = originalHeaderIconCornerRadius;
            headerIconAnimationEnabled = originalHeaderIconAnimationEnabled;
            headerIconAnimationMillis = originalHeaderIconAnimationMillis;
            rarityEnabled = originalRarityEnabled;
            textColor = originalTextColor; titleColor = originalTitleColor;
            itemPreviewEnabled = originalItemPreviewEnabled;
            itemPreviewScope = originalItemPreviewScope;
            armorPreviewEnabled = originalArmorPreviewEnabled;
            itemPreviewWidth = originalItemPreviewWidth;
            armorPreviewWidth = originalArmorPreviewWidth;
            previewWhitelist = Collections.unmodifiableList(new java.util.ArrayList<>(originalPreviewWhitelist));
            previewBlacklist = Collections.unmodifiableList(new java.util.ArrayList<>(originalPreviewBlacklist));
            zoomOverlayEnabled = originalZoomOverlayEnabled;
            zoomOverlayScope = originalZoomOverlayScope;
            zoomOverlayBlockMode = originalZoomOverlayBlockMode;
            zoomOverlayToolMode = originalZoomOverlayToolMode;
            zoomOverlayEquipmentMode = originalZoomOverlayEquipmentMode;
            zoomOverlayOtherMode = originalZoomOverlayOtherMode;
            zoomOverlayArmorModel = originalZoomOverlayArmorModel;
            zoomOverlayArmorMode = originalZoomOverlayArmorMode;
            zoomOverlayMotion = originalZoomOverlayMotion;
            zoomOverlaySide = originalZoomOverlaySide;
            zoomOverlayLayer = originalZoomOverlayLayer;
            zoomOverlaySize = originalZoomOverlaySize;
            zoomOverlayGap = originalZoomOverlayGap;
            zoomOverlayAnimation = originalZoomOverlayAnimation;
            zoomOverlayMeasureBounds = originalZoomOverlayMeasureBounds;
            zoomOverlayAnimationSwitch = originalZoomOverlayAnimationSwitch;
            zoomOverlayAnimationMillis = originalZoomOverlayAnimationMillis;
            zoomOverlayRotation = originalZoomOverlayRotation;
            zoomOverlayRotationSpeed = originalZoomOverlayRotationSpeed;
            zoomOverlayShowStackSize = originalZoomOverlayStackSize;
            zoomOverlayShowDurability = originalZoomOverlayDurability;
            zoomOverlayShowCooldown = originalZoomOverlayCooldown;
            zoomOverlayPanel = originalZoomOverlayPanel;
            zoomOverlayBorder = originalZoomOverlayBorder;
            zoomOverlayPanelInset = originalZoomOverlayInset;
            zoomOverlayBackgroundColor = originalZoomOverlayBackground;
            zoomOverlayBorderColor = originalZoomOverlayBorderColor;
            zoomOverlayWhitelist = Collections.unmodifiableList(new java.util.ArrayList<>(originalZoomOverlayWhitelist));
            zoomOverlayBlacklist = Collections.unmodifiableList(new java.util.ArrayList<>(originalZoomOverlayBlacklist));
            previewAnimationEnabled = originalPreviewAnimation;
            previewMeasureBounds = originalPreviewMeasureBounds;
            previewAnimationMillis = originalPreviewAnimationMillis;
            previewEffectsEnabled = originalPreviewEffects;
            previewParticlesEnabled = originalPreviewParticles;
            previewEffectSpeed = originalPreviewEffectSpeed;
            previewParticleCount = originalPreviewParticleCount;
            previewSoundEnabled = originalPreviewSound;
            previewSoundVolume = originalPreviewSoundVolume;
            previewSoundEvent = originalPreviewSoundEvent;
            previewSoundPitch = originalPreviewSoundPitch;
            previewSoundCooldownMillis = originalPreviewSoundCooldownMillis;
            armorPreviewModel = originalArmorPreviewModel;
            armorPreviewMode = originalArmorPreviewMode;
            armorStandBasePlate = originalArmorStandBasePlate;
            armorPlayerCopyHands = originalArmorPlayerCopyHands;
            armorPlayerSneaking = originalArmorPlayerSneaking;
            armorPlayerPose = originalArmorPlayerPose;
            dividerAlpha = originalDividerAlpha; leftPadding = originalLeftPadding;
            rightPadding = originalRightPadding;
            topPadding = originalTopPadding;
            bottomPadding = originalBottomPadding;
            lineHeight = originalLineHeight;
            dividerTopMargin = originalDividerTopMargin;
            dividerBottomMargin = originalDividerBottomMargin;
            cursorOffset = originalCursorOffset; maxWidth = originalMaxWidth;
            fillColors = originalFill.clone(); borderColors = originalBorderColors.clone();
            PROFILES.clear();
            for (Map.Entry<String, Profile> entry : originalProfiles.entrySet()) {
                PROFILES.put(entry.getKey(), entry.getValue().copy());
            }
        }

        private static Map<String, Profile> copyProfiles() {
            Map<String, Profile> copy = new LinkedHashMap<>();
            for (String id : PROFILE_IDS) copy.put(id, profile(id).copy());
            return copy;
        }
    }

    static String normalizeOwnership(String value) {
        String normalized = value == null ? "auto" : value.trim().toLowerCase(Locale.ROOT);
        return Arrays.asList("auto", "uie", "legendary", "resource-pack").contains(normalized) ? normalized : "auto";
    }
}
