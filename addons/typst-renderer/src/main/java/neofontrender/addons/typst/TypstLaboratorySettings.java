package neofontrender.addons.typst;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsSection;
import neofontrender.api.client.settings.NfrSettingsSectionContribution;
import neofontrender.api.client.settings.NfrSettingsSectionRegistry;
import neofontrender.api.client.settings.NfrSettingsSectionSession;
import neofontrender.api.text.StructuredTextApi;
import neofontrender.api.text.route.TextRenderRouteApi;

import java.util.Collections;
import java.util.List;

/** Exposes the optional Typst syntax provider in NFR's laboratory settings. */
final class TypstLaboratorySettings {
    private TypstLaboratorySettings() {}

    static void register() {
        NfrSettingsSectionRegistry.register(new NfrSettingsSectionContribution() {
            @Override public String id() { return TypstRendererMod.MOD_ID + ":renderer"; }
            @Override public NfrSettingsSection section() { return NfrSettingsSection.LABORATORY; }
            @Override public int order() { return 1100; }
            @Override public NfrSettingsSectionSession createSession() { return new Session(); }
        });
    }

    private static final class Session implements NfrSettingsSectionSession {
        private final boolean enabled = TypstConfig.enabled();

        @Override
        public List<IWidget> createControls(NfrSettingsPageContext context) {
            return Collections.singletonList(context.controls().toggleText(
                    () -> TypstI18n.tr("neofontrender_typst_renderer.gui.laboratory.enabled"),
                    () -> TypstI18n.tr("neofontrender_typst_renderer.tooltip.laboratory.enabled"),
                    TypstConfig::enabled, TypstConfig::setEnabled, TypstLaboratorySettings::invalidate));
        }

        @Override public void preview() { invalidate(); }

        @Override public void apply() {
            TypstConfig.save();
            invalidate();
        }

        @Override public void cancel() {
            TypstConfig.setEnabled(enabled);
            invalidate();
        }
    }

    private static void invalidate() {
        StructuredTextApi.invalidate();
        TextRenderRouteApi.invalidate();
    }
}
