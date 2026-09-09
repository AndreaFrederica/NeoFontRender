#version 120

uniform sampler2D u_texture;
uniform vec2 u_textureSize;
uniform vec2 u_sampleSize;
uniform vec4 u_effectBounds;
uniform vec4 u_textColor;
uniform vec4 u_outlineColor;
uniform vec4 u_glowColor;
uniform float u_effectType;
uniform float u_time;
uniform float u_glowRadius;
uniform float u_glowPasses;
uniform float u_glowAlpha;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

bool inside(vec2 pixel) {
    return pixel.x >= u_effectBounds.x && pixel.x <= u_effectBounds.z &&
           pixel.y >= u_effectBounds.y && pixel.y <= u_effectBounds.w;
}

bool textAt(vec2 uv, vec2 delta) {
    vec2 sampleUv = uv + delta / u_sampleSize;
    vec2 pixel = vec2(sampleUv.x, 1.0 - sampleUv.y) * u_textureSize;
    return inside(pixel) && texture2D(u_texture, sampleUv).a > 0.0;
}

vec3 fireColor(vec2 uv) {
    vec2 firePos = uv * u_textureSize * 0.12 + vec2(0.0, -u_time * 4.0);
    float heat = noise(firePos);
    vec3 deepRed = vec3(0.85, 0.10, 0.00);
    vec3 brightOrange = vec3(1.00, 0.45, 0.00);
    vec3 hotYellow = vec3(1.00, 0.95, 0.40);
    vec3 color = mix(deepRed, brightOrange, smoothstep(0.1, 0.5, heat));
    return mix(color, hotYellow, smoothstep(0.5, 0.85, heat));
}

// The original Brilliant shader animates fire colour only.  Add a very small
// domain warp for the flame mask so the glyph edge visibly flickers instead of
// reading as a static coloured glyph.  The displacement stays below one pixel
// at normal GUI scales and is only used by the flame branch below.
vec2 flameUv(vec2 uv) {
    vec2 p = uv * u_textureSize * 0.055;
    float t = u_time * 1.8;
    float dx = noise(p + vec2(0.0, -t)) - 0.5;
    float dy = noise(p * 1.37 + vec2(t * 0.35, 0.0)) - 0.5;
    return uv + vec2(dx * 1.35 / u_sampleSize.x, dy * 0.75 / u_sampleSize.y);
}

void main() {
    vec2 uv = gl_TexCoord[0].st;
    vec2 pixel = vec2(uv.x, 1.0 - uv.y) * u_textureSize;
    vec4 source = texture2D(u_texture, uv);
    if (!inside(pixel)) discard;

    // TextAnimator Neon: the captured glyph is an alpha mask. Generate a soft radial halo
    // directly from neighboring texels instead of redrawing the glyph repeatedly on the CPU.
    if (u_effectType > 3.5 && u_effectType < 4.5) {
        if (source.a > 0.0) {
            gl_FragColor = vec4(u_textColor.rgb, u_textColor.a * source.a);
            return;
        }
        float radius = max(1.0, u_glowRadius);
        float passes = clamp(u_glowPasses, 4.0, 32.0);
        float glow = 0.0;
        for (int i = 0; i < 32; i++) {
            if (float(i) >= passes) break;
            float angle = 6.2831853 * (float(i) + 0.5) / passes;
            vec2 direction = vec2(cos(angle), sin(angle));
            float nearAlpha = texture2D(u_texture,
                    uv + direction * (radius * 0.35) / u_sampleSize).a;
            float midAlpha = texture2D(u_texture,
                    uv + direction * (radius * 0.70) / u_sampleSize).a;
            float farAlpha = texture2D(u_texture,
                    uv + direction * radius / u_sampleSize).a;
            glow = max(glow, nearAlpha * 0.85);
            glow = max(glow, midAlpha * 0.55);
            glow = max(glow, farAlpha * 0.25);
        }
        if (glow > 0.0 && u_glowAlpha > 0.0) {
            gl_FragColor = vec4(u_textColor.rgb, min(0.9, glow * u_glowAlpha));
            return;
        }
        discard;
    }

    if (u_effectType > 2.5 && u_effectType < 3.5) {
        // Preserve the captured glyph whenever the animated UV displacement lands on a
        // transparent texel. Without this base-mask fallback the particle pass still runs,
        // leaving only floating flame particles while the text itself disappears.
        if (source.a > 0.0) {
            gl_FragColor = vec4(fireColor(uv), source.a);
            return;
        }
        vec2 warpedUv = flameUv(uv);
        vec4 warpedSource = texture2D(u_texture, warpedUv);
        if (warpedSource.a > 0.0) {
            gl_FragColor = vec4(fireColor(uv), warpedSource.a);
            return;
        }
        if (textAt(uv, vec2(0.0, 1.0)) || textAt(uv, vec2(1.0, 0.0)) ||
            textAt(uv, vec2(0.0, -1.0)) || textAt(uv, vec2(-1.0, 0.0))) {
            gl_FragColor = vec4(fireColor(uv), 1.0);
            return;
        }
        discard;
    }

    if (source.a > 0.0) {
        gl_FragColor = vec4(u_textColor.rgb, u_textColor.a * source.a);
        return;
    }
    if (textAt(uv, vec2(0.0, 1.0)) || textAt(uv, vec2(1.0, 0.0)) ||
        textAt(uv, vec2(0.0, -1.0)) || textAt(uv, vec2(-1.0, 0.0))) {
        if (u_outlineColor.a > 0.0) {
            gl_FragColor = u_outlineColor;
            return;
        }
    }
    vec2 center = (u_effectBounds.xy + u_effectBounds.zw) * 0.5;
    vec2 halfSize = max((u_effectBounds.zw - u_effectBounds.xy) * 0.5, vec2(1.0));
    float glowAlpha = clamp(1.0 - length((pixel - center) / halfSize), 0.0, 1.0);
    if (u_glowColor.a > 0.0 && glowAlpha > 0.0) {
        gl_FragColor = vec4(u_glowColor.rgb, min(0.8, u_glowColor.a * glowAlpha));
        return;
    }
    discard;
}
