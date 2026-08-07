package neofontrender.addons.electricelytra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.api.flight.FlightHudCanvas;
import neofontrender.addons.api.flight.FlightHudRenderContext;
import neofontrender.addons.api.flight.FlightHudRenderEvent;
import neofontrender.addons.api.flight.FlightTelemetry;
import neofontrender.addons.api.flight.FlightTelemetryEvent;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightRenderPose;
import neofontrender.addons.electricelytra.ElectricElytraConfig;
import neofontrender.addons.electricelytra.ElectricWarningLogic;
import neofontrender.addons.electricelytra.ElectricFlightMath;
import neofontrender.addons.electricelytra.ItemElectricElytra;

import java.util.Locale;
import java.awt.Rectangle;

public enum ElectricElytraHud {
    INSTANCE;

    private int smoothingEntityId = Integer.MIN_VALUE;
    private long lastSampleNanos;
    private double smoothedSpeed;
    private double smoothedGroundSpeed;
    private Rectangle panelBounds = new Rectangle();
    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    @SubscribeEvent
    public void onTelemetry(FlightTelemetryEvent event) {
        EntityPlayerSP player = event.getPlayer();
        ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (!ItemElectricElytra.isElectricElytra(stack)
                || ElectricElytraConfig.hudSpeedSmoothingSeconds <= 0.0D) {
            resetSmoothing();
            return;
        }
        FlightTelemetry raw = event.getTelemetry();
        long now = System.nanoTime();
        double elapsed = lastSampleNanos == 0L ? 0.0D
                : Math.min(0.25D, Math.max(0.0D, (now - lastSampleNanos) / 1_000_000_000.0D));
        boolean reset = smoothingEntityId != player.getEntityId() || lastSampleNanos == 0L
                || now - lastSampleNanos > 1_000_000_000L
                || Math.abs(raw.getSpeedBlocksPerSecond() - smoothedSpeed)
                > ElectricElytraConfig.hardSpeedLimitBlocksPerSecond * 0.55D;
        if (reset) {
            smoothingEntityId = player.getEntityId();
            smoothedSpeed = raw.getSpeedBlocksPerSecond();
            smoothedGroundSpeed = raw.getGroundSpeedBlocksPerSecond();
        } else {
            double alpha = 1.0D - Math.exp(-elapsed
                    / ElectricElytraConfig.hudSpeedSmoothingSeconds);
            smoothedSpeed += (raw.getSpeedBlocksPerSecond() - smoothedSpeed) * alpha;
            smoothedGroundSpeed += (raw.getGroundSpeedBlocksPerSecond()
                    - smoothedGroundSpeed) * alpha;
        }
        lastSampleNanos = now;
        // The lower-speed marker is an aerodynamic-model reference.  Do not inject that
        // reference into the vanilla clone, whose complete glide envelope remains Minecraft's.
        double stallSpeed = ItemElectricElytra.usesAerodynamicFlightModel(stack)
                ? ElectricFlightMath.stallSpeedBlocksPerSecond(
                ItemElectricElytra.getFlapSetting(stack))
                : raw.getLowerSpeedReferenceBlocksPerSecond();
        event.setTelemetry(new FlightTelemetry(smoothedSpeed, smoothedGroundSpeed,
                raw.getAltitudeBlocks(), raw.getVerticalBlocksPerSecond(),
                raw.getAccelerationBlocksPerSecondSquared(),
                stallSpeed, raw.getHeadingDegrees(),
                raw.getFlightPathAngleDegrees(), raw.getDriftAngleDegrees()));
    }

    @SubscribeEvent
    public void onFlightHudPost(FlightHudRenderEvent.Post event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (!ItemElectricElytra.isElectricElytra(stack)) return;
        render(event.getContext(), player, stack);
    }

    private static void render(FlightHudRenderContext context, EntityPlayerSP player,
                               ItemStack stack) {
        FlightHudCanvas canvas = context.getCanvas();
        float unit = context.getCanvasScale();
        boolean airbusLayout = context.getThemeId().startsWith("airbus-");
        float panelUnit = unit * (airbusLayout ? 0.82F : 1.0F);
        float panelWidth = 126.0F * panelUnit;
        float panelHeight = 74.0F * panelUnit;
        float controlsGap = 5.0F * panelUnit;
        float controlsHeight = 22.0F * panelUnit;
        float groupHeight = panelHeight + controlsGap + controlsHeight;
        ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
        float left;
        float top;
        if (ElectricElytraConfig.hudPositionX >= 0.0D
                && ElectricElytraConfig.hudPositionY >= 0.0D) {
            left = (float) (ElectricElytraConfig.hudPositionX
                    * Math.max(0.0F, resolution.getScaledWidth() - panelWidth));
            top = (float) (ElectricElytraConfig.hudPositionY
                    * Math.max(0.0F, resolution.getScaledHeight() - groupHeight));
        } else {
            left = airbusLayout ? context.screenX(5.0F)
                    : context.screenX(context.getCanvasWidth() - 139.0F);
            top = airbusLayout ? context.screenY(5.0F)
                    : context.screenY(context.getCanvasHeight() - 113.0F);
        }
        float right = left + panelWidth;
        float bottom = top + panelHeight;
        int primary = context.getColor("primary", 0xFF8EFFAE);
        int safe = context.getColor("safe", 0xFF69F58A);
        int warning = context.getColor("warning", 0xFFFFB84A);
        int selected = context.getColor("selected", 0xFFFF62D6);
        int halo = context.getColor("halo", 0xD0000000);
        int panel = context.getColor("panel", 0x92000810);
        int border = context.getColor("panelBorder", primary);
        float line = Math.max(0.75F, context.getLineWidth() * unit);
        float text = Math.max(0.44F, context.getTextScale() * panelUnit);

        canvas.fill(left, top, right, bottom, panel);
        canvas.outline(left, top, right, bottom, border, line);

        boolean enabled = ItemElectricElytra.isEngineEnabled(stack);
        int power = ItemElectricElytra.getEnginePower(stack);
        int throttle = ItemElectricElytra.getThrottle(stack);
        IEnergyStorage storage = ItemElectricElytra.getEnergy(stack);
        int energy = storage == null ? 0 : storage.getEnergyStored();
        int capacity = storage == null ? ElectricElytraConfig.energyCapacity : storage.getMaxEnergyStored();
        double fuel = capacity <= 0 ? 0.0D : energy / (double) capacity;
        boolean infiniteFuel = ItemElectricElytra.hasInfiniteEnergy(stack);
        int engineColor = !enabled ? warning : power >= 95 ? selected : safe;

        float gaugeX = left + 37.0F * panelUnit;
        float gaugeY = top + 38.0F * panelUnit;
        float radius = 23.0F * panelUnit;
        canvas.arc(gaugeX, gaugeY, radius, -135.0D, 135.0D, primary, line, 36);
        if (power > 0) {
            canvas.arc(gaugeX, gaugeY, radius, -135.0D,
                    -135.0D + 270.0D * power / 100.0D, engineColor, line * 2.2F, 36);
        }
        for (int i = 0; i <= 4; i++) {
            double angle = Math.toRadians(-135.0D + i * 67.5D);
            float x1 = gaugeX + (float) Math.cos(angle) * radius * 0.82F;
            float y1 = gaugeY + (float) Math.sin(angle) * radius * 0.82F;
            float x2 = gaugeX + (float) Math.cos(angle) * radius;
            float y2 = gaugeY + (float) Math.sin(angle) * radius;
            canvas.line(x1, y1, x2, y2, primary, line);
        }
        canvas.centeredText("ENG 1", gaugeX, top + 6.0F * panelUnit, text * 0.82F, primary, halo);
        canvas.centeredText(String.format(Locale.ROOT, "%d%%", power), gaugeX,
                gaugeY - 5.0F * panelUnit, text * 1.12F, engineColor, halo);
        canvas.centeredText(enabled ? "N1" : "OFF", gaugeX,
                gaugeY + 8.0F * panelUnit, text * 0.72F, engineColor, halo);
        canvas.centeredText("T" + throttle, gaugeX,
                gaugeY + 17.0F * panelUnit, text * 0.60F, primary, halo);

        float fuelLeft = left + 78.0F * panelUnit;
        float fuelTop = top + 15.0F * panelUnit;
        float fuelRight = fuelLeft + 18.0F * panelUnit;
        float fuelBottom = fuelTop + 43.0F * panelUnit;
        canvas.outline(fuelLeft, fuelTop, fuelRight, fuelBottom, primary, line);
        float filledTop = fuelBottom - (float) fuel * (fuelBottom - fuelTop);
        int fuelColor = fuel < 0.15D ? warning : safe;
        canvas.fill(fuelLeft + line, filledTop, fuelRight - line, fuelBottom - line, fuelColor);
        canvas.centeredText("FUEL", (fuelLeft + fuelRight) * 0.5F,
                top + 6.0F * panelUnit, text * 0.70F, primary, halo);
        canvas.centeredText(infiniteFuel ? "∞" : String.format(Locale.ROOT,
                        "%d%%", Math.round(fuel * 100.0D)),
                (fuelLeft + fuelRight) * 0.5F, fuelBottom + 3.0F * panelUnit,
                text * 0.72F, fuelColor, halo);

        int cruiseDrain = Math.max(ElectricElytraConfig.idleEnergyPerTick,
                ElectricElytraConfig.cruiseEnergyPerTick);
        int drain = power > 0 ? ElectricElytraConfig.idleEnergyPerTick
                + (int) Math.round((cruiseDrain
                        - ElectricElytraConfig.idleEnergyPerTick) * throttle / 100.0D)
                : Math.max(1, ElectricElytraConfig.idleEnergyPerTick);
        drain = Math.max(1, drain);
        long seconds = drain <= 0 ? 0L : energy / (long) drain / 20L;
        canvas.text("END", left + 102.0F * panelUnit, top + 18.0F * panelUnit,
                text * 0.62F, primary, halo);
        canvas.text(infiniteFuel ? "∞" : String.format(Locale.ROOT,
                        "%02d:%02d", seconds / 60L, seconds % 60L),
                left + 101.0F * panelUnit, top + 31.0F * panelUnit, text * 0.66F, fuelColor, halo);
        canvas.text(infiniteFuel ? "INF" : fuel < 0.15D ? "LOW" : "FE",
                left + 101.0F * panelUnit,
                top + 47.0F * panelUnit, text * 0.64F, fuelColor, halo);

        float controlsTop = top + panelHeight + controlsGap;
        renderFlightControls(context, player, stack, left, controlsTop,
                panelWidth, panelUnit);
        INSTANCE.panelBounds = enclosingBounds(left, top, panelWidth, groupHeight);
        if (Minecraft.getMinecraft().currentScreen instanceof ElectricElytraHudEditor) {
            canvas.outline(left - 2.0F, top - 2.0F,
                    left + panelWidth + 2.0F, top + groupHeight + 2.0F,
                    selected, Math.max(1.0F, line * 1.5F));
        }
        renderGroundWarning(context, ElectricGroundWarning.INSTANCE.sample(player, stack));
    }

    Rectangle panelBounds() {
        return new Rectangle(panelBounds);
    }

    void beginDrag(int mouseX, int mouseY) {
        if (!panelBounds.contains(mouseX, mouseY)) return;
        dragging = true;
        dragOffsetX = mouseX - panelBounds.x;
        dragOffsetY = mouseY - panelBounds.y;
    }

    void dragTo(int mouseX, int mouseY) {
        if (!dragging) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int availableX = Math.max(0, resolution.getScaledWidth() - panelBounds.width);
        int availableY = Math.max(0, resolution.getScaledHeight() - panelBounds.height);
        int left = Math.max(0, Math.min(availableX, mouseX - dragOffsetX));
        int top = Math.max(0, Math.min(availableY, mouseY - dragOffsetY));
        ElectricElytraConfig.hudPositionX = availableX == 0 ? 0.0D : left / (double) availableX;
        ElectricElytraConfig.hudPositionY = availableY == 0 ? 0.0D : top / (double) availableY;
    }

    void endDrag() {
        if (!dragging) return;
        dragging = false;
        ElectricElytraConfig.saveHudPosition(ElectricElytraConfig.hudPositionX,
                ElectricElytraConfig.hudPositionY);
    }

    private static Rectangle enclosingBounds(float left, float top,
                                             float width, float height) {
        int x = (int) Math.floor(left);
        int y = (int) Math.floor(top);
        return new Rectangle(x, y, Math.max(1, (int) Math.ceil(left + width) - x),
                Math.max(1, (int) Math.ceil(top + height) - y));
    }

    private static void renderFlightControls(FlightHudRenderContext context,
                                             EntityPlayerSP player, ItemStack stack,
                                             float left, float top, float width,
                                             float layoutUnit) {
        FlightHudCanvas canvas = context.getCanvas();
        float unit = context.getCanvasScale();
        float height = 22.0F * layoutUnit;
        float line = Math.max(0.75F, context.getLineWidth() * unit);
        float text = Math.max(0.42F, context.getTextScale() * layoutUnit);
        int primary = context.getColor("primary", 0xFF8EFFAE);
        int selected = context.getColor("selected", 0xFFFF62D6);
        int panel = context.getColor("panel", 0x92000810);
        int halo = context.getColor("halo", 0xD0000000);
        canvas.fill(left, top, left + width, top + height, panel);
        canvas.outline(left, top, left + width, top + height, primary, line);

        if (ItemElectricElytra.usesVanillaFlightModel(stack)) {
            canvas.text("MODE VANILLA", left + 4.0F * layoutUnit,
                    top + 4.0F * layoutUnit, text * 0.58F, selected, halo);
            canvas.text("MC ELYTRA PHYSICS", left + 4.0F * layoutUnit,
                    top + 13.0F * layoutUnit, text * 0.52F, primary, halo);
            canvas.text("ENGINE ONLY", left + 75.0F * layoutUnit,
                    top + 9.0F * layoutUnit, text * 0.52F, primary, halo);
            return;
        }

        String sas = !ItemElectricElytra.isSasCapable(stack) ? "SAS N/A"
                : ItemElectricElytra.isSasEnabled(stack) ? "SAS STAB" : "SAS OFF";
        int flapSetting = ItemElectricElytra.getFlapSetting(stack);
        String flaps = !ItemElectricElytra.isFlapCapable(stack) ? "FLAPS N/A"
                : flapSetting == 0 ? "FLAPS UP" : flapSetting == 1
                ? "FLAPS TO" : "FLAPS LDG";
        FlightRenderPose renderPose = FlightApi.getRenderPose(player, context.getPartialTicks());
        double bank = renderPose == null ? 0.0D
                : renderPose.getCameraAngles().rollDegrees;
        canvas.text(sas, left + 4.0F * layoutUnit, top + 4.0F * layoutUnit,
                text * 0.58F, ItemElectricElytra.isSasEnabled(stack) ? selected : primary, halo);
        canvas.text(flaps, left + 4.0F * layoutUnit, top + 13.0F * layoutUnit,
                text * 0.54F, flapSetting > 0 ? selected : primary, halo);
        canvas.text(String.format(Locale.ROOT, "BANK %+03.0f", bank),
                left + 75.0F * layoutUnit, top + 8.0F * layoutUnit,
                text * 0.55F, primary, halo);
        canvas.text(String.format(Locale.ROOT, "VS %.1f",
                        ElectricFlightMath.stallSpeedBlocksPerSecond(flapSetting)),
                left + 78.0F * layoutUnit, top + 16.0F * layoutUnit,
                text * 0.48F, flapSetting > 0 ? selected : primary, halo);
    }

    private static void renderGroundWarning(FlightHudRenderContext context,
                                            ElectricGroundWarning.Sample sample) {
        if (!ElectricElytraConfig.warningEnabled) return;
        FlightHudCanvas canvas = context.getCanvas();
        float unit = context.getCanvasScale();
        float centerX = context.screenX(context.getCanvasWidth() * 0.5F);
        float top = context.screenY(context.getCanvasHeight() * 0.205F);
        float line = Math.max(0.9F, context.getLineWidth() * unit);
        float text = Math.max(0.52F, context.getTextScale() * unit);
        int halo = context.getColor("halo", 0xE0000000);
        int caution = context.getColor("warning", 0xFFFFB84A);
        int danger = context.getColor("danger", 0xFFFF3E3E);

        ElectricWarningLogic.Warning warning = sample.warning;
        if (warning != ElectricWarningLogic.Warning.NONE) {
            int color = warning.urgent ? danger : caution;
            float width = Math.max(104.0F, 14.0F + warning.message.length() * 8.0F) * unit;
            float height = 22.0F * unit;
            float left = centerX - width * 0.5F;
            canvas.fill(left, top, left + width, top + height, 0xB0000000);
            canvas.outline(left, top, left + width, top + height, color, line * 1.5F);
            if (warning.urgent && (System.currentTimeMillis() / 250L & 1L) == 0L) {
                canvas.outline(left - 2.0F * unit, top - 2.0F * unit,
                        left + width + 2.0F * unit, top + height + 2.0F * unit,
                        color, line);
            }
            canvas.centeredText(warning.message, centerX, top + 6.0F * unit,
                    text * 1.12F, color, halo);
        }

        if (Double.isFinite(sample.radioAltitude) && sample.radioAltitude
                <= ElectricElytraConfig.warningRadioAltitudeMaximum) {
            float radioY = top + (warning == ElectricWarningLogic.Warning.NONE
                    ? 0.0F : 28.0F * unit);
            int radioColor = sample.radioAltitude <= ElectricElytraConfig.warningFlapHeight
                    ? caution : context.getColor("primary", 0xFF8EFFAE);
            canvas.centeredText(String.format(Locale.ROOT, "RA %03d",
                            Math.max(0, Math.round(sample.radioAltitude))),
                    centerX, radioY, text * 0.72F, radioColor, halo);
        }
    }

    private void resetSmoothing() {
        smoothingEntityId = Integer.MIN_VALUE;
        lastSampleNanos = 0L;
    }
}
