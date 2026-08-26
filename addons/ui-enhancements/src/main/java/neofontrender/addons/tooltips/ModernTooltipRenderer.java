package neofontrender.addons.tooltips;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.api.text.CjkParagraphLayoutProvider;

final class ModernTooltipRenderer {
    private static final int Z_LEVEL = 300;

    boolean draw(RenderTooltipEvent.Pre event, boolean[] compactLines, String profileId,
                 ThaumcraftTooltipCompat.Context thaumcraftContext) {
        if (event.getLines().isEmpty()) return false;
        MicaBackdrop.captureUiIfEnabled();
        TooltipLayout layout = TooltipLayout.calculate(event, compactLines,
                TooltipConfig.profile(profileId), thaumcraftContext);
        if (layout.lines.isEmpty()) return false;

        int[] fill = TooltipConfig.fillColors.clone();
        int[] border = TooltipConfig.borderColors.clone();
        boolean spectrum = false;
        if (TooltipConfig.adaptiveBorder) {
            // Match ModernUI: inspect the stack's hover/display name itself. Forge 1.12 prefixes
            // the rendered first line with WHITE even for COMMON items, which would otherwise
            // make every ordinary item produce an artificial white adaptive palette.
            String title = event.getStack().isEmpty() ? "" : event.getStack().getDisplayName();
            AdaptiveBorderColors.Result adaptive = AdaptiveBorderColors.compute(event.getStack(), title, border);
            border = adaptive.colors;
            spectrum = adaptive.spectrum;
        }
        spectrum |= "spectrum".equals(TooltipConfig.borderShading);
        applyBorderShading(border, TooltipConfig.borderShading);

        RenderTooltipEvent.Color colorEvent = new RenderTooltipEvent.Color(
                event.getStack(), layout.lines, layout.x, layout.y, event.getFontRenderer(),
                fill[0], border[0], border[2]);
        MinecraftForge.EVENT_BUS.post(colorEvent);
        if (colorEvent.getBackground() != fill[0]) {
            for (int i = 0; i < fill.length; i++) fill[i] = colorEvent.getBackground();
        }
        if (colorEvent.getBorderStart() != border[0] || colorEvent.getBorderEnd() != border[2]) {
            border[0] = border[1] = colorEvent.getBorderStart();
            border[2] = border[3] = colorEvent.getBorderEnd();
        }

        drawBackground(layout, fill, border, spectrum);
        beginTooltipExtensions();
        try {
            MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostBackground(
                    event.getStack(), layout.lines, layout.x, layout.y, event.getFontRenderer(),
                    layout.width, layout.height));
            drawContent(layout.x, layout.y, layout.width, layout.lines, layout.compactLines,
                    layout.titleLines, layout.profile(), event.getFontRenderer(), event.getStack(),
                    thaumcraftContext != null, layout.lineWidths);
            MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostText(
                    event.getStack(), layout.lines, layout.x, layout.y, event.getFontRenderer(),
                    layout.width, layout.height));
        } finally {
            endTooltipExtensions();
        }
        return true;
    }

    /** Matches Forge GuiUtils' GL contract while PostBackground/PostText subscribers render. */
    private static void beginTooltipExtensions() {
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

    private static void endTooltipExtensions() {
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
    }

    private static void drawBackground(TooltipLayout layout, int[] fill, int[] border, boolean spectrum) {
        float left = layout.x - TooltipConfig.horizontalPadding;
        float top = layout.y - TooltipConfig.verticalPadding;
        float right = layout.x + layout.width + TooltipConfig.horizontalPadding;
        float bottom = layout.y + layout.height + TooltipConfig.verticalPadding;
        float radius = TooltipConfig.rounded ? TooltipConfig.cornerRadius : 0.01F;
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        // Inventory and several modded screens leave face culling enabled. The rounded fill is a
        // triangle fan in GUI (Y-down) coordinates, whose winding is otherwise culled while the
        // alternating border strip can still remain partially visible.
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        try {
            boolean analyticStyle = "modernui".equals(TooltipConfig.renderStyle)
                    || "mica".equals(TooltipConfig.renderStyle);
            boolean shaderDrawn = analyticStyle
                    && (TooltipConfig.rounded || "mica".equals(TooltipConfig.renderStyle))
                    && ModernUiTooltipShader.draw(left, top, right, bottom, radius, fill, border,
                    spectrum, "mica".equals(TooltipConfig.renderStyle));
            if (!shaderDrawn) {
                if (spectrum) applyFallbackSpectrum(border);
                drawShadow(left, top, right, bottom, radius);
                drawRoundedFill(left, top, right, bottom, radius, fill);
                drawRoundedBorder(left, top, right, bottom, radius,
                        Math.min(TooltipConfig.borderWidth, Math.max(0.5F, radius)), border);
            }

        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            if (cullEnabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            RenderHelper.enableGUIStandardItemLighting();
        }
    }

    /** Draws only NFR's panel/frame around a foreign renderer's already-computed bounds. */
    static void drawCompatibleBackground(int x, int y, int width, int height, ItemStack stack) {
        MicaBackdrop.captureUiIfEnabled();
        int[] fill = TooltipConfig.fillColors.clone();
        int[] border = TooltipConfig.borderColors.clone();
        boolean spectrum = false;
        if (TooltipConfig.adaptiveBorder && stack != null && !stack.isEmpty()) {
            AdaptiveBorderColors.Result adaptive = AdaptiveBorderColors.compute(stack, stack.getDisplayName(), border);
            border = adaptive.colors;
            spectrum = adaptive.spectrum;
        }
        spectrum |= "spectrum".equals(TooltipConfig.borderShading);
        applyBorderShading(border, TooltipConfig.borderShading);

        float left = x;
        float top = y;
        float right = x + width;
        float bottom = y + height;
        float radius = TooltipConfig.rounded ? TooltipConfig.cornerRadius : 0.01F;
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            boolean analyticStyle = "modernui".equals(TooltipConfig.renderStyle)
                    || "mica".equals(TooltipConfig.renderStyle);
            boolean shaderDrawn = analyticStyle
                    && (TooltipConfig.rounded || "mica".equals(TooltipConfig.renderStyle))
                    && ModernUiTooltipShader.draw(left, top, right, bottom, radius, fill, border,
                    spectrum, "mica".equals(TooltipConfig.renderStyle));
            if (!shaderDrawn) {
                if (spectrum) applyFallbackSpectrum(border);
                drawShadow(left, top, right, bottom, radius);
                drawRoundedFill(left, top, right, bottom, radius, fill);
                drawRoundedBorder(left, top, right, bottom, radius,
                        Math.min(TooltipConfig.borderWidth, Math.max(0.5F, radius)), border);
            }
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            if (cullEnabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            RenderHelper.enableGUIStandardItemLighting();
        }
    }

    /** Draws NFR's title divider inside a foreign tooltip while preserving its content renderer. */
    static void drawCompatibleDivider(int x, int y, int width, ItemStack stack) {
        if (!TooltipConfig.titleBreak || width <= 0) return;

        int[] border = TooltipConfig.borderColors.clone();
        boolean spectrum = false;
        if (TooltipConfig.adaptiveBorder && stack != null && !stack.isEmpty()) {
            AdaptiveBorderColors.Result adaptive = AdaptiveBorderColors.compute(stack, stack.getDisplayName(), border);
            border = adaptive.colors;
            spectrum = adaptive.spectrum;
        }
        spectrum |= "spectrum".equals(TooltipConfig.borderShading);
        applyBorderShading(border, TooltipConfig.borderShading);
        if (spectrum) applyFallbackSpectrum(border);

        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableTexture2D();
        // GUI quads use a Y-down winding. If a screen left face culling enabled,
        // the divider is discarded even though the surrounding panel is visible.
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            drawQuad(x, y, x + width, y + 1.0F,
                    withAlpha(border[3], TooltipConfig.dividerAlpha),
                    withAlpha(border[2], TooltipConfig.dividerAlpha),
                    withAlpha(border[2], TooltipConfig.dividerAlpha),
                    withAlpha(border[3], TooltipConfig.dividerAlpha));
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            if (cullEnabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
        }
    }

    static void drawContent(int x, int y, int width, List<String> lines,
                            List<Boolean> compactLines, int titleLines,
                            TooltipConfig.Profile profile, FontRenderer font, ItemStack stack,
                            boolean lineBreaksAlreadyApplied, List<Integer> measuredLineWidths) {
        TooltipConfig.Profile activeProfile = profile == null ? TooltipConfig.profile("vanilla") : profile;
        int textY = y;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean compact = compactLines != null && i < compactLines.size() && compactLines.get(i);
            int lineTitleCount = Math.max(0, Math.min(titleLines, lines.size()));
            int lineX = x;
            float textScale = activeProfile.textScale * (compact ? 0.5F : 1.0F);
            int paragraphWidth = lineBreaksAlreadyApplied ? 1_000_000
                    : Math.max(1, compact ? Math.round(width * 2.0F / activeProfile.textScale)
                            : Math.round(width / activeProfile.textScale));
            CjkParagraphLayoutProvider.Layout paragraph = CjkTypographyRenderer.layout(
                    font, line, paragraphWidth,
                    compact ? ThaumcraftTooltipCompat.COMPACT_LINE_HEIGHT * 2 : TooltipConfig.lineHeight);
            int renderedWidth = lineBreaksAlreadyApplied && measuredLineWidths != null
                    && i < measuredLineWidths.size()
                    ? measuredLineWidths.get(i)
                    : lineBreaksAlreadyApplied
                    ? TooltipLayout.measuredLineWidth(font, line, compact, activeProfile.textScale)
                    : paragraph == null ? font.getStringWidth(line)
                    : CjkTypographyRenderer.measuredWidth(font, paragraph);
            if (!lineBreaksAlreadyApplied && compact) renderedWidth = (renderedWidth + 1) / 2;
            if (!lineBreaksAlreadyApplied) {
                renderedWidth = Math.max(1, Math.round(renderedWidth * activeProfile.textScale));
            }
            if (TooltipConfig.centerTitle && i < lineTitleCount) {
                lineX += Math.max(0, (width - renderedWidth) / 2);
            }
            int color = i < lineTitleCount ? TooltipConfig.titleColor : TooltipConfig.textColor;
            if (textScale != 1.0F || activeProfile.offsetX != 0.0F || activeProfile.offsetY != 0.0F) {
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.scale(textScale, textScale, 1.0F);
                    float scaledX = (lineX + activeProfile.offsetX) / textScale;
                    float scaledY = (textY + activeProfile.offsetY) / textScale;
                    if (!CjkTypographyRenderer.draw(font, paragraph, scaledX, scaledY,
                            color, TooltipConfig.textShadow)) {
                        if (TooltipConfig.textShadow) font.drawStringWithShadow(line, Math.round(scaledX), Math.round(scaledY), color);
                        else font.drawString(line, Math.round(scaledX), Math.round(scaledY), color);
                    }
                } finally {
                    GlStateManager.popMatrix();
                }
            } else if (!CjkTypographyRenderer.draw(font, paragraph, lineX, textY, color, TooltipConfig.textShadow)) {
                if (TooltipConfig.textShadow) font.drawStringWithShadow(line, lineX, textY, color);
                else font.drawString(line, lineX, textY, color);
            }
            if (i + 1 == lineTitleCount && lines.size() > lineTitleCount) {
                if (TooltipConfig.titleBreak) {
                    int dividerY = Math.round(textY + Math.max(1, Math.round(
                            (compact ? ThaumcraftTooltipCompat.COMPACT_LINE_HEIGHT
                                    : TooltipConfig.lineHeight) * activeProfile.textScale)) - 1.5F);
                    drawCompatibleDivider(x, dividerY, width, stack);
                }
                textY += TooltipConfig.titleGap;
            }
            textY += Math.max(1, Math.round((compact ? ThaumcraftTooltipCompat.COMPACT_LINE_HEIGHT
                    : TooltipConfig.lineHeight) * activeProfile.textScale));
        }
    }

    private static void applyBorderShading(int[] colors, String mode) {
        if ("solid".equals(mode)) {
            colors[1] = colors[2] = colors[3] = colors[0];
        } else if ("horizontal".equals(mode)) {
            colors[3] = colors[0];
            colors[2] = colors[1];
        } else if ("vertical".equals(mode)) {
            colors[1] = colors[0];
            colors[3] = colors[2];
        }
    }

    private static void applyFallbackSpectrum(int[] colors) {
        int alpha = Color.alpha(colors[0]);
        colors[0] = withAlpha(0xFFFF5555, alpha);
        colors[1] = withAlpha(0xFFFFFF55, alpha);
        colors[2] = withAlpha(0xFF55FFFF, alpha);
        colors[3] = withAlpha(0xFFFF55FF, alpha);
    }

    private static void drawRoundedFill(float left, float top, float right, float bottom,
                                        float radius, int[] colors) {
        List<Point> points = perimeter(left, top, right, bottom, radius);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        float centerX = (left + right) * 0.5F;
        float centerY = (top + bottom) * 0.5F;
        vertex(buffer, centerX, centerY, colorAt(colors, 0.5F, 0.5F));
        for (Point point : points) {
            float tx = normalize(point.x, left, right);
            float ty = normalize(point.y, top, bottom);
            vertex(buffer, point.x, point.y, colorAt(colors, tx, ty));
        }
        Point first = points.get(0);
        vertex(buffer, first.x, first.y, colorAt(colors,
                normalize(first.x, left, right), normalize(first.y, top, bottom)));
        tessellator.draw();
    }

    private static void drawRoundedBorder(float left, float top, float right, float bottom,
                                          float radius, float width, int[] colors) {
        List<Point> outer = perimeter(left, top, right, bottom, radius);
        List<Point> inner = perimeter(left + width, top + width, right - width, bottom - width,
                Math.max(0.01F, radius - width));
        drawStrip(outer, inner, left, top, right, bottom, colors, false);

        float aa = TooltipConfig.antialiasWidth;
        if (aa > 0.0F) {
            List<Point> fringe = perimeter(left - aa, top - aa, right + aa, bottom + aa, radius + aa);
            drawStrip(fringe, outer, left, top, right, bottom, colors, true);
        }
    }

    /** Draws non-overlapping rings whose vertex alpha follows a continuous Gaussian falloff. */
    private static void drawShadow(float left, float top, float right, float bottom, float radius) {
        float extent = TooltipConfig.shadowRadius;
        if (extent <= 0.0F || TooltipConfig.shadowAlpha <= 0) return;
        int steps = Math.max(2, TooltipConfig.shadowSteps);
        float offsetX = TooltipConfig.shadowOffsetX;
        float offsetY = TooltipConfig.shadowOffsetY;
        for (int i = steps - 1; i >= 0; i--) {
            float innerDistance = extent * i / steps;
            float outerDistance = extent * (i + 1) / steps;
            List<Point> inner = perimeter(left + offsetX - innerDistance,
                    top + offsetY - innerDistance, right + offsetX + innerDistance,
                    bottom + offsetY + innerDistance, radius + innerDistance);
            List<Point> outer = perimeter(left + offsetX - outerDistance,
                    top + offsetY - outerDistance, right + offsetX + outerDistance,
                    bottom + offsetY + outerDistance, radius + outerDistance);
            int innerAlpha = shadowAlpha(innerDistance, extent);
            int outerAlpha = shadowAlpha(outerDistance, extent);
            drawSolidStrip(outer, inner, withAlpha(TooltipConfig.shadowColor, outerAlpha),
                    withAlpha(TooltipConfig.shadowColor, innerAlpha));
        }
    }

    private static int shadowAlpha(float distance, float extent) {
        float normalized = distance / Math.max(0.001F, extent);
        return Math.round(TooltipConfig.shadowAlpha * (float) Math.exp(-3.0F * normalized * normalized));
    }

    private static void drawSolidStrip(List<Point> outer, List<Point> inner, int outerColor, int innerColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int count = Math.min(outer.size(), inner.size());
        for (int i = 0; i <= count; i++) {
            Point out = outer.get(i % count);
            Point in = inner.get(i % count);
            vertex(buffer, out.x, out.y, outerColor);
            vertex(buffer, in.x, in.y, innerColor);
        }
        tessellator.draw();
    }

    private static void drawStrip(List<Point> outer, List<Point> inner,
                                  float left, float top, float right, float bottom,
                                  int[] colors, boolean transparentOuter) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int count = Math.min(outer.size(), inner.size());
        for (int i = 0; i <= count; i++) {
            Point out = outer.get(i % count);
            Point in = inner.get(i % count);
            int outColor = colorAt(colors, normalize(out.x, left, right), normalize(out.y, top, bottom));
            if (transparentOuter) outColor = withAlpha(outColor, 0);
            int inColor = colorAt(colors, normalize(in.x, left, right), normalize(in.y, top, bottom));
            vertex(buffer, out.x, out.y, outColor);
            vertex(buffer, in.x, in.y, inColor);
        }
        tessellator.draw();
    }

    private static List<Point> perimeter(float left, float top, float right, float bottom, float requestedRadius) {
        float radius = Math.max(0.01F, Math.min(requestedRadius,
                Math.min((right - left) * 0.5F, (bottom - top) * 0.5F)));
        int segments = Math.max(3, TooltipConfig.cornerSegments);
        List<Point> points = new ArrayList<>(segments * 4 + 4);
        appendCorner(points, right - radius, top + radius, radius, -90.0F, 0.0F);
        appendCorner(points, right - radius, bottom - radius, radius, 0.0F, 90.0F);
        appendCorner(points, left + radius, bottom - radius, radius, 90.0F, 180.0F);
        appendCorner(points, left + radius, top + radius, radius, 180.0F, 270.0F);
        return points;
    }

    private static void appendCorner(List<Point> points, float centerX, float centerY,
                                     float radius, float startDegrees, float endDegrees) {
        int segments = Math.max(3, TooltipConfig.cornerSegments);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(MathUtil.lerp(startDegrees, endDegrees,
                    i / (float) segments));
            points.add(new Point(centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius));
        }
    }

    private static void drawQuad(float left, float top, float right, float bottom,
                                 int upperLeft, int upperRight, int lowerRight, int lowerLeft) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, upperLeft);
        vertex(buffer, right, top, upperRight);
        vertex(buffer, right, bottom, lowerRight);
        vertex(buffer, left, bottom, lowerLeft);
        tessellator.draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, Z_LEVEL).color(
                Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)).endVertex();
    }

    private static int colorAt(int[] colors, float x, float y) {
        return lerpColor(lerpColor(colors[0], colors[1], x), lerpColor(colors[3], colors[2], x), y);
    }

    private static int lerpColor(int from, int to, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        return (Math.round(MathUtil.lerp(Color.alpha(from), Color.alpha(to), amount)) << 24)
                | (Math.round(MathUtil.lerp(Color.red(from), Color.red(to), amount)) << 16)
                | (Math.round(MathUtil.lerp(Color.green(from), Color.green(to), amount)) << 8)
                | Math.round(MathUtil.lerp(Color.blue(from), Color.blue(to), amount));
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static float normalize(float value, float start, float end) {
        if (end <= start) return 0.5F;
        return Math.max(0.0F, Math.min(1.0F, (value - start) / (end - start)));
    }

    private static final class Point {
        final float x;
        final float y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
