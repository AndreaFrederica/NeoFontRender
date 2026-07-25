#version 110
// Derived from ModernUI-MC's rendertype_modern_tooltip shader.
// Copyright (C) 2024 BloCamLimb. LGPL-3.0-or-later.

uniform vec2 uSize;
uniform vec2 uShadowOffset;
uniform float uRadius;
uniform float uThickness;
uniform float uShadowAlpha;
uniform float uShadowSpread;
uniform float uAaScale;
uniform float uSpectrumOffset;
uniform float uSpectrumAlpha;
uniform float uMaterialMode;
uniform float uBackdropEnabled;
uniform sampler2D uBackdrop;
uniform vec4 uBackdropUv;
uniform vec4 uFill0;
uniform vec4 uFill1;
uniform vec4 uFill2;
uniform vec4 uFill3;
uniform vec4 uBorder0;
uniform vec4 uBorder1;
uniform vec4 uBorder2;
uniform vec4 uBorder3;
uniform vec3 uShadowColor;

varying vec2 fPosition;

float roundedDistance(vec2 pos) {
    vec2 d = abs(pos) - uSize + uRadius;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0) - uRadius;
}

float noise1(float seed1, float seed2) {
    return fract(seed1 + 12.34567 *
        fract(100.0 * (abs(seed1 * 0.91) + seed2 + 94.68) *
        fract((abs(seed2 * 0.41) + 45.46) *
        fract((abs(seed2) + 757.21) * fract(seed1 * 0.0171)))))
        * 1.0038 - 0.00185;
}

vec4 dither(vec4 color) {
    vec2 a = gl_FragCoord.xy;
    vec2 b = floor(a);
    float u = fract(b.x * 0.5 + b.y * b.y * 0.75);
    vec2 c = a * 0.5;
    vec2 d = floor(c);
    float v = fract(d.x * 0.5 + d.y * d.y * 0.75);
    vec2 e = c * 0.5;
    vec2 f = floor(e);
    float w = fract(f.x * 0.5 + f.y * f.y * 0.75);
    float amount = ((w * 0.25 + v) * 0.25 + u) - (63.0 / 128.0);
    return vec4(clamp(color.rgb + amount / 255.0, 0.0, 1.0), color.a);
}

vec4 gradient4(vec4 ul, vec4 ur, vec4 lr, vec4 ll, vec2 t) {
    vec3 top = mix(pow(ul.rgb, vec3(2.2)), pow(ur.rgb, vec3(2.2)), t.x);
    vec3 bottom = mix(pow(ll.rgb, vec3(2.2)), pow(lr.rgb, vec3(2.2)), t.x);
    vec3 rgb = pow(mix(top, bottom, t.y), vec3(1.0 / 2.2));
    float alpha = mix(mix(ul.a, ur.a, t.x), mix(ll.a, lr.a, t.x), t.y);
    return vec4(rgb, alpha);
}

void over(inout vec3 premul, inout float alpha, vec3 rgb, float srcAlpha) {
    premul = rgb * srcAlpha + premul * (1.0 - srcAlpha);
    alpha = srcAlpha + alpha * (1.0 - srcAlpha);
}

void main() {
    float distance = roundedDistance(fPosition);
    float shadowDistance = roundedDistance(fPosition - uShadowOffset);
    float aa = max(fwidth(distance) * max(uAaScale, 0.05), 0.0001);
    float inside = 1.0 - clamp(distance / aa + 0.5, 0.0, 1.0);
    float borderCoverage = 1.0 - clamp((abs(distance) - uThickness) / aa + 0.5, 0.0, 1.0);

    float outside = clamp(shadowDistance - uThickness, 0.0, 1.0 / uShadowSpread);
    float shadow = pow(1.0 - uShadowSpread * outside, 3.0);
    shadow *= uShadowAlpha;
    shadow = max(0.0, shadow + (noise1(gl_FragCoord.x, gl_FragCoord.y) - 1.0) * 0.05 * uShadowAlpha);
    shadow *= 1.0 - inside;

    vec2 t = clamp(0.5 * fPosition / (uSize + uThickness) + 0.5, 0.0, 1.0);
    vec4 fill = gradient4(uFill0, uFill1, uFill2, uFill3, t);
    if (uMaterialMode > 0.5) {
        if (uBackdropEnabled > 0.5) {
            vec2 backdropUv = vec2(mix(uBackdropUv.x, uBackdropUv.z, t.x),
                    mix(uBackdropUv.y, uBackdropUv.w, t.y));
            fill.rgb = texture2D(uBackdrop, backdropUv).rgb;
        } else {
            // Capture is optional at runtime; retain a deterministic dark fallback.
            fill.rgb = vec3(0.055, 0.060, 0.075);
        }
        fill.a = 1.0;
    }
    vec4 border;
    if (uSpectrumOffset > 0.0) {
        float angle = atan(-fPosition.y, -fPosition.x) * 0.1591549430918;
        float hue = mod(angle + uSpectrumOffset, 1.0);
        const vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
        vec3 rgb = clamp(abs(fract(hue + k.xyz) * 6.0 - k.w) - k.x, 0.0, 1.0);
        border = vec4(rgb * vec3(0.9, 0.85, 0.9), uSpectrumAlpha);
    } else {
        border = gradient4(uBorder0, uBorder1, uBorder2, uBorder3, t);
    }

    vec3 premul = uShadowColor * shadow;
    float alpha = shadow;
    over(premul, alpha, fill.rgb, inside * fill.a);
    over(premul, alpha, border.rgb, borderCoverage * border.a);
    if (alpha < 0.002) discard;
    gl_FragColor = dither(vec4(premul / alpha, alpha));
}
