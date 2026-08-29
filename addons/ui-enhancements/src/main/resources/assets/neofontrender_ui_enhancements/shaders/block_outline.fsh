#version 110

uniform vec4 uColor;
uniform vec2 uStart;
uniform vec2 uDirection;
uniform float uLength;
uniform float uHalfWidth;
uniform float uFeather;
uniform float uPattern;
uniform float uRoundCaps;
uniform float uDashLength;
uniform float uDashGap;

float segmentDistance(vec2 point, float start, float end, float roundCaps) {
    if (end <= 0.0 || start >= uLength || end <= start) return 100000.0;
    start = max(0.0, start);
    end = min(uLength, end);
    if (roundCaps > 0.5) {
        vec2 nearest = vec2(clamp(point.x, start, end), 0.0);
        return length(point - nearest) - uHalfWidth;
    }
    return max(abs(point.y) - uHalfWidth,
            max(start - uHalfWidth - point.x, point.x - end - uHalfWidth));
}

float solidDistance(vec2 point) {
    return segmentDistance(point, 0.0, uLength, uRoundCaps);
}

float dashedDistance(vec2 point) {
    float period = max(1.0, uDashLength + uDashGap);
    float index = floor(point.x / period);
    float d0 = segmentDistance(point, (index - 1.0) * period,
            (index - 1.0) * period + uDashLength, uRoundCaps);
    float d1 = segmentDistance(point, index * period,
            index * period + uDashLength, uRoundCaps);
    float d2 = segmentDistance(point, (index + 1.0) * period,
            (index + 1.0) * period + uDashLength, uRoundCaps);
    return min(d0, min(d1, d2));
}

float dottedDistance(vec2 point) {
    float diameter = max(1.0, uDashLength);
    float radius = max(0.5, min(uHalfWidth, diameter * 0.5));
    float period = max(1.0, diameter + uDashGap);
    float index = floor(point.x / period);
    float center0 = index * period;
    float center1 = center0 + period;
    float d0 = center0 < 0.0 || center0 > uLength
            ? 100000.0 : length(point - vec2(center0, 0.0)) - radius;
    float d1 = center1 < 0.0 || center1 > uLength
            ? 100000.0 : length(point - vec2(center1, 0.0)) - radius;
    return min(d0, d1);
}

void main() {
    vec2 relative = gl_FragCoord.xy - uStart;
    vec2 fLinePosition = vec2(dot(relative, uDirection),
            dot(relative, vec2(-uDirection.y, uDirection.x)));
    float distance;
    if (uPattern > 1.5) distance = dottedDistance(fLinePosition);
    else if (uPattern > 0.5) distance = dashedDistance(fLinePosition);
    else distance = solidDistance(fLinePosition);

    float coverage;
    if (uFeather > 0.0) {
        float aa = max(uFeather, fwidth(distance) * 0.75);
        coverage = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance);
    } else {
        coverage = distance <= 0.0 ? 1.0 : 0.0;
    }
    float alpha = uColor.a * coverage;
    if (alpha <= 0.001) discard;
    gl_FragColor = vec4(uColor.rgb, alpha);
}
