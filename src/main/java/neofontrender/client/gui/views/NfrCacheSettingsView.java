package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrResponsiveFieldGrid;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.component.business.NfrCacheManagementPanel;
import neofontrender.client.gui.component.business.NfrGlyphViewerPanel;
import neofontrender.client.gui.component.base.NfrSectionDivider;
import neofontrender.client.gui.model.NfrSettingsDraft;
import net.minecraft.client.resources.I18n;

/** Cache sizing and expiry route. */
public final class NfrCacheSettingsView extends NfrContentView<NfrCacheSettingsView> {
    public NfrCacheSettingsView(NfrSettingsDraft d, NfrSettingsControls c) {
        this(c.grid().add(c.toggle("neofontrender.gui.cache.async_font", "neofontrender.tooltip.async_font",
                () -> d.asyncFontRendering, value -> d.asyncFontRendering = value)).add(c.toggle("neofontrender.gui.cache.monospace_characters", "neofontrender.tooltip.monospace_characters",
                () -> d.monospaceCharacterCache, value -> d.monospaceCharacterCache = value)).add(c.toggle(
                "neofontrender.gui.cache.adaptive_template", "neofontrender.tooltip.adaptive_template",
                () -> d.adaptiveTextTemplateCache, value -> d.adaptiveTextTemplateCache = value)).add(c.toggle(
                "neofontrender.gui.cache.generational", "neofontrender.tooltip.generational",
                () -> d.generationalTextCache, value -> d.generationalTextCache = value)).add(c.toggle(
                "neofontrender.gui.cache.overlay_fps", "neofontrender.tooltip.overlay_fps",
                () -> d.cacheOverlayFps, value -> d.cacheOverlayFps = value)).add(c.decimalSlider(
                "neofontrender.gui.cache.overlay_opacity", "neofontrender.tooltip.overlay_opacity",
                () -> d.cacheOverlayOpacity, value -> d.cacheOverlayOpacity = value,
                0.15F, 1.0F, 0.05F, () -> {})), fields(d, c),
                createManagement(c));
    }

    private NfrCacheSettingsView(NfrOptionsGrid options, NfrResponsiveFieldGrid fields,
                                 Management management) {
        super(section(options, options::preferredHeight), section(fields, fields::preferredHeight),
                section(new NfrSectionDivider(I18n.format("neofontrender.gui.cache.management"), 1)
                        .size(260, 22), ignored -> 22),
                section(management.panel, ignored -> management.panel.preferredHeight()),
                section(management.actions, management.actions::preferredHeight),
                section(new NfrSectionDivider(I18n.format("neofontrender.gui.cache.glyph_viewer"), 1)
                        .size(260, 22), ignored -> 22),
                section(management.viewerFields,
                        management.viewerFields::preferredHeight),
                section(management.viewerActions, management.viewerActions::preferredHeight),
                section(management.viewer, ignored -> management.viewer.preferredHeight()));
    }

    private static Management createManagement(NfrSettingsControls controls) {
        NfrCacheManagementPanel panel = new NfrCacheManagementPanel();
        NfrGlyphViewerPanel viewer = new NfrGlyphViewerPanel();
        NfrResponsiveFieldGrid viewerFields = new NfrResponsiveFieldGrid()
                .add(controls.cacheField("neofontrender.gui.cache.range_start",
                        viewer::startText, viewer::setStartText))
                .add(controls.cacheField("neofontrender.gui.cache.range_end",
                        viewer::endText, viewer::setEndText));
        NfrOptionsGrid actions = controls.grid()
                .add(controls.action("neofontrender.gui.cache.clear", 180, 24, panel::clear))
                .add(controls.action("neofontrender.gui.cache.open_directory", 180, 24, panel::openDirectory))
                .add(controls.action("neofontrender.gui.cache.batch_start", 180, 24, panel::startBatch));
        NfrOptionsGrid viewerActions = controls.grid()
                .add(controls.action("neofontrender.gui.cache.preset_ascii", 120, 22,
                        viewer::presetAscii))
                .add(controls.action("neofontrender.gui.cache.preset_latin", 120, 22,
                        viewer::presetLatin))
                .add(controls.action("neofontrender.gui.cache.preset_cjk_punctuation", 150, 22,
                        viewer::presetCjkPunctuation))
                .add(controls.action("neofontrender.gui.cache.preset_cjk", 120, 22,
                        viewer::presetCjk))
                .add(controls.action("neofontrender.gui.cache.previous_page", 120, 22,
                        viewer::previousPage))
                .add(controls.action("neofontrender.gui.cache.next_page", 120, 22,
                        viewer::nextPage))
                .add(controls.action("neofontrender.gui.cache.cache_range", 180, 22,
                        () -> panel.startBatch(viewer.rangeStart(), viewer.rangeEnd())));
        return new Management(panel, actions, viewerFields, viewerActions, viewer);
    }

    private static final class Management {
        private final NfrCacheManagementPanel panel;
        private final NfrOptionsGrid actions;
        private final NfrResponsiveFieldGrid viewerFields;
        private final NfrOptionsGrid viewerActions;
        private final NfrGlyphViewerPanel viewer;

        private Management(NfrCacheManagementPanel panel, NfrOptionsGrid actions,
                           NfrResponsiveFieldGrid viewerFields, NfrOptionsGrid viewerActions,
                           NfrGlyphViewerPanel viewer) {
            this.panel = panel;
            this.actions = actions;
            this.viewerFields = viewerFields;
            this.viewerActions = viewerActions;
            this.viewer = viewer;
        }
    }

    private static NfrResponsiveFieldGrid fields(NfrSettingsDraft d, NfrSettingsControls c) {
        return new NfrResponsiveFieldGrid()
                .add(c.cacheField("neofontrender.gui.cache.character_max",
                        () -> d.monospaceCharacterCacheMax, v -> d.monospaceCharacterCacheMax = v))
                .add(c.cacheField("neofontrender.gui.cache.async_fallback_glyph_max",
                        () -> d.asyncFallbackGlyphMax, v -> d.asyncFallbackGlyphMax = v))
                .add(c.cacheField("neofontrender.gui.cache.async_workers",
                        () -> d.asyncWorkerThreads, v -> d.asyncWorkerThreads = v))
                .add(c.cacheField("neofontrender.gui.cache.text_min", () -> d.textCacheMin, v -> d.textCacheMin = v))
                .add(c.cacheField("neofontrender.gui.cache.text_max", () -> d.textCacheMax, v -> d.textCacheMax = v))
                .add(c.cacheField("neofontrender.gui.cache.text_ttl", () -> d.textCacheTtl, v -> d.textCacheTtl = v))
                .add(c.cacheField("neofontrender.gui.cache.measure_max", () -> d.measureCacheMax, v -> d.measureCacheMax = v));
    }
}
