package neofontrender.addons.flight;

import neofontrender.addons.api.flight.FlightBodyPose;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Render-thread scope that binds one provider sample to one entity render, including inactive
 * samples. The inactive entry prevents a nested render from observing an outer player's pose.
 */
public final class PlayerBodyRenderScope {
    private static final ThreadLocal<Deque<Context>> CONTEXTS =
            ThreadLocal.withInitial(ArrayDeque::new);

    private PlayerBodyRenderScope() {}

    /** Samples exactly once, pushes an active or inactive entity fence, and always restores stack. */
    public static void withSampledPose(Object entity, Supplier<FlightBodyPose> poseSampler,
                                       RenderCall render) throws Throwable {
        withSampledPose(entity, poseSampler, pose -> pose != null, render);
    }

    /**
     * Samples once while retaining raw provider presence separately from root ownership. A pose
     * rejected by the owner predicate still fences nested renders and remains visible to fallback
     * policy, but does not create a model transform.
     */
    public static void withSampledPose(Object entity, Supplier<FlightBodyPose> poseSampler,
                                       Predicate<FlightBodyPose> ownsRoot,
                                       RenderCall render) throws Throwable {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(poseSampler, "poseSampler");
        Objects.requireNonNull(ownsRoot, "ownsRoot");
        Objects.requireNonNull(render, "render");

        Context context = new Context(entity, null, null);
        Deque<Context> stack = CONTEXTS.get();
        stack.push(context);
        try (Scope ignored = new Scope(context, stack)) {
            // Install the entity fence before provider code runs. If sampling itself performs a
            // nested render, that render must not see the outer entity's active pose either.
            FlightBodyPose pose = poseSampler.get();
            context.activate(pose, pose != null && ownsRoot.test(pose));
            render.run();
        }
    }

    /**
     * Applies the existing local-flight yaw alignment to the six render fields only when the
     * provider returned no pose. The array order is pitch prev/current, head yaw prev/current,
     * and render/body yaw prev/current, matching the renderer snapshot used by its finally block.
     */
    public static boolean alignLocalRollWhenPoseAbsent(Context context, boolean localPlayer,
                                                        float roll, float previousYaw,
                                                        float currentYaw, float[] renderFields) {
        Objects.requireNonNull(renderFields, "renderFields");
        if (renderFields.length != 6) {
            throw new IllegalArgumentException("Expected six player render fields, found "
                    + renderFields.length);
        }
        if ((context != null && context.hasPose()) || !localPlayer
                || Math.abs(roll) < 0.001F) return false;

        renderFields[2] = previousYaw;
        renderFields[3] = currentYaw;
        renderFields[4] = previousYaw;
        renderFields[5] = currentYaw;
        return true;
    }

    /** Returns the top entry, including an inactive fence for a nested null-pose render. */
    public static Context current() {
        Deque<Context> stack = CONTEXTS.get();
        Context current = stack.peek();
        if (current == null) CONTEXTS.remove();
        return current;
    }

    /** Returns only the current entry for this exact entity; it never falls through to an outer one. */
    public static Context current(Object entity) {
        Objects.requireNonNull(entity, "entity");
        Context current = current();
        return current != null && current.entity == entity ? current : null;
    }

    @FunctionalInterface
    public interface RenderCall {
        void run() throws Throwable;
    }

    public static final class Context {
        private final Object entity;
        private FlightBodyPose pose;
        private PlayerBodyRenderTransform transform;

        private Context(Object entity, FlightBodyPose pose,
                        PlayerBodyRenderTransform transform) {
            this.entity = entity;
            this.pose = pose;
            this.transform = transform;
        }

        private void activate(FlightBodyPose sampledPose, boolean ownsRoot) {
            pose = sampledPose;
            transform = sampledPose == null || !ownsRoot
                    ? null : PlayerBodyRenderTransform.fromFlightPose(sampledPose);
        }

        public Object entity() { return entity; }
        public FlightBodyPose pose() { return pose; }
        public boolean hasPose() { return pose != null; }
        public PlayerBodyRenderTransform transform() { return transform; }
        public boolean active() { return transform != null; }
    }

    private static final class Scope implements AutoCloseable {
        private final Context context;
        private final Deque<Context> stack;
        private boolean closed;

        private Scope(Context context, Deque<Context> stack) {
            this.context = context;
            this.stack = stack;
        }

        @Override
        public void close() {
            if (closed) throw new IllegalStateException("Player-body render scope already closed");
            if (stack.peek() != context) {
                throw new IllegalStateException("Player-body render scopes closed out of order");
            }
            stack.pop();
            closed = true;
            if (stack.isEmpty()) CONTEXTS.remove();
        }
    }
}
