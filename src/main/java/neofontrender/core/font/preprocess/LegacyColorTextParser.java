package neofontrender.core.font.preprocess;

import neofontrender.api.text.ModernText;
import neofontrender.core.config.NeofontrenderConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared parser for legacy raw-string RGB protocols.
 */
final class LegacyColorTextParser {
    private LegacyColorTextParser() {
    }

    static PreprocessedText process(String rawText) {
        return process(rawText, false, true);
    }

    static PreprocessedText process(String rawText, boolean decodeTinkersPua,
                                    boolean decodeHexColors) {
        return process(rawText, decodeTinkersPua, decodeHexColors,
                NeofontrenderConfig.laboratoryHexChatResetStyles());
    }

    static PreprocessedText process(String rawText, boolean decodeTinkersPua,
                                    boolean decodeHexColors, boolean resetHexStyles) {
        if (rawText == null || rawText.isEmpty()) {
            return PreprocessedText.unchanged(rawText);
        }

        ModernText.Builder modern = ModernText.builder();
        StringBuilder run = new StringBuilder();
        StringBuilder visible = new StringBuilder(rawText.length());
        List<Integer> rawStarts = new ArrayList<>(rawText.length() + 1);
        List<Integer> rawEnds = new ArrayList<>(rawText.length() + 1);
        rawStarts.add(0);
        rawEnds.add(0);

        FormattingState formatting = new FormattingState();
        boolean colorOverride = false;
        int rgb = 0;
        int[] hexGradient = null;
        int hexGradientIndex = 0;
        int hexGradientLength = 0;
        boolean needsStylePrefix = false;
        boolean transformed = false;

        for (int rawIndex = 0; rawIndex < rawText.length();) {
            char current = rawText.charAt(rawIndex);
            if (decodeTinkersPua && TinkersAntiqueTextPreprocessor.isMarker(current)) {
                appendRun(modern, run, colorOverride, rgb);
                hexGradient = null;
                transformed = true;
                while (rawIndex < rawText.length()
                        && TinkersAntiqueTextPreprocessor.isMarker(rawText.charAt(rawIndex))) {
                    int remaining = rawText.length() - rawIndex;
                    if (remaining >= 3
                            && TinkersAntiqueTextPreprocessor.isMarker(
                                    rawText.charAt(rawIndex + 1))
                            && TinkersAntiqueTextPreprocessor.isMarker(
                                    rawText.charAt(rawIndex + 2))) {
                        rgb = markerValue(rawText.charAt(rawIndex)) << 16
                                | markerValue(rawText.charAt(rawIndex + 1)) << 8
                                | markerValue(rawText.charAt(rawIndex + 2));
                        colorOverride = true;
                        rawIndex += 3;
                    } else {
                        rgb = 0xFFFFFF;
                        colorOverride = true;
                        while (rawIndex < rawText.length()
                                && TinkersAntiqueTextPreprocessor.isMarker(
                                        rawText.charAt(rawIndex))) {
                            rawIndex++;
                        }
                    }
                }
                rawEnds.set(visible.length(), rawIndex);
                needsStylePrefix = true;
                continue;
            }

            if (HexChatTextPreprocessor.isMarker(rawText, rawIndex)) {
                appendRun(modern, run, colorOverride, rgb);
                int markerLength = HexChatTextPreprocessor.markerLength(rawText, rawIndex);
                hexGradient = HexChatTextPreprocessor.markerColors(rawText, rawIndex);
                hexGradientIndex = 0;
                hexGradientLength = HexChatTextPreprocessor.gradientLength(
                        rawText, rawIndex + markerLength);
                rgb = hexGradient.length == 0 ? 0xFFFFFF : hexGradient[0];
                colorOverride = true;
                if (resetHexStyles) {
                    formatting.reset();
                }
                rawIndex += markerLength;
                rawEnds.set(visible.length(), rawIndex);
                needsStylePrefix = true;
                transformed = true;
                continue;
            }

            if (needsStylePrefix) {
                run.append(formatting.prefix());
                needsStylePrefix = false;
            }

            if (hexGradient != null && hexGradient.length > 1 && run.length() == 0) {
                run.append(formatting.prefix());
            }

            run.append(current);
            visible.append(current);
            rawIndex++;
            rawStarts.add(rawIndex);
            rawEnds.add(rawIndex);

            if (current == '\u00A7' && rawIndex < rawText.length()) {
                char code = rawText.charAt(rawIndex);
                run.append(code);
                visible.append(code);
                rawIndex++;
                rawStarts.add(rawIndex);
                rawEnds.add(rawIndex);
                if (Character.toLowerCase(code) == 'r' && colorOverride) {
                    // Vanilla reset restores the draw call's base color.
                    run.setLength(run.length() - 2);
                    appendRun(modern, run, true, rgb);
                    run.append('\u00A7').append(code);
                    colorOverride = false;
                }
                char normalizedCode = Character.toLowerCase(code);
                if ((normalizedCode >= '0' && normalizedCode <= '9')
                        || (normalizedCode >= 'a' && normalizedCode <= 'f')
                        || normalizedCode == 'r') {
                    hexGradient = null;
                }
                formatting.accept(code);
            } else if (hexGradient != null && hexGradient.length > 1) {
                rgb = HexChatTextPreprocessor.interpolate(
                        hexGradient, hexGradientIndex++, hexGradientLength);
                appendRun(modern, run, true, rgb);
            }
        }
        appendRun(modern, run, colorOverride, rgb);

        if (!transformed) return PreprocessedText.unchanged(rawText);
        return new PreprocessedText(rawText, visible.toString(), modern.build(), true,
                toArray(rawStarts), toArray(rawEnds));
    }

    private static void appendRun(ModernText.Builder builder, StringBuilder run,
                                  boolean colorOverride, int rgb) {
        if (run.length() == 0) return;
        String value = run.toString();
        run.setLength(0);
        if (colorOverride) {
            builder.append(value, rgb);
        } else {
            builder.append(value);
        }
    }

    private static int markerValue(char marker) {
        return marker - TinkersAntiqueTextPreprocessor.MARKER_START;
    }

    private static int[] toArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    private static final class FormattingState {
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strikethrough;
        private boolean obfuscated;

        private void accept(char rawCode) {
            char code = Character.toLowerCase(rawCode);
            if ((code >= '0' && code <= '9')
                    || (code >= 'a' && code <= 'f')
                    || code == 'r') {
                bold = italic = underline = strikethrough = obfuscated = false;
            } else if (code == 'k') {
                obfuscated = true;
            } else if (code == 'l') {
                bold = true;
            } else if (code == 'm') {
                strikethrough = true;
            } else if (code == 'n') {
                underline = true;
            } else if (code == 'o') {
                italic = true;
            }
        }

        private String prefix() {
            StringBuilder result = new StringBuilder(10);
            if (obfuscated) result.append('\u00A7').append('k');
            if (bold) result.append('\u00A7').append('l');
            if (strikethrough) result.append('\u00A7').append('m');
            if (underline) result.append('\u00A7').append('n');
            if (italic) result.append('\u00A7').append('o');
            return result.toString();
        }

        private void reset() {
            bold = italic = underline = strikethrough = obfuscated = false;
        }
    }
}
