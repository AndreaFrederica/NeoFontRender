package neofontrender.addons.tooltips;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.core.config.NeofontrenderConfig;
import net.minecraftforge.fml.common.Loader;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class ModernTooltipSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":tooltips"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.tooltips.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final TooltipConfig.Snapshot original = TooltipConfig.snapshot();
        private String profileId = "vanilla";
        private String previewItemId = "minecraft:planks";

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            List<String> availableProfiles = availableProfileIds();
            if (!availableProfiles.contains(profileId)) profileId = "vanilla";
            NfrOptionsGrid profileSelector = c.grid()
                    .add(c.dropdownText("tooltip_profile", () -> tr("gui.profile"),
                            () -> profileId,
                            value -> {
                                profileId = TooltipConfig.normalizeProfile(value);
                                context.refresh();
                            },
                            availableProfiles,
                            value -> tr("gui.profile." + value)).size(260, 24));
            if ("vanilla".equals(profileId)) {
                profileSelector.add(new NfrLabeledTextField(tr("gui.preview_item"),
                        new TextFieldWidget().setMaxLength(256).autoUpdateOnChange(true)
                                .value(new NfrStringValue(() -> previewItemId,
                                        value -> previewItemId = value))));
            }
            NfrOptionsGrid typography = c.grid()
                    .add(c.decimalSlider(() -> tr("gui.text_scale"),
                            () -> TooltipConfig.profile(profileId).textScale,
                            value -> TooltipConfig.profile(profileId).textScale = value,
                            0.5F, 2.0F, 0.05F))
                    .add(c.decimalSlider(() -> tr("gui.text_offset_x"),
                            () -> TooltipConfig.profile(profileId).offsetX,
                            value -> TooltipConfig.profile(profileId).offsetX = value,
                            -12.0F, 12.0F, 0.1F))
                    .add(c.decimalSlider(() -> tr("gui.text_offset_y"),
                            () -> TooltipConfig.profile(profileId).offsetY,
                            value -> TooltipConfig.profile(profileId).offsetY = value,
                            -12.0F, 12.0F, 0.1F));
            NfrOptionsGrid grid = c.grid()
                    .add(c.toggleText(() -> tr("gui.enabled"), () -> tr("tooltip.enabled"),
                            () -> TooltipConfig.enabled, value -> TooltipConfig.enabled = value))
                    .add(c.toggleText(() -> tr("gui.advanced"), () -> tr("tooltip.advanced.enabled"),
                            () -> TooltipConfig.advancedEnabled, value -> TooltipConfig.advancedEnabled = value))
                    .add(c.toggleText(() -> tr("gui.advanced_ctrl"), () -> tr("tooltip.advanced.requireCtrl"),
                            () -> TooltipConfig.advancedRequireCtrl, value -> TooltipConfig.advancedRequireCtrl = value))
                    .add(c.toggleText(() -> tr("gui.advanced_oredict"), () -> tr("tooltip.advanced.oreDictionary"),
                            () -> TooltipConfig.advancedOreDictionary, value -> TooltipConfig.advancedOreDictionary = value))
                    .add(c.toggleText(() -> tr("gui.advanced_registry"), () -> tr("tooltip.advanced.registryName"),
                            () -> TooltipConfig.advancedRegistryName, value -> TooltipConfig.advancedRegistryName = value))
                    .add(c.toggleText(() -> tr("gui.advanced_nbt"), () -> tr("tooltip.advanced.nbt"),
                            () -> TooltipConfig.advancedNbt, value -> TooltipConfig.advancedNbt = value))
                    .add(c.dropdownText("tooltip_style", () -> tr("gui.style"),
                            () -> TooltipConfig.renderStyle,
                            value -> TooltipConfig.renderStyle = TooltipConfig.normalizeStyle(value),
                            Arrays.asList("modernui", "mica", "legacy"),
                            value -> tr("gui.style." + value)).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.low_brightness_mica_enhancement"),
                            () -> tr("tooltip.low_brightness_mica_enhancement"),
                            () -> TooltipConfig.lowBrightnessMicaEnhancement,
                            value -> TooltipConfig.lowBrightnessMicaEnhancement = value))
                    .add(c.toggleText(() -> tr("gui.mica_sample_ui"),
                            () -> tr("tooltip.mica_sample_ui"),
                            () -> TooltipConfig.micaSampleUi,
                            value -> TooltipConfig.micaSampleUi = value))
                    .add(c.toggleText(() -> tr("gui.legendary"), () -> tr("tooltip.legendary"),
                            () -> TooltipConfig.yieldToLegendaryTooltips, value -> TooltipConfig.yieldToLegendaryTooltips = value))
                    .add(c.dropdownText("tooltip_legendary_ownership", () -> tr("gui.legendary_ownership"),
                            () -> TooltipConfig.legendaryOwnership,
                            value -> TooltipConfig.legendaryOwnership = TooltipConfig.normalizeOwnership(value),
                            Arrays.asList("auto", "uie", "legendary", "resource-pack"), value -> value).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.hei_custom"), () -> tr("tooltip.hei_custom"),
                            () -> TooltipConfig.heiCustomTooltips,
                            value -> TooltipConfig.heiCustomTooltips = value,
                            context::refresh))
                    .add(c.toggleText(() -> tr("gui.quark_map"), () -> tr("tooltip.quark_map"),
                            () -> TooltipConfig.quarkModernMapTooltip,
                            value -> TooltipConfig.quarkModernMapTooltip = value,
                            context::refresh))
                    .add(c.toggleText(() -> tr("gui.mod_name"), () -> tr("tooltip.mod_name"),
                            () -> TooltipConfig.modNameEnabled, value -> TooltipConfig.modNameEnabled = value))
                    .add(c.toggleText(() -> tr("gui.mod_name_move_to_end"), () -> tr("tooltip.mod_name_move_to_end"),
                            () -> TooltipConfig.modNameMoveToEnd, value -> TooltipConfig.modNameMoveToEnd = value))
                    .add(c.dropdownText("tooltip_mod_name_format", () -> tr("gui.mod_name_format"),
                            () -> TooltipConfig.modNameFormat,
                            value -> TooltipConfig.modNameFormat = value,
                            Arrays.asList("", "blue italic", "gray italic", "dark_gray italic",
                                    "aqua italic", "gold italic", "blue", "gray"),
                            ModernTooltipSettingsPage::modNameFormatLabel).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.rounded"), () -> "",
                            () -> TooltipConfig.rounded, value -> TooltipConfig.rounded = value))
                    .add(c.dropdownText("tooltip_title_alignment", () -> tr("gui.title_alignment"),
                            () -> TooltipConfig.titleAlignment,
                            value -> {
                                TooltipConfig.titleAlignment = TooltipConfig.normalizeTitleAlignment(value);
                                TooltipConfig.centerTitle = "center".equals(TooltipConfig.titleAlignment);
                            },
                            Arrays.asList("left", "center", "right"),
                            value -> tr("gui.title_alignment." + value)).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.title_break"), () -> "",
                            () -> TooltipConfig.titleBreak, value -> TooltipConfig.titleBreak = value))
                    .add(c.toggleText(() -> tr("gui.adaptive_border"), () -> "",
                            () -> TooltipConfig.adaptiveBorder, value -> TooltipConfig.adaptiveBorder = value))
                    .add(c.dropdownText("tooltip_border_shading", () -> tr("gui.border_shading"),
                            () -> TooltipConfig.borderShading,
                            value -> TooltipConfig.borderShading = TooltipConfig.normalizeBorderShading(value),
                            Arrays.asList("gradient", "solid", "horizontal", "vertical", "spectrum"),
                            value -> tr("gui.border_shading." + value)).size(260, 24))
                    .add(c.dropdownText("tooltip_border_cycle", () -> tr("gui.border_cycle"),
                            () -> Integer.toString(TooltipConfig.borderCycleMillis),
                            value -> TooltipConfig.borderCycleMillis = Integer.parseInt(value),
                            Arrays.asList("250", "500", "1000", "2000", "4000", "8000"),
                            value -> value + " ms").size(260, 24))
                    .add(c.dropdownText("tooltip_corner_radius", () -> tr("gui.corner_radius"),
                            () -> number(TooltipConfig.cornerRadius), value -> TooltipConfig.cornerRadius = Float.parseFloat(value),
                            Arrays.asList("0", "2", "4", "6", "8", "12", "16"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_border_width", () -> tr("gui.border_width"),
                            () -> number(TooltipConfig.borderWidth), value -> TooltipConfig.borderWidth = Float.parseFloat(value),
                            Arrays.asList("0.5", "1", "1.25", "1.5", "2", "3", "4"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_radius", () -> tr("gui.shadow_radius"),
                            () -> number(TooltipConfig.shadowRadius), value -> TooltipConfig.shadowRadius = Float.parseFloat(value),
                            Arrays.asList("0", "2", "4", "6", "8", "10", "12"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_alpha", () -> tr("gui.shadow_alpha"),
                            () -> Integer.toString(TooltipConfig.shadowAlpha), value -> TooltipConfig.shadowAlpha = Integer.parseInt(value),
                            Arrays.asList("0", "32", "48", "72", "96", "128", "160", "192"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_x", () -> tr("gui.shadow_x"),
                            () -> number(TooltipConfig.shadowOffsetX), value -> TooltipConfig.shadowOffsetX = Float.parseFloat(value),
                            Arrays.asList("-4", "-2", "-1", "0", "1", "2", "4"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_y", () -> tr("gui.shadow_y"),
                            () -> number(TooltipConfig.shadowOffsetY), value -> TooltipConfig.shadowOffsetY = Float.parseFloat(value),
                            Arrays.asList("-4", "-2", "-1", "0", "1", "2", "4"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_shadow_steps", () -> tr("gui.shadow_steps"),
                            () -> Integer.toString(TooltipConfig.shadowSteps), value -> TooltipConfig.shadowSteps = Integer.parseInt(value),
                            Arrays.asList("4", "6", "8", "12", "16", "24", "32"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_corner_segments", () -> tr("gui.corner_segments"),
                            () -> Integer.toString(TooltipConfig.cornerSegments), value -> TooltipConfig.cornerSegments = Integer.parseInt(value),
                            Arrays.asList("4", "6", "8", "12", "16", "24", "32"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_aa_width", () -> tr("gui.aa_width"),
                            () -> number(TooltipConfig.antialiasWidth), value -> TooltipConfig.antialiasWidth = Float.parseFloat(value),
                            Arrays.asList("0", "0.35", "0.55", "0.75", "1", "1.5"), value -> value).size(260, 24))
                     .add(c.toggleText(() -> tr("gui.text_shadow"), () -> "",
                             () -> TooltipConfig.textShadow, value -> TooltipConfig.textShadow = value))
                     .add(c.toggleText(() -> tr("gui.preview_header_icon"), () -> tr("tooltip.preview.icon.enabled"),
                             () -> TooltipConfig.headerIconEnabled,
                             value -> TooltipConfig.headerIconEnabled = value))
                     .add(c.dropdownText("tooltip_preview_header_icon_alignment",
                             () -> tr("gui.preview_header_icon_alignment"),
                             () -> TooltipConfig.headerIconAlignment,
                             value -> TooltipConfig.headerIconAlignment =
                                     TooltipConfig.normalizeHeaderIconAlignment(value),
                             Arrays.asList("header", "title", "first_line"),
                             value -> tr("gui.preview_header_icon_alignment." + value))
                             .size(260, 24))
                     .add(c.toggleText(() -> tr("gui.preview_header_icon_frame"),
                             () -> tr("tooltip.preview.icon.frame.enabled"),
                             () -> TooltipConfig.headerIconFrameEnabled,
                             value -> TooltipConfig.headerIconFrameEnabled = value))
                     .add(c.toggleText(() -> tr("gui.preview_header_icon_frame_rarity"),
                             () -> tr("tooltip.preview.icon.frame.rarityColor"),
                             () -> TooltipConfig.headerIconFrameRarityColor,
                             value -> TooltipConfig.headerIconFrameRarityColor = value))
                     .add(colorPicker(c, "tooltip_preview_header_icon_frame_color",
                             "gui.preview_header_icon_frame_color",
                             () -> TooltipConfig.headerIconFrameColor,
                             value -> TooltipConfig.headerIconFrameColor = value))
                     .add(c.toggleText(() -> tr("gui.preview_header_icon_background"),
                             () -> tr("tooltip.preview.icon.background.enabled"),
                             () -> TooltipConfig.headerIconBackgroundEnabled,
                             value -> TooltipConfig.headerIconBackgroundEnabled = value))
                     .add(colorPicker(c, "tooltip_preview_header_icon_background_color",
                             "gui.preview_header_icon_background_color",
                             () -> TooltipConfig.headerIconBackgroundColor,
                             value -> TooltipConfig.headerIconBackgroundColor = value))
                     .add(c.toggleText(() -> tr("gui.preview_header_icon_rounded"),
                             () -> tr("tooltip.preview.icon.rounded"),
                             () -> TooltipConfig.headerIconRounded,
                             value -> TooltipConfig.headerIconRounded = value))
                     .add(c.dropdownText("tooltip_preview_header_icon_corner_radius",
                             () -> tr("gui.preview_header_icon_corner_radius"),
                             () -> number(TooltipConfig.headerIconCornerRadius),
                             value -> TooltipConfig.headerIconCornerRadius = Float.parseFloat(value),
                             Arrays.asList("0", "1", "2", "3", "4", "5", "6", "8", "9"),
                             value -> value + " px").size(260, 24))
                     .add(c.toggleText(() -> tr("gui.preview_header_icon_animation"),
                             () -> tr("tooltip.preview.icon.animation.enabled"),
                             () -> TooltipConfig.headerIconAnimationEnabled,
                             value -> TooltipConfig.headerIconAnimationEnabled = value))
                     .add(c.dropdownText("tooltip_preview_header_icon_animation_duration",
                             () -> tr("gui.preview_header_icon_animation_duration"),
                             () -> Integer.toString(TooltipConfig.headerIconAnimationMillis),
                             value -> TooltipConfig.headerIconAnimationMillis = Integer.parseInt(value),
                             Arrays.asList("0", "90", "120", "180", "240", "360", "500"),
                             value -> value + " ms").size(260, 24))
                     .add(c.toggleText(() -> tr("gui.preview_rarity"), () -> tr("tooltip.preview.rarity.enabled"),
                             () -> TooltipConfig.rarityEnabled,
                             value -> TooltipConfig.rarityEnabled = value))
                     .add(c.toggleText(() -> tr("gui.preview_item_enabled"), () -> tr("tooltip.preview.item.enabled"),
                            () -> TooltipConfig.itemPreviewEnabled,
                            value -> TooltipConfig.itemPreviewEnabled = value))
                    .add(c.dropdownText("tooltip_preview_item_scope", () -> tr("gui.preview_item_scope"),
                            () -> TooltipConfig.itemPreviewScope,
                            value -> TooltipConfig.itemPreviewScope = TooltipConfig.normalizeItemPreviewScope(value),
                            Arrays.asList("tools", "all"),
                            value -> tr("gui.preview_item_scope." + value)).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.preview_armor_enabled"), () -> tr("tooltip.preview.armor.enabled"),
                            () -> TooltipConfig.armorPreviewEnabled,
                            value -> TooltipConfig.armorPreviewEnabled = value))
                    .add(c.toggleText(() -> tr("gui.preview_animation"), () -> tr("tooltip.preview.animation.enabled"),
                            () -> TooltipConfig.previewAnimationEnabled,
                            value -> TooltipConfig.previewAnimationEnabled = value))
                    .add(c.dropdownText("tooltip_preview_animation_duration", () -> tr("gui.preview_animation_duration"),
                            () -> Integer.toString(TooltipConfig.previewAnimationMillis),
                            value -> TooltipConfig.previewAnimationMillis = Integer.parseInt(value),
                            Arrays.asList("0", "90", "120", "180", "240", "360", "500"), value -> value + " ms").size(260, 24))
                    .add(c.toggleText(() -> tr("gui.preview_sound"), () -> tr("tooltip.preview.sound.enabled"),
                            () -> TooltipConfig.previewSoundEnabled,
                            value -> TooltipConfig.previewSoundEnabled = value))
                    .add(c.toggleText(() -> tr("gui.preview_effects"), () -> tr("tooltip.preview.effects.enabled"),
                            () -> TooltipConfig.previewEffectsEnabled,
                            value -> TooltipConfig.previewEffectsEnabled = value))
                    .add(c.toggleText(() -> tr("gui.preview_particles"), () -> tr("tooltip.preview.effects.particles"),
                            () -> TooltipConfig.previewParticlesEnabled,
                            value -> TooltipConfig.previewParticlesEnabled = value))
                    .add(c.dropdownText("tooltip_preview_item_width", () -> tr("gui.preview_item_width"),
                            () -> Integer.toString(TooltipConfig.itemPreviewWidth),
                            value -> TooltipConfig.itemPreviewWidth = Integer.parseInt(value),
                            Arrays.asList("24", "30", "36", "44", "52", "64"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_preview_armor_width", () -> tr("gui.preview_armor_width"),
                            () -> Integer.toString(TooltipConfig.armorPreviewWidth),
                            value -> TooltipConfig.armorPreviewWidth = Integer.parseInt(value),
                            Arrays.asList("28", "36", "40", "44", "52", "64"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_preview_effect_speed", () -> tr("gui.preview_effect_speed"),
                            () -> number(TooltipConfig.previewEffectSpeed),
                            value -> TooltipConfig.previewEffectSpeed = Float.parseFloat(value),
                            Arrays.asList("0.5", "0.75", "1", "1.25", "1.5", "2", "3"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_preview_particle_count", () -> tr("gui.preview_particle_count"),
                            () -> Integer.toString(TooltipConfig.previewParticleCount),
                            value -> TooltipConfig.previewParticleCount = Integer.parseInt(value),
                            Arrays.asList("0", "4", "8", "12", "16", "24", "32"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_preview_sound_volume", () -> tr("gui.preview_sound_volume"),
                            () -> number(TooltipConfig.previewSoundVolume),
                            value -> TooltipConfig.previewSoundVolume = Float.parseFloat(value),
                            Arrays.asList("0.1", "0.2", "0.35", "0.5", "0.75", "1"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_preview_sound_pitch", () -> tr("gui.preview_sound_pitch"),
                            () -> number(TooltipConfig.previewSoundPitch),
                            value -> TooltipConfig.previewSoundPitch = Float.parseFloat(value),
                            Arrays.asList("0.5", "0.75", "1", "1.25", "1.5", "2"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_preview_sound_cooldown", () -> tr("gui.preview_sound_cooldown"),
                            () -> Integer.toString(TooltipConfig.previewSoundCooldownMillis),
                            value -> TooltipConfig.previewSoundCooldownMillis = Integer.parseInt(value),
                            Arrays.asList("0", "100", "250", "500", "1000", "2000"), value -> value + " ms").size(260, 24))
                    .add(new NfrLabeledTextField(tr("gui.preview_sound_event"),
                            new TextFieldWidget().setMaxLength(256).autoUpdateOnChange(true)
                                    .value(new NfrStringValue(() -> TooltipConfig.previewSoundEvent,
                                            value -> TooltipConfig.previewSoundEvent = value))).size(260, 46))
                    .add(ruleField("gui.preview_whitelist", () -> TooltipConfig.previewWhitelist,
                            value -> TooltipConfig.previewWhitelist = parseRules(value)))
                    .add(ruleField("gui.preview_blacklist", () -> TooltipConfig.previewBlacklist,
                            value -> TooltipConfig.previewBlacklist = parseRules(value)))
                    .add(c.dropdownText("tooltip_preview_armor_model", () -> tr("gui.preview_armor_model"),
                            () -> TooltipConfig.armorPreviewModel,
                            value -> TooltipConfig.armorPreviewModel = value,
                            Arrays.asList("armor_stand", "player"),
                            value -> tr("gui.preview_armor_model." + value)).size(260, 24))
                    .add(c.dropdownText("tooltip_preview_armor_mode", () -> tr("gui.preview_armor_mode"),
                            () -> TooltipConfig.armorPreviewMode,
                            value -> TooltipConfig.armorPreviewMode = value,
                            Arrays.asList("single_piece", "full_set"),
                            value -> tr("gui.preview_armor_mode." + value)).size(260, 24))
                    .add(c.toggleText(() -> tr("gui.preview_armor_stand_base_plate"),
                            () -> tr("tooltip.preview.armor.stand.basePlate"),
                            () -> TooltipConfig.armorStandBasePlate,
                            value -> TooltipConfig.armorStandBasePlate = value))
                    .add(c.toggleText(() -> tr("gui.preview_player_hands"), () -> tr("tooltip.preview.armor.player.copyHands"),
                            () -> TooltipConfig.armorPlayerCopyHands,
                            value -> TooltipConfig.armorPlayerCopyHands = value))
                    .add(c.toggleText(() -> tr("gui.preview_player_sneaking"), () -> tr("tooltip.preview.armor.player.sneaking"),
                            () -> TooltipConfig.armorPlayerSneaking,
                            value -> TooltipConfig.armorPlayerSneaking = value))
                    .add(c.dropdownText("tooltip_preview_player_pose", () -> tr("gui.preview_player_pose"),
                            () -> TooltipConfig.armorPlayerPose,
                            value -> TooltipConfig.armorPlayerPose = value,
                            Arrays.asList("idle", "swing"),
                            value -> tr("gui.preview_player_pose." + value)).size(260, 24))
                    .add(c.dropdownText("tooltip_divider_alpha", () -> tr("gui.divider_alpha"),
                            () -> Integer.toString(TooltipConfig.dividerAlpha), value -> TooltipConfig.dividerAlpha = Integer.parseInt(value),
                            Arrays.asList("0", "64", "96", "128", "160", "176", "208", "255"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_h_padding", () -> tr("gui.horizontal_padding"),
                            () -> Integer.toString(TooltipConfig.horizontalPadding), value -> TooltipConfig.horizontalPadding = Integer.parseInt(value),
                            integerValues(1, 12), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_v_padding", () -> tr("gui.vertical_padding"),
                            () -> Integer.toString(TooltipConfig.verticalPadding), value -> TooltipConfig.verticalPadding = Integer.parseInt(value),
                            integerValues(1, 12), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_line_height", () -> tr("gui.line_height"),
                            () -> Integer.toString(TooltipConfig.lineHeight), value -> TooltipConfig.lineHeight = Integer.parseInt(value),
                            integerValues(8, 16), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_title_gap", () -> tr("gui.title_gap"),
                            () -> Integer.toString(TooltipConfig.titleGap), value -> TooltipConfig.titleGap = Integer.parseInt(value),
                            integerValues(0, 10), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_cursor_offset", () -> tr("gui.cursor_offset"),
                            () -> Integer.toString(TooltipConfig.cursorOffset), value -> TooltipConfig.cursorOffset = Integer.parseInt(value),
                            Arrays.asList("0", "4", "8", "10", "12", "16", "20", "24", "32"), value -> value).size(260, 24))
                    .add(c.dropdownText("tooltip_max_width", () -> tr("gui.max_width"),
                            () -> Integer.toString(TooltipConfig.maxWidth), value -> TooltipConfig.maxWidth = Integer.parseInt(value),
                            Arrays.asList("0", "80", "120", "160", "200", "240", "320"),
                            value -> "0".equals(value) ? tr("gui.unlimited") : value).size(260, 24))
                    .add(cornerColorPicker(c, "tooltip_fill_ul", "gui.fill_color", 0, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_fill_ur", "gui.fill_color", 1, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_fill_lr", "gui.fill_color", 2, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_fill_ll", "gui.fill_color", 3, TooltipConfig.fillColors))
                    .add(cornerColorPicker(c, "tooltip_border_ul", "gui.border_color", 0, TooltipConfig.borderColors))
                    .add(cornerColorPicker(c, "tooltip_border_ur", "gui.border_color", 1, TooltipConfig.borderColors))
                    .add(cornerColorPicker(c, "tooltip_border_lr", "gui.border_color", 2, TooltipConfig.borderColors))
                    .add(cornerColorPicker(c, "tooltip_border_ll", "gui.border_color", 3, TooltipConfig.borderColors))
                    .add(colorPicker(c, "tooltip_shadow_color", "gui.shadow_color", () -> TooltipConfig.shadowColor,
                            value -> TooltipConfig.shadowColor = value))
                    .add(colorPicker(c, "tooltip_text_color", "gui.text_color", () -> TooltipConfig.textColor,
                            value -> TooltipConfig.textColor = value))
                    .add(colorPicker(c, "tooltip_title_color", "gui.title_color", () -> TooltipConfig.titleColor,
                            value -> TooltipConfig.titleColor = value));
            return new PageView(profileSelector,
                    new ModernTooltipPreview(() -> profileId, () -> previewItemId),
                    typography, isTextProfile(profileId), grid);
        }

        @Override public void apply() { TooltipConfig.save(); }
        @Override public void cancel() { original.restore(); }

    private static String number(float value) {
            return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
        }

        private static String tr(String suffix) {
            return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
        }

        private static List<String> integerValues(int min, int max) {
            String[] values = new String[max - min + 1];
            for (int value = min; value <= max; value++) values[value - min] = Integer.toString(value);
            return Arrays.asList(values);
        }

        private static List<String> availableProfileIds() {
            List<String> profiles = new ArrayList<>();
            profiles.add("vanilla");
            if (NeofontrenderConfig.compatThaumcraftTooltip()
                    && Loader.isModLoaded("thaumcraft")) {
                profiles.add("thaumcraft");
            }
            if (TooltipConfig.heiCustomTooltips && Loader.isModLoaded("jei")) {
                profiles.add("hei");
            }
            if (TooltipConfig.quarkModernMapTooltip && Loader.isModLoaded("quark")) {
                profiles.add("quark");
            }
            return profiles;
        }

        private static boolean isTextProfile(String id) {
            return "vanilla".equals(id) || "thaumcraft".equals(id);
        }

        private static IWidget colorPicker(NfrSettingsControls controls, String name, String label,
                                           IntSupplier getter, IntConsumer setter) {
            return controls.colorText(name, () -> tr(label), getter, setter, true).size(260, 24);
        }

        private static IWidget cornerColorPicker(NfrSettingsControls controls, String name, String label,
                                                 int corner, int[] colors) {
            String[] suffixes = {"gui.corner.ul", "gui.corner.ur", "gui.corner.lr", "gui.corner.ll"};
            return controls.colorText(name, () -> tr(label) + " · " + tr(suffixes[corner]),
                    () -> colors[corner], value -> colors[corner] = value, true).size(260, 24);
        }
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid profileSelector, ModernTooltipPreview preview,
                         NfrOptionsGrid typography, boolean showTypography,
                         NfrOptionsGrid grid) {
            super(sections(profileSelector, preview, typography, showTypography, grid));
        }

        private static NfrContentView.Section[] sections(NfrOptionsGrid profileSelector,
                                                         ModernTooltipPreview preview,
                                                         NfrOptionsGrid typography,
                                                         boolean showTypography,
                                                         NfrOptionsGrid grid) {
            if (showTypography) return new NfrContentView.Section[]{
                    section(profileSelector, profileSelector::preferredHeight),
                    section(preview, width -> preview.preferredHeight()),
                    section(typography, typography::preferredHeight),
                    section(grid, grid::preferredHeight)
            };
            return new NfrContentView.Section[]{
                    section(profileSelector, profileSelector::preferredHeight),
                    section(preview, width -> preview.preferredHeight()),
                    section(grid, grid::preferredHeight)
            };
        }
    }

    private static IWidget ruleField(String label, java.util.function.Supplier<List<String>> getter,
                                     java.util.function.Consumer<String> setter) {
        return new NfrLabeledTextField(AddonI18n.tr("neofontrender_ui_enhancements." + label),
                new TextFieldWidget().setMaxLength(4096)
                .autoUpdateOnChange(true)
                .value(new NfrStringValue(() -> String.join(", ", getter.get()), setter))).size(260, 46);
    }

    private static List<String> parseRules(String value) {
        if (value == null || value.trim().isEmpty()) return java.util.Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String token : value.split(",")) {
            String rule = token.trim();
            if (!rule.isEmpty()) result.add(rule);
        }
        return result;
    }

    private static String modNameFormatLabel(String value) {
        String suffix = value == null || value.isEmpty() ? "none" : value.replace(' ', '_');
        return AddonI18n.tr("neofontrender_ui_enhancements.gui.mod_name_format." + suffix);
    }
}
