package neofontrender.addons.flight;

import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.api.flight.FlightRegistration;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBodyRenderScopeTest {
    @Test
    void flightApiPoseIsSampledOnceAndSharedByRootAndHeadConsumers() throws Throwable {
        AtomicInteger sampleCount = new AtomicInteger();
        FlightBodyPose expectedPose = FlightBodyPose.modelUpY(FlightAttitude.IDENTITY,
                FlightAttitude.fromMinecraftDegrees(20.0D, 30.0D, 0.0D));
        FlightRegistration registration = FlightApi.registerBodyPoseProvider(
                new ResourceLocation("test", "render_scope_sample"), Integer.MAX_VALUE,
                (player, partialTicks) -> {
                    sampleCount.incrementAndGet();
                    return expectedPose;
                });
        Object entity = new Object();
        try {
            PlayerBodyRenderScope.withSampledPose(entity,
                    () -> FlightApi.queryBodyPose(null, 0.5F), () -> {
                        PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current(entity);
                        assertSame(expectedPose, context.pose());
                        PlayerBodyRenderTransform root = context.transform();
                        PlayerBodyRenderTransform head = context.transform();
                        assertSame(root, head);
                        assertEquals(1.0D, root.modelUpToWorld().y, 1.0E-6D);
                        assertEquals(-30.0D, head.ysmBoneAngles().yawDegrees, 1.0E-6D);
                        assertEquals(-20.0D, head.ysmBoneAngles().pitchDegrees, 1.0E-6D);
                    });
        } finally {
            registration.close();
        }
        assertEquals(1, sampleCount.get());
    }

    @Test
    void samplesOnceAndNestedInactiveRenderCannotObserveOuterPose() throws Throwable {
        Object outerPlayer = new Object();
        Object innerNonPlayer = new Object();
        FlightBodyPose outerPose = FlightBodyPose.modelUpY(FlightAttitude.IDENTITY);
        AtomicInteger outerSamples = new AtomicInteger();
        AtomicInteger innerSamples = new AtomicInteger();

        PlayerBodyRenderScope.withSampledPose(outerPlayer, () -> {
            outerSamples.incrementAndGet();
            return outerPose;
        }, () -> {
            PlayerBodyRenderScope.Context outer = PlayerBodyRenderScope.current(outerPlayer);
            assertTrue(outer.active());
            assertSame(outerPose, outer.pose());

            PlayerBodyRenderScope.withSampledPose(innerNonPlayer, () -> {
                innerSamples.incrementAndGet();
                return null;
            }, () -> {
                PlayerBodyRenderScope.Context inner = PlayerBodyRenderScope.current();
                assertFalse(inner.active());
                assertNull(inner.pose());
                assertNull(PlayerBodyRenderScope.current(outerPlayer));
                assertSame(innerNonPlayer, inner.entity());
            });

            assertSame(outer, PlayerBodyRenderScope.current());
            assertSame(outer, PlayerBodyRenderScope.current(outerPlayer));
        });

        assertEquals(1, outerSamples.get());
        assertEquals(1, innerSamples.get());
        assertNull(PlayerBodyRenderScope.current());
    }

    @Test
    void disabledOrNullPoseScopeStillFencesNestedHooks() throws Throwable {
        Object entity = new Object();

        PlayerBodyRenderScope.withSampledPose(entity, () -> null, () -> {
            assertFalse(PlayerBodyRenderScope.current(entity).active());
            assertNull(PlayerBodyRenderScope.current(new Object()));
        });

        assertNull(PlayerBodyRenderScope.current());
    }

    @Test
    void rawLegacyPosePreservesNativeRenderFieldsAndSuppressesOnlyTheLocalFallback() throws Throwable {
        Object entity = new Object();
        FlightBodyPose legacyPose = new FlightBodyPose(
                FlightAttitude.fromMinecraftDegrees(15.0D, 35.0D, 0.0D));
        AtomicInteger sampleCount = new AtomicInteger();
        FlightRegistration registration = FlightApi.registerBodyPoseProvider(
                new ResourceLocation("test", "legacy_nonelytra_scope"), Integer.MAX_VALUE,
                (player, partialTicks) -> {
                    sampleCount.incrementAndGet();
                    return legacyPose;
                });
        float[] renderFields = {11.0F, 12.0F, 13.0F, 14.0F, 15.0F, 16.0F};
        float[] originalFields = renderFields.clone();

        try {
            PlayerBodyRenderScope.withSampledPose(entity,
                    () -> FlightApi.queryBodyPose(null, 0.5F),
                    pose -> FlightBodyPresentationPolicy.resolve(pose, false).ownsRootQuaternion(),
                    () -> {
                        PlayerBodyRenderScope.Context context = PlayerBodyRenderScope.current(entity);
                        assertSame(legacyPose, context.pose());
                        assertTrue(context.hasPose());
                        assertFalse(context.active());
                        assertNull(context.transform());

                        boolean aligned = PlayerBodyRenderScope.alignLocalRollWhenPoseAbsent(
                                context, true, 27.0F, 40.0F, 50.0F, renderFields);

                        assertFalse(aligned,
                                "A raw provider pose suppresses the legacy local-roll alignment");
                        assertArrayEquals(originalFields, renderFields, 0.0F,
                                "A nonowning pose must preserve the cursor/native body and head fields");
                    });
        } finally {
            registration.close();
        }
        assertEquals(1, sampleCount.get(), "The raw pose is queried once for this render");

        PlayerBodyRenderScope.withSampledPose(entity, () -> null, ignored -> false, () -> {
            float[] nativeFields = {1.0F, 2.0F, 3.0F, 4.0F, 5.0F, 6.0F};
            boolean aligned = PlayerBodyRenderScope.alignLocalRollWhenPoseAbsent(
                    PlayerBodyRenderScope.current(entity), true, 27.0F,
                    40.0F, 50.0F, nativeFields);
            assertTrue(aligned, "No pose retains the original local-roll alignment path");
            assertArrayEquals(new float[]{1.0F, 2.0F, 40.0F, 50.0F, 40.0F, 50.0F},
                    nativeFields, 0.0F);
        });
    }

    @Test
    void nonowningRawPoseStillFencesNestedRootAndHeadHooks() throws Throwable {
        Object outerEntity = new Object();
        Object innerEntity = new Object();
        FlightBodyPose outerPose = FlightBodyPose.modelUpY(FlightAttitude.IDENTITY);
        FlightBodyPose innerLegacyPose = new FlightBodyPose(FlightAttitude.IDENTITY);

        PlayerBodyRenderScope.withSampledPose(outerEntity, () -> outerPose,
                ignored -> true, () -> {
                    PlayerBodyRenderScope.Context outer = PlayerBodyRenderScope.current(outerEntity);
                    PlayerBodyRenderScope.withSampledPose(innerEntity, () -> innerLegacyPose,
                            pose -> FlightBodyPresentationPolicy.resolve(pose, false)
                                    .ownsRootQuaternion(), () -> {
                                PlayerBodyRenderScope.Context inner =
                                        PlayerBodyRenderScope.current(innerEntity);
                                assertTrue(inner.hasPose());
                                assertFalse(inner.active());
                                assertSame(innerLegacyPose, inner.pose());
                                assertNull(PlayerBodyRenderScope.current(outerEntity),
                                        "Inactive raw-pose scope must fence the outer root/head sample");
                            });
                    assertSame(outer, PlayerBodyRenderScope.current(outerEntity));
                });
    }

    @Test
    void thrownRenderCausePropagatesUnchangedAndScopeIsCleared() {
        Object entity = new Object();
        RuntimeException original = new RuntimeException("render failure sentinel");

        RuntimeException observed = assertThrows(RuntimeException.class,
                () -> PlayerBodyRenderScope.withSampledPose(entity,
                        () -> FlightBodyPose.modelUpY(FlightAttitude.IDENTITY), () -> {
                            assertTrue(PlayerBodyRenderScope.current(entity).active());
                            throw original;
                        }));

        assertSame(original, observed);
        assertNull(PlayerBodyRenderScope.current());
    }

    @Test
    void nestedThrowRestoresOuterActiveScopeAndAllowsOuterRenderToContinue() throws Throwable {
        Object outerPlayer = new Object();
        Object innerEntity = new Object();
        FlightBodyPose outerPose = FlightBodyPose.modelUpY(FlightAttitude.IDENTITY);
        RuntimeException original = new RuntimeException("nested render failure sentinel");
        AtomicInteger outerContinuation = new AtomicInteger();

        PlayerBodyRenderScope.withSampledPose(outerPlayer, () -> outerPose, () -> {
            PlayerBodyRenderScope.Context outer = PlayerBodyRenderScope.current(outerPlayer);
            RuntimeException observed = assertThrows(RuntimeException.class,
                    () -> PlayerBodyRenderScope.withSampledPose(innerEntity, () -> null, () -> {
                        assertFalse(PlayerBodyRenderScope.current().active());
                        assertNull(PlayerBodyRenderScope.current(outerPlayer));
                        throw original;
                    }));

            assertSame(original, observed);
            assertSame(outer, PlayerBodyRenderScope.current());
            assertSame(outer, PlayerBodyRenderScope.current(outerPlayer));
            outerContinuation.incrementAndGet();
        });

        assertEquals(1, outerContinuation.get());
        assertNull(PlayerBodyRenderScope.current());
    }

    @Test
    void thrownSamplerDoesNotInvokeRenderOrLeaveAContext() {
        Object entity = new Object();
        RuntimeException original = new RuntimeException("provider failure sentinel");
        AtomicInteger renderCalls = new AtomicInteger();

        RuntimeException observed = assertThrows(RuntimeException.class,
                () -> PlayerBodyRenderScope.withSampledPose(entity, () -> { throw original; },
                        renderCalls::incrementAndGet));

        assertSame(original, observed);
        assertEquals(0, renderCalls.get());
        assertNull(PlayerBodyRenderScope.current());
    }
}
