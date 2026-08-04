package neofontrender.api;

import com.google.common.util.concurrent.ListenableFuture;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.api.color.TextColorPaletteCodec;
import neofontrender.api.color.TextColorPaletteProvider;
import neofontrender.api.color.TextColorPaletteRegistry;

import java.util.List;

/**
 * Stable client-side integration API for other mods.
 *
 * <p>Use {@link #updateFont()} for partial updates. Applying an update is safe from any thread:
 * work is scheduled onto Minecraft's client thread, optionally persisted, and followed by one
 * backend reload.</p>
 */
@SideOnly(Side.CLIENT)
public final class NeoFontRenderApi {
    public static final int API_VERSION = 5;

    private NeoFontRenderApi() {}

    public static FontUpdate updateFont() {
        return new FontUpdate();
    }

    /** Convenience method that selects one font across all backends and persists it. */
    public static ListenableFuture<?> setPrimaryFont(String font) {
        return updateFont().font(font).apply();
    }

    /** Schedule a backend reload without modifying configuration. */
    public static ListenableFuture<?> reload() {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.addScheduledTask(() -> reloadNow(minecraft));
    }

    /** Registers a selectable legacy text-color palette provider for the lifetime of the client. */
    public static void registerTextColorPaletteProvider(TextColorPaletteProvider provider) {
        TextColorPaletteRegistry.register(provider);
    }

    /** Includes {@code auto} followed by all built-in and mod-registered provider ids. */
    public static List<String> getTextColorPaletteProviderIds() {
        return TextColorPaletteRegistry.providerIds();
    }

    /** Refreshes renderers after a dynamic palette provider changes its internal state. */
    public static void invalidateTextColorPaletteProviders() {
        TextColorPaletteRegistry.invalidate();
    }

    public static String getSelectedTextColorPaletteProvider() {
        return NeofontrenderConfig.textColorPaletteProvider();
    }

    public static ListenableFuture<?> selectTextColorPaletteProvider(String providerId) {
        return selectTextColorPaletteProvider(providerId, true);
    }

    /** Selects a built-in or mod-registered provider. Unknown ids safely fall back to auto. */
    public static ListenableFuture<?> selectTextColorPaletteProvider(
            String providerId, boolean persist) {
        String selected = TextColorPaletteRegistry.normalizeSelection(providerId);
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.addScheduledTask(() -> {
            NeofontrenderConfig.setTextColorPaletteProvider(selected);
            if (persist) NeofontrenderConfig.save();
        });
    }

    public static ListenableFuture<?> setCustomTextColorPalette(int[] colorCodes) {
        return setCustomTextColorPalette(colorCodes, true);
    }

    /** Stores 16 foreground colors or a complete 32-entry foreground/shadow palette. */
    public static ListenableFuture<?> setCustomTextColorPalette(
            int[] colorCodes, boolean persist) {
        if (colorCodes == null || (colorCodes.length != 16 && colorCodes.length != 32)) {
            throw new IllegalArgumentException("Custom palette must contain 16 or 32 colors");
        }
        int[] copy = colorCodes.clone();
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.addScheduledTask(() -> {
            NeofontrenderConfig.setCustomTextColorCodes(copy);
            if (persist) NeofontrenderConfig.save();
        });
    }

    /** Parses the same comma/semicolon/whitespace-separated RGB format exposed by the settings UI. */
    public static ListenableFuture<?> setCustomTextColorPalette(String colorCodes) {
        return setCustomTextColorPalette(colorCodes, true);
    }

    /** Parses the same comma/semicolon/whitespace-separated RGB format exposed by the settings UI. */
    public static ListenableFuture<?> setCustomTextColorPalette(
            String colorCodes, boolean persist) {
        int[] parsed = TextColorPaletteCodec.parse(colorCodes);
        return setCustomTextColorPalette(parsed, persist);
    }

    /** Returns a point-in-time view of configuration and active backend state. */
    public static FontState getFontState() {
        FontManager manager = FontManager.INSTANCE;
        return new FontState(
                NeofontrenderConfig.fontName(),
                NeofontrenderConfig.fontFallbacks(),
                NeofontrenderConfig.cosmicFaceOverrides(),
                NeofontrenderConfig.fontSize(),
                FontStyle.fromConfig(NeofontrenderConfig.fontStyle()),
                NeofontrenderConfig.fontVariableWeight(),
                RenderingEngine.fromConfig(NeofontrenderConfig.renderingEngine()),
                manager.isSfrActive() || manager.isTextBackendActive(),
                manager.getBackendVersion());
    }

    static ListenableFuture<?> apply(FontUpdate update) {
        if (update == null) throw new IllegalArgumentException("Font update must not be null");
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.addScheduledTask(() -> {
            if (update.primaryFont != null) NeofontrenderConfig.setFontName(update.primaryFont);
            if (update.cosmicRegularFont != null) {
                NeofontrenderConfig.setCosmicRegularFont(update.cosmicRegularFont);
                NeofontrenderConfig.setCosmicBoldFont(update.cosmicBoldFont);
                NeofontrenderConfig.setCosmicItalicFont(update.cosmicItalicFont);
                NeofontrenderConfig.setCosmicBoldItalicFont(update.cosmicBoldItalicFont);
            }
            if (update.fallbackFonts != null) NeofontrenderConfig.setFontFallbacks(update.fallbackFonts);
            if (update.size != null) NeofontrenderConfig.setFontSize(update.size);
            if (update.style != null) NeofontrenderConfig.setFontStyle(update.style.configValue());
            if (update.variableWeight != null) {
                NeofontrenderConfig.setFontVariableWeight(update.variableWeight);
            }
            if (update.engine != null) {
                NeofontrenderConfig.setRenderingEngine(update.engine.configValue());
                NeofontrenderConfig.setEnabled(update.engine != RenderingEngine.VANILLA);
            }
            if (update.enabled != null) NeofontrenderConfig.setEnabled(update.enabled);
            if (update.persist) NeofontrenderConfig.save();
            reloadNow(minecraft);
        });
    }

    private static void reloadNow(Minecraft minecraft) {
        if (minecraft.getResourceManager() == null || minecraft.getTextureManager() == null) {
            throw new IllegalStateException("Minecraft resources are not initialized yet");
        }
        FontManager.INSTANCE.init(minecraft.getTextureManager());
        FontManager.INSTANCE.reload(minecraft.getResourceManager());
    }
}
