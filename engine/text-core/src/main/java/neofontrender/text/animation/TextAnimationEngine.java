package neofontrender.text.animation;

import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared glyph-level animation runtime used by syntax providers. */
public final class TextAnimationEngine {
    public GlyphAnimationFrame frame(StructuredText text, long timeMillis) {
        return frame(text, timeMillis, TextAnimationFrame.currentInstanceId());
    }

    public GlyphAnimationFrame frame(StructuredText text, long timeMillis, long instanceId) {
        if (text == null || text.plainText().isEmpty() || text.effects().isEmpty()) {
            return GlyphAnimationFrame.empty(text == null ? 0 : text.plainText().length());
        }
        List<GlyphAnimation> glyphs = new ArrayList<>();
        for (int index = 0; index < text.plainText().length(); index++) {
            List<StructuredEffectSpan> active = new ArrayList<>();
            for (StructuredEffectSpan span : text.effects()) {
                if (span.animationRenderMode() == TextAnimationRenderMode.GLYPH
                        && index >= span.start() && index < span.end()) active.add(span);
            }
            glyphs.add(new GlyphAnimation(index, text.plainText(), active, instanceId));
        }
        return new GlyphAnimationFrame(timeMillis, glyphs);
    }

    public static final class GlyphAnimationFrame {
        private final long timeMillis;
        private final List<GlyphAnimation> glyphs;
        private GlyphAnimationFrame(long timeMillis, List<GlyphAnimation> glyphs) {
            this.timeMillis = timeMillis;
            this.glyphs = Collections.unmodifiableList(glyphs);
        }
        public static GlyphAnimationFrame empty(int length) {
            return new GlyphAnimationFrame(0, Collections.emptyList());
        }
        public long timeMillis() { return timeMillis; }
        public List<GlyphAnimation> glyphs() { return glyphs; }
    }

    public static final class GlyphAnimation {
        private final int plainIndex;
        private final String plainText;
        private final List<StructuredEffectSpan> effects;
        private final long instanceId;
        private GlyphAnimation(int plainIndex, String plainText, List<StructuredEffectSpan> effects,
                               long instanceId) {
            this.plainIndex = plainIndex;
            this.plainText = plainText;
            this.effects = Collections.unmodifiableList(new ArrayList<>(effects));
            this.instanceId = instanceId;
        }
        public int plainIndex() { return plainIndex; }
        public int animationIndex() { return plainText.codePointCount(0, plainIndex); }
        public int codePoint() {
            return plainIndex < plainText.length() ? plainText.codePointAt(plainIndex) : 0;
        }
        public List<StructuredEffectSpan> effects() { return effects; }
        public boolean animated() { return !effects.isEmpty(); }

        /** Stable source identity used by stateful effects such as typewriter. */
        public String timelineKey(StructuredEffectSpan span) {
            return instanceId + "\u0000" + plainText + '\u0000'
                    + span.start() + ':' + span.end() + ':' + span.effectId();
        }

        /** Reveal unit within one typewriter span, using Unicode character/word boundaries. */
        public int revealIndex(StructuredEffectSpan span, boolean byWord) {
            int relative = Math.max(0, plainIndex - span.start());
            if (!byWord) {
                return plainText.codePointCount(span.start(), Math.min(plainIndex, span.end()));
            }
            java.text.BreakIterator iterator = java.text.BreakIterator.getLineInstance(java.util.Locale.ROOT);
            String value = plainText.substring(span.start(), span.end());
            iterator.setText(value);
            int units = 0;
            for (int boundary = iterator.first(), next = iterator.next(); next != java.text.BreakIterator.DONE;
                 boundary = next, next = iterator.next()) {
                if (relative < next) return units;
                units += Math.max(1, Math.min(5, value.codePointCount(boundary, next)));
            }
            return units;
        }
    }
}
