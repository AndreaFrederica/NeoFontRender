package neofontrender.splash;

import java.awt.Font;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/** OpenType weight-axis inspection and AWT splash weight policy. */
final class SplashFontWeight {

    static final int REGULAR_WEIGHT = 400;
    static final int BOLD_WEIGHT = 700;

    private static final int TTC_TAG = tag("ttcf");
    private static final int FVAR_TAG = tag("fvar");
    private static final int WGHT_TAG = tag("wght");
    private static final int SFNT_TRUE_TYPE = 0x00010000;
    private static final int SFNT_OTTO = tag("OTTO");
    private static final int SFNT_TRUE = tag("true");
    private static final int SFNT_TYP1 = tag("typ1");

    private SplashFontWeight() {
    }

    static WeightAxis inspect(byte[] data) throws IOException {
        if (data == null || data.length < 12) {
            throw new IOException("Font data is too short for an SFNT header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        int sfntOffset = 0;
        int signature = buffer.getInt(0);
        if (signature == TTC_TAG) {
            if (data.length < 16 || unsignedInt(buffer, 8) < 1L) {
                throw new IOException("Invalid TrueType collection header");
            }
            sfntOffset = checkedOffset(unsignedInt(buffer, 12), data.length, "TTC face");
            signature = intAt(buffer, sfntOffset, data.length, "SFNT signature");
        }
        if (!isSfntSignature(signature)) {
            throw new IOException("Unsupported SFNT signature 0x" + Integer.toHexString(signature));
        }

        requireRange(sfntOffset, 12, data.length, "SFNT header");
        int tableCount = unsignedShort(buffer, sfntOffset + 4);
        if (tableCount > 4096) {
            throw new IOException("Unreasonable SFNT table count " + tableCount);
        }
        long directoryEnd = (long) sfntOffset + 12L + (long) tableCount * 16L;
        if (directoryEnd > data.length) {
            throw new IOException("Truncated SFNT table directory");
        }

        int fvarOffset = -1;
        int fvarLength = 0;
        for (int i = 0; i < tableCount; i++) {
            int record = sfntOffset + 12 + i * 16;
            if (buffer.getInt(record) == FVAR_TAG) {
                fvarOffset = checkedOffset(unsignedInt(buffer, record + 8), data.length, "fvar table");
                long length = unsignedInt(buffer, record + 12);
                if (length > Integer.MAX_VALUE) {
                    throw new IOException("fvar table is too large");
                }
                fvarLength = (int) length;
                requireRange(fvarOffset, fvarLength, data.length, "fvar table");
                break;
            }
        }
        if (fvarOffset < 0) {
            return null;
        }
        return readWeightAxis(buffer, fvarOffset, fvarLength, data.length);
    }

    private static WeightAxis readWeightAxis(ByteBuffer buffer, int tableOffset,
                                             int tableLength, int dataLength) throws IOException {
        if (tableLength < 16) {
            throw new IOException("Truncated fvar header");
        }
        requireRange(tableOffset, 16, dataLength, "fvar header");
        int axesOffset = unsignedShort(buffer, tableOffset + 4);
        int axisCount = unsignedShort(buffer, tableOffset + 8);
        int axisSize = unsignedShort(buffer, tableOffset + 10);
        if (axisCount > 256 || axisSize < 20) {
            throw new IOException("Invalid fvar axis directory");
        }

        long axesEnd = (long) axesOffset + (long) axisCount * axisSize;
        if (axesOffset < 16 || axesEnd > tableLength) {
            throw new IOException("Truncated fvar axis records");
        }
        for (int i = 0; i < axisCount; i++) {
            int record = tableOffset + axesOffset + i * axisSize;
            if (buffer.getInt(record) != WGHT_TAG) {
                continue;
            }
            int minimum = fixedWeight(buffer.getInt(record + 4));
            int defaultWeight = fixedWeight(buffer.getInt(record + 8));
            int maximum = fixedWeight(buffer.getInt(record + 12));
            if (minimum <= 0 || minimum > defaultWeight || defaultWeight > maximum) {
                throw new IOException("Invalid fvar wght range " + minimum + ".."
                        + defaultWeight + ".." + maximum);
            }
            return new WeightAxis(minimum, defaultWeight, maximum, "fvar");
        }
        return null;
    }

    static WeightAxis inferSystemAxis(Font font, String requestedName) {
        Integer explicitlyRequested = weightSuffix(requestedName);
        if (explicitlyRequested != null && explicitlyRequested != REGULAR_WEIGHT) {
            return null;
        }

        Integer inferred = weightSuffix(font.getPSName());
        if (inferred == null) {
            inferred = weightSuffix(font.getFontName(Locale.ROOT));
        }
        if (inferred == null || inferred == REGULAR_WEIGHT) {
            return null;
        }
        return new WeightAxis(1, inferred, 1000, "font name");
    }

    static Resolution resolve(WeightAxis axis, int configuredWeight, int style, String fontName) {
        int target = configuredWeight > 0
                ? Math.max(1, Math.min(1000, configuredWeight))
                : REGULAR_WEIGHT;
        if ((style & Font.BOLD) != 0) {
            target = Math.max(target, BOLD_WEIGHT);
        }

        if (axis == null) {
            return new Resolution(style, target, target, 0, false, false);
        }
        if (target < axis.minimum || axis.defaultWeight > target) {
            throw new Fallback("font '" + fontName + "' has " + axis.source
                    + " default weight " + axis.defaultWeight + " and range "
                    + axis.minimum + "-" + axis.maximum
                    + ", which AWT cannot reduce to splash target " + target);
        }

        int appliedTarget = Math.min(target, axis.maximum);
        int awtStyle = style & ~Font.BOLD;
        return new Resolution(awtStyle, target, appliedTarget,
                appliedTarget - axis.defaultWeight, true, appliedTarget != target);
    }

    static float outlineStrokeWidth(float fontSize, int weightDelta) {
        if (fontSize <= 0.0F || weightDelta <= 0) {
            return 0.0F;
        }
        return Math.min(fontSize / 8.0F, fontSize * weightDelta / 8000.0F);
    }

    private static Integer weightSuffix(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String normalized = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.endsWith("extralight") || normalized.endsWith("ultralight")) return 200;
        if (normalized.endsWith("demilight") || normalized.endsWith("semilight")) return 350;
        if (normalized.endsWith("extrabold") || normalized.endsWith("ultrabold")) return 800;
        if (normalized.endsWith("demibold") || normalized.endsWith("semibold")) return 600;
        if (normalized.endsWith("thin")) return 100;
        if (normalized.endsWith("light")) return 300;
        if (normalized.endsWith("regular") || normalized.endsWith("normal")
                || normalized.endsWith("book")) return REGULAR_WEIGHT;
        if (normalized.endsWith("medium")) return 500;
        if (normalized.endsWith("bold")) return 700;
        if (normalized.endsWith("black") || normalized.endsWith("heavy")) return 900;
        return null;
    }

    private static int fixedWeight(int fixed) throws IOException {
        double value = fixed / 65536.0D;
        if (!Double.isFinite(value) || value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IOException("Invalid fixed-point weight value");
        }
        return (int) Math.round(value);
    }

    private static boolean isSfntSignature(int signature) {
        return signature == SFNT_TRUE_TYPE || signature == SFNT_OTTO
                || signature == SFNT_TRUE || signature == SFNT_TYP1;
    }

    private static int tag(String value) {
        return (value.charAt(0) << 24) | (value.charAt(1) << 16)
                | (value.charAt(2) << 8) | value.charAt(3);
    }

    private static int intAt(ByteBuffer buffer, int offset, int length, String label) throws IOException {
        requireRange(offset, 4, length, label);
        return buffer.getInt(offset);
    }

    private static int unsignedShort(ByteBuffer buffer, int offset) {
        return Short.toUnsignedInt(buffer.getShort(offset));
    }

    private static long unsignedInt(ByteBuffer buffer, int offset) {
        return Integer.toUnsignedLong(buffer.getInt(offset));
    }

    private static int checkedOffset(long offset, int length, String label) throws IOException {
        if (offset < 0L || offset > length || offset > Integer.MAX_VALUE) {
            throw new IOException("Invalid " + label + " offset " + offset);
        }
        return (int) offset;
    }

    private static void requireRange(int offset, int size, int length, String label) throws IOException {
        if (offset < 0 || size < 0 || (long) offset + size > length) {
            throw new IOException("Invalid or truncated " + label);
        }
    }

    static final class WeightAxis {
        final int minimum;
        final int defaultWeight;
        final int maximum;
        final String source;

        WeightAxis(int minimum, int defaultWeight, int maximum, String source) {
            this.minimum = minimum;
            this.defaultWeight = defaultWeight;
            this.maximum = maximum;
            this.source = source;
        }
    }

    static final class Resolution {
        final int awtStyle;
        final int requestedTarget;
        final int appliedTarget;
        final int emboldenDelta;
        final boolean adjusted;
        final boolean clamped;

        Resolution(int awtStyle, int requestedTarget, int appliedTarget, int emboldenDelta,
                   boolean adjusted, boolean clamped) {
            this.awtStyle = awtStyle;
            this.requestedTarget = requestedTarget;
            this.appliedTarget = appliedTarget;
            this.emboldenDelta = emboldenDelta;
            this.adjusted = adjusted;
            this.clamped = clamped;
        }
    }

    static final class Fallback extends RuntimeException {
        Fallback(String message) {
            super(message);
        }
    }
}
