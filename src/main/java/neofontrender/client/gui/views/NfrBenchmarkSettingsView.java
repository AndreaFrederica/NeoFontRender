package neofontrender.client.gui.views;

import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.base.NfrSectionDivider;
import neofontrender.client.gui.component.business.NfrBenchmarkPanel;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.model.NfrSettingsDraft;
import net.minecraft.client.resources.I18n;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/** Interactive benchmark route for comparing the active Cosmic rendering paths. */
public final class NfrBenchmarkSettingsView extends NfrContentView<NfrBenchmarkSettingsView> {
    public NfrBenchmarkSettingsView(NfrSettingsDraft draft, NfrSettingsControls controls) {
        this(createPanel(), controls);
    }

    private NfrBenchmarkSettingsView(NfrBenchmarkPanel panel, NfrSettingsControls controls) {
                super(section(new NfrSectionDivider(I18n.format("neofontrender.gui.benchmark.title"), 1)
                        .size(260, 22), ignored -> 22),
                section(panel, width -> panel.preferredHeight()),
                section(sizeField(panel), width -> 42),
                section(actions(panel, controls), width -> 56));
    }

    private static NfrBenchmarkPanel createPanel() {
        return new NfrBenchmarkPanel();
    }

    private static NfrOptionsGrid actions(NfrBenchmarkPanel panel, NfrSettingsControls controls) {
        return new NfrOptionsGrid(120, 24, 8, true)
                .add(controls.action("neofontrender.gui.benchmark.run", 120, 24, panel::start))
                .add(controls.action("neofontrender.gui.benchmark.quick", 120, 24, panel::startQuick))
                .add(controls.action("neofontrender.gui.benchmark.load", 120, 24, panel::readLastResult))
                .add(controls.action("neofontrender.gui.benchmark.open_folder", 120, 24,
                        panel::openResultsFolder));
    }

    private static NfrLabeledTextField sizeField(NfrBenchmarkPanel panel) {
        return new NfrLabeledTextField(I18n.format("neofontrender.gui.benchmark.iterations"),
                new TextFieldWidget().setMaxLength(5)
                        .value(new NfrStringValue(panel::iterationsText, panel::setIterationsText)));
    }
}

