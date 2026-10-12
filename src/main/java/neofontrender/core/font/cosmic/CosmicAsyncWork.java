package neofontrender.core.font.cosmic;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Bounded CPU-only mailbox. The worker exclusively owns the context, including destruction. */
final class CosmicAsyncWork<C> implements AutoCloseable {
    record Payload(Object value, long bytes) {}
    record Stats(int queued, int running, int ready, long bytes, long completed,
                 long dropped, long failed, boolean unavailable) {}

    private final Supplier<C> create;
    private final Consumer<C> destroy;
    private final int capacity;
    private final long maxBytes;
    private final Map<Object, Job> jobs = new LinkedHashMap<>(128, 0.75F, true);
    private final ArrayDeque<Job> queue = new ArrayDeque<>();
    private Thread thread;
    private boolean closed;
    private boolean unavailable;
    private Job running;
    private long readyBytes, completed, dropped, failed;

    private final class Job {
        final Object key;
        Function<C, Payload> action;
        Payload result;
        boolean failed;
        long requested = System.nanoTime();
        Job(Object key, Function<C, Payload> action) { this.key = key; this.action = action; }
    }

    CosmicAsyncWork(Supplier<C> create, Consumer<C> destroy) {
        this(create, destroy, 128, 32L * 1024 * 1024);
    }

    CosmicAsyncWork(Supplier<C> create, Consumer<C> destroy, int capacity, long maxBytes) {
        this.create = create;
        this.destroy = destroy;
        this.capacity = Math.max(2, capacity);
        this.maxBytes = Math.max(1, maxBytes);
    }

    synchronized void request(Object key, Function<C, Payload> action) {
        if (closed || unavailable) return;
        Job existing = jobs.get(key);
        if (existing != null) { existing.requested = System.nanoTime(); return; }
        while (jobs.size() >= capacity) {
            // Let admitted visible text finish even on screens with more unique words than slots.
            // Replacing fresh jobs on every miss can otherwise starve the entire screen forever.
            if (!evict(false)) { dropped++; return; }
        }
        Job job = new Job(key, action);
        jobs.put(key, job);
        queue.addLast(job);
        if (thread == null) {
            thread = new Thread(this::run, "NFR Cosmic Rasterizer");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.start();
        }
        notifyAll();
    }

    /** Transfers completed CPU data to the caller. Never waits for a job or runs it inline. */
    synchronized Payload take(Object key) {
        Job job = jobs.get(key);
        if (job == null || job.result == null) return null;
        jobs.remove(key);
        readyBytes -= job.result.bytes;
        return job.result;
    }

    synchronized boolean failed(Object key) {
        Job job = jobs.get(key);
        return unavailable || (job != null && job.failed);
    }

    private boolean evict(boolean completedOnly) {
        var iterator = jobs.values().iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            if (job == running || (completedOnly && job.result == null)) continue;
            if (!completedOnly && !job.failed && System.nanoTime() - job.requested < 2_000_000_000L) continue;
            iterator.remove();
            queue.remove(job);
            if (job.result != null) readyBytes -= job.result.bytes;
            dropped++;
            return true;
        }
        return false;
    }

    private void run() {
        C context = null;
        try {
            context = create.get();
            if (context == null) throw new IllegalStateException("No asynchronous font context");
            while (true) {
                Job job;
                synchronized (this) {
                    while (!closed && queue.isEmpty()) wait();
                    if (closed) return;
                    job = queue.removeFirst();
                    if (System.nanoTime() - job.requested > 2_000_000_000L) {
                        jobs.remove(job.key);
                        dropped++;
                        continue;
                    }
                    running = job;
                }
                Payload result = null;
                boolean failure = false;
                try {
                    result = job.action.apply(context);
                    failure = result == null || result.bytes < 0 || result.bytes > maxBytes;
                } catch (RuntimeException | LinkageError error) {
                    failure = true;
                }
                synchronized (this) {
                    running = null;
                    job.action = null;
                    if (closed) return;
                    if (failure) { job.failed = true; failed++; }
                    else {
                        while (readyBytes + result.bytes > maxBytes && evict(true)) { }
                        job.result = result;
                        readyBytes += result.bytes;
                        completed++;
                    }
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException | LinkageError error) {
            synchronized (this) { unavailable = true; failed++; }
        } finally {
            // close() may run while native code is executing. Only this thread can free its handle.
            if (context != null) destroy.accept(context);
            synchronized (this) {
                running = null;
                if (!closed) unavailable = true;
                jobs.clear();
                queue.clear();
                readyBytes = 0;
            }
        }
    }

    synchronized Stats stats() {
        int ready = 0;
        for (Job job : jobs.values()) if (job.result != null) ready++;
        return new Stats(queue.size(), running == null ? 0 : 1, ready, readyBytes,
                completed, dropped, failed, unavailable);
    }

    @Override public synchronized void close() {
        closed = true;
        jobs.clear();
        queue.clear();
        readyBytes = 0;
        if (thread != null) thread.interrupt();
        notifyAll();
    }
}
