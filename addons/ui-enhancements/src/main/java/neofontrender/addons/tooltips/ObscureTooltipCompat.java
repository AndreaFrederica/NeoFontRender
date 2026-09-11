package neofontrender.addons.tooltips;

import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.Minecraft;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.common.MinecraftForge;

public final class ObscureTooltipCompat {
    private static final ThreadLocal<ItemStack> ACTIVE_STACK = new ThreadLocal<>();
    private static final ThreadLocal<FontRenderer> ACTIVE_FONT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> WIDTH_LIMIT = new ThreadLocal<>();
    private static final ThreadLocal<RenderTooltipEvent.Pre> ACTIVE_EVENT = new ThreadLocal<>();
    private static final ThreadLocal<LegendaryTooltipCompat.Scope> DECOR_SCOPE = new ThreadLocal<>();

    public interface TextMetrics {
        int getHeight();
        void nfrUi$setWidthInset(int inset);
    }

    public static int headerContentHeight(int titleHeight, int labelHeight) {
        return Math.max(22, titleHeight + Math.max(5, labelHeight) + 2);
    }

    public static void begin(RenderTooltipEvent.Pre event) {
        if (!shouldLayout()) return;
        ACTIVE_EVENT.set(event);
        ACTIVE_FONT.set(event.getFontRenderer());
        int limit = TooltipLayout.screenWidthLimit(event.getScreenWidth(),
                TooltipConfig.horizontalPadding, TooltipVisualExtents.current());
        WIDTH_LIMIT.set(contentWidthLimit(limit, event.getMaxWidth(), TooltipConfig.maxWidth, 0));
    }

    static int contentWidthLimit(int screenLimit, int eventLimit, int configuredLimit, int inset) {
        int limit = screenLimit;
        if (eventLimit > 0) limit = Math.min(limit, eventLimit);
        if (configuredLimit > 0) limit = Math.min(limit, configuredLimit);
        return Math.max(1, limit - Math.max(0, inset));
    }

    public static FontRenderer font() {
        FontRenderer font = ACTIVE_FONT.get();
        return font == null ? Minecraft.getMinecraft().fontRenderer : font;
    }

    public static int textWidth(FontRenderer font, String text) {
        return TooltipLayout.measuredLineWidth(font, text, false,
                TooltipConfig.profile("vanilla").textScale);
    }

    public static List<String> wrap(FontRenderer font, String text, int width) {
        return TooltipLayout.wrapLine(font, text,
                Math.max(1, Math.round(width / TooltipConfig.profile("vanilla").textScale)));
    }

    public static TextBlock layoutText(FontRenderer font, String text, int inset) {
        Integer limit = WIDTH_LIMIT.get();
        int widthLimit = contentWidthLimit(limit == null ? Integer.MAX_VALUE : limit, 0, 0, inset);
        List<String> lines = textWidth(font, text) > widthLimit
                ? wrap(font, text, widthLimit) : Collections.singletonList(text);
        List<Integer> widths = new ArrayList<>(lines.size());
        for (String line : lines) widths.add(textWidth(font, line));
        List<Integer> advances = TooltipLayout.lineAdvances(font, lines,
                Collections.nCopies(lines.size(), false), TooltipConfig.profile("vanilla").textScale);
        return new TextBlock(lines, widths, advances);
    }

    public static final class TextBlock {
        private final List<String> lines;
        private final List<Integer> widths;
        private final List<Integer> advances;

        private TextBlock(List<String> lines, List<Integer> widths, List<Integer> advances) {
            this.lines = lines;
            this.widths = widths;
            this.advances = advances;
        }

        public int width() { return widths.stream().mapToInt(Integer::intValue).max().orElse(0); }
        public int height() { return advances.stream().mapToInt(Integer::intValue).sum(); }

        public void draw(FontRenderer font, int x, int y) {
            ModernTooltipRenderer.drawContent(x, y, width(), lines,
                    Collections.nCopies(lines.size(), false), 0, TooltipConfig.profile("vanilla"), font,
                    ACTIVE_STACK.get(), true, widths, advances, null);
        }
    }

    private ObscureTooltipCompat() {}

    public static boolean shouldReplacePanel() {
        return shouldLayout() && TooltipPanelOwner.choose(Loader.isModLoaded("legendarytooltips"),
                true, TooltipConfig.yieldToLegendaryTooltips, TooltipConfig.yieldToObscureTooltips)
                != TooltipPanelOwner.OBSCURE;
    }

    /** Layout remains modern regardless of which mod owns the panel. */
    public static boolean shouldLayout() {
        return Loader.isModLoaded("obscure_tooltips") && TooltipConfig.enabled
                && Arc3DRuntimeSupport.isAvailable();
    }

    public static void preparePanel(int x, int y, int width, int height, ItemStack stack) {
        ACTIVE_STACK.set(stack);
        int outset = shouldReplacePanel() && !LegendaryTooltipCompat.prefersPanel() ? 2 : 3;
        LegendaryTooltipCompat.Scope previous = DECOR_SCOPE.get();
        if (previous != null) previous.close();
        DECOR_SCOPE.set(LegendaryTooltipCompat.begin(x - outset, y - outset,
                x + width + outset, y + height + outset));
        RenderTooltipEvent.Pre source = ACTIVE_EVENT.get();
        RenderTooltipEvent.Color colors = new RenderTooltipEvent.Color(stack,
                source == null ? Collections.emptyList() : source.getLines(), x, y, font(),
                TooltipConfig.fillColors[0], TooltipConfig.borderColors[0], TooltipConfig.borderColors[2]);
        // Obscure normally skips Color, leaving Legendary's per-tooltip border colors stale.
        MinecraftForge.EVENT_BUS.post(colors);
        if (LegendaryTooltipCompat.prefersPanel()) {
            LegendaryTooltipCompat.drawPanel(x - outset, y - outset,
                    x + width + outset, y + height + outset,
                    colors.getBackground(), colors.getBorderStart(), colors.getBorderEnd());
        } else if (shouldReplacePanel()) {
            ModernTooltipRenderer.drawCompatibleBackground(
                    x - outset, y - outset, width + outset * 2, height + outset * 2, stack);
        }
    }

    /** Replaces Obscure's two-piece fading header line with NFR's configured divider. */
    public static boolean replaceSeparator(int x, int y, int width) {
        if (!shouldLayout()) return false;
        ModernTooltipRenderer.drawCompatibleDivider(x, y, width, ACTIVE_STACK.get());
        ACTIVE_STACK.remove();
        return true;
    }

    /** Prevent a header-less tooltip from retaining its stack in the thread local. */
    public static void clearActiveStack() {
        ACTIVE_STACK.remove();
        ACTIVE_FONT.remove();
        WIDTH_LIMIT.remove();
        ACTIVE_EVENT.remove();
        LegendaryTooltipCompat.Scope scope = DECOR_SCOPE.get();
        if (scope != null) scope.close();
        DECOR_SCOPE.remove();
    }
}
