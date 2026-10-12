#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D BlurredSampler;
uniform float Progress;

varying vec2 texCoord;

void main() {
    vec4 scene = texture2D(DiffuseSampler, texCoord);
    vec4 blurred = texture2D(BlurredSampler, texCoord);
    gl_FragColor = mix(scene, blurred, clamp(Progress, 0.0, 1.0));
}
