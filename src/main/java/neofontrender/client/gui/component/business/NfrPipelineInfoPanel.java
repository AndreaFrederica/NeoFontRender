package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import neofontrender.core.font.support.FontRenderTuning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static neofontrender.core.util.ConfigValueParser.parseFloat;

/** Live rendering-pipeline diagnostics panel for the advanced settings page. */
public final class NfrPipelineInfoPanel extends ParentWidget<NfrPipelineInfoPanel>
        implements ILayoutWidget, Interactable {
    private final Supplier<Snapshot> snapshot;
    private int panX;
    private int panY;
    private int dragX;
    private int dragY;
    private boolean dragging;

    public NfrPipelineInfoPanel(Supplier<Snapshot> snapshot) { this.snapshot = snapshot; }

    public int preferredHeight() {
        // Eight flow rows plus a separate diagnostics block. Keep enough room for the last
        // connector row; the previous value let the diagnostics text overlap the framebuffer row.
        return 390;
    }

    @Override public boolean layoutWidgets() { return true; }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        Platform.setupDrawFont();
        Snapshot state = snapshot.get();
        float configured = parseFloat(state.oversample, 8.0F, 1.0F, 16.0F);
        float effective = FontRenderTuning.rasterScale(configured);
        Gui.drawRect(4, 4, Math.max(4, getArea().w() - 4), Math.max(4, getArea().h() - 4), 0x66000000);

        Minecraft minecraft = Minecraft.getMinecraft();
        int line = Math.max(18, minecraft.fontRenderer.FONT_HEIGHT + 6);
        draw(minecraft, tr("neofontrender.gui.pipeline.title"), 8, 8, 0xFFFFFF);
        drawFlowGraph(minecraft, state, 12 + panX, 12 + panY,
                Math.max(80, getArea().w() - 24));
        int infoY = 270;
        draw(minecraft, tr("neofontrender.gui.option.engine") + ": " + state.engineName, 8, infoY, 0xFFFFFF);
        draw(minecraft, String.format(Locale.ROOT, "%s: %.1fx  %s: %.2fx",
                tr("neofontrender.gui.label.oversample"), configured,
                tr("neofontrender.gui.label.effective"), effective), 8, infoY + line, 0xD8D8D8);
        FontRenderTuning.DrawContext drawContext = state.drawContext;
        draw(minecraft, String.format(Locale.ROOT, "%s: %.2fx  %s: %s",
                tr("neofontrender.gui.label.gui_scale"), drawContext.pixelScale(),
                tr("neofontrender.gui.label.filter"),
                FontRenderTuning.useLinearFiltering(effective)
                        ? tr("neofontrender.gui.filter.linear") : tr("neofontrender.gui.filter.nearest")),
                8, infoY + line * 2, 0xD8D8D8);
        draw(minecraft, flags(flag("neofontrender.gui.option.pipeline", state.pipeline),
                flag("neofontrender.gui.option.shader", state.shader),
                flag("neofontrender.gui.option.debug_stats", state.debugStats)),
                8, infoY + line * 3, 0xD8D8D8);
        draw(minecraft, flags(flag("neofontrender.gui.option.autoscale", state.autoScale),
                flag("neofontrender.gui.option.linear", state.linear),
                flag("neofontrender.gui.option.mipmap", state.mipmap)),
                8, infoY + line * 4, 0xD8D8D8);
        draw(minecraft, flags(flag("neofontrender.gui.option.integer_scale", state.integerScale),
                flag("neofontrender.gui.option.high_mag", state.highMagnification),
                flag("neofontrender.gui.option.anisotropic", state.anisotropic)),
                8, infoY + line * 5, 0xD8D8D8);
    }

    private static void drawFlowGraph(Minecraft minecraft, Snapshot state, int x, int y, int width) {
        int nodeW = Math.max(118, Math.min(170, (width - 72) / 4));
        int nodeH = 34;
        int gapX = 18;
        int mainY = y + 62;
        int[] xs = {x, x + nodeW + gapX, x + (nodeW + gapX) * 2, x + (nodeW + gapX) * 3};
        node(minecraft, "输入文本", "raw", xs[0], mainY, nodeW, nodeH, 0xFF344B63);
        node(minecraft, "语法解析", join(state.syntaxProviderStatuses), xs[1], mainY, nodeW, nodeH, 0xFF566B48);
        node(minecraft, "结构化文本", state.structuredStatus, xs[2], mainY, nodeW, nodeH, 0xFF566B48);
        node(minecraft, "渲染路由", "Cosmic / AWT", xs[3], mainY, nodeW, nodeH, 0xFF566B48);
        edge(minecraft, xs[0] + nodeW, mainY + nodeH / 2, xs[1], mainY + nodeH / 2);
        edge(minecraft, xs[1] + nodeW, mainY + nodeH / 2, xs[2], mainY + nodeH / 2);
        edge(minecraft, xs[2] + nodeW, mainY + nodeH / 2, xs[3], mainY + nodeH / 2);

        int branchY = mainY + 82;
        node(minecraft, "布局与字形", state.engineName, xs[1], branchY, nodeW, nodeH, 0xFF566B48);
        node(minecraft, "后处理", join(state.postProcessorStatuses), xs[2], branchY, nodeW, nodeH, 0xFF566B48);
        node(minecraft, "GL / 帧缓冲", state.shader ? "shader" : "fixed", xs[3], branchY, nodeW, nodeH, 0xFF566B48);
        edge(minecraft, xs[3] + nodeW / 2, mainY + nodeH, xs[1] + nodeW / 2, branchY);
        edge(minecraft, xs[1] + nodeW, branchY + nodeH / 2, xs[2], branchY + nodeH / 2);
        edge(minecraft, xs[2] + nodeW, branchY + nodeH / 2, xs[3], branchY + nodeH / 2);

        int sideY = branchY + 76;
        node(minecraft, "阴影分支", state.shader ? "modern shadow" : "legacy shadow", xs[2], sideY, nodeW, nodeH,
                state.shader ? 0xFF566B48 : 0xFF665A42);
        node(minecraft, "GL 组件", join(state.glComponentStatuses), xs[3], sideY, nodeW, nodeH, 0xFF566B48);
        edge(minecraft, xs[2] + nodeW / 2, branchY + nodeH, xs[2] + nodeW / 2, sideY);
        edge(minecraft, xs[2] + nodeW, sideY + nodeH / 2, xs[3], sideY + nodeH / 2);
    }

    private static void node(Minecraft minecraft, String title, String detail, int x, int y,
                             int width, int height, int color) {
        Gui.drawRect(x, y, x + width, y + height, color);
        Gui.drawRect(x, y, x + width, y + 2, 0xFFB9D5E8);
        draw(minecraft, fit(title, width - 8), x + 4, y + 4, 0xFFFFFFFF);
        draw(minecraft, fit(detail, width - 8), x + 4, y + 18, 0xFFD5E2EC);
    }

    private static void edge(Minecraft minecraft, int x1, int y1, int x2, int y2) {
        int color = 0xFFD0DCE8;
        if (x1 == x2) {
            Gui.drawRect(x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2), color);
            return;
        }
        Gui.drawRect(Math.min(x1, x2), y1 - 1, Math.max(x1, x2), y1 + 1, color);
        int direction = x2 >= x1 ? 1 : -1;
        Gui.drawRect(x2 - direction * 5, y2 - 3, x2, y2 + 3, color);
    }

    @Override
    public @NotNull Interactable.Result onMousePressed(int mouseButton) {
        if (mouseButton == 0) {
            dragX = getContext().getMouseX();
            dragY = getContext().getMouseY();
            dragging = true;
            return Interactable.Result.SUCCESS;
        }
        return Interactable.Result.IGNORE;
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (dragging && mouseButton == 0) {
            int mouseX = getContext().getMouseX();
            int mouseY = getContext().getMouseY();
            panX = Math.max(-220, Math.min(220, panX + mouseX - dragX));
            panY = Math.max(-80, Math.min(80, panY + mouseY - dragY));
            dragX = mouseX;
            dragY = mouseY;
            return;
        }
        // ParentWidget does not implement Interactable; the graph owns its drag gesture.
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        dragging = false;
        return true;
    }

    private static List<String> routeStatuses() {
        List<String> result = new ArrayList<>();
        for (neofontrender.api.text.route.TextRenderRouteApi.RouteInfo info
                : neofontrender.api.text.route.TextRenderRouteApi.routes()) {
            result.add(info.id + ':' + ("disabled".equals(info.outcome)
                    ? "disabled" : "enabled"));
        }
        return result;
    }

    private static List<String> layoutProviders(String engineName) {
        List<String> result = new ArrayList<>();
        java.util.Set<String> hits = new java.util.HashSet<>(
                neofontrender.core.font.pipeline.StructuredTextRuntime.lastLineBreakProviderIds());
        for (String id : neofontrender.core.font.pipeline.StructuredTextRuntime
                .lineBreakProviderIds()) result.add(id + (hits.contains(id) ? ":hit" : ":idle"));
        if (result.isEmpty()) result.add("cjk -> " + engineName);
        return result;
    }

    private static void drawModuleRow(Minecraft minecraft, String stage, List<String> modules,
                                      int x, int y, int width, int line) {
        int stageWidth = Math.min(92, Math.max(62, width / 7));
        int gap = 4;
        int nodeHeight = Math.max(12, line - 3);
        Gui.drawRect(x, y, x + stageWidth, y + nodeHeight, 0xCC344B63);
        draw(minecraft, fit(stage, stageWidth - 6), x + 3, y + 2, 0xFFFFFFFF);
        List<String> values = modules == null || modules.isEmpty()
                ? Collections.singletonList("-") : modules;
        int available = Math.max(1, width - stageWidth - gap);
        int nodeWidth = Math.max(46, (available - gap * (values.size() - 1)) / values.size());
        int cursor = x + stageWidth + gap;
        for (String value : values) {
            if (cursor >= x + width) break;
            int right = Math.min(x + width, cursor + nodeWidth);
            Gui.drawRect(cursor, y, right, y + nodeHeight, nodeColor(value));
            draw(minecraft, fit(value, right - cursor - 6), cursor + 3, y + 2,
                    value != null && value.endsWith(":error") ? 0xFFFFBBBB : 0xFFD8E8FF);
            if (right < x + width) {
                int center = y + nodeHeight / 2;
                int next = Math.min(x + width, right + gap);
                Gui.drawRect(right, center, next, center + 2, 0xFFD0DCE8);
                if (next - right >= 4) {
                    Gui.drawRect(next - 3, center - 2, next, center + 4, 0xFFD0DCE8);
                }
            }
            cursor = right + gap;
        }
    }

    private static int nodeColor(String value) {
        if (value == null || value.endsWith(":disabled") || value.endsWith(":idle")) return 0x66505A64;
        if (value.endsWith(":error")) return 0x887A3F46;
        if (value.endsWith(":enabled") || value.endsWith(":available")
                || value.endsWith(":applied") || value.endsWith(":hit")) return 0x88606F4E;
        return 0x884A6380;
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? "-" : String.join(",", values);
    }

    private static String fit(String value, int width) {
        if (value == null) return "-";
        int max = Math.max(4, width / 6);
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 3)) + "...";
    }

    private static void draw(Minecraft minecraft, String text, int x, int y, int color) {
        minecraft.fontRenderer.drawString(text, x, y, color);
    }
    private static String flag(String key, boolean value) { return tr(key) + ": " + onOff(value); }
    private static String flags(String first, String second, String third) { return first + "  " + second + "  " + third; }
    private static String tr(String key) { return I18n.format(key); }
    private static String onOff(boolean value) { return tr(value ? "neofontrender.gui.on" : "neofontrender.gui.off"); }

    /** Immutable data consumed by this panel's renderer. */
    public static final class Snapshot {
        public final String engineName;
        public final String oversample;
        public final boolean pipeline, shader, autoScale, linear, mipmap, integerScale,
                highMagnification, anisotropic, debugStats;
        public final List<String> rawMiddlewareIds, syntaxProviderStatuses, postProcessorIds,
                postProcessorStatuses, glComponentStatuses;
        public final String structuredStatus;
        public final boolean glAvailable;
        public final FontRenderTuning.DrawContext drawContext;

        public Snapshot(String engineName, String oversample, boolean pipeline, boolean shader,
                        boolean autoScale, boolean linear, boolean mipmap, boolean integerScale,
                        boolean highMagnification, boolean anisotropic, boolean debugStats,
                        List<String> rawMiddlewareIds, List<String> syntaxProviderStatuses,
                        String structuredStatus, List<String> postProcessorIds,
                        List<String> postProcessorStatuses,
                        List<String> glComponentStatuses, boolean glAvailable,
                        FontRenderTuning.DrawContext drawContext) {
            this.engineName = engineName; this.oversample = oversample; this.pipeline = pipeline;
            this.shader = shader; this.autoScale = autoScale; this.linear = linear; this.mipmap = mipmap;
            this.integerScale = integerScale; this.highMagnification = highMagnification;
            this.anisotropic = anisotropic; this.debugStats = debugStats;
            this.rawMiddlewareIds = immutable(rawMiddlewareIds);
            this.syntaxProviderStatuses = immutable(syntaxProviderStatuses);
            this.structuredStatus = structuredStatus == null ? "-" : structuredStatus;
            this.postProcessorIds = immutable(postProcessorIds);
            this.postProcessorStatuses = immutable(postProcessorStatuses);
            this.glComponentStatuses = immutable(glComponentStatuses);
            this.glAvailable = glAvailable;
            this.drawContext = drawContext == null ? FontRenderTuning.currentDrawContext() : drawContext;
        }

        private static List<String> immutable(List<String> values) {
            return values == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(values));
        }
    }
}
