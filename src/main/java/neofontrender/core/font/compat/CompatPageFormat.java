package neofontrender.core.font.compat;

import java.util.Locale;

/**
 * Texture format used for compat glyph pages.
 *
 * <p>{@link #ALPHA} uploads the coverage mask as {@code GL_ALPHA}, the legacy SmoothFont layout.
 * It needs no per-texture sampling state, but {@code GL_ALPHA} is not a valid internal format in
 * a core profile: with a no-error context the upload fails silently, the texture stays
 * incomplete, sampling returns {@code (0,0,0,1)} and every glyph quad is painted as a solid
 * rectangle in the text colour.</p>
 *
 * <p>{@link #R8_SWIZZLE} keeps the same one byte per texel but uploads {@code GL_R8} and applies
 * {@code GL_TEXTURE_SWIZZLE_R/G/B = GL_ONE}, {@code GL_TEXTURE_SWIZZLE_A = GL_RED}. Sampling then
 * returns {@code (1,1,1,mask)}, which the existing texture-environment combine
 * ({@code RGB = primary colour}, {@code ALPHA = texture}) turns into the expected glyph. Both the
 * format and the swizzle are core-profile features (OpenGL 3.3).</p>
 */
enum CompatPageFormat {
    /** Legacy {@code GL_ALPHA}; only valid outside a core profile. */
    ALPHA,
    /** {@code GL_R8} plus texture swizzle; core-profile safe, one byte per texel. */
    R8_SWIZZLE;

    /**
     * @param configured {@code compat.rasterFormat}: auto, alpha, swizzle/r8
     * @param opengl33   whether the current context exposes OpenGL 3.3 (swizzle) capabilities
     */
    static CompatPageFormat select(String configured, boolean opengl33) {
        String mode = configured == null ? "auto" : configured.trim().toLowerCase(Locale.ROOT);
        if ("alpha".equals(mode)) return ALPHA;
        if ("swizzle".equals(mode) || "r8".equals(mode)) {
            return opengl33 ? R8_SWIZZLE : ALPHA;
        }
        // auto: swizzle whenever the context can express it, otherwise the legacy format.
        return opengl33 ? R8_SWIZZLE : ALPHA;
    }

    /** Capability probe kept separate so {@link #select} stays testable without a GL context. */
    static boolean opengl33() {
        try {
            return org.lwjgl.opengl.GLContext.getCapabilities().OpenGL33;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
