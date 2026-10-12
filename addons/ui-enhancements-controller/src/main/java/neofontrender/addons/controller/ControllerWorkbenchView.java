package neofontrender.addons.controller;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import neofontrender.client.gui.views.NfrContentView;

/** Scrollable NFR page body that refreshes SDL once for all embedded components. */
final class ControllerWorkbenchView extends NfrContentView<ControllerWorkbenchView> {
    private final ControllerWorkbenchModel model;

    ControllerWorkbenchView(ControllerWorkbenchModel model,
                            ControllerOptionsPanel generalSettings,
                            ControllerOptionsPanel guiSettings,
                            ControllerOptionsPanel cameraSettings,
                            ControllerOptionsPanel flightSettings,
                            ControllerTestPanel test,
                            ControllerBindingsPanel playerBindings,
                            ControllerBindingsPanel guiBindings,
                            ControllerBindingsPanel cameraBindings,
                            ControllerBindingsPanel flightBindings,
                            ControllerForgeBindingsPanel forgeBindings,
                            java.util.function.IntUnaryOperator generalHeight,
                            java.util.function.IntUnaryOperator guiHeight,
                            java.util.function.IntUnaryOperator cameraHeight,
                            java.util.function.IntUnaryOperator flightHeight) {
        super(section(generalSettings, generalHeight),
                section(guiSettings, guiHeight),
                section(cameraSettings, cameraHeight),
                section(flightSettings, flightHeight),
                section(test, width -> ControllerTestPanel.HEIGHT),
                section(playerBindings, width -> playerBindings.preferredHeight()),
                section(guiBindings, width -> guiBindings.preferredHeight()),
                section(cameraBindings, width -> cameraBindings.preferredHeight()),
                section(flightBindings, width -> flightBindings.preferredHeight()),
                section(forgeBindings, width -> forgeBindings.preferredHeight()));
        this.model = model;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        model.refresh();
        super.draw(context, widgetTheme);
    }
}
