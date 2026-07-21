package neofontrender.client.gui.util;

import neofontrender.client.gui.font.FontEntry;
import neofontrender.core.config.NeofontrenderConfig;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Discovers the font choices used by the client settings screen. */
public final class FontCatalog {
    private FontCatalog() {
    }

    public static List<String> localFonts() {
        try {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
            Set<String> installed = new LinkedHashSet<>();
            for (String name : names) installed.add(name.toLowerCase(Locale.ROOT));
            List<String> fonts = new ArrayList<>();
            for (String name : names) if (!isExposedStyleFamily(name, installed)) fonts.add(name);
            Collections.sort(fonts, String.CASE_INSENSITIVE_ORDER);
            return fonts;
        } catch (Throwable ignored) {
            return Collections.singletonList("SansSerif");
        }
    }

    public static List<FontEntry> folderFonts() {
        File dir = NeofontrenderConfig.ensureFontDirectory();
        File[] files = dir.listFiles(file -> file.isFile() && isFontFile(file.getName()));
        if (files == null || files.length == 0) return Collections.emptyList();
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        List<FontEntry> fonts = new ArrayList<>();
        for (File file : files) fonts.add(new FontEntry(file.getName(), file.getAbsolutePath()));
        return fonts;
    }

    public static List<FontEntry> builtinFonts() {
        List<FontEntry> fonts = new ArrayList<>();
        for (NeofontrenderConfig.BuiltinFont font : NeofontrenderConfig.builtinFonts()) {
            fonts.add(new FontEntry(font.displayName(), font.location()));
        }
        return fonts;
    }

    public static boolean isFontFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
    }

    private static boolean isExposedStyleFamily(String name, Set<String> installedFamilies) {
        String candidate = name;
        boolean stripped = false;
        String[] suffixes = {" Extra Bold", " ExtraBold", " Demi Bold", " DemiBold", " Semi Bold", " SemiBold",
                " Extra Light", " ExtraLight", " Bold Italic", " Bold Oblique", " Italic", " Oblique", " Regular",
                " Normal", " Medium", " Semilight", " DemiLight", " Light", " Thin", " Bold", " Heavy", " Black"};
        boolean changed;
        do {
            changed = false;
            for (String suffix : suffixes) {
                if (candidate.length() > suffix.length()
                        && candidate.regionMatches(true, candidate.length() - suffix.length(), suffix, 0, suffix.length())) {
                    candidate = candidate.substring(0, candidate.length() - suffix.length()).trim();
                    stripped = true;
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return stripped && installedFamilies.contains(candidate.toLowerCase(Locale.ROOT));
    }
}
