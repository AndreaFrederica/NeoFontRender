package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CosmicMonospaceComposerTest {
    @Test
    void pendingBackgroundProbeDoesNotPoisonThePlanCacheOrIncurSynchronousDebt() {
        AtomicLong now = new AtomicLong();
        AtomicInteger calls = new AtomicInteger();
        CosmicMonospaceComposer composer = new CosmicMonospaceComposer((text, flags, size, scale) -> {
            now.addAndGet(1_000_000L);
            return calls.incrementAndGet() < 3 ? null : new int[]{1, 2};
        }, now::get);
        assertEquals(List.of("12"), composer.split("12", 0, 9, 12, true, true, true));
        assertEquals(List.of("12"), composer.split("12", 0, 9, 12, true, true, true));
        assertEquals(List.of("1", "2"), composer.split("12", 0, 9, 12, true, true, true));
        assertEquals(List.of("1", "2"), composer.split("12", 0, 9, 12, true, true, true));
        assertEquals(3, calls.get());
    }
    @Test
    void disabledAndComplexSequencesDoNotProbeOrReuseAnEnabledPlan() {
        AtomicInteger calls = new AtomicInteger();
        CosmicMonospaceComposer composer = new CosmicMonospaceComposer((text, flags, size, scale) -> {
            calls.incrementAndGet();
            return new int[]{1, 2};
        }, () -> 0L);
        assertEquals(List.of("1", "2"), composer.split("12", 0, 9, 12, true));
        assertEquals(List.of("12"), composer.split("12", 0, 9, 12, false));
        for (String text : List.of("か\u3099", "葛\uDB40\uDD00", "👩‍💻", "a\u0301", "مرحبا")) {
            assertEquals(List.of(text), composer.split(text, 0, 9, 12, true));
        }
        assertEquals(1, calls.get());
    }

    @Test
    void cachesBothAcceptedAndRejectedPlansPerStyleSizeAndRasterScale() {
        AtomicInteger calls = new AtomicInteger();
        CosmicMonospaceComposer composer = new CosmicMonospaceComposer((text, flags, size, scale) -> {
            calls.incrementAndGet();
            return new int[0];
        }, () -> 0L);
        for (int i = 0; i < 3; i++) assertEquals(List.of("ffi"), composer.split("ffi", 0, 9, 12, true));
        assertEquals(1, calls.get());
        composer.split("ffi", 1, 9, 12, true);
        composer.split("ffi", 0, 10, 12, true);
        composer.split("ffi", 0, 9, 2, true);
        assertEquals(4, calls.get());
        composer.clear();
        composer.split("ffi", 0, 9, 12, true);
        assertEquals(5, calls.get());
    }

    @Test
    void cjkAndSupplementaryHanUseUtf16Endpoints() {
        CosmicMonospaceComposer composer = new CosmicMonospaceComposer((text, flags, size, scale) -> new int[]{2, 3}, () -> 0L);
        assertEquals(List.of("𠀀", "中"), composer.split("𠀀中", 0, 9, 12, true));
    }

    @Test
    void expensiveProbeIncursDebtButCachedPlansStayUsableAndDeferredPlansCanRetry() {
        AtomicLong now = new AtomicLong();
        CosmicMonospaceComposer composer = new CosmicMonospaceComposer((text, flags, size, scale) -> {
            now.addAndGet(400_000L);
            return new int[]{1, 2};
        }, now::get);
        assertEquals(List.of("1", "2"), composer.split("12", 0, 9, 12, true));
        for (int i = 0; i < 100; i++) assertEquals(List.of("13"), composer.split("13", 0, 9, 12, true));
        assertEquals(1, composer.probeCalls());
        assertEquals(400_000L, composer.probeNanos());
        assertEquals(100, composer.deferredProbes());
        assertEquals(List.of("1", "2"), composer.split("12", 0, 9, 12, true));
        now.addAndGet(10_000_000L);
        assertEquals(List.of("1", "3"), composer.split("13", 0, 9, 12, true));
        assertEquals(2, composer.probeCalls());
    }

    @Test
    void stableDrawsMergeWhileMeasurementAloneDoesNotPromoteText() {
        AtomicLong now = new AtomicLong();
        CosmicMonospaceComposer composer = new CosmicMonospaceComposer((text, flags, size, scale) ->
                new int[]{1, 2}, now::get);
        for (int i = 0; i < 20; i++) {
            assertEquals(List.of("中", "文"), composer.split("中文", 0, 9, 12, true, false));
            now.addAndGet(50_000_000L);
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(List.of("中", "文"), composer.split("中文", 0, 9, 12, true));
            now.addAndGet(50_000_000L);
        }
        assertEquals(List.of("中文"), composer.split("中文", 0, 9, 12, true));
        assertEquals(List.of("中文"), composer.split("中文", 0, 9, 12, true, false));
        assertEquals(1, composer.probeCalls());
        assertEquals(1, composer.mergedDraws());
    }

    @Test
    void changingNumbersDoNotBecomeStableJustBecauseTheScreenKeepsDrawing() {
        AtomicLong now = new AtomicLong();
        CosmicMonospaceComposer composer = new CosmicMonospaceComposer((text, flags, size, scale) ->
                new int[]{1, 2}, now::get);
        for (int number = 10; number < 99; number++) {
            assertEquals(2, composer.split(Integer.toString(number), 0, 9, 12, true).size());
            now.addAndGet(50_000_000L);
        }
        assertEquals(0, composer.mergedDraws());
    }
}
