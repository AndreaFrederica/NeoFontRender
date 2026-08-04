package neofontrender.addons.chat;

import neofontrender.addons.ui.UiEnhancementsConfig;
import neofontrender.api.config.NfrConfigFile;

/** Independent configuration for image-based emoji and generic inline images. */
final class EmojiAndImageConfig {
    static boolean goslingImageGlyphs = false;
    static boolean localImageGlyphs = false;
    static boolean externalImageGlyphs = false;
    static boolean imageGlyphHover = true;
    static String imageAllowlist = "";
    static String imageBlocklist = "";

    private EmojiAndImageConfig() {}

    static void load() {
        NfrConfigFile file = UiEnhancementsConfig.file();
        file.define("emojiImages.goslingImageGlyphs", false,
                        "Render Gosling-compatible aliases and Discord custom tags as image glyphs; raw Unicode uses fonts.")
                .define("emojiImages.externalImageGlyphs", false,
                        "Render allowlisted <img:https://...> tokens as image glyphs.")
                .define("emojiImages.localImageGlyphs", false,
                        "Render :alias: tokens from the client-owned neofontrender/images gallery.")
                .define("emojiImages.imageGlyphHover", true,
                        "Show a large cached preview when hovering an image glyph.")
                .define("emojiImages.imageAllowlist", "",
                        "Comma-separated exact hosts or *.domain rules allowed for external image glyphs.")
                .define("emojiImages.imageBlocklist", "",
                        "Comma-separated image hosts denied before the allowlist and built-in providers.");
        goslingImageGlyphs = file.getBoolean("emojiImages.goslingImageGlyphs", false);
        externalImageGlyphs = file.getBoolean("emojiImages.externalImageGlyphs", false);
        localImageGlyphs = file.getBoolean("emojiImages.localImageGlyphs", false);
        imageGlyphHover = file.getBoolean("emojiImages.imageGlyphHover", true);
        imageAllowlist = file.getString("emojiImages.imageAllowlist", "");
        imageBlocklist = file.getString("emojiImages.imageBlocklist", "");
        file.save();
    }

    static void save() {
        UiEnhancementsConfig.file()
                .set("emojiImages.goslingImageGlyphs", goslingImageGlyphs)
                .set("emojiImages.externalImageGlyphs", externalImageGlyphs)
                .set("emojiImages.localImageGlyphs", localImageGlyphs)
                .set("emojiImages.imageGlyphHover", imageGlyphHover)
                .set("emojiImages.imageAllowlist", imageAllowlist)
                .set("emojiImages.imageBlocklist", imageBlocklist)
                .save();
    }
}
