package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import neofontrender.addons.cjk.CjkTypographyRenderer;
import neofontrender.api.text.paragraph.TextParagraphProvider;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** CPU-tessellated rounded tooltip renderer compatible with the 1.7.10 OpenGL pipeline. */
final class ModernTooltipRenderer {
    boolean draw(
            List<String> source, ItemStack stack, int mouseX, int mouseY,
            int screenWidth, int screenHeight, FontRenderer font) {
        if (source == null || source.isEmpty() || font == null) return false;
        // 1.7.10 has no RenderTooltipEvent, so the standalone Item Zoom surface is driven from the
        // renderer that every tooltip path shares instead of from a Forge tooltip pre-event.
        ItemZoomOverlay.beforeTooltip(stack, mouseX, mouseY);
        TooltipLayout layout = TooltipLayout.calculate(source, mouseX, mouseY, screenWidth, screenHeight, font);
        AdaptiveBorderColors.Result adaptive = TooltipConfig.adaptiveBorder
                ? AdaptiveBorderColors.compute(stack, source.get(0), TooltipConfig.borderColors)
                : AdaptiveBorderColors.Result.unchanged(TooltipConfig.borderColors);
        int[] border = effectiveBorder(adaptive);

        float left = layout.x - TooltipConfig.horizontalPadding;
        float top = layout.y - TooltipConfig.verticalPadding;
        float right = layout.x + layout.width + TooltipConfig.horizontalPadding;
        float bottom = layout.y + layout.height + TooltipConfig.verticalPadding;
        float radius = TooltipConfig.rounded ? TooltipConfig.cornerRadius : 0.0F;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 300.0F);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        drawBackground(left, top, right, bottom, radius, border);

        if (TooltipConfig.titleBreak && layout.lines.size() > layout.titleLines) {
            float dividerY = layout.y + layout.titleLines * TooltipConfig.lineHeight + TooltipConfig.titleGap / 2.0F;
            int[] divider = border.clone();
            for (int index = 0; index < divider.length; index++) {
                divider[index] = divider[index] & 0x00FFFFFF | TooltipConfig.dividerAlpha << 24;
            }
            drawGradientQuad(layout.x, dividerY, layout.x + layout.width, dividerY + 1.0F, divider);
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        int textY = layout.y;
        for (int index = 0; index < layout.lines.size(); index++) {
            String line = layout.lines.get(index);
            int color = index < layout.titleLines ? TooltipConfig.titleColor : TooltipConfig.textColor;
            int textX = layout.x;
            TextParagraphProvider.Layout paragraph = CjkTypographyRenderer.layout(
                    font, line, Math.max(1, layout.width), TooltipConfig.lineHeight);
            int renderedWidth = paragraph == null ? font.getStringWidth(line)
                    : CjkTypographyRenderer.measuredWidth(font, paragraph);
            if (TooltipConfig.centerTitle && index < layout.titleLines) {
                textX += Math.max(0, (layout.width - renderedWidth) / 2);
            }
            if (!CjkTypographyRenderer.draw(font, paragraph, textX, textY,
                    color, TooltipConfig.textShadow)) {
                if (TooltipConfig.textShadow) font.drawStringWithShadow(line, textX, textY, color);
                else font.drawString(line, textX, textY, color);
            }
            textY += TooltipConfig.lineHeight;
            if (index + 1 == layout.titleLines && layout.lines.size() > layout.titleLines) {
                textY += TooltipConfig.titleGap;
            }
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
        return true;
    }

    static void drawCompatibleBackground(int x, int y, int width, int height, ItemStack stack) {
        drawCompatibleBackground(x, y, width, height, stack, "");
    }

    private static void drawCompatibleBackground(
            int x, int y, int width, int height, ItemStack stack, String title) {
        AdaptiveBorderColors.Result adaptive = TooltipConfig.adaptiveBorder
                ? AdaptiveBorderColors.compute(stack, title, TooltipConfig.borderColors)
                : AdaptiveBorderColors.Result.unchanged(TooltipConfig.borderColors);
        int[] border = effectiveBorder(adaptive);
        float left = x;
        float top = y;
        float right = x + width;
        float bottom = y + height;
        float radius = TooltipConfig.rounded ? TooltipConfig.cornerRadius : 0.0F;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 300.0F);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        drawBackground(left, top, right, bottom, radius, border);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    static void drawCompatibleTextPage(
            List<String> lines, int x, int y, int width, int height, int[] lineHeights,
            boolean hasTitle, boolean paged, ItemStack stack, FontRenderer font) {
        String adaptiveTitle = hasTitle ? lines.get(0) : "";
        drawCompatibleBackground(x - 4, y - 4, width + 7, height + 7, stack, adaptiveTitle);
        if (hasTitle && TooltipConfig.titleBreak && (lines.size() > 1 || paged)) {
            drawCompatibleDivider(x, y + Math.max(1, lineHeights[0] - 2), width, stack);
        }

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        int textY = y;
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            boolean titleLine = hasTitle && index == 0;
            int color = titleLine ? TooltipConfig.titleColor : TooltipConfig.textColor;
            int textX = titleLine && TooltipConfig.centerTitle
                    ? x + (width - font.getStringWidth(line)) / 2 : x;
            if (TooltipConfig.textShadow) font.drawStringWithShadow(line, textX, textY, color);
            else font.drawString(line, textX, textY, color);
            textY += lineHeights[index];
        }
        GL11.glPopAttrib();
    }

    /** Draws NFR's title divider inside a foreign tooltip while preserving its content renderer. */
    static void drawCompatibleDivider(int x, int y, int width, ItemStack stack) {
        if (!TooltipConfig.titleBreak || width <= 0) return;
        AdaptiveBorderColors.Result adaptive = TooltipConfig.adaptiveBorder && stack != null && stack.stackSize > 0
                ? AdaptiveBorderColors.compute(stack, stack.getDisplayName(), TooltipConfig.borderColors)
                : AdaptiveBorderColors.Result.unchanged(TooltipConfig.borderColors);
        int[] border = effectiveBorder(adaptive);
        int leftColor = border[3] & 0x00FFFFFF | TooltipConfig.dividerAlpha << 24;
        int rightColor = border[2] & 0x00FFFFFF | TooltipConfig.dividerAlpha << 24;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        drawGradientQuad(x, y, x + width, y + 1.0F,
                new int[] {leftColor, rightColor, rightColor, leftColor});
        GL11.glPopAttrib();
    }

    private static void drawBackground(float left, float top, float right, float bottom, float radius, int[] border) {
        boolean shaderStyle = !"legacy".equals(TooltipConfig.renderStyle);
        boolean mica = "mica".equals(TooltipConfig.renderStyle);
        boolean spectrum = "spectrum".equals(TooltipConfig.borderShading);
        if (shaderStyle && ModernUiTooltipShader.draw(
                left, top, right, bottom, radius, TooltipConfig.fillColors, border, spectrum, mica)) {
            return;
        }
        drawShadow(left, top, right, bottom, radius);
        drawRoundedFill(left, top, right, bottom, radius, border);
        float inset = TooltipConfig.borderWidth;
        drawRoundedFill(
                left + inset, top + inset, right - inset, bottom - inset,
                Math.max(0.0F, radius - inset), TooltipConfig.fillColors);
    }

    private static void drawShadow(float left, float top, float right, float bottom, float radius) {
        int steps = Math.max(1, TooltipConfig.shadowSteps);
        for (int step = steps; step >= 1; step--) {
            float progress = step / (float) steps;
            int alpha = Math.round(TooltipConfig.shadowAlpha * (1.0F - progress) * (1.0F - progress));
            int color = TooltipConfig.shadowColor & 0x00FFFFFF | alpha << 24;
            float spread = TooltipConfig.shadowRadius * progress;
            drawRoundedFill(
                    left - spread + TooltipConfig.shadowOffsetX,
                    top - spread + TooltipConfig.shadowOffsetY,
                    right + spread + TooltipConfig.shadowOffsetX,
                    bottom + spread + TooltipConfig.shadowOffsetY,
                    radius + spread, new int[] {color, color, color, color});
        }
    }

    /** Fills a rounded rectangle; also used by the tooltip header and the HEI scrollbar skin. */
    static void drawRoundedFill(
            float left, float top, float right, float bottom, float radius, int[] colors) {
        float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(right - left, bottom - top) / 2.0F));
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        color(average(colors));
        GL11.glVertex3f((left + right) / 2.0F, (top + bottom) / 2.0F, 0.0F);
        if (safeRadius == 0.0F) {
            corner(left, top, colors[0]);
            corner(right, top, colors[1]);
            corner(right, bottom, colors[2]);
            corner(left, bottom, colors[3]);
            corner(left, top, colors[0]);
        } else {
            arc(right - safeRadius, top + safeRadius, safeRadius, -90.0F, 0.0F, colors, left, top, right, bottom);
            arc(right - safeRadius, bottom - safeRadius, safeRadius, 0.0F, 90.0F, colors, left, top, right, bottom);
            arc(left + safeRadius, bottom - safeRadius, safeRadius, 90.0F, 180.0F, colors, left, top, right, bottom);
            arc(left + safeRadius, top + safeRadius, safeRadius, 180.0F, 270.0F, colors, left, top, right, bottom);
            arc(right - safeRadius, top + safeRadius, safeRadius, -90.0F, -90.0F, colors, left, top, right, bottom);
        }
        GL11.glEnd();
    }

    /**
     * Draws a ring of the same palette along the rounded outline. Foreign panels that carry their
     * own fill use this instead of the fill-plus-inset pair of {@link #drawBackground}.
     */
    static void drawRoundedBorder(float left, float top, float right, float bottom,
                                  float radius, float width, int[] colors) {
        List<Point> outer = perimeter(left, top, right, bottom, radius);
        List<Point> inner = perimeter(left + width, top + width, right - width, bottom - width,
                Math.max(0.01F, radius - width));
        drawStrip(outer, inner, left, top, right, bottom, colors, false);

        float antialias = TooltipConfig.antialiasWidth;
        if (antialias > 0.0F) {
            List<Point> fringe = perimeter(left - antialias, top - antialias,
                    right + antialias, bottom + antialias, radius + antialias);
            drawStrip(fringe, outer, left, top, right, bottom, colors, true);
        }
    }

    private static List<Point> perimeter(float left, float top, float right, float bottom,
                                         float requestedRadius) {
        float radius = Math.max(0.01F, Math.min(requestedRadius,
                Math.min((right - left) * 0.5F, (bottom - top) * 0.5F)));
        List<Point> points = new ArrayList<Point>(TooltipConfig.cornerSegments * 4 + 4);
        appendCorner(points, right - radius, top + radius, radius, -90.0F, 0.0F);
        appendCorner(points, right - radius, bottom - radius, radius, 0.0F, 90.0F);
        appendCorner(points, left + radius, bottom - radius, radius, 90.0F, 180.0F);
        appendCorner(points, left + radius, top + radius, radius, 180.0F, 270.0F);
        return points;
    }

    private static void appendCorner(List<Point> points, float centerX, float centerY,
                                     float radius, float startDegrees, float endDegrees) {
        int segments = Math.max(1, TooltipConfig.cornerSegments);
        for (int segment = 0; segment <= segments; segment++) {
            double radians = Math.toRadians(startDegrees + (endDegrees - startDegrees) * segment / segments);
            points.add(new Point(centerX + (float) Math.cos(radians) * radius,
                    centerY + (float) Math.sin(radians) * radius));
        }
    }

    private static void drawStrip(List<Point> outer, List<Point> inner,
                                  float left, float top, float right, float bottom,
                                  int[] colors, boolean transparentOuter) {
        int count = Math.min(outer.size(), inner.size());
        if (count < 3) return;
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int index = 0; index <= count; index++) {
            Point out = outer.get(index % count);
            Point in = inner.get(index % count);
            int outerColor = bilerp(colors, normalize(out.x, left, right), normalize(out.y, top, bottom));
            if (transparentOuter) outerColor = withAlpha(outerColor, 0);
            color(outerColor);
            GL11.glVertex3f(out.x, out.y, 0.0F);
            color(bilerp(colors, normalize(in.x, left, right), normalize(in.y, top, bottom)));
            GL11.glVertex3f(in.x, in.y, 0.0F);
        }
        GL11.glEnd();
    }

    private static void arc(
            float centerX, float centerY, float radius, float startDegrees, float endDegrees,
            int[] colors, float left, float top, float right, float bottom) {
        int segments = startDegrees == endDegrees ? 0 : Math.max(1, TooltipConfig.cornerSegments);
        for (int segment = 0; segment <= segments; segment++) {
            float degrees = segments == 0 ? startDegrees
                    : startDegrees + (endDegrees - startDegrees) * segment / segments;
            double radians = Math.toRadians(degrees);
            float x = centerX + (float) Math.cos(radians) * radius;
            float y = centerY + (float) Math.sin(radians) * radius;
            color(bilerp(colors, normalize(x, left, right), normalize(y, top, bottom)));
            GL11.glVertex3f(x, y, 0.0F);
        }
    }

    private static void corner(float x, float y, int color) {
        color(color);
        GL11.glVertex3f(x, y, 0.0F);
    }

    private static void drawGradientQuad(float left, float top, float right, float bottom, int[] colors) {
        GL11.glBegin(GL11.GL_QUADS);
        corner(left, top, colors[0]);
        corner(right, top, colors[1]);
        corner(right, bottom, colors[2]);
        corner(left, bottom, colors[3]);
        GL11.glEnd();
    }

    private static int bilerp(int[] colors, float x, float y) {
        return lerp(lerp(colors[0], colors[1], x), lerp(colors[3], colors[2], x), y);
    }

    private static int average(int[] colors) {
        int result = colors[0];
        for (int index = 1; index < colors.length; index++) result = lerp(result, colors[index], 1.0F / (index + 1));
        return result;
    }

    private static int lerp(int from, int to, float amount) {
        int alpha = Math.round((from >>> 24) + ((to >>> 24) - (from >>> 24)) * amount);
        int red = Math.round((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * amount);
        int green = Math.round((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * amount);
        int blue = Math.round((from & 255) + ((to & 255) - (from & 255)) * amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static float normalize(float value, float start, float end) {
        if (end <= start) return 0.5F;
        return Math.max(0.0F, Math.min(1.0F, (value - start) / (end - start)));
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static int[] spectrumPalette() {
        int[] colors = new int[4];
        float cycle = Math.max(250, TooltipConfig.borderCycleMillis);
        float phase = System.currentTimeMillis() % (long) (cycle * 4.0F) / cycle;
        for (int index = 0; index < colors.length; index++) {
            int rgb = Color.HSBtoRGB((phase + index) / 4.0F % 1.0F, 0.65F, 0.9F);
            colors[index] = TooltipConfig.borderColors[index] & 0xFF000000 | rgb & 0x00FFFFFF;
        }
        return colors;
    }

    private static int[] effectiveBorder(AdaptiveBorderColors.Result adaptive) {
        int[] colors = adaptive.spectrum || "spectrum".equals(TooltipConfig.borderShading)
                ? spectrumPalette() : adaptive.colors.clone();
        if ("solid".equals(TooltipConfig.borderShading)) {
            return new int[] {colors[0], colors[0], colors[0], colors[0]};
        }
        if ("horizontal".equals(TooltipConfig.borderShading)) {
            return new int[] {colors[0], colors[1], colors[1], colors[0]};
        }
        if ("vertical".equals(TooltipConfig.borderShading)) {
            return new int[] {colors[0], colors[0], colors[3], colors[3]};
        }
        return colors;
    }

    private static void color(int argb) {
        GL11.glColor4f((argb >> 16 & 255) / 255.0F, (argb >> 8 & 255) / 255.0F,
                (argb & 255) / 255.0F, (argb >>> 24) / 255.0F);
    }

    private static final class Point {
        final float x;
        final float y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Restores the fixed-function state a foreign tooltip renderer can leave behind. 1.7.10 has no
     * GlStateManager cache to repair, so the OpenGL attribute stack plus the texture bindings of
     * the units item rendering touches are enough to keep the caller's state intact.
     */
    static final class CallerGlState implements AutoCloseable {
        private static final int[] TEXTURE_UNITS = {GL13.GL_TEXTURE0, GL13.GL_TEXTURE1, GL13.GL_TEXTURE2};

        private final int activeTexture;
        private final int[] bindings = new int[TEXTURE_UNITS.length];
        private boolean closed;

        private CallerGlState() {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            for (int index = 0; index < TEXTURE_UNITS.length; index++) {
                GL13.glActiveTexture(TEXTURE_UNITS[index]);
                bindings[index] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            GL13.glActiveTexture(activeTexture);
        }

        static CallerGlState capture() {
            return new CallerGlState();
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            for (int index = 0; index < TEXTURE_UNITS.length; index++) {
                GL13.glActiveTexture(TEXTURE_UNITS[index]);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, bindings[index]);
            }
            GL13.glActiveTexture(activeTexture);
            GL11.glPopAttrib();
        }
    }
}
