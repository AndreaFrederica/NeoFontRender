package neofontrender.core.font.support;

import org.lwjgl.opengl.GL20;

import java.lang.reflect.Method;

/**
 * Binds a custom shader while keeping optional Angelica/Actinium state tracking coherent.
 *
 * <p>NFR cannot link Actinium directly because it must also run without that optional renderer.
 * This bridge therefore uses the public Actinium state and streaming APIs through reflection,
 * while falling back to ordinary LWJGL calls and no-op draw scopes when Actinium is absent or an
 * older build does not expose the API.</p>
 */
public final class OptionalGlProgramBinding {
    private static final String ANGELICA_STATE_MANAGER =
            "com.gtnewhorizons.angelica.glsm.GLStateManager";
    private static final String STREAMING_DRAW_OPTIONS =
            "com.gtnewhorizons.angelica.glsm.streaming.StreamingDrawOptions";
    private static volatile boolean resolved;
    private static volatile Method stateManagerUseProgram;
    private static volatile boolean streamingOptionsResolved;
    private static volatile Method preserveExternalProgram;

    private OptionalGlProgramBinding() {
    }

    public static void useProgram(int program) {
        Method method = resolve();
        if (method != null) {
            try {
                method.invoke(null, program);
                return;
            } catch (Throwable ignored) {
                // The optional renderer may be present but not initialized for this context.
                // The raw call still gives vanilla/OpenGL the correct behavior.
            }
        }
        GL20.glUseProgram(program);
    }

    /**
     * Keeps an optional Actinium streaming draw on the caller's shader program.
     *
     * <p>The returned scope is a no-op when Actinium or its public streaming API is absent, so
     * the same NFR draw path remains valid with the vanilla renderer.</p>
     */
    public static ExternalProgramScope preserveExternalProgram() {
        Method method = resolvePreserveExternalProgram();
        if (method == null) {
            return ExternalProgramScope.NOOP;
        }
        try {
            Object scope = method.invoke(null);
            if (scope instanceof AutoCloseable) {
                return new ExternalProgramScope((AutoCloseable) scope);
            }
        } catch (Throwable ignored) {
            // Older Actinium builds do not expose this optional API.
        }
        return ExternalProgramScope.NOOP;
    }

    public static final class ExternalProgramScope implements AutoCloseable {
        public static final ExternalProgramScope NOOP = new ExternalProgramScope(null);
        private final AutoCloseable delegate;
        private boolean closed;

        private ExternalProgramScope(AutoCloseable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (delegate == null) {
                return;
            }
            try {
                delegate.close();
            } catch (Exception error) {
                throw new IllegalStateException("Unable to close Actinium streaming draw scope", error);
            }
        }
    }

    private static Method resolve() {
        if (resolved) return stateManagerUseProgram;
        synchronized (OptionalGlProgramBinding.class) {
            if (resolved) return stateManagerUseProgram;
            try {
                ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
                Class<?> type;
                try {
                    type = Class.forName(ANGELICA_STATE_MANAGER, false,
                            contextLoader == null ? OptionalGlProgramBinding.class.getClassLoader()
                                    : contextLoader);
                } catch (ClassNotFoundException missingFromContext) {
                    type = Class.forName(ANGELICA_STATE_MANAGER, false,
                            OptionalGlProgramBinding.class.getClassLoader());
                }
                stateManagerUseProgram = type.getMethod("glUseProgram", int.class);
            } catch (Throwable ignored) {
                stateManagerUseProgram = null;
            }
            resolved = true;
            return stateManagerUseProgram;
        }
    }

    private static Method resolvePreserveExternalProgram() {
        if (streamingOptionsResolved) return preserveExternalProgram;
        synchronized (OptionalGlProgramBinding.class) {
            if (streamingOptionsResolved) return preserveExternalProgram;
            try {
                ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
                ClassLoader loader = contextLoader == null
                        ? OptionalGlProgramBinding.class.getClassLoader() : contextLoader;
                Class<?> type;
                try {
                    type = Class.forName(STREAMING_DRAW_OPTIONS, false, loader);
                } catch (ClassNotFoundException missingFromContext) {
                    type = Class.forName(STREAMING_DRAW_OPTIONS, false,
                            OptionalGlProgramBinding.class.getClassLoader());
                }
                preserveExternalProgram = type.getMethod("preserveExternalProgram");
            } catch (Throwable ignored) {
                preserveExternalProgram = null;
            }
            streamingOptionsResolved = true;
            return preserveExternalProgram;
        }
    }
}
