package neofontrender.text.syntax;

import neofontrender.text.SourceMap;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.StyledSpan;
import neofontrender.text.TextStyle;
import neofontrender.text.UnresolvedSyntax;
import neofontrender.text.animation.TextAnimationRenderMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Single-pass syntax dispatcher and semantic-state reducer. */
public final class TextSyntaxEngine {
    private final List<TextSyntaxProvider> providers;
    private final Set<Character> triggers;

    private TextSyntaxEngine(List<TextSyntaxProvider> providers) {
        List<TextSyntaxProvider> ordered = new ArrayList<>(providers);
        ordered.sort(Comparator.comparingInt(TextSyntaxProvider::priority).reversed()
                .thenComparing(TextSyntaxProvider::id));
        this.providers = Collections.unmodifiableList(ordered);
        Set<Character> foundTriggers = new HashSet<>();
        for (TextSyntaxProvider provider : ordered) foundTriggers.add(provider.trigger());
        this.triggers = Collections.unmodifiableSet(foundTriggers);
    }

    public static Builder builder() { return new Builder(); }

    public StructuredText parse(String rawText) {
        String source = rawText == null ? "" : rawText;
        Parser parser = new Parser(source);
        while (parser.sourceIndex < source.length()) {
            char current = source.charAt(parser.sourceIndex);
            SyntaxMatch selected = null;
            TextSyntaxProvider owner = null;
            boolean acceptedTrigger = false;
            SyntaxCursor cursor = parser.cursor();
            for (TextSyntaxProvider provider : providers) {
                if (!provider.isEnabled()) continue;
                if (provider.isFallback()) continue;
                if (!provider.acceptsTrigger(current)) continue;
                acceptedTrigger = true;
                SyntaxMatch candidate = provider.match(cursor);
                if (candidate == null || candidate.consumedLength() > cursor.remaining()) continue;
                if (selected == null || candidate.consumedLength() > selected.consumedLength()) {
                    selected = candidate;
                    owner = provider;
                }
            }
            if (selected == null) {
                for (TextSyntaxProvider provider : providers) {
                    if (!provider.isEnabled()) continue;
                    if (!provider.isFallback() || !provider.acceptsTrigger(current)) continue;
                    acceptedTrigger = true;
                    SyntaxMatch candidate = provider.match(cursor);
                    if (candidate == null || candidate.consumedLength() > cursor.remaining()) continue;
                    if (selected == null || candidate.consumedLength() > selected.consumedLength()) {
                        selected = candidate;
                        owner = provider;
                    }
                }
            }
            if (selected == null) {
                if (acceptedTrigger || triggers.contains(current)) parser.recordUnresolved();
                parser.emitSourceCodePoint();
            } else {
                parser.apply(selected, Objects.requireNonNull(owner, "owner"));
            }
        }
        return parser.finish();
    }

    public List<String> providerIds() {
        List<String> ids = new ArrayList<>(providers.size());
        for (TextSyntaxProvider provider : providers) if (provider.isEnabled()) ids.add(provider.id());
        return Collections.unmodifiableList(ids);
    }

    public TextAnimationRenderMode animationMode(StructuredText text) {
        if (text == null) return TextAnimationRenderMode.WHOLE_RUN;
        for (StructuredEffectSpan effect : text.effects()) {
            if (effect.animationRenderMode() == TextAnimationRenderMode.GLYPH) {
                return TextAnimationRenderMode.GLYPH;
            }
        }
        return TextAnimationRenderMode.WHOLE_RUN;
    }

    public static final class Builder {
        private final List<TextSyntaxProvider> providers = new ArrayList<>();
        private final Map<String, String> fixedOwners = new HashMap<>();

        public Builder register(TextSyntaxProvider provider) {
            Objects.requireNonNull(provider, "provider");
            if (!provider.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Provider id must be namespaced: " + provider.id());
            }
            for (TextSyntaxProvider existing : providers) {
                if (existing.id().equals(provider.id())) {
                    throw new IllegalArgumentException("Duplicate syntax provider id: " + provider.id());
                }
            }
            if (provider instanceof FixedCodeSyntaxProvider) {
                FixedCodeSyntaxProvider fixed = (FixedCodeSyntaxProvider) provider;
                for (char code : fixed.codes()) {
                    String key = Integer.toHexString(provider.trigger()) + ':' + Character.toLowerCase(code);
                    String previous = fixedOwners.putIfAbsent(key, provider.id());
                    if (previous != null) {
                        throw new IllegalArgumentException("Syntax " + key + " is already owned by " + previous);
                    }
                }
            }
            providers.add(provider);
            return this;
        }

        public TextSyntaxEngine build() { return new TextSyntaxEngine(providers); }
    }

    private static final class Parser {
        final String source;
        final StringBuilder plain = new StringBuilder();
        final List<Integer> starts = new ArrayList<>();
        final List<Integer> ends = new ArrayList<>();
        final List<StyledSpan> styles = new ArrayList<>();
        final List<StructuredEffectSpan> effects = new ArrayList<>();
        final List<UnresolvedSyntax> unresolved = new ArrayList<>();
        final Set<String> appliedProviders = new java.util.LinkedHashSet<>();
        final List<ActiveEffect> activeEffects = new ArrayList<>();
        TextStyle style = TextStyle.DEFAULT;
        int styleStart;
        int sourceIndex;
        int lineStartPlainIndex;
        boolean lineLeading = true;

        Parser(String source) {
            this.source = source;
            starts.add(0);
            ends.add(0);
        }

        SyntaxCursor cursor() {
            Set<String> groups = new HashSet<>();
            for (ActiveEffect active : activeEffects) groups.add(active.descriptor.groupId());
            String top = activeEffects.isEmpty() ? null
                    : activeEffects.get(activeEffects.size() - 1).descriptor.groupId();
            return new SyntaxCursor(source, sourceIndex, plain.length(), lineStartPlainIndex,
                    lineLeading, groups, top);
        }

        void recordUnresolved() {
            int end = Math.min(source.length(), sourceIndex + 2);
            unresolved.add(new UnresolvedSyntax(sourceIndex, end, source.substring(sourceIndex, end)));
        }

        void emitSourceCodePoint() {
            int codePoint = source.codePointAt(sourceIndex);
            int count = Character.charCount(codePoint);
            String emitted = source.substring(sourceIndex, sourceIndex + count);
            emit(emitted, sourceIndex, sourceIndex + count);
            sourceIndex += count;
        }

        void apply(SyntaxMatch match, TextSyntaxProvider owner) {
            appliedProviders.add(owner.id());
            int start = sourceIndex;
            int end = sourceIndex + match.consumedLength();
            for (SyntaxOperation operation : match.operations()) apply(operation);
            ends.set(ends.size() - 1, end);
            if (!match.emittedText().isEmpty()) emit(match.emittedText(), start, end);
            sourceIndex = end;
        }

        void apply(SyntaxOperation operation) {
            closeEffectsFor(operation.event());
            TextStyle next = style;
            switch (operation.kind()) {
                case SET_COLOR: next = style.withColor(operation.color()); break;
                case SET_COLOR_OVERRIDE: next = style.withColorOverride(operation.color()); break;
                case RESET_STYLE: next = TextStyle.DEFAULT; break;
                case SET_OBFUSCATED: next = style.withObfuscated(operation.enabled()); break;
                case SET_BOLD: next = style.withBold(operation.enabled()); break;
                case SET_ITALIC: next = style.withItalic(operation.enabled()); break;
                case SET_UNDERLINE: next = style.withUnderline(operation.enabled()); break;
                case SET_STRIKETHROUGH: next = style.withStrikethrough(operation.enabled()); break;
                case BEGIN_EFFECT: beginEffect(operation.effect()); break;
                case END_EFFECT: closeEffect(operation.effectGroup(), plain.length()); break;
                default: throw new IllegalStateException("Unhandled syntax operation " + operation.kind());
            }
            if (!style.equals(next)) {
                closeStyle(plain.length());
                style = next;
                styleStart = plain.length();
            }
        }

        void closeEffectsFor(SyntaxEvent event) {
            for (int index = activeEffects.size() - 1; index >= 0; index--) {
                if (activeEffects.get(index).descriptor.terminationEvents().contains(event)) {
                    closeEffectAt(index, plain.length());
                }
            }
        }

        void beginEffect(EffectDescriptor descriptor) {
            if (!descriptor.stackable()) closeEffect(descriptor.groupId(), plain.length());
            int start = descriptor.lineWide() && lineLeading
                    ? lineStartPlainIndex : plain.length();
            activeEffects.add(new ActiveEffect(descriptor, start));
        }

        void closeEffect(String groupId, int end) {
            for (int index = activeEffects.size() - 1; index >= 0; index--) {
                if (activeEffects.get(index).descriptor.groupId().equals(groupId)) {
                    closeEffectAt(index, end);
                    return;
                }
            }
        }

        void closeEffectAt(int index, int end) {
            ActiveEffect active = activeEffects.remove(index);
            if (active != null && end > active.start) {
                effects.add(new StructuredEffectSpan(active.start, end,
                        active.descriptor.effectId(), active.descriptor.parameters(),
                        active.descriptor.lineWide(), active.descriptor.animationRenderMode()));
            }
        }

        void emit(String value, int rawStart, int rawEnd) {
            if (value.isEmpty()) return;
            for (int index = 0; index < value.length(); index++) {
                char ch = value.charAt(index);
                plain.append(ch);
                int mapped = value.length() == rawEnd - rawStart
                        ? rawStart + index + 1 : rawEnd;
                starts.add(mapped);
                ends.add(mapped);
                if (ch == '\n') {
                    lineStartPlainIndex = plain.length();
                    lineLeading = true;
                } else if (!Character.isWhitespace(ch)) {
                    lineLeading = false;
                }
            }
        }

        void closeStyle(int end) {
            if (end > styleStart) styles.add(new StyledSpan(styleStart, end, style));
        }

        StructuredText finish() {
            closeStyle(plain.length());
            while (!activeEffects.isEmpty()) {
                closeEffectAt(activeEffects.size() - 1, plain.length());
            }
            int[] startMap = starts.stream().mapToInt(Integer::intValue).toArray();
            int[] endMap = ends.stream().mapToInt(Integer::intValue).toArray();
            return new StructuredText(source, plain.toString(), styles, effects, unresolved,
                    new SourceMap(source.length(), startMap, endMap),
                    new ArrayList<>(appliedProviders));
        }
    }

    private static final class ActiveEffect {
        final EffectDescriptor descriptor;
        final int start;

        ActiveEffect(EffectDescriptor descriptor, int start) {
            this.descriptor = descriptor;
            this.start = start;
        }
    }
}
