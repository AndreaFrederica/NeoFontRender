package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;

/** Shadow route. */
public final class NfrShadowSettingsView extends NfrContentView<NfrShadowSettingsView> {
    public NfrShadowSettingsView(NfrSettingsControls controls) {
        this(options(controls));
    }

    private NfrShadowSettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsControls c) {
        return c.grid().add(c.shadowMode()).add(c.shadowValue(false)).add(c.shadowValue(true));
    }
}
