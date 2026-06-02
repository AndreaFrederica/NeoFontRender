package neofontrender.client;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.EncodedImageFormat;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.skija.paragraph.FontCollection;
import io.github.humbleui.skija.paragraph.Paragraph;
import io.github.humbleui.skija.paragraph.ParagraphBuilder;
import io.github.humbleui.skija.paragraph.ParagraphStyle;
import io.github.humbleui.skija.paragraph.TextStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import neofontrender.client.gui.NeofontrenderEmojiTestScreen;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.awt.FontSet;
import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.skia.SkiaTextSegmenter;
import neofontrender.core.font.skia.SkijaTextRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NeofontrenderCommand extends CommandBase {

    @Override
    public String getName() {
        return "neofontrender";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/" + getName() + " fonts|info|reload|export [text]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Neo Font Render Commands:"));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  /neofontrender fonts - Show current font families"));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  /neofontrender info - Show engine status"));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  /neofontrender reload - Reload fonts"));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  /neofontrender export [text] - Export diagnostic PNGs at multiple oversample scales"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "fonts":
                executeFonts(sender);
                break;
            case "info":
                executeInfo(sender);
                break;
            case "reload":
                executeReload(sender);
                break;
            case "test":
                executeTest(sender);
                break;
            case "gui":
                executeGui(sender);
                break;
            case "export":
                executeExport(sender, args);
                break;
            default:
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown subcommand: " + args[0]));
        }
    }

    private void executeFonts(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Font Families ==="));

        if (!FontManager.INSTANCE.isSkiaActive()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Skia engine not active. Current engine: "
                    + (FontManager.INSTANCE.isSfrActive() ? "SFR" : "vanilla")));
            return;
        }

        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Text render backend unavailable"));
            return;
        }

        String[] families = backend.getFontFamilies();
        if (families == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "fontFamilies is null"));
            return;
        }

        sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "fontFamilies count: " + families.length));
        for (int i = 0; i < families.length; i++) {
            String family = families[i];
            sender.sendMessage(new TextComponentString(
                    TextFormatting.WHITE + "  [" + i + "] " + TextFormatting.GREEN + family));
        }

        // Check what FontMgr.getDefault() can find
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== System FontMgr Resolution ==="));
        try {
            FontMgr defaultMgr = FontMgr.getDefault();

            // Check each configured family against system fonts
            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Configured Family Resolution ==="));
            for (String family : families) {
                try {
                    Typeface tf = defaultMgr.matchFamilyStyle(family, FontStyle.NORMAL);
                    if (tf != null) {
                        String resolvedName = tf.getFamilyName();
                        sender.sendMessage(new TextComponentString(
                                TextFormatting.WHITE + "  " + family + " -> "
                                        + TextFormatting.GREEN + resolvedName));
                        tf.close();
                    } else {
                        sender.sendMessage(new TextComponentString(
                                TextFormatting.WHITE + "  " + family + " -> "
                                        + TextFormatting.RED + "NOT FOUND"));
                    }
                } catch (Throwable e) {
                    sender.sendMessage(new TextComponentString(
                            TextFormatting.WHITE + "  " + family + " -> "
                                    + TextFormatting.RED + "ERROR: " + e.getMessage()));
                }
            }
        } catch (Throwable t) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Error querying FontMgr: " + t.getMessage()));
        }
    }

    private void executeInfo(ICommandSender sender) {
        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Engine Status ==="));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  SFR active: " + FontManager.INSTANCE.isSfrActive()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia active: " + FontManager.INSTANCE.isSkiaActive()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Engine config: " + neofontrender.core.config.NeofontrenderConfig.renderingEngine()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Enabled: " + neofontrender.core.config.NeofontrenderConfig.enabled()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Font name: " + neofontrender.core.config.NeofontrenderConfig.fontName()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Font size: " + neofontrender.core.config.NeofontrenderConfig.fontSize()));
        sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Oversample: " + neofontrender.core.config.NeofontrenderConfig.fontOversample()));
        SkijaTextRenderer renderer = FontManager.INSTANCE.getSkijaTextRenderer();
        if (renderer != null) {
            SkijaTextRenderer.DebugState state = renderer.debugState();
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia GPU offscreen requested: " + state.gpuRequested()));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia GPU mode: "
                    + (state.gpuRequested()
                    ? (neofontrender.core.config.NeofontrenderConfig.skiaGpuSubmitViaCpuTexture()
                    ? "isolated-context + DynamicTexture submit"
                    : "isolated-context + shared texture")
                    : "off")));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia last raster path: " + state.lastRasterPath()));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia GPU context: " + state.gpuContextCreated()));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia GPU unavailable: " + state.gpuUnavailable()));
            if (state.lastGpuFallbackReason() != null && !state.lastGpuFallbackReason().isEmpty()) {
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "  Skia GPU fallback reason: " + state.lastGpuFallbackReason()));
            }
            if (state.lastDrawState() != null && !state.lastDrawState().isEmpty()) {
                sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia last draw: " + state.lastDrawState()));
            }
            if (state.lastRasterStats() != null && !state.lastRasterStats().isEmpty()) {
                sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia raster stats: " + state.lastRasterStats()));
            }
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia cache: text "
                    + state.renderCacheSize() + "/" + state.renderCacheMax()
                    + ", segment " + state.segmentCacheSize() + "/" + state.segmentCacheMax()
                    + ", measure " + state.measureCacheSize() + "/" + state.measureCacheMax()));
            if (neofontrender.core.config.NeofontrenderConfig.debugRenderStats()) {
                sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia cache h/m/e: text "
                        + state.renderCacheHits() + "/" + state.renderCacheMisses() + "/" + state.renderCacheEvictions()
                        + ", segment " + state.segmentCacheHits() + "/" + state.segmentCacheMisses()
                        + "/" + state.segmentCacheEvictions()
                        + ", measure " + state.measureCacheHits() + "/" + state.measureCacheMisses()
                        + "/" + state.measureCacheEvictions()));
                SkiaTextSegmenter.DebugState segmentState = SkiaTextSegmenter.debugState();
                sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia segment cache: "
                        + segmentState.enabled()
                        + " attempts/runs/reject/segs " + segmentState.attempts()
                        + "/" + segmentState.segmentedRuns()
                        + "/" + segmentState.rejectedRuns()
                        + "/" + segmentState.emittedSegments()));
            } else {
                sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia debug stats: disabled"));
            }
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Skia raster count cpu/gpu: "
                    + state.cpuRasterCount() + "/" + state.gpuRasterCount()));
        }
        FontSet.DebugState sfrState = FontManager.INSTANCE.getSfrDebugState();
        if (sfrState != null) {
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  SFR glyph cache: info "
                    + sfrState.glyphInfoCacheSize()
                    + ", baked " + sfrState.bakedGlyphCacheSize()
                    + ", buckets " + sfrState.glyphWidthBuckets()));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  SFR glyph h/m: info "
                    + sfrState.glyphInfoHits() + "/" + sfrState.glyphInfoMisses()
                    + ", baked " + sfrState.bakedGlyphHits() + "/" + sfrState.bakedGlyphMisses()));
            sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  SFR layout cache: "
                    + sfrState.layoutCacheSize() + "/" + sfrState.layoutCacheMax()
                    + " h/m/e " + sfrState.layoutCacheHits()
                    + "/" + sfrState.layoutCacheMisses()
                    + "/" + sfrState.layoutCacheEvictions()));
        }
    }

    private void executeReload(ICommandSender sender) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getResourceManager() != null) {
            FontManager.INSTANCE.reload(mc.getResourceManager());
            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Fonts reloaded."));
        } else {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Resource manager not available."));
        }
    }

    private void executeTest(ICommandSender sender) {
        if (!FontManager.INSTANCE.isSkiaActive()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Skia engine not active."));
            return;
        }
        TextRenderBackend backend = FontManager.INSTANCE.getTextRenderBackend();
        if (backend == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Text render backend unavailable"));
            return;
        }

        String[] testCases = {
                "Hello",
                "😀",
                "Hello 😀 World",
                "❤️",
                "Test 😀 ❤️ 🎉"
        };

        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Emoji Render Test ==="));
        for (String test : testCases) {
            float width = backend.measure(test, false, false);
            TextRenderResult rendered = backend.render(test, 0xFFFFFFFF, false, false);
            boolean success = rendered.advance() > 0;
            sender.sendMessage(new TextComponentString(
                    TextFormatting.WHITE + "  \"" + test + "\""
                            + TextFormatting.AQUA + " width=" + String.format("%.1f", width)
                            + (success ? TextFormatting.GREEN + " OK" : TextFormatting.RED + " EMPTY")));
        }
    }

    private void executeGui(ICommandSender sender) {
        Minecraft.getMinecraft().addScheduledTask(() -> NeofontrenderEmojiTestScreen.open());
    }

    private void executeExport(ICommandSender sender, String[] args) {
        if (!FontManager.INSTANCE.isSkiaActive()) {
            sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Skia engine not active."));
            return;
        }
        SkijaTextRenderer renderer = FontManager.INSTANCE.getSkijaTextRenderer();
        if (renderer == null) {
            sender.sendMessage(new TextComponentString(TextFormatting.RED + "SkijaTextRenderer unavailable"));
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) sb.append(' ');
            sb.append(args[i]);
        }
        String text = sb.length() > 0 ? sb.toString() : "Hello World 你好世界";
        String finalText = text;

        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            StringBuilder log = new StringBuilder();
            try {
                File outputDir = new File(mc.gameDir, "neofontrender-export");
                if (!outputDir.isDirectory()) {
                    outputDir.mkdirs();
                }

                log.append("=== NeoFontRender Pipeline Diagnostic Report ===\n");
                log.append("Text: \"").append(finalText).append("\"\n\n");

                // === Config ===
                float fontSize = NeofontrenderConfig.fontSize();
                float oversample = NeofontrenderConfig.fontOversample();
                log.append("--- Config ---\n");
                log.append("fontSize: ").append(fontSize).append("\n");
                log.append("oversample: ").append(oversample).append("\n");
                log.append("antialiasMode: ").append(NeofontrenderConfig.fontAntialiasMode()).append("\n");
                log.append("premultipliedAlpha: ").append(NeofontrenderConfig.enablePremultipliedAlpha()).append("\n");
                log.append("textureEdgeBleed: ").append(NeofontrenderConfig.textureEdgeBleed()).append("\n");
                log.append("skiaGpuOffscreen: ").append(NeofontrenderConfig.skiaGpuOffscreen()).append("\n");
                log.append("skiaGpuSubmitViaCpuTexture: ").append(NeofontrenderConfig.skiaGpuSubmitViaCpuTexture()).append("\n");
                log.append("interpolation: ").append(NeofontrenderConfig.renderingInterpolation()).append("\n");
                log.append("mipmap: ").append(NeofontrenderConfig.renderingMipmap()).append("\n");
                log.append("adaptiveRasterScale: ").append(NeofontrenderConfig.adaptiveRasterScale()).append("\n");
                log.append("adaptiveRasterMin: ").append(NeofontrenderConfig.adaptiveRasterMin()).append("\n");
                log.append("adaptiveRasterMax: ").append(NeofontrenderConfig.adaptiveRasterMax()).append("\n");
                log.append("excludeIntegerScale: ").append(NeofontrenderConfig.excludeIntegerScale()).append("\n");
                log.append("excludeHighMagnification: ").append(NeofontrenderConfig.excludeHighMagnification()).append("\n");
                log.append("enhancedTextPipeline: ").append(NeofontrenderConfig.enhancedTextPipeline()).append("\n");
                log.append("shaderTextPipeline: ").append(NeofontrenderConfig.shaderTextPipeline()).append("\n");
                log.append("brightness: ").append(NeofontrenderConfig.renderingBrightness()).append("\n");
                log.append("brightnessAuto: ").append(NeofontrenderConfig.renderingBrightnessAuto()).append("\n");
                log.append("displaySize: ").append(mc.displayWidth).append("x").append(mc.displayHeight).append("\n");
                log.append("guiScale: ").append(mc.gameSettings.guiScale).append("\n\n");

                log.append("--- Part 0: CPU vs Isolated GPU Texture Export ---\n");
                try {
                    String textureReport = renderer.exportGpuDiagnostics(outputDir, finalText);
                    log.append(textureReport).append("\n");
                    log.append("  files: cpu_reference_texture.png, gpu_isolated_readback.png\n\n");
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN
                            + "CPU/GPU texture PNGs exported."));
                } catch (Throwable t) {
                    log.append("  texture export FAILED: ").append(t.getClass().getSimpleName())
                            .append(": ").append(t.getMessage()).append("\n\n");
                    sender.sendMessage(new TextComponentString(TextFormatting.RED
                            + "CPU/GPU texture export failed: " + t.getMessage()));
                }

                FontCollection fonts = renderer.getFontCollection();
                String[] families = renderer.getFontFamilies();
                int[] scales = {1, 2, 4, 8, 12, 16};

                // === Part 1: Raw Skia PNGs ===
                log.append("--- Part 1: Raw Skia PNGs ---\n");
                for (int scale : scales) {
                    try {
                        float effectiveFontSize = fontSize * scale;
                        int border = Math.max(4, (int) (scale * 8.0f / 16.0f) * 2);
                        border += border % 2;
                        border = Math.max(border, 4);

                        TextStyle style = new TextStyle()
                                .setColor(0xFFFFFFFF)
                                .setFontSize(effectiveFontSize)
                                .setFontFamilies(families)
                                .setHeight(1.0F);
                        ParagraphStyle ps = new ParagraphStyle();
                        ps.setTextStyle(style);

                        Paragraph paragraph;
                        try (ParagraphBuilder builder = new ParagraphBuilder(ps, fonts)) {
                            builder.pushStyle(style);
                            builder.addText(finalText);
                            paragraph = builder.build();
                        }

                        paragraph.layout(100000.0F);
                        int paraW = (int) Math.ceil(paragraph.getMaxIntrinsicWidth());
                        int paraH = (int) Math.ceil(paragraph.getHeight());
                        float ascent = effectiveFontSize * 0.8f;
                        float descent = effectiveFontSize * 0.2f;
                        int vOffset = (int) ((paraH - (ascent + descent)) / 2.0f);
                        if (vOffset < 0) vOffset = 0;

                        int width = Math.max(1, paraW + border * 2);
                        int height = Math.max(1, paraH + border * 2);

                        try (Surface surface = Surface.makeRasterN32Premul(width, height)) {
                            Canvas canvas = surface.getCanvas();
                            canvas.clear(0xFF000000);
                            paragraph.paint(canvas, border, border + vOffset);

                            try (Image image = surface.makeImageSnapshot();
                                 Data png = image.encodeToData(EncodedImageFormat.PNG)) {
                                String filename = String.format("raw_oversample_%02dx.png", scale);
                                java.nio.file.Files.write(new File(outputDir, filename).toPath(), png.getBytes());
                                log.append(String.format("  %s: %dx%dpx, fontSize=%d%n",
                                        filename, width, height, (int) effectiveFontSize));
                            }
                        }
                        paragraph.close();
                    } catch (Throwable t) {
                        log.append(String.format("  scale %dx FAILED: %s%n", scale, t.getMessage()));
                    }
                }

                // === Part 2: Multi-scenario screen captures ===
                log.append("\n--- Part 2: Screen Captures (multi-scenario) ---\n");
                FontRenderer fr = mc.fontRenderer;
                float baseGuiScale = mc.gameSettings.guiScale;
                if (baseGuiScale <= 0) baseGuiScale = mc.displayWidth / 640.0f;
                int baseScaleInt = Math.max(1, Math.round(baseGuiScale));

                // Define test scenarios: {name, guiScale, scaleMultiplier, blendMode, text}
                // blendMode: 0=default, 1=GL_SRC_ALPHA/GL_ONE_MINUS_SRC_ALPHA, 2=GL_ONE/GL_ONE_MINUS_SRC_ALPHA
                Object[][] scenarios = {
                        {"baseline",     baseScaleInt, 1.0f,  0, finalText},
                        {"scale_2x",     2,            1.0f,  0, finalText},
                        {"scale_4x",     4,            1.0f,  0, finalText},
                        {"small_text",   baseScaleInt, 0.5f,  0, finalText},
                        {"large_text",   baseScaleInt, 2.0f,  0, finalText},
                        {"inventory",    baseScaleInt, 1.0f,  1, finalText},
                        {"premult_blend",baseScaleInt, 1.0f,  2, finalText},
                };

                // Save current GL state
                boolean origBlend = GL11.glIsEnabled(GL11.GL_BLEND);
                int origSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
                int origDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);

                for (Object[] scenario : scenarios) {
                    String name = (String) scenario[0];
                    int scenarioScale = (int) scenario[1];
                    float textScale = (float) scenario[2];
                    int blendMode = (int) scenario[3];
                    String scenarioText = (String) scenario[4];

                    try {
                        // Set up GL state for this scenario
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(textScale, textScale, 1.0f);

                        if (blendMode == 1) {
                            GL11.glEnable(GL11.GL_BLEND);
                            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                        } else if (blendMode == 2) {
                            GL11.glEnable(GL11.GL_BLEND);
                            GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                        }

                        // Draw text
                        int screenX = 5, screenY = 5;
                        int textWidth = fr.drawString(text, screenX, screenY, 0xFFFFFF);

                        // Capture GL state during/after draw
                        int texMinFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
                        int texMagFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
                        boolean blendNow = GL11.glIsEnabled(GL11.GL_BLEND);
                        int curProgram = GL11.glGetInteger(0x8B8D);

                        // Update draw context for this scale
                        neofontrender.core.font.support.FontRenderTuning.DrawContext ctx =
                                neofontrender.core.font.support.FontRenderTuning.currentDrawContext();
                        float rasterScale = neofontrender.core.font.support.FontRenderTuning.rasterScale(oversample);
                        boolean linear = neofontrender.core.font.support.FontRenderTuning.useLinearFiltering(rasterScale);
                        float screenScale = ctx.pixelScale();
                        float ratio = screenScale / Math.max(1.0F, rasterScale);

                        // Capture framebuffer region
                        int scaledW = (int)((textWidth + 20) * textScale);
                        int scaledH = (int)((fr.FONT_HEIGHT + 6) * textScale);
                        int fbX = (int)(screenX * textScale * baseScaleInt);
                        int fbY = (mc.displayHeight - 1) - (int)((screenY + scaledH / textScale) * textScale * baseScaleInt);
                        int fbW = Math.min(mc.displayWidth, Math.max(scaledW, 100) * baseScaleInt);
                        int fbH = Math.min(scaledH * baseScaleInt, mc.displayHeight);
                        fbY = Math.max(0, Math.min(fbY, mc.displayHeight - fbH));

                        if (fbW > 0 && fbH > 0) {
                            IntBuffer buf = BufferUtils.createIntBuffer(fbW * fbH);
                            GL11.glReadPixels(fbX, fbY, fbW, fbH, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buf);
                            int[] pixels = new int[fbW * fbH];
                            buf.get(pixels);
                            buf.flip();

                            BufferedImage img = new BufferedImage(fbW, fbH, BufferedImage.TYPE_INT_ARGB);
                            for (int y = 0; y < fbH; y++) {
                                for (int y2 = 0; y2 < fbH; y2++) {
                                    int glY = fbH - 1 - y;
                                    int px = pixels[glY * fbW + y2];
                                    int a = (px >>> 24) & 0xFF;
                                    int r = (px >> 16) & 0xFF;
                                    int g = (px >> 8) & 0xFF;
                                    int b = px & 0xFF;
                                    img.setRGB(y2, y, (a << 24) | (r << 16) | (g << 8) | b);
                                }
                            }
                            String filename = "screen_" + name + ".png";
                            ImageIO.write(img, "PNG", new File(outputDir, filename));

                            String blendStr = blendNow ? glBlendFuncName(GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB))
                                    + "/" + glBlendFuncName(GL11.glGetInteger(GL14.GL_BLEND_DST_RGB)) : "off";
                            log.append(String.format("  %s: %dx%dpx filter=%s blend=%s program=%d linear=%b screen=%.1f raster=%.1f ratio=%.3f%n",
                                    filename, fbW, fbH,
                                    glFilterName(texMinFilter),
                                    blendStr,
                                    curProgram,
                                    linear,
                                    screenScale, rasterScale, ratio));
                        }

                        // Restore GL state
                        GlStateManager.popMatrix();
                        if (blendMode != 0) {
                            if (!origBlend) GL11.glDisable(GL11.GL_BLEND);
                            GL14.glBlendFuncSeparate(origSrcRgb, origDstRgb, origSrcRgb, origDstRgb);
                        }
                    } catch (Throwable t) {
                        log.append("  ").append(name).append(" FAILED: ").append(t.getMessage()).append("\n");
                    }
                }

                // === Part 3: GL state diagnostics ===
                log.append("\n--- Part 3: GL State ---\n");
                try {
                    int texMinFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
                    int texMagFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
                    int texWrapS = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
                    int texWrapT = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);
                    boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
                    int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
                    int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
                    int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
                    int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
                    int currentProgram = GL11.glGetInteger(0x8B8D);
                    IntBuffer viewportBuf = BufferUtils.createIntBuffer(4);
                    GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuf);
                    int[] viewport = new int[4];
                    viewportBuf.get(viewport);
                    int boundTex = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

                    log.append("  MIN_FILTER: ").append(glFilterName(texMinFilter)).append(" (").append(texMinFilter).append(")\n");
                    log.append("  MAG_FILTER: ").append(glFilterName(texMagFilter)).append(" (").append(texMagFilter).append(")\n");
                    log.append("  WRAP_S/T: ").append(texWrapS).append(" / ").append(texWrapT).append("\n");
                    log.append("  BOUND_TEX: ").append(boundTex).append("\n");
                    log.append("  blendEnabled: ").append(blendEnabled).append("\n");
                    log.append("  srcRgb/dstRgb: ").append(glBlendFuncName(srcRgb)).append(" / ").append(glBlendFuncName(dstRgb)).append("\n");
                    log.append("  srcAlpha/dstAlpha: ").append(glBlendFuncName(srcAlpha)).append(" / ").append(glBlendFuncName(dstAlpha)).append("\n");
                    log.append("  currentProgram: ").append(currentProgram);
                    log.append(currentProgram == 0 ? " (fixed-function)\n" : " (shader active)\n");
                    log.append("  viewport: ").append(viewport[0]).append(",").append(viewport[1])
                            .append(" ").append(viewport[2]).append("x").append(viewport[3]).append("\n");

                    // Chat: GL state summary
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== GL State ==="));
                    sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Filter: " + TextFormatting.GREEN
                            + glFilterName(texMinFilter) + " / " + glFilterName(texMagFilter)));
                    sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Blend: " + blendEnabled
                            + " " + glBlendFuncName(srcRgb) + "/" + glBlendFuncName(dstRgb)));
                    sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  Shader: " + (currentProgram == 0 ? "none" : "active#" + currentProgram)));

                    // === Part 4: Filter decision analysis ===
                    log.append("\n--- Part 4: Filter Decision Analysis ---\n");
                    float rasterScale = neofontrender.core.font.support.FontRenderTuning.rasterScale(oversample);
                    boolean linearFilter = neofontrender.core.font.support.FontRenderTuning.useLinearFiltering(rasterScale);
                    neofontrender.core.font.support.FontRenderTuning.DrawContext ctx =
                            neofontrender.core.font.support.FontRenderTuning.currentDrawContext();
                    float screenScale = ctx.pixelScale();
                    float roundedScale = ctx.roundedPixelScale();
                    float ratio = screenScale / Math.max(1.0F, rasterScale);
                    float fontResolution = rasterScale * 8.0F;

                    log.append("  rasterScale: ").append(rasterScale).append("\n");
                    log.append("  screenPixelScale: ").append(screenScale).append("\n");
                    log.append("  roundedPixelScale: ").append(roundedScale).append("\n");
                    log.append("  fontResolution: ").append(fontResolution).append("\n");
                    log.append("  orthographic: ").append(ctx.orthographic()).append("\n");
                    log.append("  rotation: ").append(ctx.rotation()).append("\n");
                    log.append("  shadow: ").append(ctx.shadow()).append("\n");
                    log.append("  ratio (screen/raster): ").append(ratio).append("\n");
                    log.append("  1/ratio: ").append(ratio > 0 ? String.valueOf(1.0f / ratio) : "N/A").append("\n");
                    log.append("  useLinearFiltering(): ").append(linearFilter).append("\n");

                    // Walk through each exclusion condition
                    log.append("\n  --- Exclusion condition walk-through ---\n");
                    if (!NeofontrenderConfig.renderingInterpolation()) {
                        log.append("  [1] renderingInterpolation=false -> NEAREST (early return)\n");
                    } else if (!NeofontrenderConfig.adaptiveRasterScale()) {
                        log.append("  [1] adaptiveRasterScale=false -> LINEAR (early return)\n");
                    } else {
                        log.append("  [1] interpolation=true, adaptive=true -> continue checks\n");
                        if (ctx.shadow() && fontResolution < NeofontrenderConfig.smoothShadowThreshold()) {
                            log.append("  [2] shadow=true, fontRes(").append(fontResolution).append(") < threshold(")
                                    .append(NeofontrenderConfig.smoothShadowThreshold()).append(") -> NEAREST\n");
                        } else {
                            log.append("  [2] shadow check passed (not shadow or above threshold)\n");
                        }
                        if (NeofontrenderConfig.excludeHighMagnification()
                                && roundedScale >= rasterScale * NeofontrenderConfig.limitMagnification()) {
                            log.append("  [3] highMag exclusion: roundedScale(").append(roundedScale)
                                    .append(") >= rasterScale*limit(").append(rasterScale * NeofontrenderConfig.limitMagnification())
                                    .append(") -> NEAREST\n");
                        } else {
                            log.append("  [3] highMag check passed\n");
                        }
                        if (NeofontrenderConfig.excludeIntegerScale() && ctx.orthographic()
                                && Math.abs(roundedScale / Math.max(1.0F, rasterScale) - Math.round(roundedScale / Math.max(1.0F, rasterScale))) <= 0.03f) {
                            log.append("  [4] integerScale exclusion: roundedScale/rasterScale=")
                                    .append(roundedScale / Math.max(1.0F, rasterScale)).append(" -> NEAREST\n");
                        } else {
                            log.append("  [4] integerScale check passed\n");
                        }
                        // [5][6] removed: old ratio-based exclusions were too aggressive.
                        // At screenScale=3, rasterScale=6: ratio=0.5, 1/ratio=2.0 (integer)
                        // triggered NEAREST even though this is a normal GUI scenario.
                        log.append("  [5-6] removed (old ratio exclusions) -> LINEAR\n");
                    }

                    log.append("\n  FINAL DECISION: ").append(linearFilter ? "GL_LINEAR" : "GL_NEAREST").append("\n");

                    // Chat: Filter decision summary
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Filter Decision ==="));
                    sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  rasterScale=" + rasterScale
                            + " screenScale=" + screenScale + " ratio=" + String.format("%.3f", ratio)
                            + " 1/ratio=" + (ratio > 0 ? String.format("%.3f", 1.0f / ratio) : "N/A")));
                    TextFormatting filterColor = linearFilter ? TextFormatting.GREEN : TextFormatting.RED;
                    sender.sendMessage(new TextComponentString(TextFormatting.WHITE + "  useLinearFiltering(): "
                            + filterColor + linearFilter));
                    if (!linearFilter) {
                        // Find which exclusion triggered
                        String reason = "unknown";
                        if (!NeofontrenderConfig.renderingInterpolation()) reason = "interpolation=false";
                        else if (!NeofontrenderConfig.adaptiveRasterScale()) reason = "adaptive=false";
                        else if (ctx.shadow() && fontResolution < NeofontrenderConfig.smoothShadowThreshold())
                            reason = "shadow below threshold";
                        else if (NeofontrenderConfig.excludeHighMagnification()
                                && roundedScale >= rasterScale * NeofontrenderConfig.limitMagnification())
                            reason = "high magnification";
                        else if (NeofontrenderConfig.excludeIntegerScale() && ctx.orthographic()
                                && Math.abs(roundedScale / Math.max(1.0F, rasterScale) - Math.round(roundedScale / Math.max(1.0F, rasterScale))) <= 0.03f)
                            reason = "integer pixel scale";
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "  >> CAUSE: " + reason));
                    }

                } catch (Throwable t) {
                    log.append("  GL diagnostics FAILED: ").append(t.getMessage()).append("\n");
                }

                // Write report file
                File reportFile = new File(outputDir, "report.txt");
                java.nio.file.Files.write(reportFile.toPath(), log.toString().getBytes("UTF-8"));

                sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Export complete! " + outputDir.getAbsolutePath()));
                sender.sendMessage(new TextComponentString(TextFormatting.GRAY + "  report.txt + *.png written"));
            } catch (Throwable t) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "Export failed: " + t.getMessage()));
                // Still write partial report
                try {
                    java.nio.file.Files.write(
                            new File(mc.gameDir, "neofontrender-export/report.txt").toPath(),
                            ("FAILED: " + t.getMessage() + "\n\n" + log).getBytes("UTF-8"));
                } catch (Exception ignored) {}
            }
        });
    }

    private static String glFilterName(int filter) {
        switch (filter) {
            case GL11.GL_NEAREST: return "GL_NEAREST";
            case GL11.GL_LINEAR: return "GL_LINEAR";
            case GL11.GL_NEAREST_MIPMAP_NEAREST: return "GL_NEAREST_MIPMAP_NEAREST";
            case GL11.GL_LINEAR_MIPMAP_NEAREST: return "GL_LINEAR_MIPMAP_NEAREST";
            case GL11.GL_NEAREST_MIPMAP_LINEAR: return "GL_NEAREST_MIPMAP_LINEAR";
            case GL11.GL_LINEAR_MIPMAP_LINEAR: return "GL_LINEAR_MIPMAP_LINEAR";
            default: return "0x" + Integer.toHexString(filter);
        }
    }

    private static String glBlendFuncName(int func) {
        switch (func) {
            case GL11.GL_ZERO: return "GL_ZERO";
            case GL11.GL_ONE: return "GL_ONE";
            case GL11.GL_SRC_COLOR: return "GL_SRC_COLOR";
            case GL11.GL_ONE_MINUS_SRC_COLOR: return "GL_ONE_MINUS_SRC_COLOR";
            case GL11.GL_SRC_ALPHA: return "GL_SRC_ALPHA";
            case GL11.GL_ONE_MINUS_SRC_ALPHA: return "GL_ONE_MINUS_SRC_ALPHA";
            case GL11.GL_DST_ALPHA: return "GL_DST_ALPHA";
            case GL11.GL_ONE_MINUS_DST_ALPHA: return "GL_ONE_MINUS_DST_ALPHA";
            case GL11.GL_DST_COLOR: return "GL_DST_COLOR";
            case GL11.GL_ONE_MINUS_DST_COLOR: return "GL_ONE_MINUS_DST_COLOR";
            default: return "0x" + Integer.toHexString(func);
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if ("fonts".startsWith(prefix)) completions.add("fonts");
            if ("info".startsWith(prefix)) completions.add("info");
            if ("reload".startsWith(prefix)) completions.add("reload");
            if ("export".startsWith(prefix)) completions.add("export");
            return completions;
        }
        return Collections.emptyList();
    }
}
