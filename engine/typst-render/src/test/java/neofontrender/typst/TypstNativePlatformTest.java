package neofontrender.typst;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TypstNativePlatformTest {
    @Test void matchesBundlePathsForAllSupportedOperatingSystemsAndArchitectureAliases() {
        for (String arch : new String[]{"amd64", "x86_64", "aarch64", "arm64"}) {
            String expected = arch.equals("amd64") || arch.equals("x86_64") ? "x86_64" : "aarch64";
            assertPlatform("Windows 11", arch, "windows-" + expected, "neofontrender_typst.dll");
            assertPlatform("Mac OS X", arch, "macos-" + expected, "libneofontrender_typst.dylib");
            assertPlatform("Darwin", arch, "macos-" + expected, "libneofontrender_typst.dylib");
            assertPlatform("Linux", arch, "linux-" + expected + "-gnu", "libneofontrender_typst.so");
        }
        assertNull(TypstNativeRuntime.detectPlatform("Linux", "x86"));
        assertNull(TypstNativeRuntime.detectPlatform("FreeBSD", "amd64"));
    }

    private static void assertPlatform(String os, String arch, String directory, String library) {
        var actual = TypstNativeRuntime.detectPlatform(os, arch);
        assertNotNull(actual);
        assertEquals(directory, actual.directory);
        assertEquals(library, actual.libraryName);
    }
}
