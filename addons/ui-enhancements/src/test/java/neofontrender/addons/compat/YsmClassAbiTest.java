package neofontrender.addons.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YsmClassAbiTest {
    private static final String YSM_119_SHA256 =
            "5DFE1A06E058FF7EB006918E4EBC60C18C450FFC441CAC1A5CFF0766251F1D60";

    @Test
    void verifiesThePinnedYsm119JarStructureWithoutLoadingItsClasses() throws Exception {
        String configuredPath = System.getenv("UIE_YSM_JAR");
        assertTrue(configuredPath != null && !configuredPath.isBlank(),
                "Set UIE_YSM_JAR to run the real YSM 1.1.9 structure gate");
        Path jar = Path.of(configuredPath).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(jar), "YSM JAR does not exist: " + jar);
        assertEquals(YSM_119_SHA256, sha256(jar));

        try (URLClassLoader loader = loader(jar.toUri().toURL())) {
            YsmClassAbi.Result result = YsmClassAbi.inspect(loader);
            assertEquals(YsmClassAbi.Result.Status.SUPPORTED, result.status(), result.reason());
            assertTrue(result.supported());
            YsmClassAbi.VersionMetadata version = YsmClassAbi.versionMetadata(loader);
            assertEquals("yes_steve_model", YsmClassAbi.MOD_ID);
            assertEquals("1.1.9", version.version());
            assertEquals("mcmod.info", version.reason());
            assertTrue(version.cause() == null);
        }
    }

    @Test
    void classResourceAbsenceIsDistinctFromUnsupportedInstalledAbi() throws Exception {
        try (URLClassLoader absent = new URLClassLoader(new URL[0], null)) {
            assertEquals(YsmClassAbi.Result.Status.ABSENT,
                    YsmClassAbi.inspect(absent).status());
            assertEquals(YsmClassAbi.VersionMetadata.UNAVAILABLE,
                    YsmClassAbi.versionMetadata(absent).version());
        }

        Path partialJar = Files.createTempFile("uie-ysm-partial-install-", ".jar");
        try {
            try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(partialJar))) {
                output.putNextEntry(new JarEntry(YsmClassAbi.MODEL_DATA + ".class"));
                output.write(markerClass(YsmClassAbi.MODEL_DATA));
                output.closeEntry();
            }
            try (URLClassLoader partial = loader(partialJar.toUri().toURL())) {
                YsmClassAbi.Result result = YsmClassAbi.inspect(partial);
                assertEquals(YsmClassAbi.Result.Status.UNSUPPORTED, result.status());
                assertTrue(result.reason().contains(YsmClassAbi.RENDERER + ".class is missing"));
            }
        } finally {
            Files.deleteIfExists(partialJar);
        }

        Path unsupportedJar = Files.createTempFile("uie-ysm-unsupported-abi-", ".jar");
        try {
            try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(unsupportedJar))) {
                output.putNextEntry(new JarEntry(YsmClassAbi.RENDERER + ".class"));
                output.write(rendererWithUnsupportedDescriptor());
                output.closeEntry();
            }
            try (URLClassLoader unsupported = loader(unsupportedJar.toUri().toURL())) {
                YsmClassAbi.Result result = YsmClassAbi.inspect(unsupported);
                assertEquals(YsmClassAbi.Result.Status.UNSUPPORTED, result.status());
                assertTrue(result.reason().contains("missing " + YsmClassAbi.RENDERER + ".render"));
            }
        } finally {
            Files.deleteIfExists(unsupportedJar);
        }
    }

    @Test
    void mixinRuleGatesAbsentUnsupportedDisabledAndNonClientCases() throws Exception {
        String mixin = YsmCompat.RENDER_MIXIN;
        AtomicBoolean enabledRead = new AtomicBoolean();
        try (URLClassLoader absent = new URLClassLoader(new URL[0], null)) {
            assertFalse(YsmCompat.permits(mixin, absent, true, () -> {
                enabledRead.set(true);
                return true;
            }));
        }
        assertFalse(enabledRead.get(), "Config must not be consulted when YSM is absent");

        Path unsupportedJar = Files.createTempFile("uie-ysm-unsupported-gate-", ".jar");
        try {
            writeUnsupportedRendererJar(unsupportedJar);
            try (URLClassLoader unsupported = loader(unsupportedJar.toUri().toURL())) {
                assertFalse(YsmCompat.permits(mixin, unsupported, true, () -> true));
            }
        } finally {
            Files.deleteIfExists(unsupportedJar);
        }

        try (URLClassLoader absent = new URLClassLoader(new URL[0], null)) {
            assertFalse(YsmCompat.permits(mixin, absent, false, () -> true));
            assertTrue(YsmCompat.permits("neofontrender.addons.mixin.OtherMixin", absent,
                    true, () -> false));
        }

        String configuredPath = System.getenv("UIE_YSM_JAR");
        assertTrue(configuredPath != null && !configuredPath.isBlank());
        try (URLClassLoader actual = loader(Path.of(configuredPath).toUri().toURL())) {
            assertFalse(YsmCompat.permits(mixin, actual, true, () -> false));
            assertTrue(YsmCompat.permits(mixin, actual, true, () -> true));
        }
    }

    @Test
    void classResourceIoFaultFailsAtTheGateWithTheOriginalCause() throws Exception {
        IOException original = new IOException("class resource read failure sentinel");
        ClassLoader broken = new ClassLoader(null) {
            @Override
            public URL getResource(String name) {
                if (!name.equals(YsmClassAbi.RENDERER + ".class")) return null;
                try {
                    return new URL(null, "resource-failure:/" + name,
                            new java.net.URLStreamHandler() {
                                @Override
                                protected java.net.URLConnection openConnection(URL url) {
                                    return new java.net.URLConnection(url) {
                                        @Override public void connect() {}
                                        @Override public java.io.InputStream getInputStream()
                                                throws IOException { throw original; }
                                    };
                                }
                            });
                } catch (java.net.MalformedURLException error) {
                    throw new AssertionError(error);
                }
            }
        };

        IllegalStateException observed = assertThrows(IllegalStateException.class,
                () -> YsmClassAbi.inspect(broken));
        assertSame(original, observed.getCause());
        assertTrue(observed.getMessage().contains(YsmClassAbi.RENDERER + ".class"));
    }

    private static URLClassLoader loader(URL jar) {
        return new URLClassLoader(new URL[]{jar}, ClassLoader.getPlatformClassLoader());
    }

    private static String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        return HexFormat.of().withUpperCase().formatHex(digest);
    }

    private static byte[] rendererWithUnsupportedDescriptor() {
        return markerClass(YsmClassAbi.RENDERER);
    }

    private static byte[] markerClass(String internalName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
                internalName, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void writeUnsupportedRendererJar(Path jar) throws IOException {
        try (OutputStream stream = Files.newOutputStream(jar);
             JarOutputStream output = new JarOutputStream(stream)) {
            output.putNextEntry(new JarEntry(YsmClassAbi.RENDERER + ".class"));
            output.write(rendererWithUnsupportedDescriptor());
            output.closeEntry();
        }
    }
}
