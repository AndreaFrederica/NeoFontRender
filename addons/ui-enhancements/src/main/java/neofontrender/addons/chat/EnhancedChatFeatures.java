package neofontrender.addons.chat;

/** Public bridge used by the embedded Tabby renderer without exposing mutable config fields. */
public final class EnhancedChatFeatures {
    private EnhancedChatFeatures() {}

    public static boolean playerHeads() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.playerHeads;
    }

    public static boolean headShadow() {
        return EnhancedChatConfig.headShadow;
    }

    public static boolean itemIcons() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.itemIcons;
    }

    public static boolean copySelection() {
        return EnhancedChatConfig.enabled && EnhancedChatConfig.copySelection;
    }

    public static boolean goslingImageGlyphs() {
        return EmojiAndImageConfig.goslingImageGlyphs;
    }

    public static boolean externalImageGlyphs() {
        return EmojiAndImageConfig.externalImageGlyphs;
    }

    public static boolean localImageGlyphs() {
        return EmojiAndImageConfig.localImageGlyphs;
    }

    public static boolean inlineGlyphs() {
        return goslingImageGlyphs() || externalImageGlyphs() || localImageGlyphs();
    }

    public static boolean imageGlyphHover() {
        return EmojiAndImageConfig.imageGlyphHover;
    }

    public static String imageAllowlist() {
        return EmojiAndImageConfig.imageAllowlist;
    }

    public static String imageBlocklist() {
        return EmojiAndImageConfig.imageBlocklist;
    }

    static boolean copyFormattingCodes() {
        return EnhancedChatConfig.copyFormattingCodes;
    }

    static boolean ampersandFormatting() {
        return EnhancedChatConfig.ampersandFormatting;
    }
}
