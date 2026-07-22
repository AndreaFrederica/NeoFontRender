/*
 * Adaptive tooltip border palette derived from ModernUI-MC TooltipRenderer.
 * Copyright (C) 2019-2026 BloCamLimb et al.
 * Licensed under LGPL-3.0-or-later.
 */
package neofontrender.addons.tooltips;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ports ModernUI's formatted-title color collection and HSV palette synthesis to 1.12. */
final class AdaptiveBorderColors {
    private static final int[] MINECRAFT_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private AdaptiveBorderColors() {}

    static Result compute(ItemStack stack, String formattedTitle, int[] configured) {
        if (stack == null || stack.isEmpty()) return Result.unchanged(configured);
        String registryName = stack.getItem().getRegistryName() == null
                ? "" : stack.getItem().getRegistryName().getPath();
        if ("dragon_egg".equals(registryName) || "debug_stick".equals(registryName)) {
            return new Result(configured.clone(), true);
        }

        Integer base = rarityColor(stack.getRarity());
        Set<Integer> titleColors = collectFormattedColors(formattedTitle, base);
        if (titleColors.isEmpty()) return Result.unchanged(configured);

        return synthesize(titleColors, stack.hasEffect(), configured);
    }

    static Result synthesize(Set<Integer> titleColors, boolean enchanted, int[] configured) {
        List<float[]> hsvColors = new ArrayList<>(titleColors.size());
        for (int color : titleColors) {
            float[] hsv = new float[3];
            Color.RGBToHSV(color, hsv);
            hsv[1] = Math.min(hsv[1], 0.9F);
            hsv[2] = MathUtil.clamp(hsv[2], 0.2F, 0.85F);
            hsvColors.add(hsv);
        }
        if (hsvColors.size() > 4) return new Result(configured.clone(), true);

        int size = hsvColors.size();
        int c1 = Color.HSVToColor(hsvColors.get(0));
        int c2;
        int c3;
        int c4;
        if (size > 2) {
            c2 = Color.HSVToColor(hsvColors.get(1));
            c3 = Color.HSVToColor(hsvColors.get(2));
            if (size == 4) {
                c4 = Color.HSVToColor(hsvColors.get(3));
            } else {
                float[] inverted = hsvColors.get(1).clone();
                inverted[0] = (inverted[0] + 180.0F) % 360.0F;
                c4 = Color.HSVToColor(inverted);
            }
        } else if (size == 2) {
            c3 = Color.HSVToColor(hsvColors.get(1));
            c2 = lerpColor(c1, c3, 0.5F);
            float[] middle = new float[3];
            Color.RGBToHSV(c2, middle);
            c4 = adjustColor(middle, false, true, false, enchanted);
        } else {
            float[] hsv = hsvColors.get(0);
            c2 = adjustColor(hsv, false, true, false, enchanted);
            c3 = adjustColor(hsv, true, true, true, enchanted);
            c4 = adjustColor(hsv, true, false, true, enchanted);
        }

        int[] result = configured.clone();
        result[0] = preserveAlpha(result[0], c1);
        result[1] = preserveAlpha(result[1], c2);
        result[2] = preserveAlpha(result[2], c3);
        result[3] = preserveAlpha(result[3], c4);
        return new Result(result, false);
    }

    static Set<Integer> collectFormattedColors(String text, Integer baseColor) {
        Set<Integer> colors = new LinkedHashSet<>();
        Integer current = baseColor;
        if (text == null) return colors;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\u00A7' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                int index = "0123456789abcdef".indexOf(code);
                if (index >= 0) current = MINECRAFT_COLORS[index];
                else if (code == 'r') current = baseColor;
                continue;
            }
            if (!Character.isWhitespace(ch) && current != null && colors.size() < 16) {
                colors.add(current);
            }
        }
        return colors;
    }

    private static Integer rarityColor(EnumRarity rarity) {
        if (rarity == null || rarity == EnumRarity.COMMON) return null;
        TextFormatting formatting = rarity.color;
        int index = formatting == null ? -1 : formatting.getColorIndex();
        return index >= 0 && index < MINECRAFT_COLORS.length ? MINECRAFT_COLORS[index] : null;
    }

    private static int adjustColor(float[] hsv, boolean hue, boolean saturation, boolean value,
                                   boolean magnified) {
        float h = hsv[0];
        float s = hsv[1];
        float v = hsv[2];
        if (hue) {
            if (h >= 60.0F && h <= 240.0F) h += magnified ? 27.0F : 15.0F;
            else h -= magnified ? 18.0F : 10.0F;
            h = (h + 360.0F) % 360.0F;
        }
        if (saturation) {
            if (s < 0.6F) s += magnified ? 0.18F : 0.12F;
            else s -= magnified ? 0.12F : 0.06F;
        }
        if (value) {
            if (v < 0.6F) v += magnified ? 0.12F : 0.08F;
            else v -= magnified ? 0.08F : 0.04F;
        }
        return Color.HSVToColor(h, s, v);
    }

    private static int lerpColor(int from, int to, float amount) {
        int r = Math.round(MathUtil.lerp(Color.red(from), Color.red(to), amount));
        int g = Math.round(MathUtil.lerp(Color.green(from), Color.green(to), amount));
        int b = Math.round(MathUtil.lerp(Color.blue(from), Color.blue(to), amount));
        return (r << 16) | (g << 8) | b;
    }

    private static int preserveAlpha(int original, int rgb) {
        return (original & 0xFF000000) | (rgb & 0x00FFFFFF);
    }

    static final class Result {
        final int[] colors;
        final boolean spectrum;

        Result(int[] colors, boolean spectrum) {
            this.colors = colors;
            this.spectrum = spectrum;
        }

        static Result unchanged(int[] colors) {
            return new Result(colors.clone(), false);
        }
    }
}
