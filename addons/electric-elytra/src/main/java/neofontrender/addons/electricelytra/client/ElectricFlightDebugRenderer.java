package neofontrender.addons.electricelytra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.electricelytra.ElectricFlightDebug;
import neofontrender.addons.electricelytra.ItemElectricElytra;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightHudCanvas;
import org.lwjgl.opengl.GL11;

import java.util.Locale;
import java.util.function.ToDoubleFunction;

public enum ElectricFlightDebugRenderer {
    INSTANCE;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || !minecraft.gameSettings.showDebugInfo) return;
        ElectricFlightDebug.Sample sample = ElectricFlightDebug.get(minecraft.player);
        if (sample == null) return;

        float partial = event.getPartialTicks();
        double x = minecraft.player.lastTickPosX
                + (minecraft.player.posX - minecraft.player.lastTickPosX) * partial
                - minecraft.getRenderManager().viewerPosX;
        double y = minecraft.player.lastTickPosY
                + (minecraft.player.posY - minecraft.player.lastTickPosY) * partial
                - minecraft.getRenderManager().viewerPosY + 1.0D;
        double z = minecraft.player.lastTickPosZ
                + (minecraft.player.posZ - minecraft.player.lastTickPosZ) * partial
                - minecraft.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GL11.glLineWidth(2.5F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        line(buffer, x, y, z, sample.thrust, 0.25D, 255, 70, 255);
        line(buffer, x, y, z, sample.lift, 0.25D, 80, 255, 100);
        line(buffer, x, y, z, sample.drag, 0.25D, 255, 70, 70);
        line(buffer, x, y, z, sample.side, 0.25D, 60, 230, 255);
        line(buffer, x, y, z, sample.gravity, 0.25D, 255, 220, 60);
        line(buffer, x, y, z, sample.bodyAxis, 3.0D, 255, 255, 255);
        tessellator.draw();
        GL11.glLineWidth(1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onDebugOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL
                || minecraft.player == null || !minecraft.gameSettings.showDebugInfo) return;
        ElectricFlightDebug.Sample[] history = ElectricFlightDebug.history(minecraft.player);
        if (history.length < 2) return;
        FlightHudCanvas canvas = FlightApi.getHudCanvas();
        if (canvas == null) return;
        ElectricFlightDebug.Sample latest = history[history.length - 1];
        boolean sasCapable = ItemElectricElytra.isSasCapable(
                minecraft.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST));
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int width = Math.min(370, Math.max(230, resolution.getScaledWidth() / 3));
        int panelHeight = 58;
        int gap = 4;
        int panels = sasCapable ? 3 : 2;
        int left = 7;
        int top = Math.max(7, resolution.getScaledHeight()
                - panels * panelHeight - (panels - 1) * gap - 7);

        drawGraph(canvas, "AERO FORCE  m/s²", history, left, top, width, panelHeight, 10.0D,
                new Series("T", 0xFFFF46FF, value -> value.thrustAcceleration),
                new Series("L", 0xFF50FF64, value -> value.liftAcceleration),
                new Series("D", 0xFFFF4646, value -> value.dragAcceleration),
                new Series("Y", 0xFF3CE6FF, value -> value.sideAcceleration));
        top += panelHeight + gap;
        drawGraph(canvas, "AIR DATA  deg / deg·s⁻¹", history, left, top, width, panelHeight, 30.0D,
                new Series("AOA", 0xFFFFDC3C,
                        value -> Math.toDegrees(value.angleOfAttackRadians)),
                new Series("BETA", 0xFF3CE6FF,
                        value -> Math.toDegrees(value.sideslipAngleRadians)),
                new Series("YR", 0xFFFFFFFF,
                        value -> Math.toDegrees(value.yawRateRadiansPerSecond)));
        if (sasCapable) {
            top += panelHeight + gap;
            drawGraph(canvas, latest.sasEnabled ? "SAS CONTROL  deg / deg·s⁻²"
                            : "SAS CONTROL  OFF", history, left, top, width, panelHeight, 20.0D,
                    new Series("ERR", 0xFFFFB84A,
                            value -> Math.toDegrees(value.sasYawErrorRadians)),
                    new Series("CMD", 0xFFFF62D6,
                            value -> Math.toDegrees(
                                    value.sasControlAccelerationRadiansPerSecondSquared)),
                    new Series("OUT", 0xFF8EFFAE,
                            value -> Math.toDegrees(
                                    value.yawAccelerationRadiansPerSecondSquared)));
        }
    }

    private static void drawGraph(FlightHudCanvas canvas, String title,
                                  ElectricFlightDebug.Sample[] history,
                                  int left, int top, int width, int height,
                                  double minimumScale, Series... series) {
        canvas.fill(left, top, left + width, top + height, 0xB0000810);
        canvas.outline(left, top, left + width, top + height, 0xCC70F0A0, 1.0F);
        float textScale = 0.68F;
        int halo = 0xD0000000;
        canvas.text(title, left + 4, top + 3, textScale, 0xFFB8FFD0, halo);

        double scale = minimumScale;
        for (ElectricFlightDebug.Sample sample : history) {
            for (Series value : series) scale = Math.max(scale, Math.abs(value.value.applyAsDouble(sample)));
        }
        scale *= 1.08D;
        int chartTop = top + 14;
        int chartBottom = top + height - 4;
        int center = (chartTop + chartBottom) / 2;
        canvas.line(left + 3, center, left + width - 3, center,
                0x7740B070, 1.0F);

        final double graphScale = scale;
        for (Series value : series) {
            float[] points = new float[history.length * 2];
            for (int i = 0; i < history.length; i++) {
                double fraction = history.length <= 1 ? 1.0D : i / (double) (history.length - 1);
                float x = (float) (left + 3.0D + fraction * (width - 6.0D));
                double normalized = Math.max(-1.0D, Math.min(1.0D,
                        value.value.applyAsDouble(history[i]) / graphScale));
                float y = (float) (center - normalized
                        * (chartBottom - chartTop) * 0.5D);
                points[i * 2] = x;
                points[i * 2 + 1] = y;
            }
            canvas.clip(left + 3, chartTop, left + width - 3, chartBottom,
                    () -> canvas.polyline(points, value.color, 1.35F));
        }

        ElectricFlightDebug.Sample latest = history[history.length - 1];
        float textX = left + canvas.textWidth(title, textScale) + 10.0F;
        for (Series value : series) {
            String text = String.format(Locale.ROOT, "%s %+.1f", value.label,
                    value.value.applyAsDouble(latest));
            float textWidth = canvas.textWidth(text, textScale);
            if (textX + textWidth >= left + width - 3) break;
            canvas.text(text, textX, top + 3, textScale, value.color, halo);
            textX += textWidth + 6.0F;
        }
    }

    private static void line(BufferBuilder buffer, double x, double y, double z,
                             Vec3d vector, double scale, int red, int green, int blue) {
        buffer.pos(x, y, z).color(red, green, blue, 230).endVertex();
        buffer.pos(x + vector.x * scale, y + vector.y * scale, z + vector.z * scale)
                .color(red, green, blue, 230).endVertex();
    }

    private static final class Series {
        final String label;
        final int color;
        final ToDoubleFunction<ElectricFlightDebug.Sample> value;

        Series(String label, int color,
               ToDoubleFunction<ElectricFlightDebug.Sample> value) {
            this.label = label;
            this.color = color;
            this.value = value;
        }
    }
}
