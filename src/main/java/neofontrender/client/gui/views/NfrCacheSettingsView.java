package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrResponsiveFieldGrid;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Cache sizing and expiry route. */
public final class NfrCacheSettingsView extends NfrContentView<NfrCacheSettingsView> {
    public NfrCacheSettingsView(NfrSettingsDraft d, NfrSettingsControls c) {
        this(c.grid().add(c.toggle("neofontrender.gui.cache.async_font", "neofontrender.tooltip.async_font",
                () -> d.asyncFontRendering, value -> d.asyncFontRendering = value)).add(c.toggle("neofontrender.gui.cache.monospace_characters", "neofontrender.tooltip.monospace_characters",
                () -> d.monospaceCharacterCache, value -> d.monospaceCharacterCache = value)), fields(d, c));
    }

    private NfrCacheSettingsView(NfrOptionsGrid options, NfrResponsiveFieldGrid fields) {
        super(section(options, options::preferredHeight), section(fields, fields::preferredHeight));
    }

    private static NfrResponsiveFieldGrid fields(NfrSettingsDraft d, NfrSettingsControls c) {
        return new NfrResponsiveFieldGrid()
                .add(c.cacheField("neofontrender.gui.cache.character_max",
                        () -> d.monospaceCharacterCacheMax, v -> d.monospaceCharacterCacheMax = v))
                .add(c.cacheField("neofontrender.gui.cache.text_min", () -> d.textCacheMin, v -> d.textCacheMin = v))
                .add(c.cacheField("neofontrender.gui.cache.text_max", () -> d.textCacheMax, v -> d.textCacheMax = v))
                .add(c.cacheField("neofontrender.gui.cache.text_ttl", () -> d.textCacheTtl, v -> d.textCacheTtl = v))
                .add(c.cacheField("neofontrender.gui.cache.measure_max", () -> d.measureCacheMax, v -> d.measureCacheMax = v));
    }
}
