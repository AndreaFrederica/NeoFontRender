package neofontrender.addons.chat;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

/** Image-based emoji aliases, external image glyphs and their network policy. */
final class EmojiAndImageSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":emoji_and_images"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.inline_glyph.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1039; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean gosling = EmojiAndImageConfig.goslingImageGlyphs;
        private final boolean external = EmojiAndImageConfig.externalImageGlyphs;
        private final boolean local = EmojiAndImageConfig.localImageGlyphs;
        private final boolean hover = EmojiAndImageConfig.imageGlyphHover;
        private final String allow = EmojiAndImageConfig.imageAllowlist;
        private final String block = EmojiAndImageConfig.imageBlocklist;

        @Override
        public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid emoji = c.grid()
                    .add(c.toggleText(() -> tr("gui.inline_glyph.gosling"),
                            () -> tr("tooltip.inline_glyph.gosling"),
                            () -> EmojiAndImageConfig.goslingImageGlyphs,
                            value -> EmojiAndImageConfig.goslingImageGlyphs = value));
            NfrOptionsGrid images = c.grid()
                    .add(c.toggleText(() -> tr("gui.inline_glyph.local"),
                            () -> tr("tooltip.inline_glyph.local"),
                            () -> EmojiAndImageConfig.localImageGlyphs,
                            value -> EmojiAndImageConfig.localImageGlyphs = value))
                    .add(c.toggleText(() -> tr("gui.inline_glyph.external"),
                            () -> tr("tooltip.inline_glyph.external"),
                            () -> EmojiAndImageConfig.externalImageGlyphs,
                            value -> EmojiAndImageConfig.externalImageGlyphs = value))
                    .add(c.toggleText(() -> tr("gui.inline_glyph.hover"),
                            () -> tr("tooltip.inline_glyph.hover"),
                            () -> EmojiAndImageConfig.imageGlyphHover,
                            value -> EmojiAndImageConfig.imageGlyphHover = value));
            NfrOptionsGrid hosts = c.grid()
                    .add(hostField("gui.inline_glyph.allowlist",
                            () -> EmojiAndImageConfig.imageAllowlist,
                            value -> EmojiAndImageConfig.imageAllowlist = value))
                    .add(hostField("gui.inline_glyph.blocklist",
                            () -> EmojiAndImageConfig.imageBlocklist,
                            value -> EmojiAndImageConfig.imageBlocklist = value));
            return new PageView(emoji, images, hosts);
        }

        @Override public void apply() {
            EmojiAndImageConfig.save();
            InlineTextEngine.invalidateLayouts();
        }

        @Override public void cancel() {
            EmojiAndImageConfig.goslingImageGlyphs = gosling;
            EmojiAndImageConfig.externalImageGlyphs = external;
            EmojiAndImageConfig.localImageGlyphs = local;
            EmojiAndImageConfig.imageGlyphHover = hover;
            EmojiAndImageConfig.imageAllowlist = allow;
            EmojiAndImageConfig.imageBlocklist = block;
            InlineTextEngine.invalidateLayouts();
        }
    }

    private static NfrLabeledTextField hostField(String label,
                                                  java.util.function.Supplier<String> getter,
                                                  java.util.function.Consumer<String> setter) {
        return new NfrLabeledTextField(tr(label), new TextFieldWidget().setMaxLength(2048)
                .value(new NfrStringValue(getter, setter))).size(260, 46);
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid emoji, NfrOptionsGrid images, NfrOptionsGrid hosts) {
            super(section(emoji, emoji::preferredHeight), section(images, images::preferredHeight),
                    section(hosts, hosts::preferredHeight));
        }
    }
}
