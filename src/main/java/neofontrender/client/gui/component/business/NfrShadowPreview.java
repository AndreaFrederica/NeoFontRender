package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import neofontrender.api.color.TextColorPaletteCodec;
import neofontrender.api.color.TextColorPaletteRegistry;
import neofontrender.api.text.ModernTextApi;
import neofontrender.api.text.ModernTextLayout;
import neofontrender.client.gui.model.NfrSettingsDraft;
import neofontrender.core.font.support.ShadowColorPolicy;
import neofontrender.core.font.support.ShadowColorRemapRules;

/** Large draft-only preview for the Shadow settings route. */
public final class NfrShadowPreview extends Widget<NfrShadowPreview> {
    private static final float FONT_SIZE = 42.0F;
    private static final int[] TEXT_COLORS = {
            0xFFFFFFFF, 0xFFE02D5F, 0xFF008FA8, 0xFFFFAA00
    };
    private static final float[][] BLUR_OFFSETS = {
            {0.0F, 0.0F},
            {-0.45F, 0.0F}, {0.45F, 0.0F}, {0.0F, -0.45F}, {0.0F, 0.45F},
            {-0.32F, -0.32F}, {0.32F, -0.32F}, {-0.32F, 0.32F}, {0.32F, 0.32F},
            {-0.85F, 0.0F}, {0.85F, 0.0F}, {0.0F, -0.85F}, {0.0F, 0.85F},
            {-0.60F, -0.60F}, {0.60F, -0.60F}, {-0.60F, 0.60F}, {0.60F, 0.60F}
    };

    private final NfrSettingsDraft draft;

    public NfrShadowPreview(NfrSettingsDraft draft) {
        this.draft = draft;
    }

    public int preferredHeight() {
        return 174;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        int width = getArea().w();
        int height = getArea().h();
        int right = Math.max(4, width - 4);
        int middle = Math.max(4, width / 2);
        int stageTop = 34;
        int stageBottom = Math.max(stageTop, height - 29);
        Gui.drawRect(4, 4, right, stageTop, 0xFF17222E);
        Gui.drawRect(4, stageTop, middle, stageBottom, 0xFFDCE4EC);
        Gui.drawRect(middle, stageTop, right, stageBottom, 0xFF46576A);
        Gui.drawRect(4, stageBottom, right, Math.max(stageBottom, height - 4), 0xFF17222E);
        Gui.drawRect(4, 4, right, 5, 0xFF00AEB8);
        Gui.drawRect(middle, stageTop, middle + 1, stageBottom, 0x668FA5BA);

        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.fontRenderer.drawString(
                I18n.format("neofontrender.gui.preview.shadow_title"), 10, 11, 0xFFB8C8D8);

        String[] sample = previewParts();
        if (ModernTextApi.isAvailable()) {
            drawNativePreview(sample, width);
        } else {
            drawScaledFallback(minecraft, sample, width);
        }

        Platform.setupDrawFont();
        String details = I18n.format("neofontrender.gui.preview.shadow_values",
                draft.shadowOffsetX, draft.shadowOffsetY,
                draft.modernShadow ? draft.shadowBlurRadius : 0.0F,
                Math.round(draft.shadowOpacity * 100.0F));
        minecraft.fontRenderer.drawString(
                minecraft.fontRenderer.trimStringToWidth(details, Math.max(1, width - 20)),
                10, Math.max(10, height - minecraft.fontRenderer.FONT_HEIGHT - 10), 0xFF8292A5);
    }

    private void drawNativePreview(String[] sample, int width) {
        ModernTextLayout[] foreground = new ModernTextLayout[sample.length];
        float totalAdvance = 0.0F;
        for (int index = 0; index < sample.length; index++) {
            foreground[index] = ModernTextApi.layout(
                    sample[index], FONT_SIZE, TEXT_COLORS[index % TEXT_COLORS.length]);
            totalAdvance += foreground[index].advance();
        }
        float x = Math.max(12.0F, (width - totalAdvance) * 0.5F);
        float y = 61.0F;
        float previewScale = previewScale();
        if (shadowEnabled()) {
            float cursor = x;
            for (int index = 0; index < sample.length; index++) {
                int configuredColor = shadowColor(TEXT_COLORS[index % TEXT_COLORS.length]);
                float opacity = draft.shadowOpacity
                        * ((configuredColor >>> 24) & 255) / 255.0F;
                ModernTextLayout shadow = ModernTextApi.layout(
                        sample[index], FONT_SIZE, 0xFF000000 | configuredColor & 0xFFFFFF);
                drawShadowSamples(shadow,
                        cursor + draft.shadowOffsetX * previewScale,
                        y + draft.shadowOffsetY * previewScale,
                        draft.modernShadow ? draft.shadowBlurRadius * previewScale : 0.0F,
                        opacity);
                cursor += foreground[index].advance();
            }
        }
        float cursor = x;
        for (ModernTextLayout part : foreground) {
            part.draw(cursor, y);
            cursor += part.advance();
        }
    }

    private static void drawShadowSamples(ModernTextLayout shadow, float x, float y,
                                          float blurRadius, float opacity) {
        float radius = Math.max(0.0F, Math.min(24.0F, blurRadius));
        int samples = radius < 0.05F ? 1 : radius <= 2.0F ? 9 : BLUR_OFFSETS.length;
        float targetOpacity = Math.max(0.0F, Math.min(1.0F, opacity));
        float sampleOpacity = samples == 1 ? targetOpacity
                : 1.0F - (float) Math.pow(1.0F - targetOpacity, 1.0F / samples);
        for (int index = 0; index < samples; index++) {
            shadow.draw(x + BLUR_OFFSETS[index][0] * radius,
                    y + BLUR_OFFSETS[index][1] * radius, sampleOpacity);
        }
    }

    private void drawScaledFallback(Minecraft minecraft, String[] sample, int width) {
        float scale = 4.0F;
        int[] advances = new int[sample.length];
        int textWidth = 0;
        for (int index = 0; index < sample.length; index++) {
            advances[index] = minecraft.fontRenderer.getStringWidth(sample[index]);
            textWidth += advances[index];
        }
        float x = Math.max(12.0F, (width - textWidth * scale) * 0.5F);
        float y = 65.0F;
        float previewScale = previewScale();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        if (shadowEnabled()) {
            float cursor = x;
            for (int index = 0; index < sample.length; index++) {
                int configuredColor = shadowColor(TEXT_COLORS[index % TEXT_COLORS.length]);
                int alpha = Math.round(((configuredColor >>> 24) & 255) * draft.shadowOpacity);
                if (alpha > 0) {
                    int color = alpha << 24 | configuredColor & 0xFFFFFF;
                    minecraft.fontRenderer.drawString(sample[index],
                            (cursor + draft.shadowOffsetX * previewScale) / scale,
                            (y + draft.shadowOffsetY * previewScale) / scale, color, false);
                }
                cursor += advances[index] * scale;
            }
        }
        float cursor = x;
        for (int index = 0; index < sample.length; index++) {
            minecraft.fontRenderer.drawString(sample[index], cursor / scale, y / scale,
                    TEXT_COLORS[index % TEXT_COLORS.length], false);
            cursor += advances[index] * scale;
        }
        GlStateManager.popMatrix();
    }

    private int shadowColor(int foregroundColor) {
        String mode = ShadowColorPolicy.normalizeMode(draft.shadowColorMode);
        int candidate = ShadowColorPolicy.SOLID.equals(mode)
                ? draft.shadowColor : ShadowColorPolicy.darken(foregroundColor);
        return ShadowColorRemapRules.parse(draft.shadowColorOverrides)
                .remap(foregroundColor, candidate, previewPalette());
    }

    private int[] previewPalette() {
        if (TextColorPaletteRegistry.CUSTOM.equals(
                TextColorPaletteRegistry.normalizeSelection(draft.textColorPaletteProvider))) {
            return TextColorPaletteCodec.parse(draft.customTextColorPalette);
        }
        return TextColorPaletteRegistry.resolve(draft.textColorPaletteProvider, null);
    }

    private static String[] previewParts() {
        return new String[] {
                I18n.format("neofontrender.gui.preview.shadow_part_1"),
                I18n.format("neofontrender.gui.preview.shadow_part_2"),
                I18n.format("neofontrender.gui.preview.shadow_part_3"),
                I18n.format("neofontrender.gui.preview.shadow_part_4")
        };
    }

    private boolean shadowEnabled() {
        return draft.shadowOpacity > 0.0F && !"none".equals(draft.shadowMode);
    }

    private float previewScale() {
        float configured;
        try {
            configured = Float.parseFloat(draft.fontSize);
        } catch (RuntimeException ignored) {
            configured = 8.5F;
        }
        if (!Float.isFinite(configured)) configured = 8.5F;
        configured = Math.max(4.0F, Math.min(64.0F, configured));
        return Math.max(1.0F, FONT_SIZE / configured);
    }
}
