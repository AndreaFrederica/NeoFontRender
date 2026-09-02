package neofontrender.core.font.pipeline.builtin;

import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.api.text.pipeline.ProcessedText;
import neofontrender.api.text.pipeline.RawTextMiddleware;
import neofontrender.api.text.pipeline.TextTrigger;

/**
 * Decodes Tinkers' Construct/TinkersAntique's three-character PUA RGB protocol.
 */
public final class TinkersAntiqueTextPreprocessor implements RawTextMiddleware {
    public static final TinkersAntiqueTextPreprocessor INSTANCE =
            new TinkersAntiqueTextPreprocessor();

    public static final char MARKER_START = '\uE700';
    public static final char MARKER_END = '\uE7FF';

    private TinkersAntiqueTextPreprocessor() {
    }

    @Override public String id() { return "neofontrender:tinkers_rgb"; }
    @Override public int priority() { return 100; }
    @Override public TextTrigger trigger() { return TextTrigger.range(MARKER_START, MARKER_END); }

    @Override
    public boolean isEnabled() {
        return NeofontrenderConfig.compatTinkersAntique();
    }

    public boolean matches(String rawText) {
        if (rawText == null) return false;
        for (int i = 0; i < rawText.length(); i++) {
            if (isMarker(rawText.charAt(i))) return true;
        }
        return false;
    }

    @Override
    public ProcessedText process(ProcessedText input) {
        String rawText = input.visibleText();
        if (rawText.isEmpty() || !matches(rawText)) {
            return null;
        }
        ProcessedText transformed = LegacyColorTextParser.process(rawText, true,
                NeofontrenderConfig.laboratoryHexChat());
        return ProcessedText.compose(input, transformed);
    }

    public static boolean isMarker(char character) {
        return character >= MARKER_START && character <= MARKER_END;
    }

}
