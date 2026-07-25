#version 110

uniform sampler2D uTexture;
uniform vec2 uTexelStep;
uniform vec2 uSourceTexel;
uniform vec2 uUvOrigin;
uniform vec2 uUvExtent;
uniform float uPassMode;

uniform float uWeight0;
uniform float uWeight1;
uniform float uWeight2;
uniform float uWeight3;
uniform float uWeight4;
uniform float uWeight5;
uniform float uWeight6;
uniform float uWeight7;
uniform float uWeight8;
uniform float uWeight9;
uniform float uWeight10;
uniform float uWeight11;
uniform float uOffset0;
uniform float uOffset1;
uniform float uOffset2;
uniform float uOffset3;
uniform float uOffset4;
uniform float uOffset5;
uniform float uOffset6;
uniform float uOffset7;
uniform float uOffset8;
uniform float uOffset9;
uniform float uOffset10;

varying vec2 fTexCoord;

float srgbChannelToLinear(float value) {
    return value <= 0.04045
        ? value / 12.92
        : pow((value + 0.055) / 1.055, 2.4);
}

vec3 srgbToLinear(vec3 color) {
    return vec3(srgbChannelToLinear(color.r),
            srgbChannelToLinear(color.g), srgbChannelToLinear(color.b));
}

float linearChannelToSrgb(float value) {
    value = max(value, 0.0);
    return value <= 0.0031308
        ? value * 12.92
        : 1.055 * pow(value, 1.0 / 2.4) - 0.055;
}

vec3 linearToSrgb(vec3 color) {
    return vec3(linearChannelToSrgb(color.r),
            linearChannelToSrgb(color.g), linearChannelToSrgb(color.b));
}

vec3 gaussian(vec2 coord) {
    vec3 color = texture2D(uTexture, coord).rgb * uWeight0;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset0).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset0).rgb) * uWeight1;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset1).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset1).rgb) * uWeight2;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset2).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset2).rgb) * uWeight3;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset3).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset3).rgb) * uWeight4;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset4).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset4).rgb) * uWeight5;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset5).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset5).rgb) * uWeight6;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset6).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset6).rgb) * uWeight7;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset7).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset7).rgb) * uWeight8;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset8).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset8).rgb) * uWeight9;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset9).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset9).rgb) * uWeight10;
    color += (texture2D(uTexture, coord + uTexelStep * uOffset10).rgb
            + texture2D(uTexture, coord - uTexelStep * uOffset10).rgb) * uWeight11;
    return color;
}

void main() {
    vec2 sampleCoord = uUvOrigin + fTexCoord * uUvExtent;
    vec3 color;
    if (uPassMode < 0.5) {
        // Fetch the complete 2x2 source footprint and decode before averaging. Letting hardware
        // minification interpolate encoded sRGB values would alias and darken sharp transitions.
        vec2 halfTexel = uSourceTexel * 0.5;
        color = srgbToLinear(texture2D(uTexture, sampleCoord + vec2(-halfTexel.x, -halfTexel.y)).rgb);
        color += srgbToLinear(texture2D(uTexture, sampleCoord + vec2(halfTexel.x, -halfTexel.y)).rgb);
        color += srgbToLinear(texture2D(uTexture, sampleCoord + vec2(-halfTexel.x, halfTexel.y)).rgb);
        color += srgbToLinear(texture2D(uTexture, sampleCoord + vec2(halfTexel.x, halfTexel.y)).rgb);
        color *= 0.25;
    } else {
        color = gaussian(sampleCoord);
    }

    if (uPassMode > 1.5) {
        // Dark mode only. Material operations remain in linear light until the final output.
        float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
        color = mix(vec3(luminance), color, 0.90);
        color = color / (vec3(1.0) + color * 0.65);
        color = mix(color, srgbToLinear(vec3(0.032, 0.036, 0.048)), 0.92);
        color *= 0.80;
        color = linearToSrgb(color);
    }
    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
