package neofontrender.core.font.support;

import java.awt.Font;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Converts font files in the game directory between portable config locations and real files. */
public final class FontFileResolver {
    public static final String GAME_FONT_PREFIX = "neofontrender/fonts/";
    private static final String[] STYLE_SUFFIXES = {
            " Extra Bold", " ExtraBold", " Demi Bold", " DemiBold", " Semi Bold", " SemiBold",
            " Extra Light", " ExtraLight", " Bold Italic", " Bold Oblique", " SemiBold Italic",
            " Medium Italic", " Italic", " Oblique", " Regular", " Normal", " Medium",
            " Semilight", " DemiLight", " Light", " Thin", " Bold", " Heavy", " Black"
    };

    private FontFileResolver() {
    }

    public static String normalizeLocation(File gameDirectory, String location) {
        if (location == null || location.isEmpty()) {
            return location;
        }
        File file = new File(location);
        return file.isAbsolute() ? portableLocation(gameDirectory, file) : location;
    }

    public static String portableLocation(File gameDirectory, File fontFile) {
        Path fontRoot = fontRoot(gameDirectory);
        Path target = fontFile.toPath().toAbsolutePath().normalize();
        if (!target.startsWith(fontRoot) || target.equals(fontRoot)) {
            return fontFile.getPath();
        }
        return GAME_FONT_PREFIX + fontRoot.relativize(target).toString().replace('\\', '/');
    }

    public static File resolve(File gameDirectory, String location) {
        if (location == null || location.isEmpty()) {
            return new File("");
        }
        String portable = location.replace('\\', '/');
        if (portable.startsWith(GAME_FONT_PREFIX)) {
            return new File(gameDirectory, portable.replace('/', File.separatorChar));
        }
        File direct = new File(location);
        if (direct.isFile() || looksLikeLocation(location)) {
            return direct;
        }
        File byFamily = findGameFontByFamily(gameDirectory, location);
        return byFamily == null ? direct : byFamily;
    }

    public static List<String> normalizeLocations(File gameDirectory, Iterable<?> values) {
        List<String> normalized = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            for (String part : value.toString().split("[,;]")) {
                String location = normalizeLocation(gameDirectory, part.trim());
                if (!location.isEmpty() && !normalized.contains(location)) {
                    normalized.add(location);
                }
            }
        }
        return normalized;
    }

    /**
     * Finds every file in the game font folder whose internal family metadata matches the selected
     * family. This lets native backends register split Regular/Bold/Italic files as one family.
     */
    public static List<File> familyFiles(File gameDirectory, String family) {
        List<File> matches = new ArrayList<>();
        if (family == null || family.trim().isEmpty()) return matches;
        String requestedFamily = normalizeFamilyName(family);
        File directory = fontRoot(gameDirectory).toFile();
        File[] files = directory.listFiles(file -> file.isFile() && looksLikeLocation(file.getName()));
        if (files == null) return matches;
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            try {
                if (normalizeFamilyName(familyName(file)).equalsIgnoreCase(requestedFamily)) {
                    matches.add(file);
                }
            } catch (Throwable ignored) {
                // Invalid/unsupported collections are not part of the discoverable family.
            }
        }
        return matches;
    }

    /** Canonical family used by the UI and directory grouping, with style suffixes removed. */
    public static String familyName(File file) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, file);
            String face = normalizeFamilyName(font.getFontName(Locale.ROOT));
            return face.isEmpty() ? normalizeFamilyName(font.getFamily(Locale.ROOT)) : face;
        } catch (Throwable ignored) {
            String name = file == null ? "" : file.getName();
            int dot = name.lastIndexOf('.');
            return normalizeFamilyName(dot > 0 ? name.substring(0, dot) : name);
        }
    }

    public static String faceName(File file) {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, file).getFontName(Locale.ROOT);
        } catch (Throwable ignored) {
            String name = file == null ? "" : file.getName();
            int dot = name.lastIndexOf('.');
            return dot > 0 ? name.substring(0, dot) : name;
        }
    }

    private static Path fontRoot(File gameDirectory) {
        return new File(gameDirectory, "neofontrender" + File.separator + "fonts")
                .toPath().toAbsolutePath().normalize();
    }

    private static File findGameFontByFamily(File gameDirectory, String family) {
        for (File file : familyFiles(gameDirectory, family)) {
            return file;
        }
        return null;
    }

    static String normalizeFamilyName(String value) {
        String result = value == null ? "" : value.trim();
        boolean changed;
        do {
            changed = false;
            for (String suffix : STYLE_SUFFIXES) {
                if (result.length() > suffix.length()
                        && result.regionMatches(true, result.length() - suffix.length(),
                        suffix, 0, suffix.length())) {
                    result = result.substring(0, result.length() - suffix.length()).trim();
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return result;
    }

    private static boolean looksLikeLocation(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc")
                || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0;
    }
}
