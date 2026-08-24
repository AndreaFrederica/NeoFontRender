package neofontrender.addons.controller.sdl;

import dev.isxander.sdl.Sdl;
import org.apache.logging.log4j.LogManager;

/** Manual release check that exercises extraction, FFM binding, and SDL initialization. */
public final class SdlNativeSmoke {
    private SdlNativeSmoke() {}

    public static void main(String[] args) {
        Sdl sdl = SdlRuntime.open(LogManager.getLogger("Revo UI SDL smoke test"));
        if (sdl == null) throw new IllegalStateException("Bundled SDL3 native failed to initialize");
        try {
            System.out.println("Bundled SDL3 native initialized successfully: "
                    + sdl.version().SDL_GetVersion());
        } finally {
            sdl.init().SDL_Quit();
        }
    }
}
