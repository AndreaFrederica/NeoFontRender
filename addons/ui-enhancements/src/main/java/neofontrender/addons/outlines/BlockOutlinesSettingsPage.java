package neofontrender.addons.outlines;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class BlockOutlinesSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":block_outlines"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.outlines.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1018; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final BlockOutlineConfig.Snapshot original = BlockOutlineConfig.snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid general = c.grid()
                    .add(toggle(c, "enabled", () -> BlockOutlineConfig.enabled,
                            value -> BlockOutlineConfig.enabled = value))
                    .add(c.colorText("outline_global_color", () -> tr("global_color"),
                            () -> BlockOutlineConfig.globalColor,
                            value -> BlockOutlineConfig.globalColor = value, true).size(260, 24))
                    .add(slider(c, "global_width", () -> BlockOutlineConfig.globalLineWidth,
                            value -> BlockOutlineConfig.globalLineWidth = value));
            NfrOptionsGrid normalRules = new NfrOptionsGrid(260, 42, 8, true)
                    .add(ruleField("block_overrides",
                            () -> BlockOutlineConfig.editorRules(BlockOutlineConfig.blockOverrides),
                            value -> {
                                BlockOutlineConfig.blockOverrides = BlockOutlineConfig.parseEditorRules(value);
                                BlockOutlineResolver.reload();
                            }));
            NfrOptionsGrid noHarvest = c.grid()
                    .add(toggle(c, "no_harvest", () -> BlockOutlineConfig.noHarvestEnabled,
                            value -> BlockOutlineConfig.noHarvestEnabled = value))
                    .add(c.colorText("outline_no_harvest_color", () -> tr("no_harvest_color"),
                            () -> BlockOutlineConfig.noHarvestColor,
                            value -> BlockOutlineConfig.noHarvestColor = value, true).size(260, 24))
                    .add(slider(c, "no_harvest_width", () -> BlockOutlineConfig.noHarvestLineWidth,
                            value -> BlockOutlineConfig.noHarvestLineWidth = value));
            NfrOptionsGrid noHarvestRules = new NfrOptionsGrid(260, 42, 8, true)
                    .add(ruleField("no_harvest_overrides",
                            () -> BlockOutlineConfig.editorRules(BlockOutlineConfig.noHarvestOverrides),
                            value -> {
                                BlockOutlineConfig.noHarvestOverrides = BlockOutlineConfig.parseEditorRules(value);
                                BlockOutlineResolver.reload();
                            }));
            return new PageView(general, normalRules, noHarvest, noHarvestRules);
        }

        @Override public void preview() { BlockOutlineResolver.reload(); }
        @Override public void apply() { BlockOutlineConfig.save(); }
        @Override public void cancel() { original.restore(); }
    }

    private static IWidget toggle(NfrSettingsControls controls, String key, Supplier<Boolean> getter,
                                  Consumer<Boolean> setter) {
        return controls.toggleText(() -> tr(key), () -> tooltip(key), getter, setter);
    }

    private static IWidget slider(NfrSettingsControls controls, String key, Supplier<Float> getter,
                                  Consumer<Float> setter) {
        return controls.decimalSlider(() -> tr(key), getter, setter, 0.0F, 1000.0F, 1.0F);
    }

    private static NfrLabeledTextField ruleField(String key, Supplier<String> getter,
                                                 Consumer<String> setter) {
        TextFieldWidget editor = new TextFieldWidget().setMaxLength(8192)
                .value(new NfrStringValue(getter, setter));
        return new NfrLabeledTextField(tr(key), editor)
                .tooltip(new RichTooltip().showUpTimer(8).addLine(tooltip("rule_format")));
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.gui.outlines." + key);
    }

    private static String tooltip(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.tooltip.outlines." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid general, NfrOptionsGrid normalRules,
                         NfrOptionsGrid noHarvest, NfrOptionsGrid noHarvestRules) {
            super(section(general, general::preferredHeight),
                    section(normalRules, normalRules::preferredHeight),
                    section(noHarvest, noHarvest::preferredHeight),
                    section(noHarvestRules, noHarvestRules::preferredHeight));
        }
    }
}
