#version 110
// Ported from ModernUI-MC's rendertype_modern_tooltip shader.
// Copyright (C) 2024 BloCamLimb. LGPL-3.0-or-later.

uniform vec2 uCenter;
varying vec2 fPosition;

void main() {
    fPosition = gl_Vertex.xy - uCenter;
    gl_Position = ftransform();
}
