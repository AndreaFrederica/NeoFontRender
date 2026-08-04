package neofontrender.api.text;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CjkParagraphLayoutRegistryTest {
    private static final CjkParagraphLayoutProvider.Request REQUEST =
            new CjkParagraphLayoutProvider.Request("中文", 20, 9, "zh_cn", text -> text.length());

    @AfterEach
    void clearRegistry() {
        CjkParagraphLayoutRegistry.clearForTests();
    }

    @Test
    void choosesHighestPriorityHandledProvider() {
        AtomicInteger lowCalls = new AtomicInteger();
        CjkParagraphLayoutRegistry.register(provider("low", 1, lowCalls, layout(1)));
        CjkParagraphLayoutRegistry.register(provider("high", 10, new AtomicInteger(), layout(2)));

        CjkParagraphLayoutProvider.Layout result = CjkParagraphLayoutRegistry.layout(REQUEST);

        assertNotNull(result);
        assertEquals(2, result.firstRawBoundary(0));
        assertEquals(0, lowCalls.get());
    }

    @Test
    void fallsThroughNullAndProviderFailure() {
        CjkParagraphLayoutRegistry.register(provider("fallback", 0,
                new AtomicInteger(), layout(2)));
        CjkParagraphLayoutRegistry.register(provider("empty", 5,
                new AtomicInteger(), null));
        CjkParagraphLayoutRegistry.register(new CjkParagraphLayoutProvider() {
            @Override public String id() { return "broken"; }
            @Override public int priority() { return 10; }
            @Override public Layout layout(Request request) { throw new LinkageError("test"); }
        });

        assertEquals(2, CjkParagraphLayoutRegistry.layout(REQUEST).firstRawBoundary(0));
    }

    @Test
    void blocksRecursiveDispatch() {
        CjkParagraphLayoutRegistry.register(new CjkParagraphLayoutProvider() {
            @Override public String id() { return "recursive"; }
            @Override public Layout layout(Request request) {
                assertNull(CjkParagraphLayoutRegistry.layout(request));
                return CjkParagraphLayoutRegistryTest.layout(2);
            }
        });

        assertNotNull(CjkParagraphLayoutRegistry.layout(REQUEST));
    }

    private static CjkParagraphLayoutProvider provider(
            String id, int priority, AtomicInteger calls, CjkParagraphLayoutProvider.Layout layout) {
        return new CjkParagraphLayoutProvider() {
            @Override public String id() { return id; }
            @Override public int priority() { return priority; }
            @Override public Layout layout(Request request) {
                calls.incrementAndGet();
                return layout;
            }
        };
    }

    private static CjkParagraphLayoutProvider.Layout layout(int end) {
        return new CjkParagraphLayoutProvider.Layout(Collections.singletonList(
                new CjkParagraphLayoutProvider.Line(0, end, 0, false,
                        Collections.emptyList())));
    }
}
