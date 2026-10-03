package neofontrender.lab;

import neofontrender.text.InlineContent;
import neofontrender.text.InlineRaster;
import neofontrender.text.InlineSpan;
import neofontrender.text.StructuredText;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

final class InlineRenderSupport {
    private InlineRenderSupport() {}

    static InlineSpan at(StructuredText text, int index) {
        for (InlineSpan span : text.inlineSpans()) if (span.start() == index) return span;
        return null;
    }

    static int height(InlineContent content, int fontSize) {
        return Math.max(1, Math.round(content.layout().resolve(content.raster(), fontSize).height()));
    }

    static int width(InlineContent content, int fontSize) {
        return Math.max(1, Math.round(
                content.layout().resolve(content.raster(), fontSize).width()));
    }

    static void draw(Graphics2D graphics, InlineContent content, int x, int baseline,
                     int fontSize, int argb, boolean shadow) {
        int height = height(content, fontSize);
        int width = width(content, fontSize);
        int top = Math.round(content.layout().top(baseline - fontSize, baseline, height));
        InlineRaster raster = content.raster();
        if (raster == null) {
            graphics.setColor(new Color(shadow ? 0x50000000 : 0xFF9AA4B2, true));
            graphics.drawRect(x + (shadow ? 1 : 0), top + (shadow ? 1 : 0),
                    Math.max(1, width - 1), Math.max(1, height - 1));
            return;
        }
        int[] pixels = raster.argb();
        if (content.tint() || shadow) {
            int tint = shadow ? 0x60000000 : argb;
            int tr = tint >>> 16 & 255, tg = tint >>> 8 & 255, tb = tint & 255;
            int ta = tint >>> 24;
            for (int index = 0; index < pixels.length; index++) {
                int pixel = pixels[index];
                int alpha = (pixel >>> 24) * ta / 255;
                int red = content.tint() ? (pixel >>> 16 & 255) * tr / 255 : pixel >>> 16 & 255;
                int green = content.tint() ? (pixel >>> 8 & 255) * tg / 255 : pixel >>> 8 & 255;
                int blue = content.tint() ? (pixel & 255) * tb / 255 : pixel & 255;
                pixels[index] = alpha << 24 | red << 16 | green << 8 | blue;
            }
        }
        BufferedImage image = new BufferedImage(raster.width(), raster.height(),
                BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, raster.width(), raster.height(), pixels, 0, raster.width());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image, x + (shadow ? 1 : 0), top + (shadow ? 1 : 0),
                width, height, null);
    }
}
