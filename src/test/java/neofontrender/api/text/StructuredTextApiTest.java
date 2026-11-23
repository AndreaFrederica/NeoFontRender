package neofontrender.api.text;

import neofontrender.text.StructuredText;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.StructuredTextRewriter;
import neofontrender.text.pipeline.TextPipelinePlugin;
import neofontrender.text.syntax.SyntaxCursor;
import neofontrender.text.syntax.SyntaxMatch;
import neofontrender.text.syntax.SyntaxOperation;
import neofontrender.text.syntax.TextSyntaxProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredTextApiTest {
    @Test
    void registersAndRemovesSyntaxProviders() {
        TextSyntaxProvider provider = new TextSyntaxProvider() {
            @Override public String id() { return "test:bold_marker"; }
            @Override public int priority() { return 500; }
            @Override public char trigger() { return '~'; }
            @Override public SyntaxMatch match(SyntaxCursor cursor) {
                return cursor.charAt(0) == '~'
                        ? SyntaxMatch.operations(1, SyntaxOperation.bold(true)) : null;
            }
        };

        try (StructuredTextRegistration ignored = StructuredTextApi.register(provider)) {
            StructuredText parsed = StructuredTextApi.parse("~text");
            assertEquals("text", parsed.plainText());
            assertTrue(parsed.styleAt(0).bold());
            assertTrue(parsed.appliedSyntaxProviderIds().contains(provider.id()));
        }

        StructuredText parsed = StructuredTextApi.parse("~text");
        assertEquals("~text", parsed.plainText());
        assertFalse(parsed.styleAt(0).bold());
    }

    @Test
    void registersStructuredMiddlewareWithSourceMapping() {
        StructuredTextMiddleware middleware = new StructuredTextMiddleware() {
            @Override public String id() { return "test:brackets"; }
            @Override public StructuredText process(StructuredText input) {
                int start = input.plainText().indexOf('[');
                int end = input.plainText().indexOf(']', start + 1);
                if (start < 0 || end < 0) return input;
                return StructuredTextRewriter.rewrite(input, Collections.singletonList(
                        StructuredTextRewriter.text(start, end + 1,
                                input.plainText().substring(start + 1, end), style -> style)), id());
            }
        };
        TextPipelinePlugin plugin = new TextPipelinePlugin() {
            @Override public String id() { return "test:plugin"; }
            @Override public java.util.Collection<? extends StructuredTextMiddleware>
            structuredMiddlewares() { return Collections.singletonList(middleware); }
        };

        try (StructuredTextRegistration ignored = StructuredTextApi.register(plugin)) {
            StructuredText parsed = StructuredTextApi.parse("A[x]B");
            assertEquals("AxB", parsed.plainText());
            assertEquals(1, parsed.sourceMap().sourceStart(1));
            assertEquals(4, parsed.sourceMap().sourceEnd(2));
            assertTrue(parsed.appliedMiddlewareIds().contains(middleware.id()));
        }
    }
}
