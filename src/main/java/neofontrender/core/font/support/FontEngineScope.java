package neofontrender.core.font.support;

import neofontrender.core.config.NeofontrenderConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Thread-local engine override for a single draw scope.
 *
 * <p>Some screens need a different text engine than the rest of the game. The F3 overlay is the
 * first user: its engine can be chosen independently so a broken or slow engine can be compared
 * side by side without restarting. The scope is pushed around the screen's draw call and read by
 * the {@code FontRenderer} dispatcher and the compatibility leaf hooks.</p>
 */
public final class FontEngineScope {

    public enum Mode {
        /** Follow the global rendering engine. */
        INHERIT,
        /** Draw with Minecraft's bitmap font. */
        VANILLA,
        /** Draw with a scoped cosmic renderer. */
        COSMIC,
        /** Draw with a scoped compatibility glyph provider. */
        COMPAT
    }

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private FontEngineScope() {
    }

    public static Mode current() {
        Scope scope = CURRENT.get();
        return scope == null ? Mode.INHERIT : scope.mode;
    }

    public static boolean isActive() {
        return current() != Mode.INHERIT;
    }

    public static Mode fromConfig(String value) {
        if (value == null) return Mode.INHERIT;
        switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "vanilla":
            case "minecraft":
            case "bitmap":
                return Mode.VANILLA;
            case "cosmic":
            case "modern":
                return Mode.COSMIC;
            case "compat":
            case "compatibility":
                return Mode.COMPAT;
            default:
                return Mode.INHERIT;
        }
    }

    public static String normalize(String value) {
        return fromConfig(value).name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Runs {@code action} with the scope pushed, restoring the previous value afterwards. */
    public static void run(Mode mode, Runnable action) {
        call(mode, () -> {
            action.run();
            return 0;
        });
    }

    /**
     * Overrides one measurement/draw in Forge's debug columns. With F3 hidden or the setting
     * on inherit, ordinary HUD text keeps its caller's engine. Even a failed font call must
     * restore the previous scope before Forge dispatches another overlay or GUI.
     */
    public static int callForOverlay(boolean debugVisible, Mode mode, IntSupplier action) {
        if (!debugVisible || mode == null || mode == Mode.INHERIT) return action.getAsInt();
        return call(mode, action);
    }

    private static int call(Mode mode, IntSupplier action) {
        Scope previous = CURRENT.get();
        Scope scope = new Scope(mode == null ? Mode.INHERIT : mode);
        CURRENT.set(scope);
        try {
            return action.getAsInt();
        } finally {
            scope.restoreMetrics();
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /**
     * Saves the two width-table entries before a scoped font changes them. Only touched glyphs
     * are saved; copying the entire Unicode table for each F3 row would be wasteful.
     * Returns false outside an explicit scope, where compat keeps its normal persistent metrics.
     */
    public static boolean preserveMetrics(int[] asciiWidths, int asciiIndex,
                                          byte[] unicodeWidths, int codePoint) {
        Scope scope = CURRENT.get();
        if (scope == null || scope.mode == Mode.INHERIT) return false;
        if (scope.metrics == null) scope.metrics = new ArrayList<>();
        int index = asciiIndex >= 0 && asciiIndex < asciiWidths.length ? asciiIndex : -1;
        scope.metrics.add(new MetricChange(asciiWidths, index, index < 0 ? 0 : asciiWidths[index],
                unicodeWidths, codePoint, unicodeWidths[codePoint]));
        return true;
    }

    private static final class Scope {
        final Mode mode;
        List<MetricChange> metrics;

        Scope(Mode mode) { this.mode = mode; }

        void restoreMetrics() {
            if (metrics == null) return;
            // A row may touch the same character several times. Undo in reverse order.
            for (int i = metrics.size() - 1; i >= 0; i--) metrics.get(i).restore();
        }
    }

    private record MetricChange(int[] asciiWidths, int asciiIndex, int asciiWidth,
                                byte[] unicodeWidths, int codePoint, byte unicodeWidth) {
        void restore() {
            if (asciiIndex >= 0) asciiWidths[asciiIndex] = asciiWidth;
            unicodeWidths[codePoint] = unicodeWidth;
        }
    }

    /** The F3 overlay's configured mode. */
    public static Mode overlayMode() {
        return fromConfig(NeofontrenderConfig.debugOverlayEngine());
    }
}
