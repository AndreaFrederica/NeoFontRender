package neofontrender.client.gui.views;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Compatibility fixes route. */
public final class NfrFixesSettingsView extends NfrContentView<NfrFixesSettingsView> {
    public NfrFixesSettingsView(NfrSettingsDraft d, NfrSettingsControls c) { this(options(d, c)); }

    public NfrFixesSettingsView(NfrSettingsDraft d, NfrSettingsControls c,
                                Iterable<IWidget> contributedControls) {
        this(options(d, c, contributedControls));
    }

    private NfrFixesSettingsView(NfrOptionsGrid options) {
        super(section(options, options::preferredHeight));
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c) {
        return options(d, c, null);
    }

    private static NfrOptionsGrid options(NfrSettingsDraft d, NfrSettingsControls c,
                                          Iterable<IWidget> contributedControls) {
        NfrOptionsGrid grid = c.grid()
                .add(c.toggle("neofontrender.gui.option.fix_ime", "neofontrender.tooltip.fix_ime",
                        () -> d.fixImeInput, value -> d.fixImeInput = value))
                .add(c.toggle("neofontrender.gui.option.fix_unicode_delete", "neofontrender.tooltip.fix_unicode_delete",
                        () -> d.fixUnicodeTextDeletion, value -> d.fixUnicodeTextDeletion = value))
                .add(c.toggle("neofontrender.gui.option.fix_cjk_line_break", "neofontrender.tooltip.fix_cjk_line_break",
                        () -> d.fixCjkLineBreak, value -> d.fixCjkLineBreak = value))
                .add(c.toggle("neofontrender.gui.option.sign_paste", "neofontrender.tooltip.sign_paste",
                        () -> d.allowSignPaste, value -> d.allowSignPaste = value));
        if (contributedControls != null) {
            for (IWidget widget : contributedControls) {
                if (widget != null) grid.add(widget);
            }
        }
        return grid;
    }
}
