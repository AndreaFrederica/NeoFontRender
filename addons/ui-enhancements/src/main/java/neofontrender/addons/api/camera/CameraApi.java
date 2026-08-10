package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Public camera facade. Consumers may read frames without acquiring a control session. */
public final class CameraApi {
    public static final int API_VERSION = 2;
    private static final Logger LOGGER = LogManager.getLogger("UIE Camera API");
    private static volatile Backend backend = new UnavailableBackend();
    private static final CopyOnWriteArrayList<CameraProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CameraModifier> MODIFIERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CameraLensProvider> LENS_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CameraFrameObserver> OBSERVERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CameraPickingProvider> PICKING_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CameraCollisionProvider> COLLISION_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<CameraAdaptiveItemProvider> ADAPTIVE_ITEM_PROVIDERS = new CopyOnWriteArrayList<>();
    private static long evaluatedSampleId = Long.MIN_VALUE;
    private static float evaluatedPartialTicks = -1.0F;
    private static CameraFrame evaluatedBaseFrame;
    private static CameraFrame evaluatedFrame;
    private static boolean evaluatedProviderOwnsView;
    private static String evaluatedProviderId;
    private static List<String> evaluatedModifierIds = java.util.Collections.emptyList();
    private static String evaluatedLensProviderId;
    private static String evaluatedPickingProviderId;
    private static String evaluatedCollisionProviderId;
    private static String evaluatedAdaptiveProviderId;
    private static volatile CameraVector positionOverride;
    private static volatile LoggingSession activeControlSession;

    private CameraApi() {}

    public static int getApiVersion() { return API_VERSION; }
    public static synchronized CameraFrame getFrame(float partialTicks) {
        CameraFrame base = backendCall("getFrame", () -> backend.getFrame(partialTicks));
        if (evaluatedFrame != null && evaluatedBaseFrame == base
                && evaluatedSampleId == base.sampleId()
                && Math.abs(evaluatedPartialTicks - partialTicks) < 1.0E-5F) {
            return evaluatedFrame;
        }
        CameraFrame frame = base;
        boolean uiViewProxy = false;
        boolean providerOwnsView = false;
        String providerId = null;
        List<String> modifierIds = new java.util.ArrayList<>();
        for (CameraProvider provider : sortedProviders()) {
            String owner = callbackId("provider", provider, provider::id);
            CameraFrame provided;
            try { provided = provider.frame(frame, partialTicks); }
            catch (RuntimeException error) { throw callbackFailure(owner, "frame", error); }
            if (provided != null) {
                frame = provided;
                providerId = owner;
                try {
                    providerOwnsView = provider.ownsView();
                    uiViewProxy = providerOwnsView && provider.requiresUiViewProxy();
                } catch (RuntimeException error) {
                    throw callbackFailure(owner, "view ownership", error);
                }
                break;
            }
        }
        for (CameraModifier modifier : sortedModifiers()) {
            String owner = callbackId("modifier", modifier, modifier::id);
            CameraFrame next;
            try { next = modifier.apply(frame, partialTicks); }
            catch (RuntimeException error) { throw callbackFailure(owner, "apply", error); }
            if (next != null) {
                frame = next;
                modifierIds.add(owner);
            }
        }
        CameraVector override = positionOverride;
        if (override != null) {
            frame = withPosition(frame, override);
            uiViewProxy = true;
        }
        for (CameraFrameObserver observer : OBSERVERS) {
            try { observer.onFrame(frame); }
            catch (RuntimeException error) {
                throw callbackFailure("observer:" + observer.getClass().getName(), "onFrame", error);
            }
        }
        CameraFrame appliedFrame = frame;
        boolean appliedProxy = uiViewProxy;
        backendRun("applyFrame", () -> backend.applyFrame(appliedFrame, appliedProxy));
        evaluatedSampleId = base.sampleId();
        evaluatedPartialTicks = partialTicks;
        evaluatedBaseFrame = base;
        evaluatedFrame = frame;
        evaluatedProviderOwnsView = providerOwnsView;
        evaluatedProviderId = providerId;
        evaluatedModifierIds = java.util.Collections.unmodifiableList(modifierIds);
        return frame;
    }
    /** Measurement is read-only and never acquires a session or changes picking state. */
    public static CameraMeasurement measure(float partialTicks) {
        CameraFrame frame = getFrame(partialTicks);
        return new CameraMeasurement(frame, lens(frame, partialTicks));
    }
    public static CameraProjection project(CameraVector world, float partialTicks) {
        return measure(partialTicks).project(world);
    }
    public static CameraRay screenRay(double pixelX, double pixelY, float partialTicks) {
        return measure(partialTicks).screenRay(pixelX, pixelY);
    }
    public static boolean isWithinFrustum(AxisAlignedBounds bounds, float partialTicks) {
        return measure(partialTicks).isWithinFrustum(bounds);
    }
    public static ScreenBounds projectBounds(AxisAlignedBounds bounds, float partialTicks) {
        return measure(partialTicks).projectBounds(bounds);
    }
    public static CameraHorizon horizon(float partialTicks) {
        return measure(partialTicks).horizon();
    }
    public static CameraRelativePose relativeTo(CameraVector world, float partialTicks) {
        return measure(partialTicks).relativeTo(world);
    }
    public static CameraHit interactionTarget(CameraPickingPurpose purpose, float partialTicks) {
        return measure(partialTicks).interactionTarget(purpose);
    }
    public static CameraHit pick(CameraPickingRequest request) {
        if (request == null) return null;
        for (CameraPickingProvider provider : sortedPickingProviders()) {
            String owner = callbackId("picking", provider, provider::id);
            CameraHit hit;
            try { hit = provider.pick(request); }
            catch (RuntimeException error) { throw callbackFailure(owner, "pick", error); }
            if (hit != null) {
                evaluatedPickingProviderId = owner;
                return hit;
            }
        }
        evaluatedPickingProviderId = "backend";
        return backendCall("pick", () -> backend.pick(request));
    }
    public static CameraVector resolveCollision(CameraCollisionQuery query) {
        if (query == null) return null;
        CameraVector result = query.to();
        for (CameraCollisionProvider provider : sortedCollisionProviders()) {
            String owner = callbackId("collision", provider, provider::id);
            CameraVector next;
            try { next = provider.resolve(query); }
            catch (RuntimeException error) { throw callbackFailure(owner, "resolve", error); }
            if (next != null) {
                evaluatedCollisionProviderId = owner;
                return next;
            }
        }
        evaluatedCollisionProviderId = "backend";
        return result;
    }
    public static CameraSession acquire(CameraRigRequest request) {
        if (request == null) throw new NullPointerException("request");
        CameraProviderContext context = new CameraProviderContext(getFrame(0.0F), measure(0.0F));
        for (ControlProvider entry : controlProviders()) {
            CameraProvider provider = entry.provider;
            String owner = entry.owner;
            boolean supports;
            try {
                supports = provider.supports(request);
            } catch (RuntimeException error) {
                logControlFailure(owner, "supports", error);
                throw error;
            }
            if (!supports) continue;
            CameraSession session;
            try {
                session = provider.acquire(request, context);
            } catch (RuntimeException error) {
                logControlFailure(owner, "acquire", error);
                throw error;
            }
            if (session == null) continue;
            session = new LoggingSession(owner, request.id().toString(), session);
            boolean active;
            try {
                active = session.isActive();
            } catch (RuntimeException error) {
                try {
                    session.close();
                } catch (RuntimeException closeError) {
                    error.addSuppressed(closeError);
                }
                throw error;
            }
            if (active) {
                activeControlSession = (LoggingSession) session;
                return session;
            }
            session.close();
        }
        try {
            CameraSession session = backend.acquire(request);
            if (session == null) return null;
            LoggingSession logging = new LoggingSession("backend", request.id().toString(), session);
            activeControlSession = logging;
            return logging;
        } catch (RuntimeException error) {
            logControlFailure("backend", "acquire", error);
            throw error;
        }
    }
    public static CameraRegistration registerProvider(CameraProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = requireId("provider", provider, provider::id);
        PROVIDERS.removeIf(value -> id.equals(callbackId("provider", value, value::id)));
        PROVIDERS.add(provider);
        invalidateEvaluation();
        return () -> { PROVIDERS.remove(provider); invalidateEvaluation(); };
    }
    public static CameraRegistration registerModifier(CameraModifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        String id = requireId("modifier", modifier, modifier::id);
        MODIFIERS.removeIf(value -> id.equals(callbackId("modifier", value, value::id)));
        MODIFIERS.add(modifier);
        invalidateEvaluation();
        return () -> { MODIFIERS.remove(modifier); invalidateEvaluation(); };
    }
    public static CameraRegistration registerLensProvider(CameraLensProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = requireId("lens", provider, provider::id);
        LENS_PROVIDERS.removeIf(value -> id.equals(callbackId("lens", value, value::id)));
        LENS_PROVIDERS.add(provider);
        invalidateEvaluation();
        return () -> { LENS_PROVIDERS.remove(provider); invalidateEvaluation(); };
    }
    public static CameraRegistration registerFrameObserver(CameraFrameObserver observer) {
        Objects.requireNonNull(observer, "observer"); OBSERVERS.addIfAbsent(observer);
        invalidateEvaluation();
        return () -> { OBSERVERS.remove(observer); invalidateEvaluation(); };
    }
    public static CameraRegistration registerPickingProvider(CameraPickingProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = requireId("picking", provider, provider::id);
        PICKING_PROVIDERS.removeIf(value -> id.equals(callbackId("picking", value, value::id)));
        PICKING_PROVIDERS.add(provider);
        invalidateEvaluation();
        return () -> { PICKING_PROVIDERS.remove(provider); invalidateEvaluation(); };
    }
    public static CameraRegistration registerCollisionProvider(CameraCollisionProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = requireId("collision", provider, provider::id);
        COLLISION_PROVIDERS.removeIf(value -> id.equals(callbackId("collision", value, value::id)));
        COLLISION_PROVIDERS.add(provider);
        invalidateEvaluation();
        return () -> { COLLISION_PROVIDERS.remove(provider); invalidateEvaluation(); };
    }
    public static CameraRegistration registerAdaptiveItemProvider(CameraAdaptiveItemProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = requireId("adaptive", provider, provider::id);
        ADAPTIVE_ITEM_PROVIDERS.removeIf(value -> id.equals(callbackId("adaptive", value, value::id)));
        ADAPTIVE_ITEM_PROVIDERS.add(provider);
        invalidateEvaluation();
        return () -> { ADAPTIVE_ITEM_PROVIDERS.remove(provider); invalidateEvaluation(); };
    }

    public static boolean resolveAdaptiveAiming(net.minecraft.entity.EntityLivingBase entity,
                                                boolean configuredResult) {
        evaluatedAdaptiveProviderId = null;
        List<CameraAdaptiveItemProvider> providers = new java.util.ArrayList<>(ADAPTIVE_ITEM_PROVIDERS);
        providers.sort(Comparator.comparingInt((CameraAdaptiveItemProvider value) ->
                        callbackPriority("adaptive", value, value::priority)).reversed()
                .thenComparing(value -> callbackId("adaptive", value, value::id)));
        for (CameraAdaptiveItemProvider provider : providers) {
            String owner = callbackId("adaptive", provider, provider::id);
            Boolean result;
            try { result = provider.isAiming(entity, configuredResult); }
            catch (RuntimeException error) { throw callbackFailure(owner, "isAiming", error); }
            if (result != null) {
                evaluatedAdaptiveProviderId = owner;
                return result;
            }
        }
        return configuredResult;
    }
    public static CameraLens lens(float partialTicks) { return lens(getFrame(partialTicks), partialTicks); }
    private static CameraLens lens(CameraFrame frame, float partialTicks) {
        CameraLens fallback = backendCall("lens", () -> new CameraLens(backend.viewportWidth(),
                backend.viewportHeight(), backend.verticalFov(), backend.nearPlane(), backend.farPlane()));
        List<CameraLensProvider> providers = new java.util.ArrayList<>(LENS_PROVIDERS);
        providers.sort(Comparator.comparingInt((CameraLensProvider value) ->
                        callbackPriority("lens", value, value::priority)).reversed()
                .thenComparing(value -> callbackId("lens", value, value::id)));
        for (CameraLensProvider provider : providers) {
            String owner = callbackId("lens", provider, provider::id);
            CameraLens result;
            try { result = provider.lens(frame, partialTicks, fallback); }
            catch (RuntimeException error) { throw callbackFailure(owner, "lens", error); }
            if (result != null) {
                evaluatedLensProviderId = owner;
                return result;
            }
        }
        evaluatedLensProviderId = "backend";
        return fallback;
    }
    public static List<CameraProvider> providers() { return java.util.Collections.unmodifiableList(sortedProviders()); }
    public static List<CameraModifier> modifiers() { return java.util.Collections.unmodifiableList(sortedModifiers()); }
    public static List<CameraLensProvider> lensProviders() {
        List<CameraLensProvider> result = new java.util.ArrayList<>(LENS_PROVIDERS);
        result.sort(Comparator.comparingInt((CameraLensProvider value) ->
                        callbackPriority("lens", value, value::priority)).reversed()
                .thenComparing(value -> callbackId("lens", value, value::id)));
        return java.util.Collections.unmodifiableList(result);
    }
    public static boolean hasLensProviders() { return !LENS_PROVIDERS.isEmpty(); }
    public static List<CameraPickingProvider> pickingProviders() {
        return java.util.Collections.unmodifiableList(sortedPickingProviders());
    }
    public static List<CameraCollisionProvider> collisionProviders() {
        return java.util.Collections.unmodifiableList(sortedCollisionProviders());
    }

    private static List<CameraProvider> sortedProviders() {
        List<CameraProvider> result = new java.util.ArrayList<>(PROVIDERS);
        result.sort(Comparator.comparingInt((CameraProvider value) ->
                        callbackPriority("provider", value, value::priority)).reversed()
                .thenComparing(value -> callbackId("provider", value, value::id)));
        return result;
    }
    private static List<ControlProvider> controlProviders() {
        List<ControlProvider> result = new java.util.ArrayList<>();
        for (CameraProvider provider : PROVIDERS) {
            String fallback = fallbackId("provider", provider);
            try {
                ResourceLocation id = Objects.requireNonNull(provider.id(), "provider id");
                result.add(new ControlProvider(provider, "provider:" + id, provider.priority()));
            } catch (RuntimeException error) {
                logControlFailure(fallback, "id/priority", error);
                throw error;
            }
        }
        result.sort(Comparator.comparingInt((ControlProvider value) -> value.priority).reversed()
                .thenComparing(value -> value.owner));
        return result;
    }
    private static List<CameraModifier> sortedModifiers() {
        List<CameraModifier> result = new java.util.ArrayList<>(MODIFIERS);
        result.sort(Comparator.comparingInt((CameraModifier value) ->
                        callbackPriority("modifier", value, value::priority)).reversed()
                .thenComparing(value -> callbackId("modifier", value, value::id)));
        return result;
    }
    private static List<CameraPickingProvider> sortedPickingProviders() {
        List<CameraPickingProvider> result = new java.util.ArrayList<>(PICKING_PROVIDERS);
        result.sort(Comparator.comparingInt((CameraPickingProvider value) ->
                        callbackPriority("picking", value, value::priority)).reversed()
                .thenComparing(value -> callbackId("picking", value, value::id)));
        return result;
    }
    private static List<CameraCollisionProvider> sortedCollisionProviders() {
        List<CameraCollisionProvider> result = new java.util.ArrayList<>(COLLISION_PROVIDERS);
        result.sort(Comparator.comparingInt((CameraCollisionProvider value) ->
                        callbackPriority("collision", value, value::priority)).reversed()
                .thenComparing(value -> callbackId("collision", value, value::id)));
        return result;
    }
    public static boolean isDroneActive() { return backendCall("isDroneActive", backend::isDroneActive); }
    public static boolean isFreeLookActive() { return backendCall("isFreeLookActive", backend::isFreeLookActive); }
    public static boolean isShoulderActive() { return backendCall("isShoulderActive", backend::isShoulderActive); }
    public static boolean isShoulderLeft() { return backendCall("isShoulderLeft", backend::isShoulderLeft); }
    public static void swapShoulder() { backendRun("swapShoulder", backend::swapShoulder); }
    /** Whether the current render sample has an authoritative UIE camera view. */
    public static synchronized boolean isRenderOverrideActive() {
        if (positionOverride != null) return true;
        if (backendCall("isRenderOverrideActive", backend::isRenderOverrideActive)) return true;
        if (evaluatedFrame == null) getFrame(0.0F);
        return evaluatedProviderOwnsView;
    }
    public static CameraShaderCompatibility shaderCompatibility() {
        return backendCall("shaderCompatibility", backend::shaderCompatibility);
    }
    public static float shaderResolutionMultiplier() {
        return backendCall("shaderResolutionMultiplier", backend::shaderResolutionMultiplier);
    }
    public static CameraVector getPosition(float partialTicks) { return getFrame(partialTicks).position(); }
    /** Overrides only the final camera origin; body and view attitudes remain independently owned. */
    public static synchronized void setPosition(CameraVector position) {
        positionOverride = Objects.requireNonNull(position, "position");
        invalidateEvaluation();
    }
    public static void setPosition(double x, double y, double z) {
        setPosition(new CameraVector(x, y, z));
    }
    public static synchronized void clearPositionOverride() {
        positionOverride = null;
        invalidateEvaluation();
    }
    public static boolean hasPositionOverride() { return positionOverride != null; }
    public static void setDronePose(CameraVector position, CameraAttitude attitude) {
        CameraVector checkedPosition = Objects.requireNonNull(position, "position");
        CameraAttitude checkedAttitude = Objects.requireNonNull(attitude, "attitude");
        backendRun("setDronePose", () -> backend.setDronePose(checkedPosition, checkedAttitude));
    }
    public static void clearDronePose() { backendRun("clearDronePose", backend::clearDronePose); }

    public static synchronized CameraDiagnostics diagnostics(float partialTicks) {
        CameraFrame frame = getFrame(partialTicks);
        LoggingSession session = activeControlSession;
        if (session != null && !session.isActive()) {
            activeControlSession = null;
            session = null;
        }
        String rig = session == null ? backendCall("activeRigId", backend::activeRigId) : session.rigId;
        String owner = session == null ? backendCall("sessionOwner", backend::sessionOwner) : session.owner;
        String failClosed = backendCall("failClosedReason", backend::failClosedReason);
        return new CameraDiagnostics(frame.sampleId(), rig, owner, evaluatedProviderId,
                evaluatedModifierIds, evaluatedLensProviderId, evaluatedPickingProviderId,
                evaluatedCollisionProviderId, evaluatedAdaptiveProviderId, failClosed,
                positionOverride != null,
                isRenderOverrideActive());
    }

    /** Installed once by UIE's client camera module; integrations only use the static facade. */
    public static void installBackend(Backend value) {
        backend = Objects.requireNonNull(value, "value");
        activeControlSession = null;
        evaluatedSampleId = Long.MIN_VALUE;
        evaluatedBaseFrame = null;
        evaluatedFrame = null;
        evaluatedProviderOwnsView = false;
        evaluatedProviderId = null;
        evaluatedModifierIds = java.util.Collections.emptyList();
    }

    private static synchronized void invalidateEvaluation() {
        evaluatedSampleId = Long.MIN_VALUE;
        evaluatedPartialTicks = -1.0F;
        evaluatedBaseFrame = null;
        evaluatedFrame = null;
        evaluatedProviderOwnsView = false;
        evaluatedProviderId = null;
        evaluatedModifierIds = java.util.Collections.emptyList();
    }

    private static RuntimeException callbackFailure(String owner, String operation,
                                                    RuntimeException error) {
        LOGGER.error("Camera API callback {} failed for {}", operation, owner, error);
        return error;
    }

    private static <T> T backendCall(String operation, Supplier<T> callback) {
        try {
            return callback.get();
        } catch (RuntimeException error) {
            throw callbackFailure("backend", operation, error);
        }
    }

    private static void backendRun(String operation, Runnable callback) {
        try {
            callback.run();
        } catch (RuntimeException error) {
            throw callbackFailure("backend", operation, error);
        }
    }

    private static void logControlFailure(String owner, String operation,
                                          RuntimeException error) {
        LOGGER.error("Camera control callback {} failed for {}", operation, owner, error);
    }

    private static String requireId(String kind, Object callback,
                                    Supplier<ResourceLocation> supplier) {
        try {
            return kind + ":" + Objects.requireNonNull(supplier.get(), "id");
        } catch (RuntimeException error) {
            throw callbackFailure(fallbackId(kind, callback), "id", error);
        }
    }

    private static String callbackId(String kind, Object callback,
                                     Supplier<ResourceLocation> supplier) {
        try {
            return kind + ":" + Objects.requireNonNull(supplier.get(), "id");
        } catch (RuntimeException error) {
            throw callbackFailure(fallbackId(kind, callback), "id", error);
        }
    }

    private static int callbackPriority(String kind, Object callback, IntSupplier supplier) {
        try {
            return supplier.getAsInt();
        } catch (RuntimeException error) {
            throw callbackFailure(fallbackId(kind, callback), "priority", error);
        }
    }

    private static String fallbackId(String kind, Object callback) {
        return kind + ":" + callback.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(callback));
    }

    /** Logs explicit session failures and preserves their original exception semantics. */
    private static final class LoggingSession implements CameraSession {
        private final String owner;
        private final String rigId;
        private final CameraSession delegate;

        private LoggingSession(String owner, String rigId, CameraSession delegate) {
            this.owner = owner;
            this.rigId = rigId;
            this.delegate = delegate;
        }

        @Override public boolean isActive() {
            try {
                boolean active = delegate.isActive();
                if (!active && activeControlSession == this) activeControlSession = null;
                return active;
            } catch (RuntimeException error) {
                logControlFailure(owner, "session.isActive", error);
                throw error;
            }
        }

        @Override public void close() {
            try {
                delegate.close();
            } catch (RuntimeException error) {
                logControlFailure(owner, "session.close", error);
                throw error;
            } finally {
                if (activeControlSession == this) activeControlSession = null;
            }
        }
    }

    private static final class ControlProvider {
        final CameraProvider provider;
        final String owner;
        final int priority;

        private ControlProvider(CameraProvider provider, String owner, int priority) {
            this.provider = provider;
            this.owner = owner;
            this.priority = priority;
        }
    }

    /** Internal implementation boundary kept out of public camera value types. */
    public interface Backend {
        CameraFrame getFrame(float partialTicks);
        CameraSession acquire(CameraRigRequest request);
        boolean isDroneActive();
        boolean isFreeLookActive();
        boolean isShoulderActive();
        default boolean isShoulderLeft() { return true; }
        default void swapShoulder() {}
        boolean isRenderOverrideActive();
        void setDronePose(CameraVector position, CameraAttitude attitude);
        void clearDronePose();
        default String activeRigId() { return null; }
        default String sessionOwner() { return null; }
        default String failClosedReason() { return null; }
        default int viewportWidth() { return 1; }
        default int viewportHeight() { return 1; }
        default double verticalFov() { return 70.0D; }
        default double nearPlane() { return 0.05D; }
        default double farPlane() { return 1024.0D; }
        default CameraShaderCompatibility shaderCompatibility() { return CameraShaderCompatibility.NONE; }
        default float shaderResolutionMultiplier() { return 1.0F; }
        default CameraHit pick(CameraPickingRequest request) { return null; }
        default void applyFrame(CameraFrame frame) {}
        default void applyFrame(CameraFrame frame, boolean uiViewProxy) { applyFrame(frame); }
    }

    private static final class UnavailableBackend implements Backend {
        private static final CameraFrame EMPTY = new CameraFrame(0L, 0.0F,
                CameraAttitude.IDENTITY, CameraAttitude.IDENTITY,
                new CameraVector(0.0D, 0.0D, 0.0D), new CameraVector(0.0D, 0.0D, 0.0D), true);
        private static final CameraSession REJECTED = new CameraSession() {
            @Override public boolean isActive() { return false; }
            @Override public void close() {}
        };

        @Override public CameraFrame getFrame(float partialTicks) { return EMPTY; }
        @Override public CameraSession acquire(CameraRigRequest request) { return REJECTED; }
        @Override public boolean isDroneActive() { return false; }
        @Override public boolean isFreeLookActive() { return false; }
        @Override public boolean isShoulderActive() { return false; }
        @Override public boolean isRenderOverrideActive() { return false; }
        @Override public void setDronePose(CameraVector position, CameraAttitude attitude) {}
        @Override public void clearDronePose() {}
    }

    private static CameraFrame withPosition(CameraFrame frame, CameraVector position) {
        return new CameraFrame(frame.sampleId(), frame.partialTicks(), frame.bodyAttitude(),
                frame.viewAttitude(), frame.bodyPosition(), position, frame.targetPosition(),
                frame.linearVelocity(), frame.angularVelocity(), false);
    }
}
