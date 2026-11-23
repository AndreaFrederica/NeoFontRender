package neofontrender.text;

import java.util.Locale;

/** Shared suffix parser for inline layout and source-raster density options. */
public final class InlineRenderOptions {
    private final InlineLayout layout;
    private final float supersample;
    private final int end;

    private InlineRenderOptions(InlineLayout layout, float supersample, int end) {
        this.layout = layout;
        this.supersample = supersample;
        this.end = end;
    }

    public InlineLayout layout() { return layout; }
    public float supersample() { return supersample; }
    public int end() { return end; }

    public static InlineRenderOptions parse(CharSequence source, int start,
                                            InlineLayout defaults,
                                            float defaultSupersample) {
        InlineLayout base = defaults == null ? InlineLayout.oneLine() : defaults;
        float sampling = bounded(defaultSupersample, 2.0F, 0.25F, 16.0F);
        if (source == null || start < 0 || start >= source.length()
                || source.charAt(start) != '[') {
            return new InlineRenderOptions(base, sampling, start);
        }
        int close = indexOf(source, ']', start + 1, Math.min(source.length(), start + 512));
        if (close < 0) return new InlineRenderOptions(base, sampling, start);

        float rows = base.rows();
        float columns = base.columns();
        float maximumColumns = base.maximumColumns();
        InlineLayout.Alignment alignment = base.alignment();
        InlineLayout.Flow flow = base.flow();
        float scale = 1.0F;
        boolean recognized = false;
        String body = source.subSequence(start + 1, close).toString().trim();
        if (body.isEmpty()) return new InlineRenderOptions(base, sampling, start);
        for (String raw : body.split("[,;]")) {
            String[] assignment = raw.trim().split("=", 2);
            if (assignment.length != 2) return new InlineRenderOptions(base, sampling, start);
            String key = assignment[0].trim().toLowerCase(Locale.ROOT)
                    .replace('_', '-');
            String value = assignment[1].trim().toLowerCase(Locale.ROOT);
            try {
                switch (key) {
                    case "scale":
                        scale = bounded(Float.parseFloat(value), 1.0F, 0.25F, 4.0F);
                        break;
                    case "height":
                    case "rows":
                        rows = parseRows(value);
                        break;
                    case "width":
                    case "columns":
                    case "cols":
                        columns = parseColumns(value, true);
                        break;
                    case "max-width":
                    case "max-columns":
                    case "max-cols":
                        maximumColumns = parseColumns(value, false);
                        break;
                    case "supersample":
                    case "oversample":
                        sampling = bounded(Float.parseFloat(value), sampling, 0.25F, 16.0F);
                        break;
                    case "align":
                    case "alignment":
                        alignment = InlineLayout.Alignment.valueOf(value.toUpperCase(Locale.ROOT));
                        break;
                    case "flow":
                        flow = InlineLayout.Flow.valueOf(value.toUpperCase(Locale.ROOT));
                        break;
                    default:
                        return new InlineRenderOptions(base, sampling, start);
                }
            } catch (IllegalArgumentException failure) {
                return new InlineRenderOptions(base, sampling, start);
            }
            recognized = true;
        }
        InlineLayout layout = new InlineLayout(
                rows * scale, columns, maximumColumns, alignment, flow);
        return recognized ? new InlineRenderOptions(layout, sampling, close + 1)
                : new InlineRenderOptions(base, sampling, start);
    }

    private static float parseRows(String value) {
        String number = value;
        float divisor = 1.0F;
        if (number.endsWith("lh") || number.endsWith("em")) {
            number = number.substring(0, number.length() - 2).trim();
        } else if (number.endsWith("px")) {
            number = number.substring(0, number.length() - 2).trim();
            divisor = 18.0F;
        }
        return bounded(Float.parseFloat(number) / divisor, 1.0F, 0.125F, 64.0F);
    }

    private static float parseColumns(String value, boolean allowAuto) {
        if (allowAuto && "auto".equals(value)) return Float.NaN;
        String number = value;
        for (String suffix : new String[]{"columns", "column", "cols", "col", "em", "ch"}) {
            if (number.endsWith(suffix)) {
                number = number.substring(0, number.length() - suffix.length()).trim();
                break;
            }
        }
        return bounded(Float.parseFloat(number), 1.0F, 0.125F, 256.0F);
    }

    private static int indexOf(CharSequence source, char target, int start, int limit) {
        for (int index = start; index < limit; index++) if (source.charAt(index) == target) return index;
        return -1;
    }

    private static float bounded(float value, float fallback, float minimum, float maximum) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(minimum, Math.min(maximum, value));
    }
}
