package neofontrender.core.font.cosmic;

import com.cleanroommc.kirino.KirinoClientCore;
import com.cleanroommc.kirino.KirinoCommonCore;

/**
 * Cleanroom/Kirino integration boundary for Cosmic text.
 *
 * <p>This is deliberately the only Cosmic class that references Kirino. A port to another
 * Minecraft version or loader can replace/remove this file and keep the rest of the renderer
 * unchanged. HDR is only selected when Kirino owns the world render delegate; GUI rendering after
 * Kirino's finalizer remains on the regular Minecraft framebuffer.</p>
 */
public final class KirinoHdrCompat {
    private KirinoHdrCompat() {
    }

    public static boolean useHdrTexture() {
        try {
            return KirinoCommonCore.KIRINO_CONFIG_HUB != null
                    && KirinoCommonCore.KIRINO_CONFIG_HUB.isEnable()
                    && KirinoCommonCore.KIRINO_CONFIG_HUB.isEnableRenderDelegate()
                    && KirinoCommonCore.KIRINO_CONFIG_HUB.isEnableHDR()
                    && !KirinoClientCore.isRenderUnsupported();
        } catch (LinkageError | RuntimeException ignored) {
            // A version without Kirino, or a loader with a different integration surface, uses RGBA8.
            return false;
        }
    }
}
