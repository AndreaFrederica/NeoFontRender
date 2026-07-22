package neofontrender.addons.tooltips;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Derives a stable four-corner border palette from item and title colors. */
final class AdaptiveBorderColors {
    private static final int[] MINECRAFT_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private AdaptiveBorderColors() {}

    static Result compute(ItemStack stack, String formattedTitle, int[] configured) {
        Integer base = stack == null ? null : rarityColor(stack.getRarity());
        Set<Integer> titleColors = collectFormattedColors(formattedTitle, base);
        if (titleColors.isEmpty()) return Result.unchanged(configured);
        return synthesize(titleColors, stack != null && stack.hasEffect(), configured);
    }

    static Result synthesize(Set<Integer> titleColors, boolean enchanted, int[] configured) {
        if (titleColors.size() > 4) return new Result(configured.clone(), true);
        List<float[]> colors = new ArrayList<float[]>(titleColors.size());
        for (int color : titleColors) {
            float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
            hsb[1] = Math.min(hsb[1], 0.9F);
            hsb[2] = clamp(hsb[2], 0.2F, 0.85F);
            colors.add(hsb);
        }
        if (colors.isEmpty()) return Result.unchanged(configured);
        int[] result = configured.clone();
        int first = Color.HSBtoRGB(colors.get(0)[0], colors.get(0)[1], colors.get(0)[2]);
        int second;
        int third;
        int fourth;
        if (colors.size() >= 3) {
            second = rgb(colors.get(1));
            third = rgb(colors.get(2));
            fourth = colors.size() == 4 ? rgb(colors.get(3)) : adjust(colors.get(1), true, enchanted);
        } else if (colors.size() == 2) {
            third = rgb(colors.get(1));
            second = lerpRgb(first, third, 0.5F);
            fourth = adjust(Color.RGBtoHSB(second >> 16 & 255, second >> 8 & 255, second & 255, null), true, enchanted);
        } else {
            second = adjust(colors.get(0), false, enchanted);
            third = adjust(colors.get(0), true, enchanted);
            fourth = adjust(colors.get(0), true, !enchanted);
        }
        result[0] = preserveAlpha(result[0], first);
        result[1] = preserveAlpha(result[1], second);
        result[2] = preserveAlpha(result[2], third);
        result[3] = preserveAlpha(result[3], fourth);
        return new Result(result, false);
    }

    static Set<Integer> collectFormattedColors(String text, Integer baseColor) {
        Set<Integer> colors = new LinkedHashSet<Integer>();
        Integer current = baseColor;
        if (text == null) return colors;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == '\u00A7' && index + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++index));
                int colorIndex = "0123456789abcdef".indexOf(code);
                if (colorIndex >= 0) current = MINECRAFT_COLORS[colorIndex];
                else if (code == 'r') current = baseColor;
            } else if (!Character.isWhitespace(character) && current != null) {
                colors.add(current);
            }
        }
        return colors;
    }

    private static Integer rarityColor(EnumRarity rarity) {
        if (rarity == null || rarity == EnumRarity.common) return null;
        EnumChatFormatting formatting = rarity.rarityColor;
        String code = formatting == null ? "" : formatting.toString();
        if (code.length() < 2) return null;
        int index = "0123456789abcdef".indexOf(Character.toLowerCase(code.charAt(1)));
        return index < 0 ? null : MINECRAFT_COLORS[index];
    }

    private static int rgb(float[] hsb) {
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    private static int adjust(float[] source, boolean hueShift, boolean enchanted) {
        float hue = source[0] + (hueShift ? (enchanted ? 0.075F : 0.04F) : -0.025F);
        hue -= (float) Math.floor(hue);
        float saturation = clamp(source[1] + (enchanted ? 0.12F : 0.06F), 0.0F, 1.0F);
        float brightness = clamp(source[2] + (source[2] < 0.6F ? 0.10F : -0.06F), 0.0F, 1.0F);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    private static int lerpRgb(int from, int to, float amount) {
        int red = Math.round((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * amount);
        int green = Math.round((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * amount);
        int blue = Math.round((from & 255) + ((to & 255) - (from & 255)) * amount);
        return red << 16 | green << 8 | blue;
    }

    private static int preserveAlpha(int original, int rgb) {
        return original & 0xFF000000 | rgb & 0x00FFFFFF;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
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
