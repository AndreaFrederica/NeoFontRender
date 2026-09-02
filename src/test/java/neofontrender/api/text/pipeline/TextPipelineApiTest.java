package neofontrender.api.text.pipeline;

import net.minecraft.client.gui.FontRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextPipelineApiTest {
    private static final ParagraphLayoutMiddleware.Request PARAGRAPH_REQUEST =
            new ParagraphLayoutMiddleware.Request("text", 20, 9, "en_us", text -> text.length());

    @AfterEach
    void clearRegistry() {
        TextPipelineApi.clearForTests();
    }

    @Test
    void indexesInlineDispatchByTriggerAndPriority() {
        AtomicInteger starCalls = new AtomicInteger();
        AtomicInteger dollarCalls = new AtomicInteger();
        TestContent low = new TestContent("low");
        TestContent high = new TestContent("high");
        TextPipelineApi.register(inline("test:star", 1, '*', starCalls, low, true));
        TextPipelineApi.register(inline("test:dollar_low", 1, '$', dollarCalls, low, true));
        TextPipelineApi.register(inline("test:dollar_high", 10, '$', dollarCalls, high, true));

        assertNull(TextPipelineApi.matchInline("plain", 0));
        assertEquals(0, starCalls.get());
        assertEquals(0, dollarCalls.get());
        assertSame(high, TextPipelineApi.matchInline("$", 0).content());
        assertEquals(1, dollarCalls.get());
    }

    @Test
    void replacementHandleCannotRemoveNewerRegistration() {
        TestContent old = new TestContent("old");
        TestContent replacement = new TestContent("replacement");
        TextMiddlewareRegistration oldHandle = TextPipelineApi.register(
                inline("test:same", 1, 'x', new AtomicInteger(), old, true));
        TextMiddlewareRegistration replacementHandle = TextPipelineApi.register(
                inline("test:same", 1, 'x', new AtomicInteger(), replacement, true));

        oldHandle.close();
        assertSame(replacement, TextPipelineApi.matchInline("x", 0).content());
        replacementHandle.close();
        assertFalse(TextPipelineApi.hasInlineContentMiddleware());
    }

    @Test
    void skipsDisabledAndIsolatesFailingProviders() {
        TextPipelineApi.register(inline("test:fallback", 0, 'x', new AtomicInteger(),
                new TestContent("fallback"), true));
        TextPipelineApi.register(inline("test:disabled", 20, 'x', new AtomicInteger(),
                new TestContent("disabled"), false));
        TextPipelineApi.register(new InlineContentMiddleware() {
            @Override public String id() { return "test:broken"; }
            @Override public int priority() { return 10; }
            @Override public TextTrigger trigger() { return TextTrigger.exact('x'); }
            @Override public InlineContentMatch match(CharSequence source, int sourceIndex) {
                throw new LinkageError("test");
            }
        });

        assertEquals("fallback", TextPipelineApi.matchInline("x", 0).content().description());
    }

    @Test
    void cachesDisabledFastPathUntilExplicitInvalidation() {
        boolean[] enabled = {false};
        TextPipelineApi.register(new InlineContentMiddleware() {
            @Override public String id() { return "test:dynamic"; }
            @Override public boolean isEnabled() { return enabled[0]; }
            @Override public TextTrigger trigger() { return TextTrigger.exact('x'); }
            @Override public InlineContentMatch match(CharSequence source, int sourceIndex) {
                return new InlineContentMatch(sourceIndex, sourceIndex + 1,
                        new TestContent("dynamic"));
            }
        });

        assertFalse(TextPipelineApi.hasInlineContentMiddleware());
        enabled[0] = true;
        assertFalse(TextPipelineApi.hasInlineContentMiddleware());
        TextPipelineApi.invalidate();
        assertTrue(TextPipelineApi.hasInlineContentMiddleware());
    }

    @Test
    void blocksRecursiveInlineDispatch() {
        TextPipelineApi.register(new InlineContentMiddleware() {
            @Override public String id() { return "test:recursive_inline"; }
            @Override public TextTrigger trigger() { return TextTrigger.exact('x'); }
            @Override public InlineContentMatch match(CharSequence source, int sourceIndex) {
                assertNull(TextPipelineApi.matchInline(source, sourceIndex));
                return new InlineContentMatch(sourceIndex, sourceIndex + 1,
                        new TestContent("recursive"));
            }
        });

        assertNotNull(TextPipelineApi.matchInline("x", 0));
    }

    @Test
    void paragraphDispatchUsesPriorityAndBlocksRecursion() {
        TextPipelineApi.register(new ParagraphLayoutMiddleware() {
            @Override public String id() { return "test:fallback"; }
            @Override public Layout layout(Request request) { return paragraph(1); }
        });
        TextPipelineApi.register(new ParagraphLayoutMiddleware() {
            @Override public String id() { return "test:recursive"; }
            @Override public int priority() { return 10; }
            @Override public Layout layout(Request request) {
                assertNull(TextPipelineApi.layoutParagraph(request));
                return paragraph(2);
            }
        });

        assertEquals(2, TextPipelineApi.layoutParagraph(PARAGRAPH_REQUEST).firstRawBoundary(0));
    }

    @Test
    void paragraphMiddlewareCanConsumeRawMiddlewareWithoutOpeningRawRecursion() {
        AtomicInteger rawCalls = new AtomicInteger();
        TextPipelineApi.register(new RawTextMiddleware() {
            @Override public String id() { return "test:raw"; }
            @Override public TextTrigger trigger() { return TextTrigger.exact('<'); }
            @Override public ProcessedText process(ProcessedText input) {
                rawCalls.incrementAndGet();
                assertFalse(TextPipelineApi.processRaw(input.visibleText()).transformed());
                String visible = input.visibleText().replace("<raw>", "");
                return ProcessedText.transformed(input.visibleText(), visible,
                        neofontrender.api.text.ModernText.of(visible),
                        new int[] {5, 6}, new int[] {5, 6});
            }
        });
        TextPipelineApi.register(new ParagraphLayoutMiddleware() {
            @Override public String id() { return "test:paragraph_consumes_raw"; }
            @Override public Layout layout(Request request) {
                ProcessedText processed = TextPipelineApi.processRaw(request.formattedText());
                assertTrue(processed.transformed());
                assertEquals("x", processed.visibleText());
                return paragraph(processed.rawEndForVisibleBoundary(1));
            }
        });

        ParagraphLayoutMiddleware.Request request = new ParagraphLayoutMiddleware.Request(
                "<raw>x", 20, 9, "en_us", text -> text.length());
        assertEquals(6, TextPipelineApi.layoutParagraph(request).firstRawBoundary(0));
        assertEquals(1, rawCalls.get());
    }

    @Test
    void composedRawTransformsRetainOriginalBoundaries() {
        ProcessedText first = ProcessedText.transformed("a<X>b", "ab",
                neofontrender.api.text.ModernText.of("ab"),
                new int[] {0, 1, 5}, new int[] {0, 4, 5});
        ProcessedText second = ProcessedText.transformed("ab", "b",
                neofontrender.api.text.ModernText.of("b"),
                new int[] {1, 2}, new int[] {1, 2});

        ProcessedText composed = ProcessedText.compose(first, second);

        assertEquals("a<X>b", composed.rawText());
        assertEquals("b", composed.visibleText());
        assertEquals(1, composed.rawStartForVisibleBoundary(0));
        assertEquals(5, composed.rawEndForVisibleBoundary(1));
    }

    private static InlineContentMiddleware inline(String id, int priority, char trigger,
                                                   AtomicInteger calls, InlineContent content,
                                                   boolean enabled) {
        return new InlineContentMiddleware() {
            @Override public String id() { return id; }
            @Override public int priority() { return priority; }
            @Override public boolean isEnabled() { return enabled; }
            @Override public TextTrigger trigger() { return TextTrigger.exact(trigger); }
            @Override public InlineContentMatch match(CharSequence source, int sourceIndex) {
                calls.incrementAndGet();
                return new InlineContentMatch(sourceIndex, sourceIndex + 1, content);
            }
        };
    }

    private static ParagraphLayoutMiddleware.Layout paragraph(int end) {
        return new ParagraphLayoutMiddleware.Layout(Collections.singletonList(
                new ParagraphLayoutMiddleware.Line(0, end, 0, false,
                        Collections.emptyList())));
    }

    private static final class TestContent implements InlineContent {
        private final String description;
        TestContent(String description) { this.description = description; }
        @Override public int advance(FontRenderer font) { return 1; }
        @Override public int height(FontRenderer font) { return 1; }
        @Override public void draw(float x, float y, int argb, boolean shadow) {}
        @Override public String description() { return description; }
    }
}
