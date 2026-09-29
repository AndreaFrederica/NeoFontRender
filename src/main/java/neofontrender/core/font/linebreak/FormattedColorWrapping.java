package neofontrender.core.font.linebreak;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextComponentString;
import neofontrender.core.font.pipeline.LayoutText;
import neofontrender.core.font.pipeline.StructuredTextRuntime;
import neofontrender.core.font.pipeline.builtin.HexChatStructuredMiddleware;
import neofontrender.core.font.pipeline.builtin.TinkersAntiqueSyntaxProvider;
import neofontrender.text.StructuredText;

import java.util.List;
import java.util.stream.Collectors;

/** Keeps decoded RGB colors stable when a legacy string API splits a paragraph. */
public final class FormattedColorWrapping {
    private FormattedColorWrapping() {}

    public static String prepare(String source) {
        if (!mayHaveExtendedColor(source)) return source;
        StructuredText parsed = StructuredTextRuntime.parse(source);
        // These protocols need their original source and route-owned geometry.
        if (!parsed.inlineSpans().isEmpty() || !parsed.effects().isEmpty()) return source;
        if (!parsed.appliedMiddlewareIds().contains(HexChatStructuredMiddleware.ID)
                && !parsed.appliedSyntaxProviderIds().contains(TinkersAntiqueSyntaxProvider.INSTANCE.id())) {
            return source;
        }
        LayoutText layout = LayoutText.fromStructured(parsed);
        return layout.formattedRange(0, layout.visibleText().length());
    }

    /** Returns null when this text should stay on its existing wrapping path. */
    public static List<String> wrap(FontRenderer font, String source, int width) {
        String prepared = prepare(source);
        if (prepared == source) return null;
        return CjkComponentLineWrapper.wrap(new TextComponentString(prepared), width, font, true, true)
                .stream().map(component -> component.getFormattedText()).collect(Collectors.toList());
    }

    static String continuation(String before, String after) {
        if (!mayHaveExtendedColor(before)) return FontRenderer.getFormatFromString(before) + after;
        // Read through the next character so a marker immediately before the break takes effect.
        LayoutText layout = LayoutText.process(before + after);
        int offset = LayoutText.process(before).visibleText().length();
        return "\u00a7r" + layout.formattedDisplay(offset, "") + after;
    }

    static int firstSafeBoundary(String source) {
        if (mayHaveExtendedColor(source)) {
            LayoutText layout = LayoutText.process(source);
            if (!layout.visibleText().isEmpty()) {
                return layout.rawEndBoundary(Character.charCount(layout.visibleText().codePointAt(0)));
            }
        }
        int index = 0;
        while (index + 1 < source.length() && source.charAt(index) == '\u00a7') index += 2;
        return index >= source.length() ? index : index + Character.charCount(source.codePointAt(index));
    }

    private static boolean mayHaveExtendedColor(String source) {
        if (source.indexOf('#') >= 0) return true;
        for (int i = 0; i < source.length(); i++) {
            if (TinkersAntiqueSyntaxProvider.isMarker(source.charAt(i))) return true;
        }
        return false;
    }
}
