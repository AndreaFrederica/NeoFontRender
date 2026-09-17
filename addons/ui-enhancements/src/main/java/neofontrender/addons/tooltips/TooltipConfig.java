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
    static boolean suppressImmersiveEngineering = true;
    static boolean suppressEnderCore = true;
    static boolean rounded = true;
    static boolean centerTitle = true;
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
    static int textColor = 0xFFFFFFFF;
    static int titleColor = 0xFFFFFFFF;
    static int dividerAlpha = 176;
    static int horizontalPadding = 5;
    static int verticalPadding = 5;
    static int lineHeight = 10;
    static int titleGap = 3;
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
        suppressImmersiveEngineering = config.getBoolean("tooltip.suppress.immersiveEngineering", true);
        suppressEnderCore = config.getBoolean("tooltip.suppress.enderCore", true);
        rounded = config.getBoolean("tooltip.rounded", true);
        centerTitle = config.getBoolean("tooltip.centerTitle", true);
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
        textColor = parseColor(config.getString("text.color", "#FFFFFFFF"), 0xFFFFFFFF);
        titleColor = parseColor(config.getString("text.titleColor", "#FFFFFFFF"), 0xFFFFFFFF);
        dividerAlpha = config.getInt("text.dividerAlpha", 176, 0, 255);
        horizontalPadding = config.getInt("layout.horizontalPadding", 5, 1, 24);
        verticalPadding = config.getInt("layout.verticalPadding", 5, 1, 24);
        lineHeight = config.getInt("layout.lineHeight", 10, 8, 24);
        titleGap = config.getInt("layout.titleGap", 3, 0, 16);
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
                .set("tooltip.suppress.immersiveEngineering", suppressImmersiveEngineering)
                .set("tooltip.suppress.enderCore", suppressEnderCore)
                .set("tooltip.rounded", rounded)
                .set("tooltip.centerTitle", centerTitle)
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
                .set("text.color", colorString(textColor))
                .set("text.titleColor", colorString(titleColor))
                .set("text.dividerAlpha", dividerAlpha)
                .set("layout.horizontalPadding", horizontalPadding)
                .set("layout.verticalPadding", verticalPadding)
                .set("layout.lineHeight", lineHeight)
                .set("layout.titleGap", titleGap)
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
                .define("tooltip.suppress.immersiveEngineering", true, "Suppress Immersive Engineering ore tooltip lines.")
                .define("tooltip.suppress.enderCore", true, "Suppress EnderCore ore tooltip lines.")
                .define("tooltip.rounded", true, "Draw rounded antialiased corners.")
                .define("tooltip.centerTitle", true, "Center the first tooltip line.")
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
                .define("text.color", "#FFFFFFFF", "ARGB body text color.")
                .define("text.titleColor", "#FFFFFFFF", "ARGB title text color.")
                .define("text.dividerAlpha", 176, "Title divider alpha (0-255).")
                .define("layout.horizontalPadding", 5, "Horizontal background padding.")
                .define("layout.verticalPadding", 5, "Vertical background padding.")
                .define("layout.lineHeight", 10, "Distance between text baselines.")
                .define("layout.titleGap", 3, "Extra gap after the title.")
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

    private static int[] defaults(int color) { return new int[]{color, color, color, color}; }

    static final class Snapshot {
        private final boolean originalEnabled = enabled;
        private final String originalRenderStyle = renderStyle;
        private final boolean originalLowBrightnessMicaEnhancement = lowBrightnessMicaEnhancement;
        private final boolean originalMicaSampleUi = micaSampleUi;
        private final boolean originalYield = yieldToLegendaryTooltips;
        private final boolean originalHeiCustomTooltips = heiCustomTooltips;
        private final boolean originalQuarkModernMapTooltip = quarkModernMapTooltip;
        private final boolean originalModNameEnabled = modNameEnabled;
        private final boolean originalModNameMoveToEnd = modNameMoveToEnd;
        private final String originalModNameFormat = modNameFormat;
        private final boolean originalRounded = rounded;
        private final boolean originalCenterTitle = centerTitle;
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
        private final boolean originalPreviewAnimation = previewAnimationEnabled;
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
        private final int originalHorizontalPadding = horizontalPadding;
        private final int originalVerticalPadding = verticalPadding;
        private final int originalLineHeight = lineHeight;
        private final int originalTitleGap = titleGap;
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
            heiCustomTooltips = originalHeiCustomTooltips;
            quarkModernMapTooltip = originalQuarkModernMapTooltip;
            modNameEnabled = originalModNameEnabled; modNameMoveToEnd = originalModNameMoveToEnd;
            modNameFormat = originalModNameFormat;
            centerTitle = originalCenterTitle; titleBreak = originalTitleBreak; adaptiveBorder = originalAdaptive;
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
            previewAnimationEnabled = originalPreviewAnimation;
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
            dividerAlpha = originalDividerAlpha; horizontalPadding = originalHorizontalPadding;
            verticalPadding = originalVerticalPadding; lineHeight = originalLineHeight; titleGap = originalTitleGap;
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
