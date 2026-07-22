package neofontrender.addons.ui;

import net.minecraft.client.resources.I18n;
import neofontrender.api.client.settings.NfrInfoLine;
import neofontrender.api.client.settings.NfrInfoPage;
import neofontrender.api.client.settings.NfrInfoPageContribution;
import neofontrender.api.client.settings.NfrInfoPageRegistry;

import java.util.Arrays;
import java.util.List;

final class UiEnhancementsInfoContributions {
    private UiEnhancementsInfoContributions() {}

    static void register() {
        NfrInfoPageRegistry.register(new NfrInfoPageContribution() {
            @Override public String id() { return NfrUiEnhancements.MOD_ID + ":about"; }
            @Override public NfrInfoPage page() { return NfrInfoPage.ABOUT; }
            @Override public List<NfrInfoLine> lines() {
                return Arrays.asList(
                        NfrInfoLine.spaced("NFR UI Enhancements", 0xFFFFFF),
                        NfrInfoLine.line(() -> tr("neofontrender_ui_enhancements.info.version") + ": "
                                + NfrUiEnhancements.VERSION, 0xD8D8D8),
                        NfrInfoLine.line(() -> tr("neofontrender_ui_enhancements.info.description"), 0xBFC7D1),
                        NfrInfoLine.line("github.com/AndreaFrederica/NeoFontRender", 0x00DCE8));
            }
        });
        NfrInfoPageRegistry.register(new NfrInfoPageContribution() {
            @Override public String id() { return NfrUiEnhancements.MOD_ID + ":licenses"; }
            @Override public NfrInfoPage page() { return NfrInfoPage.LICENSES; }
            @Override public List<NfrInfoLine> lines() {
                return Arrays.asList(
                        NfrInfoLine.spaced("NFR UI Enhancements - MIT", 0xD8D8D8),
                        NfrInfoLine.line("TabbyChat 2 Reforged - Apache-2.0", 0xD8D8D8),
                        NfrInfoLine.line("Jazzy spell checker - LGPL-2.1", 0xD8D8D8));
            }
        });
    }

    private static String tr(String key) {
        return I18n.format(key);
    }
}
