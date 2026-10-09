package neofontrender.addons.camera;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.profiler.Profiler;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import neofontrender.addons.api.camera.CameraApi;
import neofontrender.addons.api.camera.CameraAttitude;
import neofontrender.addons.api.camera.CameraEyeAnchor;
import neofontrender.addons.api.camera.CameraEyeAnchorProvider;
import neofontrender.addons.api.camera.CameraRegistration;
import neofontrender.addons.api.camera.CameraVector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraEyeAnchorSamplerTest {
    private double savedOffsetX;
    private double savedOffsetY;
    private double savedOffsetZ;
    private double savedTransitionSpeed;
    private double savedLookDownDegrees;
    private boolean savedDynamicOffsets;
    private boolean savedCollision;
    private boolean savedCenterWhenClimbing;

    @BeforeAll
    static void initializeRealMinecraftRegistries() {
        Bootstrap.register();
    }

    @BeforeEach
    void configureRealShoulderRig() {
        savedOffsetX = ShoulderCameraConfig.offsetX;
        savedOffsetY = ShoulderCameraConfig.offsetY;
        savedOffsetZ = ShoulderCameraConfig.offsetZ;
        savedTransitionSpeed = ShoulderCameraConfig.transitionSpeed;
        savedLookDownDegrees = ShoulderCameraConfig.centerWhenLookingDownDegrees;
        savedDynamicOffsets = ShoulderCameraConfig.dynamicallyAdjustOffsets;
        savedCollision = ShoulderCameraConfig.collision;
        savedCenterWhenClimbing = ShoulderCameraConfig.centerWhenClimbing;
        ShoulderCameraConfig.offsetX = -0.75D;
        ShoulderCameraConfig.offsetY = 0.0D;
        ShoulderCameraConfig.offsetZ = 3.0D;
        ShoulderCameraConfig.transitionSpeed = 0.25D;
        ShoulderCameraConfig.centerWhenLookingDownDegrees = 0.0D;
        ShoulderCameraConfig.dynamicallyAdjustOffsets = true;
        ShoulderCameraConfig.collision = true;
        ShoulderCameraConfig.centerWhenClimbing = false;
    }

    @AfterEach
    void restoreShoulderConfig() {
        ShoulderCameraConfig.offsetX = savedOffsetX;
        ShoulderCameraConfig.offsetY = savedOffsetY;
        ShoulderCameraConfig.offsetZ = savedOffsetZ;
        ShoulderCameraConfig.transitionSpeed = savedTransitionSpeed;
        ShoulderCameraConfig.centerWhenLookingDownDegrees = savedLookDownDegrees;
        ShoulderCameraConfig.dynamicallyAdjustOffsets = savedDynamicOffsets;
        ShoulderCameraConfig.collision = savedCollision;
        ShoulderCameraConfig.centerWhenClimbing = savedCenterWhenClimbing;
    }

    @Test
    void acceptedEyeIsTheActualVanillaOrbitAndEightRayOrigin() {
        TestWorld world = new TestWorld(connection());
        EntityPlayerSP player = player(world);
        BlockPos obstruction = new BlockPos(0, 1, -2);
        world.doPreChunk(0, -1, true);
        assertTrue(world.setBlockState(obstruction, Blocks.STONE.getDefaultState()));
        assertEquals(Blocks.STONE, world.getBlockState(obstruction).getBlock());
        Vec3d nativeRayStart = new Vec3d(0.1D, player.getEyeHeight(), 0.1D);
        RayTraceResult nativeHit = world.rayTraceBlocks(nativeRayStart,
                nativeRayStart.add(0.0D, 0.0D, -5.0D), false, true, false);
        assertNotNull(nativeHit, "the real WorldClient ray must hit the placed block");
        world.takeRayStarts();

        CameraVector acceptedEye = new CameraVector(4.0D, 0.6D, 0.0D);
        CameraRegistration registration = CameraApi.registerEyeAnchorProvider(
                new CameraEyeAnchorProvider() {
                    @Override public ResourceLocation id() {
                        return new ResourceLocation("uie_test", "accepted_eye");
                    }
                    @Override public CameraEyeAnchor anchor(
                            neofontrender.addons.api.camera.CameraEyeAnchorContext context) {
                        assertEquals(player, context.player());
                        assertEquals(41L, context.sampleId());
                        assertEquals(0.5F, context.partialTicks());
                        assertEquals(player.getEyeHeight(), context.nativeEye().y, 1.0E-6D);
                        return new CameraEyeAnchor(acceptedEye.x, acceptedEye.y, acceptedEye.z);
                    }
                });
        try {
            CameraVector nativeEye = CameraEyeAnchorSampler.nativeEye(player, 0.5F);
            FreeLookCameraRig.Sample oldOrbit = FreeLookCameraRig.resolveVanilla(
                    player, CameraAttitude.IDENTITY, nativeEye, 5.0D);
            assertTrue(oldOrbit.position.subtract(nativeEye).length() < 5.0D,
                    "native-eye vanilla orbit should be shortened by the real block");
            world.takeRayStarts();

            CameraEyeAnchorSampler.Sample sampled = CameraEyeAnchorSampler.sample(player, 41L, 0.5F);
            FreeLookCameraRig.Sample acceptedOrbit = FreeLookCameraRig.resolveVanilla(
                    player, CameraAttitude.IDENTITY, sampled.bodyPosition(), 5.0D);
            List<Vec3d> orbitRayStarts = world.takeRayStarts();
            assertAll("Runtime's pre-rig sample must feed the original real collision path",
                    () -> assertEquals(acceptedEye.x, sampled.bodyPosition().x, 1.0E-9D),
                    () -> assertEquals(acceptedEye.y, sampled.bodyPosition().y, 1.0E-9D),
                    () -> assertEquals(8, orbitRayStarts.size()),
                    () -> assertEquals(acceptedEye.x - 0.1D, orbitRayStarts.get(0).x, 1.0E-9D),
                    () -> assertEquals(5.0D,
                            acceptedOrbit.position.subtract(acceptedEye).length(), 1.0E-6D));
        } finally {
            registration.close();
        }
    }

    @Test
    void providerPriorityNullCloseAndSampleCacheUseOneOwnerPerRenderSample() {
        TestWorld world = new TestWorld(connection());
        EntityPlayerSP player = player(world);
        java.util.concurrent.atomic.AtomicInteger fallbackCalls =
                new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger preferredCalls =
                new java.util.concurrent.atomic.AtomicInteger();
        CameraRegistration fallback = CameraApi.registerEyeAnchorProvider(
                new CameraEyeAnchorProvider() {
                    @Override public ResourceLocation id() {
                        return new ResourceLocation("uie_test", "eye_z_fallback");
                    }
                    @Override public int priority() { return 1; }
                    @Override public CameraEyeAnchor anchor(
                            neofontrender.addons.api.camera.CameraEyeAnchorContext context) {
                        fallbackCalls.incrementAndGet();
                        return new CameraEyeAnchor(2.0D, 0.5D, 0.0D);
                    }
                });
        CameraRegistration preferred = CameraApi.registerEyeAnchorProvider(
                new CameraEyeAnchorProvider() {
                    @Override public ResourceLocation id() {
                        return new ResourceLocation("uie_test", "eye_a_preferred");
                    }
                    @Override public int priority() { return 10; }
                    @Override public CameraEyeAnchor anchor(
                            neofontrender.addons.api.camera.CameraEyeAnchorContext context) {
                        preferredCalls.incrementAndGet();
                        return null;
                    }
                });
        try {
            CameraEyeAnchorSampler.Sample first = CameraEyeAnchorSampler.sample(player, 61L, 0.25F);
            assertTrue(first.hasAcceptedAnchor(), "null should defer to the next provider");
            assertEquals(2.0D, first.bodyPosition().x, 1.0E-9D);
            preferred.close();
            fallback.close();
            CameraEyeAnchorSampler.Sample cached = CameraEyeAnchorSampler.sample(player, 61L, 0.25F);
            assertTrue(cached.hasAcceptedAnchor(), "closing registrations must not split one sample");
            assertEquals(1, preferredCalls.get());
            assertEquals(1, fallbackCalls.get());

            CameraEyeAnchorSampler.Sample next = CameraEyeAnchorSampler.sample(player, 62L, 0.25F);
            assertTrue(!next.hasAcceptedAnchor(), "closed providers must be absent next sample");
            assertEquals(CameraEyeAnchorSampler.nativeEye(player, 0.25F).x,
                    next.bodyPosition().x, 1.0E-9D);
        } finally {
            preferred.close();
            fallback.close();
        }
    }

    @Test
    void providerFailurePropagatesTheOriginalExceptionAndDoesNotCacheFallback() {
        TestWorld world = new TestWorld(connection());
        EntityPlayerSP player = player(world);
        IllegalStateException expected = new IllegalStateException("accepted eye unavailable");
        CameraRegistration registration = CameraApi.registerEyeAnchorProvider(
                new CameraEyeAnchorProvider() {
                    @Override public ResourceLocation id() {
                        return new ResourceLocation("uie_test", "eye_failure");
                    }
                    @Override public CameraEyeAnchor anchor(
                            neofontrender.addons.api.camera.CameraEyeAnchorContext context) {
                        throw expected;
                    }
                });
        try {
            assertSame(expected, assertThrows(IllegalStateException.class,
                    () -> CameraEyeAnchorSampler.sample(player, 71L, 0.75F)));
        } finally {
            registration.close();
        }
        assertTrue(!CameraEyeAnchorSampler.sample(player, 71L, 0.75F).hasAcceptedAnchor(),
                "a failed callback must not be cached as a successful native fallback");
    }

    @Test
    void acceptedAnchorRejectsNonFiniteCoordinatesBeforeVectorSanitization() {
        assertThrows(IllegalArgumentException.class,
                () -> new CameraEyeAnchor(Double.NaN, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new CameraEyeAnchor(0.0D, Double.POSITIVE_INFINITY, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new CameraEyeAnchor(0.0D, 0.0D, Double.NEGATIVE_INFINITY));
    }

    @Test
    void shoulderAcquireResetAndResolveUseAcceptedEyeWhenNativeEyeIsBlocked() {
        assertShoulderResetUsesAcceptedEye("shoulder_native_blocked", true);
    }

    @Test
    void cursorShoulderAcquireResetAndResolveUseAcceptedEyeWhenAcceptedEyeIsBlocked() {
        assertShoulderResetUsesAcceptedEye("cursor_shoulder_accepted_blocked", false);
    }

    @Test
    void shoulderTickKeepsAcceptedClearOffsetWhenOnlyNativeEyeIsBlocked() {
        assertShoulderTickUsesAcceptedEye("shoulder_tick_native_blocked", true);
    }

    @Test
    void cursorShoulderTickKeepsAcceptedBlockedOffsetWhenNativeEyeIsClear() {
        assertShoulderTickUsesAcceptedEye("cursor_shoulder_tick_accepted_blocked", false);
    }

    @Test
    void shutdownAndRegistrationCloseKeepTheInFlightEyeSampleStable() {
        TestWorld world = new TestWorld(connection());
        EntityPlayerSP player = player(world);
        CameraVector anchor = new CameraVector(7.0D, 1.25D, -2.0D);
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        CameraRegistration registration = acceptedEyeProvider(
                "eye_mode_shutdown_keeps_sample", player, anchor, calls);
        neofontrender.addons.api.camera.CameraEyeAnchorContext context =
                new neofontrender.addons.api.camera.CameraEyeAnchorContext(
                        player, 810L, 0.375F, CameraEyeAnchorSampler.nativeEye(player, 0.375F));
        try {
            assertVectorEquals(anchor,
                    CameraApi.sampleEyeAnchor(context).toCameraVector());
            registration.close();
            CameraRuntime.shutdown();
            CameraEyeAnchor cached = CameraApi.sampleEyeAnchor(context);
            assertAll("ordinary rig shutdown and registration close cannot split an active sample",
                    () -> assertNotNull(cached),
                    () -> assertVectorEquals(anchor, cached.toCameraVector()),
                    () -> assertEquals(1, calls.get()));
        } finally {
            registration.close();
            CameraApi.clearEyeAnchorSampleCache();
        }
    }

    @Test
    void worldUnloadEventReleasesCachedPlayerAndResultButKeepsTheProviderRegistered() {
        TestWorld world = new TestWorld(connection());
        assertWorldBoundaryClearsEyeSample("eye_world_unload", world, () ->
                new CameraModule().worldUnload(new WorldEvent.Unload(world)));
    }

    @Test
    void disconnectEventReleasesCachedPlayerAndResultButKeepsTheProviderRegistered() {
        TestWorld world = new TestWorld(connection());
        assertWorldBoundaryClearsEyeSample("eye_disconnect", world, () ->
                new CameraModule().disconnected(
                        new FMLNetworkEvent.ClientDisconnectionFromServerEvent(
                                new NetworkManager(EnumPacketDirection.CLIENTBOUND))));
    }

    private static void assertShoulderResetUsesAcceptedEye(String id, boolean nativeBlocked) {
        TestWorld world = new TestWorld(connection());
        EntityPlayerSP player = player(world);
        loadWall(world, nativeBlocked ? -1 : 3, nativeBlocked ? 1 : 5);
        CameraVector acceptedEye = new CameraVector(4.0D, player.getEyeHeight(), 0.0D);
        CameraVector nativeEye = CameraEyeAnchorSampler.nativeEye(player, 0.5F);
        RayTraceResult nativeHit = traceBehind(world, nativeEye);
        RayTraceResult acceptedHit = traceBehind(world, acceptedEye);
        assertEquals(nativeBlocked, nativeHit != null,
                "the real WorldClient ray must establish native-eye obstruction state");
        assertEquals(!nativeBlocked, acceptedHit != null,
                "the real WorldClient ray must establish accepted-eye obstruction state");
        world.takeRayStarts();

        java.util.concurrent.atomic.AtomicInteger providerCalls =
                new java.util.concurrent.atomic.AtomicInteger();
        CameraRegistration registration = acceptedEyeProvider(id, player, acceptedEye,
                providerCalls);
        try {
            CameraVector resetAnchor = CameraRuntime.resetShoulderRig(player,
                    CameraAttitude.IDENTITY, 310L, 0.5F);
            ShoulderCameraRig.Sample resolved = CameraRuntime.resolveShoulderRig(
                    player, CameraAttitude.IDENTITY, resetAnchor, 0.5F);
            List<Vec3d> actualRigRays = world.takeRayStarts();
            assertTrue(!actualRigRays.isEmpty(),
                    "production dynamic shoulder reset/resolve must ray-trace the real world");

            if (nativeBlocked) {
                assertAll("shoulder initialization must use the accepted clear eye",
                        () -> assertEquals(1, providerCalls.get()),
                        () -> assertVectorEquals(acceptedEye, resetAnchor),
                        () -> assertEquals(acceptedEye.x - 0.75D, resolved.target.x, 0.02D),
                        () -> assertEquals(acceptedEye.y, resolved.target.y, 0.02D));
            } else {
                assertAll("cursor-shoulder initialization must honor accepted-eye collision",
                        () -> assertEquals(1, providerCalls.get()),
                        () -> assertVectorEquals(acceptedEye, resetAnchor),
                        () -> assertTrue(resolved.position.subtract(acceptedEye).length()
                                        < Math.sqrt(0.75D * 0.75D + 3.0D * 3.0D) - 0.05D,
                                "accepted-eye wall must shorten the real camera orbit"));
            }
        } finally {
            registration.close();
        }
    }

    private static void assertShoulderTickUsesAcceptedEye(String id, boolean nativeBlocked) {
        TestWorld world = new TestWorld(connection());
        EntityPlayerSP player = player(world);
        loadWall(world, nativeBlocked ? -1 : 3, nativeBlocked ? 1 : 5);
        CameraVector acceptedEye = new CameraVector(4.0D, player.getEyeHeight(), 0.0D);
        CameraVector nativeEye = CameraEyeAnchorSampler.nativeEye(player, 1.0F);
        assertEquals(nativeBlocked, traceBehind(world, nativeEye) != null,
                "the actual WorldClient establishes the native-eye ray state");
        assertEquals(!nativeBlocked, traceBehind(world, acceptedEye) != null,
                "the actual WorldClient establishes the accepted-eye ray state");
        world.takeRayStarts();

        ShoulderCameraRig.reset(player, CameraAttitude.IDENTITY, acceptedEye);
        CameraVector before = CameraRuntime.resolveShoulderRig(player,
                CameraAttitude.IDENTITY, acceptedEye, 1.0F).target;
        java.util.concurrent.atomic.AtomicInteger providerCalls =
                new java.util.concurrent.atomic.AtomicInteger();
        CameraRegistration registration = acceptedEyeProvider(id, player, acceptedEye,
                providerCalls);
        try {
            CameraVector tickAnchor = CameraRuntime.tickShoulderRig(player,
                    CameraAttitude.IDENTITY, 411L, 1.0F);
            CameraVector after = CameraRuntime.resolveShoulderRig(player,
                    CameraAttitude.IDENTITY, acceptedEye, 1.0F).target;
            List<Vec3d> actualRigRays = world.takeRayStarts();
            assertTrue(!actualRigRays.isEmpty(),
                    "production shoulder tick must advance from actual WorldClient rays");
            assertAll("the tick must keep the accepted eye's collision target through render resolve",
                    () -> assertEquals(1, providerCalls.get()),
                    () -> assertVectorEquals(acceptedEye, tickAnchor),
                    () -> assertVectorEquals(before, after));
        } finally {
            registration.close();
        }
    }

    private static CameraRegistration acceptedEyeProvider(String id, EntityPlayerSP player,
                                                           CameraVector eye,
                                                           java.util.concurrent.atomic.AtomicInteger calls) {
        return CameraApi.registerEyeAnchorProvider(new CameraEyeAnchorProvider() {
            @Override public ResourceLocation id() {
                return new ResourceLocation("uie_test", id);
            }
            @Override public CameraEyeAnchor anchor(
                    neofontrender.addons.api.camera.CameraEyeAnchorContext context) {
                assertSame(player, context.player());
                calls.incrementAndGet();
                return new CameraEyeAnchor(eye.x, eye.y, eye.z);
            }
        });
    }

    private static void loadWall(TestWorld world, int minX, int maxX) {
        world.doPreChunk(minX >> 4, -1, true);
        world.doPreChunk(maxX >> 4, -1, true);
        for (int x = minX; x <= maxX; x++) {
            assertTrue(world.setBlockState(new BlockPos(x, 1, -2),
                    Blocks.STONE.getDefaultState()));
        }
    }

    private static RayTraceResult traceBehind(TestWorld world, CameraVector eye) {
        return world.rayTraceBlocks(new Vec3d(eye.x, eye.y, eye.z),
                new Vec3d(eye.x, eye.y, eye.z - 5.0D), false, true, false);
    }

    private static void assertVectorEquals(CameraVector expected, CameraVector actual) {
        assertAll(
                () -> assertEquals(expected.x, actual.x, 1.0E-6D),
                () -> assertEquals(expected.y, actual.y, 1.0E-6D),
                () -> assertEquals(expected.z, actual.z, 1.0E-6D));
    }

    private static void assertWorldBoundaryClearsEyeSample(String id, TestWorld world,
                                                           Runnable lifecycle) {
        EntityPlayerSP player = player(world);
        CameraVector anchor = new CameraVector(8.0D, 1.4D, 3.0D);
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        CameraRegistration registration = acceptedEyeProvider(id, player, anchor, calls);
        neofontrender.addons.api.camera.CameraEyeAnchorContext context =
                new neofontrender.addons.api.camera.CameraEyeAnchorContext(
                        player, 811L, 0.625F, CameraEyeAnchorSampler.nativeEye(player, 0.625F));
        try {
            assertNotNull(CameraApi.sampleEyeAnchor(context));
            RuntimeException teardownFailure = null;
            try {
                lifecycle.run();
            } catch (RuntimeException error) {
                teardownFailure = error;
            }
            if (net.minecraft.client.Minecraft.getMinecraft() == null) {
                assertTrue(teardownFailure instanceof NullPointerException,
                        "only the uninitialized Minecraft singleton may block this headless event fixture");
                assertNull(teardownFailure.getCause(),
                        "the headless boundary must preserve the original direct NullPointerException");
                assertTrue(java.util.Arrays.stream(teardownFailure.getStackTrace())
                                .anyMatch(frame -> frame.getClassName().equals(
                                        "neofontrender.addons.camera.CameraPerspectiveController")),
                        "the test must reach real session teardown before the headless client boundary");
            } else {
                assertNull(teardownFailure,
                        "a live client must complete its actual world boundary handler");
            }
            CameraEyeAnchor resampled = CameraApi.sampleEyeAnchor(context);
            assertAll("world unload and disconnect clear the exact eye cache but keep providers",
                    () -> assertNotNull(resampled),
                    () -> assertVectorEquals(anchor, resampled.toCameraVector()),
                    () -> assertEquals(2, calls.get()));
        } finally {
            registration.close();
            CameraApi.clearEyeAnchorSampleCache();
        }
    }


    private static NetHandlerPlayClient connection() {
        return new NetHandlerPlayClient(null, null, (NetworkManager) null,
                new GameProfile(UUID.randomUUID(), "camera-eye-test"));
    }

    private static EntityPlayerSP player(TestWorld world) {
        EntityPlayerSP player = new EntityPlayerSP(null, world, world.connection,
                new StatisticsManager(), new RecipeBook());
        player.setPosition(0.0D, 0.0D, 0.0D);
        player.prevPosX = player.prevPosY = player.prevPosZ = 0.0D;
        return player;
    }

    private static final class TestWorld extends WorldClient {
        private final NetHandlerPlayClient connection;
        private final List<Vec3d> rayStarts = new ArrayList<>();

        private TestWorld(NetHandlerPlayClient connection) {
            super(connection, new WorldSettings(0L, GameType.SURVIVAL, false, false,
                    WorldType.DEFAULT), 0, EnumDifficulty.NORMAL, new Profiler());
            this.connection = connection;
        }

        @Override public RayTraceResult rayTraceBlocks(Vec3d start, Vec3d end,
                                                       boolean stopOnLiquid,
                                                       boolean ignoreBlockWithoutBoundingBox,
                                                       boolean returnLastUncollidableBlock) {
            rayStarts.add(start);
            return super.rayTraceBlocks(start, end, stopOnLiquid,
                    ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock);
        }

        private List<Vec3d> takeRayStarts() {
            List<Vec3d> result = new ArrayList<>(rayStarts);
            rayStarts.clear();
            return result;
        }
    }
}
