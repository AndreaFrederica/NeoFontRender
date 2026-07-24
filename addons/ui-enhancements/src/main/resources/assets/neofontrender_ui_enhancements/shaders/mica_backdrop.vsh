#version 110

varying vec2 fTexCoord;

void main() {
    fTexCoord = gl_MultiTexCoord0.xy;
    gl_Position = gl_Vertex;
}
