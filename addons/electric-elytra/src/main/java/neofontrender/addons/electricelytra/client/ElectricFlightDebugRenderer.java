package neofontrender.addons.electricelytra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.electricelytra.ElectricFlightDebug;
import neofontrender.addons.electricelytra.ItemElectricElytra;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightHudCanvas;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.Locale;
import java.util.function.ToDoubleFunction;

public enum ElectricFlightDebugRenderer {
    INSTANCE;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || !minecraft.gameSettings.showDebugInfo) return;
        ElectricFlightDebug.Sample sample = ElectricFlightDebug.get(minecraft.thePlayer);
        if (sample == null) return;

        float partial = event.partialTicks;
        double x = minecraft.thePlayer.lastTickPosX
                + (minecraft.thePlayer.posX - minecraft.thePlayer.lastTickPosX) * partial
                - RenderManager.instance.viewerPosX;
        double y = minecraft.thePlayer.lastTickPosY
                + (minecraft.thePlayer.posY - minecraft.thePlayer.lastTickPosY) * partial
                - RenderManager.instance.viewerPosY + 1.0D;
        double z = minecraft.thePlayer.lastTickPosZ
                + (minecraft.thePlayer.posZ - minecraft.thePlayer.lastTickPosZ) * partial
                - RenderManager.instance.viewerPosZ;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glLineWidth(2.5F);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        line(tessellator, x, y, z, sample.thrust, 0.25D, 255, 70, 255);
        line(tessellator, x, y, z, sample.lift, 0.25D, 80, 255, 100);
        line(tessellator, x, y, z, sample.drag, 0.25D, 255, 70, 70);
        line(tessellator, x, y, z, sample.side, 0.25D, 60, 230, 255);
        line(tessellator, x, y, z, sample.gravity, 0.25D, 255, 220, 60);
        line(tessellator, x, y, z, sample.bodyAxis, 3.0D, 255, 255, 255);
        tessellator.draw();
        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

    @SubscribeEvent
    public void onDebugOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.ALL
                || minecraft.thePlayer == null || !minecraft.gameSettings.showDebugInfo) return;
        ElectricFlightDebug.Sample[] history = ElectricFlightDebug.history(minecraft.thePlayer);
        if (history.length < 2) return;
        FlightHudCanvas canvas = FlightApi.getHudCanvas();
        if (canvas == null) return;
        ElectricFlightDebug.Sample latest = history[history.length - 1];
        boolean sasCapable = ItemElectricElytra.isSasCapable(
                EntityEquipmentSlot.getChest(minecraft.thePlayer));
        ScaledResolution resolution = new ScaledResolution(minecraft,
                minecraft.displayWidth, minecraft.displayHeight);
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

    private static void line(Tessellator tessellator, double x, double y, double z,
                             Vec3 vector, double scale, int red, int green, int blue) {
        tessellator.setColorRGBA(red, green, blue, 230);
        tessellator.addVertex(x, y, z);
        tessellator.setColorRGBA(red, green, blue, 230);
        tessellator.addVertex(x + vector.xCoord * scale, y + vector.yCoord * scale,
                z + vector.zCoord * scale);
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
