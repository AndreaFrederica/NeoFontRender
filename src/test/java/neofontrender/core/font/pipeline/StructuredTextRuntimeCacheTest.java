package neofontrender.core.font.pipeline;

import neofontrender.api.text.StructuredTextRegistration;
import neofontrender.text.StructuredText;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.TextPipelinePlugin;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class StructuredTextRuntimeCacheTest {
    @Test
    void identicalParagraphUsesOneStructuredEvaluationUntilInvalidated() {
        AtomicInteger evaluations = new AtomicInteger();
        StructuredTextMiddleware middleware = new StructuredTextMiddleware() {
            @Override public String id() { return "neofontrender_test:counting_cache"; }
            @Override public StructuredText process(StructuredText input) {
                evaluations.incrementAndGet();
                return input;
            }
        };
        TextPipelinePlugin plugin = new TextPipelinePlugin() {
            @Override public String id() { return "neofontrender_test:cache"; }
            @Override public Collection<? extends StructuredTextMiddleware> structuredMiddlewares() {
                return Collections.singletonList(middleware);
            }
        };

        try (StructuredTextRegistration ignored = StructuredTextRuntime.register(plugin)) {
            StructuredText first = StructuredTextRuntime.parse("paragraph cache probe");
            StructuredText second = StructuredTextRuntime.parse("paragraph cache probe");

            assertSame(first, second);
            assertEquals(1, evaluations.get());

            StructuredTextRuntime.invalidate();
            StructuredText third = StructuredTextRuntime.parse("paragraph cache probe");
            assertNotSame(first, third);
            assertEquals(2, evaluations.get());
        }
    }
}
