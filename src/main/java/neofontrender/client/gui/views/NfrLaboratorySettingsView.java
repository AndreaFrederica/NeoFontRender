package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Experimental settings route. */
public final class NfrLaboratorySettingsView extends NfrContentView<NfrLaboratorySettingsView> {
    public NfrLaboratorySettingsView(NfrSettingsDraft d, NfrSettingsControls c) { this(options(d, c)); }

    private NfrLaboratorySettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        return c.grid().add(c.toggle("neofontrender.gui.option.hex_chat", "neofontrender.tooltip.hex_chat",
                () -> d.laboratoryHexChat, value -> d.laboratoryHexChat = value));
    }
}
