package neofontrender.addons.controller.sdl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SdlRuntimeTest {
    @Test
    void mapsSupportedPlatformsToControlifyNativeLayout() {
        assertEquals("/win32-x86-64/SDL3.dll",
                SdlRuntime.bundledNativeResource("Windows 11", "amd64"));
        assertEquals("/win32-aarch64/SDL3.dll",
                SdlRuntime.bundledNativeResource("Windows 11", "arm64"));
        assertEquals("/linux-x86-64/libSDL3.so",
                SdlRuntime.bundledNativeResource("Linux", "x86_64"));
        assertEquals("/linux-aarch64/libSDL3.so",
                SdlRuntime.bundledNativeResource("Linux", "aarch64"));
        assertEquals("/darwin-x86-64/libSDL3.dylib",
                SdlRuntime.bundledNativeResource("Mac OS X", "x86_64"));
        assertEquals("/darwin-aarch64/libSDL3.dylib",
                SdlRuntime.bundledNativeResource("Darwin", "aarch64"));
        assertNull(SdlRuntime.bundledNativeResource("Linux", "riscv64"));
        assertNull(SdlRuntime.bundledNativeResource("FreeBSD", "amd64"));
    }

    @Test
    void packagesEverySupportedNativeResource() {
        String[] resources = {
                "/win32-x86-64/SDL3.dll", "/win32-aarch64/SDL3.dll",
                "/linux-x86-64/libSDL3.so", "/linux-aarch64/libSDL3.so",
                "/darwin-x86-64/libSDL3.dylib", "/darwin-aarch64/libSDL3.dylib"
        };
        for (String resource : resources) {
            assertNotNull(SdlRuntime.class.getResource(resource), resource);
        }
    }
}
