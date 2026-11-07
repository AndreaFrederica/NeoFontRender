package neofontrender.addons.cjk;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrSectionDivider;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;
import neofontrender.core.config.NeofontrenderConfig;

import java.util.Arrays;

final class CjkTypographySettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":cjk_typography"; }
    @Override public String titleKey() {
        return "neofontrender_ui_enhancements.gui.cjk_typography.category";
    }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1004; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final String originalEngine = CjkTypographyConfig.engine;
        // Draft values; NFR's config is only written on apply.
        private boolean protocolWrap = NeofontrenderConfig.protocolWrapEnabled();
        private boolean protocolWrapChat = NeofontrenderConfig.protocolWrapChat();
        private boolean protocolWrapTooltip = NeofontrenderConfig.protocolWrapTooltip();
        private boolean protocolWrapSign = NeofontrenderConfig.protocolWrapSign();
        private boolean protocolWrapOther = NeofontrenderConfig.protocolWrapOther();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid()
                    .add(controls.dropdownText(
                            "cjk_typography_engine",
                            () -> tr("gui.cjk_typography.engine"),
                            () -> CjkTypographyConfig.engine,
                            value -> CjkTypographyConfig.engine = CjkTypographyConfig.normalize(value),
                            Arrays.asList(CjkTypographyConfig.ENGINE_TIQIAN,
                                    CjkTypographyConfig.ENGINE_LEGACY),
                            value -> tr("gui.cjk_typography.engine." + value)).size(260, 24))
                    .add(new NfrSectionDivider(tr("gui.cjk_typography.section_wrapping"), 1).size(260, 22))
                    .add(controls.toggleText(
                            () -> tr("gui.cjk_typography.protocol_wrap"),
                            () -> tr("tooltip.cjk_typography.protocol_wrap"),
                            () -> protocolWrap, value -> protocolWrap = value))
                    .add(controls.toggleText(
                            () -> tr("gui.cjk_typography.protocol_wrap_chat"),
                            () -> tr("tooltip.cjk_typography.protocol_wrap_chat"),
                            () -> protocolWrapChat, value -> protocolWrapChat = value))
                    .add(controls.toggleText(
                            () -> tr("gui.cjk_typography.protocol_wrap_tooltip"),
                            () -> tr("tooltip.cjk_typography.protocol_wrap_tooltip"),
                            () -> protocolWrapTooltip, value -> protocolWrapTooltip = value))
                    .add(controls.toggleText(
                            () -> tr("gui.cjk_typography.protocol_wrap_sign"),
                            () -> tr("tooltip.cjk_typography.protocol_wrap_sign"),
                            () -> protocolWrapSign, value -> protocolWrapSign = value))
                    .add(controls.toggleText(
                            () -> tr("gui.cjk_typography.protocol_wrap_other"),
                            () -> tr("tooltip.cjk_typography.protocol_wrap_other"),
                            () -> protocolWrapOther, value -> protocolWrapOther = value));
            return new PageView(grid);
        }

        @Override public void apply() {
            CjkTypographyConfig.save();
            NeofontrenderConfig.setProtocolWrapEnabled(protocolWrap);
            NeofontrenderConfig.setProtocolWrapChat(protocolWrapChat);
            NeofontrenderConfig.setProtocolWrapTooltip(protocolWrapTooltip);
            NeofontrenderConfig.setProtocolWrapSign(protocolWrapSign);
            NeofontrenderConfig.setProtocolWrapOther(protocolWrapOther);
        }

        @Override public void cancel() {
            CjkTypographyConfig.engine = originalEngine;
            TiqianParagraphProvider.INSTANCE.clearCache();
        }
    }

    private static String tr(String suffix) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + suffix);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
