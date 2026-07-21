package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrResponsiveFieldGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Cache sizing and expiry route. */
public final class NfrCacheSettingsView extends NfrContentView<NfrCacheSettingsView> {
    public NfrCacheSettingsView(NfrSettingsDraft d, NfrSettingsControls c) { this(fields(d, c)); }

    private NfrCacheSettingsView(NfrResponsiveFieldGrid fields) {
        super(section(fields, fields::preferredHeight));
    }

    private static NfrResponsiveFieldGrid fields(NfrSettingsDraft d, NfrSettingsControls c) {
        return new NfrResponsiveFieldGrid()
                .add(c.cacheField("neofontrender.gui.cache.text_min", () -> d.textCacheMin, v -> d.textCacheMin = v))
                .add(c.cacheField("neofontrender.gui.cache.text_max", () -> d.textCacheMax, v -> d.textCacheMax = v))
                .add(c.cacheField("neofontrender.gui.cache.text_ttl", () -> d.textCacheTtl, v -> d.textCacheTtl = v))
                .add(c.cacheField("neofontrender.gui.cache.measure_max", () -> d.measureCacheMax, v -> d.measureCacheMax = v))
                .add(c.cacheField("neofontrender.gui.cache.segment_min", () -> d.segmentCacheMin, v -> d.segmentCacheMin = v))
                .add(c.cacheField("neofontrender.gui.cache.segment_max", () -> d.segmentCacheMax, v -> d.segmentCacheMax = v))
                .add(c.cacheField("neofontrender.gui.cache.segment_ttl", () -> d.segmentCacheTtl, v -> d.segmentCacheTtl = v));
    }
}
