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

    static boolean copyFormattingCodes() {
        return EnhancedChatConfig.copyFormattingCodes;
    }

    static boolean ampersandFormatting() {
        return EnhancedChatConfig.ampersandFormatting;
    }

    public static boolean inlineGlyphs() {
        return EnhancedChatConfig.enabled
                && (EmojiAndImageConfig.goslingImageGlyphs
                    || EmojiAndImageConfig.localImageGlyphs
                    || EmojiAndImageConfig.externalImageGlyphs);
    }

    public static boolean imageGlyphHover() {
        return EnhancedChatConfig.enabled && EmojiAndImageConfig.imageGlyphHover;
    }

    public static boolean goslingImageGlyphs() {
        return EnhancedChatConfig.enabled && EmojiAndImageConfig.goslingImageGlyphs;
    }

    public static boolean localImageGlyphs() {
        return EnhancedChatConfig.enabled && EmojiAndImageConfig.localImageGlyphs;
    }

    public static boolean externalImageGlyphs() {
        return EnhancedChatConfig.enabled && EmojiAndImageConfig.externalImageGlyphs;
    }

    public static String imageAllowlist() {
        return EmojiAndImageConfig.imageAllowlist;
    }

    public static String imageBlocklist() {
        return EmojiAndImageConfig.imageBlocklist;
    }
}
