package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CosmicAdaptiveTextTemplateCacheTest {
    private static final int ARGB = 0xFFFFFFFF;

    @Test
    void compilesOnlyAfterWarmupAndAChangingValue() {
        CosmicAdaptiveTextTemplateCache cache = new CosmicAdaptiveTextTemplateCache();
        AtomicLong now = new AtomicLong();
        for (int i = 0; i < 7; i++) {
            assertNull(cache.observe("XYZ: 123", ARGB, 0, 9.0F, 12.0F,
                    true, now.addAndGet(75_000_000L)));
        }
        assertNotNull(cache.observe("XYZ: 124", ARGB, 0, 9.0F, 12.0F,
                true, now.addAndGet(75_000_000L)));
        CosmicAdaptiveTextTemplateCache.Plan plan = cache.observe("XYZ: 125", ARGB, 0,
                9.0F, 12.0F, true, now.addAndGet(75_000_000L));
        assertNotNull(plan);
        assertEquals(2, plan.segments().size());
        assertEquals("XYZ: ", plan.segments().get(0).text());
        assertTrue(plan.segments().get(1).dynamic());
        assertEquals(1, cache.stats().compiledEntries);
    }

    @Test
    void sameSkeletonWithMultipleNumericSlotsIsInstantiatedPerDraw() {
        CosmicAdaptiveTextTemplateCache cache = new CosmicAdaptiveTextTemplateCache();
        AtomicLong now = new AtomicLong();
        String[] values = {"XYZ: 123 / 45", "XYZ: 124 / 46", "XYZ: 125 / 47"};
        for (int i = 0; i < 7; i++) {
            assertNull(cache.observe(values[i % values.length], ARGB, 0, 9.0F, 12.0F,
                    true, now.addAndGet(75_000_000L)));
        }
        assertNotNull(cache.observe("XYZ: 126 / 48", ARGB, 0, 9.0F, 12.0F,
                true, now.addAndGet(75_000_000L)));
        CosmicAdaptiveTextTemplateCache.Plan plan = cache.observe("XYZ: 127 / 49", ARGB, 0,
                9.0F, 12.0F, true, now.addAndGet(75_000_000L));
        assertNotNull(plan);
        assertEquals(4, plan.segments().size());
        assertEquals("127", plan.segments().get(1).text());
        assertEquals("49", plan.segments().get(3).text());
    }

    @Test
    void rejectsIdentifiersUnicodeAndPureNumbers() {
        CosmicAdaptiveTextTemplateCache cache = new CosmicAdaptiveTextTemplateCache();
        AtomicLong now = new AtomicLong();
        for (String value : new String[]{"F3", "X3Y", "foo_123", "12345", "坐标 123"}) {
            assertNull(cache.observe(value, ARGB, 0, 9.0F, 12.0F, true,
                    now.addAndGet(1_000_000L)));
        }
        assertTrue(cache.stats().rejected >= 5);
    }

    @Test
    void disabledAndClearDoNotReuseLearnedPlans() {
        CosmicAdaptiveTextTemplateCache cache = new CosmicAdaptiveTextTemplateCache();
        AtomicLong now = new AtomicLong();
        for (int i = 0; i < 8; i++) {
            cache.observe("HP: " + (100 + i), ARGB, 0, 9.0F, 12.0F,
                    true, now.addAndGet(75_000_000L));
        }
        assertNotNull(cache.observe("HP: 108", ARGB, 0, 9.0F, 12.0F,
                true, now.addAndGet(75_000_000L)));
        assertNull(cache.observe("HP: 109", ARGB, 0, 9.0F, 12.0F,
                false, now.addAndGet(75_000_000L)));
        cache.clear();
        assertNull(cache.observe("HP: 110", ARGB, 0, 9.0F, 12.0F,
                true, now.addAndGet(75_000_000L)));
        assertEquals(1, cache.stats().entries);
        assertEquals(0, cache.stats().compiledEntries);
    }

    @Test
    void boundsLearnedTemplatesWithLruEviction() {
        CosmicAdaptiveTextTemplateCache cache = new CosmicAdaptiveTextTemplateCache();
        AtomicLong now = new AtomicLong();
        char[] punctuation = "!#$%&'()*+,-./:;<=>?@[\\]^`{|}~".toCharArray();
        for (int value = 0; value < 300; value++) {
            int encoded = value;
            StringBuilder prefix = new StringBuilder();
            do {
                prefix.append(punctuation[encoded % punctuation.length]);
                encoded /= punctuation.length;
            } while (encoded != 0);
            cache.observe(prefix + " 1", ARGB, 0, 9.0F, 12.0F, true,
                    now.addAndGet(1_000_000L));
        }
        assertEquals(256, cache.stats().entries);
        assertTrue(cache.stats().evictions > 0);
    }
}
