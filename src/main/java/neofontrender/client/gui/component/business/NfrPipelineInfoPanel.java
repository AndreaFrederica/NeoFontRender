package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import neofontrender.core.font.support.FontRenderTuning;

import java.util.Locale;
import java.util.function.Supplier;

import static neofontrender.core.util.ConfigValueParser.parseFloat;

/** Live rendering-pipeline diagnostics panel for the advanced settings page. */
public final class NfrPipelineInfoPanel extends ParentWidget<NfrPipelineInfoPanel> implements ILayoutWidget {
    private final Supplier<Snapshot> snapshot;

    public NfrPipelineInfoPanel(Supplier<Snapshot> snapshot) {
        this.snapshot = snapshot;
    }

    public int preferredHeight() {
        return Math.max(18, Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 6) * 7 + 16;
    }

    @Override
    public boolean layoutWidgets() {
        return true;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        Platform.setupDrawFont();
        Snapshot state = snapshot.get();
        FontRenderTuning.updateFromCurrentGlState();
        float configured = parseFloat(state.oversample, 8.0F, 1.0F, 16.0F);
        float effective = FontRenderTuning.rasterScale(configured);
        float guiScale = FontRenderTuning.currentGuiScale();
        Gui.drawRect(4, 4, Math.max(4, getArea().w() - 4), Math.max(4, getArea().h() - 4), 0x66000000);

        Minecraft minecraft = Minecraft.getMinecraft();
        int line = Math.max(18, minecraft.fontRenderer.FONT_HEIGHT + 6);
        draw(minecraft, tr("neofontrender.gui.option.engine") + ": " + state.engineName, 8, 8, 0xFFFFFF);
        draw(minecraft, String.format(Locale.ROOT, "%s: %.1fx  %s: %.2fx",
                tr("neofontrender.gui.label.oversample"), configured,
                tr("neofontrender.gui.label.effective"), effective), 8, 8 + line, 0xD8D8D8);
        draw(minecraft, String.format(Locale.ROOT, "%s: %.2fx  %s: %s",
                tr("neofontrender.gui.label.gui_scale"), guiScale,
                tr("neofontrender.gui.label.filter"),
                FontRenderTuning.useLinearFiltering(effective)
                        ? tr("neofontrender.gui.filter.linear") : tr("neofontrender.gui.filter.nearest")),
                8, 8 + line * 2, 0xD8D8D8);
        draw(minecraft, flags(
                flag("neofontrender.gui.option.pipeline", state.pipeline),
                flag("neofontrender.gui.option.shader", state.shader),
                flag("neofontrender.gui.option.edge_bleed", state.edgeBleed)), 8, 8 + line * 3, 0xD8D8D8);
        draw(minecraft, flags(
                flag("neofontrender.gui.option.autoscale", state.autoScale),
                flag("neofontrender.gui.option.linear", state.linear),
                flag("neofontrender.gui.option.mipmap", state.mipmap)), 8, 8 + line * 4, 0xD8D8D8);
        draw(minecraft, flags(
                flag("neofontrender.gui.option.integer_scale", state.integerScale),
                flag("neofontrender.gui.option.high_mag", state.highMagnification),
                flag("neofontrender.gui.option.anisotropic", state.anisotropic)), 8, 8 + line * 5, 0xD8D8D8);
        draw(minecraft, flags(
                flag("neofontrender.gui.option.gpu_offscreen", state.gpuOffscreen),
                flag("neofontrender.gui.option.gpu_cpu_submit", state.cpuSubmit),
                flag("neofontrender.gui.option.debug_stats", state.debugStats)), 8, 8 + line * 6, 0xD8D8D8);
    }

    private static void draw(Minecraft minecraft, String text, int x, int y, int color) {
        minecraft.fontRenderer.drawString(text, x, y, color);
    }

    private static String flag(String key, boolean value) {
        return tr(key) + ": " + onOff(value);
    }

    private static String flags(String first, String second, String third) {
        return first + "  " + second + "  " + third;
    }

    private static String tr(String key) {
        return I18n.format(key);
    }

    private static String onOff(boolean value) {
        return tr(value ? "neofontrender.gui.on" : "neofontrender.gui.off");
    }

    /** Immutable data consumed by this panel's renderer. */
    public static final class Snapshot {
        public final String engineName;
        public final String oversample;
        public final boolean pipeline;
        public final boolean shader;
        public final boolean edgeBleed;
        public final boolean autoScale;
        public final boolean linear;
        public final boolean mipmap;
        public final boolean integerScale;
        public final boolean highMagnification;
        public final boolean anisotropic;
        public final boolean gpuOffscreen;
        public final boolean cpuSubmit;
        public final boolean debugStats;

        public Snapshot(String engineName, String oversample, boolean pipeline, boolean shader,
                        boolean edgeBleed, boolean autoScale, boolean linear, boolean mipmap,
                        boolean integerScale, boolean highMagnification, boolean anisotropic,
                        boolean gpuOffscreen, boolean cpuSubmit, boolean debugStats) {
            this.engineName = engineName;
            this.oversample = oversample;
            this.pipeline = pipeline;
            this.shader = shader;
            this.edgeBleed = edgeBleed;
            this.autoScale = autoScale;
            this.linear = linear;
            this.mipmap = mipmap;
            this.integerScale = integerScale;
            this.highMagnification = highMagnification;
            this.anisotropic = anisotropic;
            this.gpuOffscreen = gpuOffscreen;
            this.cpuSubmit = cpuSubmit;
            this.debugStats = debugStats;
        }
    }
}
