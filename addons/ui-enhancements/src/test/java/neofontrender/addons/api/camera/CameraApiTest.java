package neofontrender.addons.api.camera;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CameraApiTest {
    @Test
    void evaluatesAuthoritativeStagesOncePerSample() {
        CameraFrame base = new CameraFrame(77L, 0.25F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        CameraApi.installBackend(new TestBackend(base));
        AtomicInteger observed = new AtomicInteger();
        CameraRegistration provider = CameraApi.registerProvider(new CameraProvider() {
            @Override public ResourceLocation id() { return testId("owner"); }
            @Override public int priority() { return 20; }
            @Override public CameraSession acquire(CameraRigRequest request, CameraProviderContext context) {
                return null;
            }
            @Override public CameraFrame frame(CameraFrame fallback, float partialTicks) {
                return new CameraFrame(fallback.sampleId(), partialTicks, fallback.bodyAttitude(),
                        CameraAttitude.axisAngle(new CameraVector(0.0D, 1.0D, 0.0D), 0.5D),
                        fallback.bodyPosition(), new CameraVector(1.0D, 2.0D, 3.0D), false);
            }
        });
        CameraRegistration modifier = CameraApi.registerModifier(new CameraModifier() {
            @Override public ResourceLocation id() { return testId("modifier"); }
            @Override public CameraFrame apply(CameraFrame frame, float partialTicks) {
                return new CameraFrame(frame.sampleId(), frame.partialTicks(), frame.bodyAttitude(),
                        frame.viewAttitude(), frame.bodyPosition(), frame.position().add(
                                new CameraVector(1.0D, 0.0D, 0.0D)), false);
            }
        });
        CameraRegistration lens = CameraApi.registerLensProvider(new CameraLensProvider() {
            @Override public ResourceLocation id() { return testId("lens"); }
            @Override public CameraLens lens(CameraFrame frame, float partialTicks, CameraLens fallback) {
                return new CameraLens(800, 600, 80.0D, 0.1D, 512.0D);
            }
        });
        CameraRegistration observer = CameraApi.registerFrameObserver(frame -> observed.incrementAndGet());
        try {
            CameraFrame first = CameraApi.getFrame(0.25F);
            CameraFrame second = CameraApi.getFrame(0.25F);
            assertSame(first, second);
            assertEquals(2.0D, first.position().x, 1.0E-9D);
            assertEquals(1, observed.get());
            assertEquals(800, CameraApi.lens(0.25F).width());
        } finally {
            observer.close();
            lens.close();
            modifier.close();
            provider.close();
        }
    }

    @Test
    void forwardsUiManagedViewProxyOwnershipWithTheFinalFrame() {
        CameraFrame base = new CameraFrame(91L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        TestBackend backend = new TestBackend(base);
        CameraApi.installBackend(backend);
        CameraRegistration provider = CameraApi.registerProvider(new CameraProvider() {
            @Override public ResourceLocation id() { return testId("proxy_owner"); }
            @Override public CameraSession acquire(CameraRigRequest request, CameraProviderContext context) {
                return null;
            }
            @Override public CameraFrame frame(CameraFrame fallback, float partialTicks) {
                return new CameraFrame(fallback.sampleId(), partialTicks, fallback.bodyAttitude(),
                        fallback.viewAttitude(), fallback.bodyPosition(),
                        new CameraVector(9.0D, 8.0D, 7.0D), false);
            }
            @Override public boolean ownsView() { return true; }
            @Override public boolean requiresUiViewProxy() { return true; }
        });
        try {
            CameraFrame result = CameraApi.getFrame(0.0F);
            assertTrue(CameraApi.isRenderOverrideActive());
            assertSame(result, backend.appliedFrame);
            assertTrue(backend.uiViewProxy);
        } finally {
            provider.close();
        }
    }

    @Test
    void providerMetadataFailuresAfterRegistrationPropagateToCaller() {
        CameraFrame base = new CameraFrame(108L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(4.0D, 5.0D, 6.0D), false);
        CameraApi.installBackend(new TestBackend(base));
        AtomicBoolean broken = new AtomicBoolean();
        CameraRegistration registration = CameraApi.registerProvider(new CameraProvider() {
            @Override public ResourceLocation id() {
                if (broken.get()) throw new IllegalStateException("metadata failure");
                return testId("unstable_metadata");
            }
            @Override public int priority() {
                if (broken.get()) throw new IllegalStateException("priority failure");
                return 10;
            }
            @Override public CameraSession acquire(CameraRigRequest request,
                                                   CameraProviderContext context) { return null; }
            @Override public CameraFrame frame(CameraFrame fallback, float partialTicks) {
                return fallback;
            }
        });
        try {
            broken.set(true);
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> CameraApi.getFrame(0.0F));
            assertEquals("metadata failure", error.getMessage());
        } finally {
            registration.close();
        }
    }

    @Test
    void onlySelectedProviderCanOwnRenderedView() {
        CameraFrame base = new CameraFrame(109L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        CameraApi.installBackend(new TestBackend(base));
        CameraRegistration lowerOwner = CameraApi.registerProvider(provider(
                "lower_owner", 1, true));
        CameraRegistration selectedReader = CameraApi.registerProvider(provider(
                "selected_reader", 10, false));
        try {
            CameraApi.getFrame(0.0F);
            assertFalse(CameraApi.isRenderOverrideActive());
        } finally {
            selectedReader.close();
            lowerOwner.close();
        }
    }

    @Test
    void providerSessionStateFailurePropagatesWithCleanupFailureAttached() {
        CameraFrame base = new CameraFrame(110L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        CameraApi.installBackend(new TestBackend(base));
        java.util.concurrent.atomic.AtomicInteger closeCount =
                new java.util.concurrent.atomic.AtomicInteger();
        CameraRegistration registration = CameraApi.registerProvider(new CameraProvider() {
            @Override public ResourceLocation id() { return testId("broken_session"); }
            @Override public boolean supports(CameraRigRequest request) { return true; }
            @Override public CameraSession acquire(CameraRigRequest request,
                                                   CameraProviderContext context) {
                return new CameraSession() {
                    @Override public boolean isActive() { throw new IllegalStateException("broken"); }
                    @Override public void close() {
                        closeCount.incrementAndGet();
                        throw new IllegalStateException("broken close");
                    }
                };
            }
            @Override public CameraFrame frame(CameraFrame fallback, float partialTicks) { return null; }
        });
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> CameraApi.acquire(CameraRigRequest.drone(10)));
            assertEquals("broken", error.getMessage());
            assertEquals(1, error.getSuppressed().length);
            assertEquals("broken close", error.getSuppressed()[0].getMessage());
            assertEquals(1, closeCount.get());
        } finally {
            registration.close();
        }
    }

    @Test
    void providerSessionFailureAfterAcquirePropagatesToItsCaller() {
        CameraFrame base = new CameraFrame(111L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        CameraApi.installBackend(new TestBackend(base));
        AtomicInteger stateCalls = new AtomicInteger();
        AtomicInteger closeCount = new AtomicInteger();
        CameraRegistration registration = CameraApi.registerProvider(new CameraProvider() {
            @Override public ResourceLocation id() { return testId("late_session_failure"); }
            @Override public boolean supports(CameraRigRequest request) { return true; }
            @Override public CameraSession acquire(CameraRigRequest request,
                                                   CameraProviderContext context) {
                return new CameraSession() {
                    @Override public boolean isActive() {
                        if (stateCalls.incrementAndGet() > 1)
                            throw new IllegalStateException("late state failure");
                        return true;
                    }
                    @Override public void close() { closeCount.incrementAndGet(); }
                };
            }
        });
        try {
            CameraSession session = CameraApi.acquire(CameraRigRequest.drone(10));
            assertTrue(session != null);
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    session::isActive);
            assertEquals("late state failure", error.getMessage());
            assertEquals(0, closeCount.get());
            session.close();
            assertEquals(1, closeCount.get());
        } finally {
            registration.close();
        }
    }

    @Test
    void explicitAcquireDoesNotMaskProviderOrderingFailures() {
        CameraFrame base = new CameraFrame(112L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        CameraApi.installBackend(new TestBackend(base));
        CameraRegistration registration = CameraApi.registerProvider(new CameraProvider() {
            @Override public ResourceLocation id() { return testId("broken_priority"); }
            @Override public int priority() { throw new IllegalStateException("priority failure"); }
            @Override public boolean supports(CameraRigRequest request) { return true; }
            @Override public CameraSession acquire(CameraRigRequest request,
                                                   CameraProviderContext context) { return null; }
        });
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> CameraApi.acquire(CameraRigRequest.drone(10)));
            assertEquals("priority failure", error.getMessage());
        } finally {
            registration.close();
        }
    }

    @Test
    void positionOverrideIsFinalFrameStateAndRequiresViewProxy() {
        CameraFrame base = new CameraFrame(113L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(1.0D, 2.0D, 3.0D), false);
        TestBackend backend = new TestBackend(base);
        CameraApi.installBackend(backend);
        CameraVector override = new CameraVector(7.0D, 8.0D, 9.0D);
        CameraApi.setPosition(override);
        try {
            assertEquals(override.x, CameraApi.getPosition(0.0F).x, 1.0E-9D);
            assertSame(override, CameraApi.getFrame(0.0F).position());
            assertTrue(CameraApi.hasPositionOverride());
            assertTrue(CameraApi.isRenderOverrideActive());
            assertTrue(backend.uiViewProxy);
            assertTrue(CameraApi.diagnostics(0.0F).isPositionOverridden());
        } finally {
            CameraApi.clearPositionOverride();
        }
        assertEquals(base.position().x, CameraApi.getPosition(0.0F).x, 1.0E-9D);
    }

    @Test
    void frameCallbackFailureIsLoggedAndPropagatedWithoutFallback() {
        CameraFrame base = new CameraFrame(114L, 0.0F, CameraAttitude.IDENTITY,
                CameraAttitude.IDENTITY, new CameraVector(0.0D, 0.0D, 0.0D),
                new CameraVector(0.0D, 0.0D, 0.0D), false);
        CameraApi.installBackend(new TestBackend(base));
        CameraRegistration registration = CameraApi.registerProvider(new CameraProvider() {
            @Override public ResourceLocation id() { return testId("frame_failure"); }
            @Override public CameraSession acquire(CameraRigRequest request,
                                                   CameraProviderContext context) { return null; }
            @Override public CameraFrame frame(CameraFrame fallback, float partialTicks) {
                throw new IllegalStateException("frame failure");
            }
        });
        try {
            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> CameraApi.getFrame(0.0F));
            assertEquals("frame failure", error.getMessage());
        } finally {
            registration.close();
        }
    }

    private static CameraProvider provider(String id, int priority, boolean ownsView) {
        return new CameraProvider() {
            @Override public ResourceLocation id() { return testId(id); }
            @Override public int priority() { return priority; }
            @Override public CameraSession acquire(CameraRigRequest request,
                                                   CameraProviderContext context) { return null; }
            @Override public CameraFrame frame(CameraFrame fallback, float partialTicks) {
                return fallback;
            }
            @Override public boolean ownsView() { return ownsView; }
        };
    }

    private static ResourceLocation testId(String path) {
        return new ResourceLocation("uie_camera_test", path);
    }

    private static final class TestBackend implements CameraApi.Backend {
        private final CameraFrame frame;
        private CameraFrame appliedFrame;
        private boolean uiViewProxy;
        private TestBackend(CameraFrame frame) { this.frame = frame; }
        @Override public CameraFrame getFrame(float partialTicks) { return frame; }
        @Override public CameraSession acquire(CameraRigRequest request) { return null; }
        @Override public boolean isDroneActive() { return false; }
        @Override public boolean isFreeLookActive() { return false; }
        @Override public boolean isShoulderActive() { return false; }
        @Override public boolean isRenderOverrideActive() { return false; }
        @Override public void setDronePose(CameraVector position, CameraAttitude attitude) {}
        @Override public void clearDronePose() {}
        @Override public int viewportWidth() { return 800; }
        @Override public int viewportHeight() { return 600; }
        @Override public double verticalFov() { return 70.0D; }
        @Override public void applyFrame(CameraFrame value, boolean proxy) {
            appliedFrame = value;
            uiViewProxy = proxy;
        }
    }
}
