/*
 * Tooltip SDF rendering path derived from ModernUI-MC.
 * Copyright (C) 2024 BloCamLimb.
 * Licensed under LGPL-3.0-or-later.
 */
package neofontrender.addons.tooltips;

import icyllis.arc3d.core.Color;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Single-quad analytic rounded rectangle, border and shadow renderer. */
final class ModernUiTooltipShader {
    private static final String ROOT = "/assets/neofontrender_ui_enhancements/shaders/";
    private static int program;
    private static boolean unavailable;

    private ModernUiTooltipShader() {}

    static boolean draw(float left, float top, float right, float bottom, float radius,
                        int[] fill, int[] border, boolean spectrum, boolean mica) {
        int shader = getOrCreateProgram();
        if (shader == 0) return false;

        float centerX = (left + right) * 0.5F;
        float centerY = (top + bottom) * 0.5F;
        float halfWidth = (right - left) * 0.5F;
        float halfHeight = (bottom - top) * 0.5F;
        float shadowRadius = Math.max(0.001F, TooltipConfig.shadowRadius);
        float extent = TooltipConfig.borderWidth * 0.5F + 1.0F + shadowRadius * 1.2F
                + Math.max(Math.abs(TooltipConfig.shadowOffsetX), Math.abs(TooltipConfig.shadowOffsetY));
        int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        try {
            GL20.glUseProgram(shader);
            uniform2(shader, "uCenter", centerX, centerY);
            uniform2(shader, "uSize", halfWidth, halfHeight);
            uniform2(shader, "uShadowOffset", TooltipConfig.shadowOffsetX, TooltipConfig.shadowOffsetY);
            uniform1(shader, "uRadius", Math.max(0.01F, radius));
            uniform1(shader, "uThickness", Math.max(0.01F, TooltipConfig.borderWidth * 0.5F));
            uniform1(shader, "uShadowAlpha", TooltipConfig.shadowRadius <= 0.0F
                    ? 0.0F : TooltipConfig.shadowAlpha / 255.0F);
            uniform1(shader, "uShadowSpread", 1.25F / shadowRadius);
            uniform1(shader, "uAaScale", Math.max(0.05F, TooltipConfig.antialiasWidth / 0.55F));
            float cycle = Math.max(250, TooltipConfig.borderCycleMillis) * 4.0F;
            float spectrumOffset = spectrum ? 1.0F + (System.currentTimeMillis() % (long) cycle) / cycle : 0.0F;
            uniform1(shader, "uSpectrumOffset", spectrumOffset);
            uniform1(shader, "uSpectrumAlpha", Color.alpha(border[0]) / 255.0F);
            uniform1(shader, "uMaterialMode", mica ? 1.0F : 0.0F);
            for (int i = 0; i < 4; i++) {
                uniformColor(shader, "uFill" + i, fill[i]);
                uniformColor(shader, "uBorder" + i, border[i]);
            }
            int shadow = TooltipConfig.shadowColor;
            int location = GL20.glGetUniformLocation(shader, "uShadowColor");
            GL20.glUniform3f(location, Color.red(shadow) / 255.0F,
                    Color.green(shadow) / 255.0F, Color.blue(shadow) / 255.0F);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            buffer.pos(left - extent, top - extent, 300).endVertex();
            buffer.pos(left - extent, bottom + extent, 300).endVertex();
            buffer.pos(right + extent, bottom + extent, 300).endVertex();
            buffer.pos(right + extent, top - extent, 300).endVertex();
            tessellator.draw();
            return true;
        } catch (Throwable t) {
            unavailable = true;
            TooltipModule.LOGGER.warn("ModernUI tooltip shader failed; using geometry fallback", t);
            return false;
        } finally {
            GL20.glUseProgram(previous);
        }
    }

    private static int getOrCreateProgram() {
        if (unavailable) return 0;
        if (program != 0) return program;
        try {
            int vertex = compile(GL20.GL_VERTEX_SHADER, read("modern_tooltip.vsh"));
            int fragment = compile(GL20.GL_FRAGMENT_SHADER, read("modern_tooltip.fsh"));
            int created = GL20.glCreateProgram();
            GL20.glAttachShader(created, vertex);
            GL20.glAttachShader(created, fragment);
            GL20.glLinkProgram(created);
            if (GL20.glGetProgrami(created, GL20.GL_LINK_STATUS) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(created, 8192));
            }
            GL20.glDetachShader(created, vertex);
            GL20.glDetachShader(created, fragment);
            GL20.glDeleteShader(vertex);
            GL20.glDeleteShader(fragment);
            program = created;
            return created;
        } catch (Throwable t) {
            unavailable = true;
            TooltipModule.LOGGER.warn("ModernUI tooltip shader is unavailable", t);
            return 0;
        }
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            String log = GL20.glGetShaderInfoLog(shader, 8192);
            GL20.glDeleteShader(shader);
            throw new IllegalStateException(log);
        }
        return shader;
    }

    private static String read(String name) throws IOException {
        try (InputStream stream = ModernUiTooltipShader.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new IOException("Missing shader " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void uniform1(int shader, String name, float value) {
        GL20.glUniform1f(GL20.glGetUniformLocation(shader, name), value);
    }

    private static void uniform2(int shader, String name, float x, float y) {
        GL20.glUniform2f(GL20.glGetUniformLocation(shader, name), x, y);
    }

    private static void uniformColor(int shader, String name, int color) {
        GL20.glUniform4f(GL20.glGetUniformLocation(shader, name),
                Color.red(color) / 255.0F, Color.green(color) / 255.0F,
                Color.blue(color) / 255.0F, Color.alpha(color) / 255.0F);
    }
}
