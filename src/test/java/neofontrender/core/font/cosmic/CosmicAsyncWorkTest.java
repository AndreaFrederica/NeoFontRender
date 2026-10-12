package neofontrender.core.font.cosmic;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class CosmicAsyncWorkTest {
    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException error) { throw new AssertionError(error); }
    }

    private static void eventually(BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            while (!condition.getAsBoolean()) Thread.sleep(1);
        });
    }

    @Test
    void deduplicatesAndTransfersReadyResultsWithoutRunningWorkOnCaller() {
        Thread caller = Thread.currentThread();
        AtomicReference<Thread> owner = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        CountDownLatch destroyed = new CountDownLatch(1);
        var worker = new CosmicAsyncWork<>(() -> {
            owner.set(Thread.currentThread());
            return 42;
        }, value -> { assertEquals(owner.get(), Thread.currentThread()); destroyed.countDown(); });
        try {
            worker.request("same", value -> {
                assertEquals(42, value);
                calls.incrementAndGet();
                started.countDown();
                await(release);
                return new CosmicAsyncWork.Payload("pixels", 6);
            });
            await(started);
            assertNotEquals(caller, owner.get());
            for (int i = 0; i < 100; i++) {
                worker.request("same", value -> { fail("duplicate executed"); return null; });
                assertNull(worker.take("same"));
            }
            release.countDown();
            eventually(() -> worker.stats().ready() == 1);
            assertEquals("pixels", worker.take("same").value());
            assertNull(worker.take("same"));
            assertEquals(0, worker.stats().bytes());
            assertEquals(1, calls.get());
        } finally { release.countDown(); worker.close(); }
        await(destroyed);
    }

    @Test
    void saturatedQueueNeverExecutesInlineAndStaysBounded() {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        Thread caller = Thread.currentThread();
        try (var worker = new CosmicAsyncWork<>(() -> 1, ignored -> {}, 4, 32)) {
            worker.request("running", value -> {
                started.countDown(); await(release);
                return new CosmicAsyncWork.Payload("first", 1);
            });
            await(started);
            try {
                for (int i = 0; i < 500; i++) {
                    worker.request(i, value -> {
                        assertNotEquals(caller, Thread.currentThread());
                        return new CosmicAsyncWork.Payload("x", 1);
                    });
                    assertTrue(worker.stats().queued() <= 3);
                }
                assertTrue(worker.stats().dropped() > 0);
                assertNull(worker.take("running"));
            } finally { release.countDown(); }
        }
    }

    @Test
    void completedMemoryIsBoundedAndOversizeAndFailedJobsDoNotRetryEveryFrame() {
        AtomicInteger attempts = new AtomicInteger();
        try (var worker = new CosmicAsyncWork<>(() -> 1, ignored -> {}, 16, 10)) {
            worker.request("a", value -> new CosmicAsyncWork.Payload("a", 7));
            eventually(() -> worker.stats().completed() == 1);
            worker.request("b", value -> new CosmicAsyncWork.Payload("b", 7));
            eventually(() -> worker.stats().completed() == 2);
            assertTrue(worker.stats().bytes() <= 10);
            assertNull(worker.take("a"));
            assertEquals("b", worker.take("b").value());
            worker.request("oversize", value -> new CosmicAsyncWork.Payload("large", 11));
            worker.request("bad", value -> { attempts.incrementAndGet(); throw new IllegalStateException("test"); });
            eventually(() -> worker.stats().failed() == 2);
            for (int i = 0; i < 100; i++) {
                worker.request("bad", value -> { attempts.incrementAndGet(); return null; });
                assertTrue(worker.failed("bad"));
                assertTrue(worker.failed("oversize"));
            }
            assertEquals(1, attempts.get());
        }
    }

    @Test
    void closeDoesNotFreeAnInFlightNativeContextOrWaitForIt() {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        CountDownLatch destroyed = new CountDownLatch(1);
        AtomicInteger creates = new AtomicInteger();
        var worker = new CosmicAsyncWork<>(creates::incrementAndGet, value -> destroyed.countDown());
        worker.request("raster", value -> {
            started.countDown();
            // JNI cannot be preempted by Thread.interrupt(). Model that behavior explicitly.
            boolean done = false;
            while (!done) {
                try { release.await(); done = true; }
                catch (InterruptedException ignored) { }
            }
            assertEquals(1, destroyed.getCount(), "handle must remain alive during rasterization");
            return new CosmicAsyncWork.Payload("late", 1);
        });
        await(started);
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(1), worker::close);
            assertEquals(1, destroyed.getCount());
            worker.request("after-close", value -> { fail("closed worker restarted"); return null; });
        } finally { release.countDown(); }
        await(destroyed);
        assertNull(worker.take("raster"));
        assertEquals(0, worker.stats().ready());
        assertEquals(1, creates.get());
    }

    @Test
    void initializationFailureIsStickyAndDoesNotRestartForEveryRequest() {
        AtomicInteger creates = new AtomicInteger();
        try (var worker = new CosmicAsyncWork<Integer>(() -> {
            creates.incrementAndGet(); throw new IllegalStateException("init failure");
        }, value -> fail("no context to destroy"))) {
            worker.request("first", value -> null);
            eventually(() -> worker.stats().unavailable());
            for (int i = 0; i < 100; i++) worker.request(i, value -> null);
            assertEquals(1, creates.get());
            assertTrue(worker.failed("anything"));
            assertEquals(0, worker.stats().queued());
        }
    }
}
