package neofontrender.addons.tooltips;

import neofontrender.api.text.TextVisualBounds;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import neofontrender.addons.inline.EmbeddedContentConfig;
import neofontrender.api.text.route.TextInlineBounds;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.core.font.support.TooltipLayoutCompat;

final class ModernTooltipRenderer {
    private static final int Z_LEVEL = 300;

    boolean draw(RenderTooltipEvent.Pre event, boolean[] compactLines, String profileId,
                 ThaumcraftTooltipCompat.Context thaumcraftContext,
                 boolean preserveCallerState) {
        CallerGlState callerState = preserveCallerState ? CallerGlState.capture() : null;
        try {
            return drawInternal(event, compactLines, profileId, thaumcraftContext);
        } finally {
            if (callerState != null) callerState.close();
        }
    }

    private boolean drawInternal(RenderTooltipEvent.Pre event, boolean[] compactLines,
                                 String profileId,
                                 ThaumcraftTooltipCompat.Context thaumcraftContext) {
        if (event.getLines().isEmpty()) return false;
        ItemZoomOverlay.beforeTooltip(event.getStack(), event.getX(), event.getY());
        MicaBackdrop.captureUiIfEnabled();
        TooltipLayout layout = TooltipLayout.calculate(event, compactLines,
                TooltipConfig.profile(profileId), thaumcraftContext);
        if (layout.lines.isEmpty()) return false;

        int[] fill = TooltipConfig.fillColors.clone();
        int[] border = TooltipConfig.borderColors.clone();
        boolean spectrum = false;
        // Resource-pack frame definitions take precedence over adaptive rarity coloring.
        // The texture region is still rendered by UIE's panel path, so no foreign asset is bundled.
        LegendaryResourceCompat.Frame resourceFrame = LegendaryResourceCompat.INSTANCE.match(event.getStack());
        if (TooltipConfig.adaptiveBorder) {
            // Match ModernUI: inspect the stack's hover/display name itself. Forge 1.12 prefixes
            // the rendered first line with WHITE even for COMMON items, which would otherwise
            // make every ordinary item produce an artificial white adaptive palette.
            String title = event.getStack().isEmpty() ? "" : event.getStack().getDisplayName();
            if (resourceFrame == null) {
                AdaptiveBorderColors.Result adaptive = AdaptiveBorderColors.compute(event.getStack(), title, border);
                border = adaptive.colors;
                spectrum = adaptive.spectrum;
            }
        }
        spectrum |= "spectrum".equals(TooltipConfig.borderShading);
        applyBorderShading(border, TooltipConfig.borderShading);

        RenderTooltipEvent.Color colorEvent = new RenderTooltipEvent.Color(
                event.getStack(), layout.lines, layout.x, layout.y, event.getFontRenderer(),
                fill[0], border[0], border[2]);
        MinecraftForge.EVENT_BUS.post(colorEvent);
        if (colorEvent.getBackground() != fill[0]) {
            for (int i = 0; i < fill.length; i++) fill[i] = colorEvent.getBackground();
        }
        if (colorEvent.getBorderStart() != border[0] || colorEvent.getBorderEnd() != border[2]) {
            border[0] = border[1] = colorEvent.getBorderStart();
            border[2] = border[3] = colorEvent.getBorderEnd();
        }

        TooltipPanelBounds panel = layout.panelBounds();
        int panelLeft = panel.left, panelTop = panel.top;
        int panelRight = panel.right, panelBottom = panel.bottom;
        if (LegendaryTooltipCompat.prefersPanel(event.getStack())) {
            LegendaryTooltipCompat.drawPanel(panelLeft, panelTop, panelRight, panelBottom,
                    fill[0], border[0], border[2]);
            LegendaryTooltipCompat.drawResourceFrame(panelLeft, panelTop, panelRight, panelBottom,
                    LegendaryResourceCompat.INSTANCE.match(event.getStack()));
        } else {
            drawBackground(layout, fill, border, spectrum);
        }
        beginTooltipExtensions();
        try (LegendaryTooltipCompat.Scope ignored = LegendaryTooltipCompat.begin(
                panelLeft, panelTop, panelRight, panelBottom)) {
            MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostBackground(
                    event.getStack(), layout.lines, layout.x, layout.y, event.getFontRenderer(),
                    layout.width, layout.height));
            drawContent(layout, event.getFontRenderer(), event.getStack());
            QuarkTooltipVisuals.beginModernPostText();
            TooltipLayoutCompat.publish(event.getFontRenderer(), layout.lines, layout.x, layout.y,
                    layout.width, layout.height);
            try {
                MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostText(
                        event.getStack(), layout.lines, layout.x, layout.y, event.getFontRenderer(),
                        layout.width, layout.height));
            } finally {
                TooltipLayoutCompat.clear();
                QuarkTooltipVisuals.endModernPostText();
            }
            drawDebugLayout(layout, event.getFontRenderer(), event.getStack());
        } finally {
            endTooltipExtensions();
        }
        return true;
    }

    /** F3 overlay for inspecting logical rows before ScaledResolution projects them. */
    private static void drawDebugLayout(TooltipLayout layout, FontRenderer font, ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings == null || !minecraft.gameSettings.showDebugInfo
                || !EmbeddedContentConfig.tooltipLayoutDebug()) return;

        int contentTop = layout.y + layout.visualTop;
        int contentBottom = layout.y + layout.visualBottom;
        outline(layout.x, contentTop, layout.x + layout.width, contentBottom, 0xD000FFFF);
        TooltipPanelBounds panel = layout.panelBounds();
        outline(panel.left, panel.top, panel.right, panel.bottom, 0xA08899FF);
        List<String> debugLines = new ArrayList<>();
        debugLines.add("tooltip " + layout.width + "x" + Math.max(1, contentBottom - contentTop));
        debugLines.add("panel=" + panel.left + "," + panel.top + ".." + panel.right + "," + panel.bottom
                + " padding L/R/T/B=" + TooltipConfig.leftPadding + "/" + TooltipConfig.rightPadding
                + "/" + TooltipConfig.topPadding + "/" + TooltipConfig.bottomPadding);
        debugLines.add("content=" + layout.x + "," + layout.y + " divider=" + TooltipConfig.dividerTopMargin + "/"
                + TooltipConfig.dividerBottomMargin);

        TooltipContentLayout content = TooltipContentLayout.build(layout, font, stack);
        int sideWidth = content.sideWidth;
        int titleCount = Math.max(0, Math.min(layout.titleLines, layout.lines.size()));
        HeaderMetrics header = content.header;
        if (sideWidth > 0 && layout.visualPlan != null) {
            outline(layout.x, content.sideY, layout.x + sideWidth,
                    content.sideY + layout.visualPlan.sideHeight(), 0xD0FF9E4D);
            debugLines.add("preview flow " + sideWidth + "x" + layout.visualPlan.sideHeight());
            for (TooltipVisualPlan.SidePlacement placement : layout.visualPlan.sidePlacements()) {
                int left = layout.x + placement.x;
                int top = content.sideY + placement.y;
                outline(left, top, left + placement.block.width(),
                        top + placement.block.height(), 0xE0FFCF66);
                debugLines.add(placement.block.debugLabel() + " "
                        + placement.block.width() + "x" + placement.block.height());
                for (TooltipVisualBlock.DebugBounds nested : placement.block.debugBounds()) {
                    outline(left + nested.x, top + nested.y,
                            left + nested.x + nested.width,
                            top + nested.y + nested.height, 0xB0FFB14D);
                    debugLines.add("  " + nested.label + " " + nested.width + "x" + nested.height);
                }
            }
        }
        if (TooltipHeaderLayout.hasIcon(stack)) {
            int iconX = layout.x + sideWidth;
            int iconY = content.textTop + header.iconY;
            int iconInset = TooltipHeaderLayout.iconDecorationInset();
            int iconBoxSize = TooltipHeaderLayout.iconDecorationSize();
            outline(iconX - iconInset, iconY - iconInset,
                    iconX - iconInset + iconBoxSize,
                    iconY - iconInset + iconBoxSize, 0xE0FFFFFF);
            debugLines.add(TooltipConfig.headerIconFrameEnabled
                    ? "header icon frame 18x18"
                    : TooltipConfig.headerIconBackgroundEnabled
                    ? "header icon background 18x18" : "header icon 16x16");
            debugLines.add("header icon anchor=" + TooltipConfig.headerIconAlignment
                    + " y=" + iconY);
        }
        if (content.rarity != null) {
            TextVisualBounds bounds =
                    content.rarity.bounds.translate(content.rarityX, content.rarityY);
            outline((int) Math.floor(bounds.left), (int) Math.floor(bounds.top),
                    (int) Math.ceil(bounds.right), (int) Math.ceil(bounds.bottom), 0xE0B58CFF);
            debugLines.add("header rarity @" + content.rarityX + "," + content.rarityY
                    + " alignment=" + TooltipConfig.rarityAlignment);
        }
        if (titleCount > 0 && !content.rows.isEmpty()) {
            TooltipContentLayout.Row first = content.rows.get(0);
            float scale = layout.profile().textScale * (layout.compactLines.get(0) ? 0.5F : 1);
            TextVisualBounds bounds = first.text.bounds.scale(scale);
            int titleCenter = Math.round(first.textX + layout.profile().offsetX
                    + (bounds.left + bounds.right) * 0.5F);
            debugLines.add("header measured title=" + bounds.width() + " center=" + titleCenter);
            Gui.drawRect(titleCenter, layout.y, titleCenter + 1,
                    layout.y + Math.max(1, header.headerHeight), 0xB0FFEA4D);
            if (content.rarity != null) {
                int rarityCenter = Math.round(content.rarityX
                        + (content.rarity.bounds.left + content.rarity.bounds.right) * 0.5F);
                Gui.drawRect(rarityCenter, layout.y, rarityCenter + 1,
                        layout.y + Math.max(1, header.headerHeight), 0xB0FF7DFF);
            }
        }

        for (TooltipContentLayout.Row row : content.rows) {
            int i = row.index;
            int rowBottom = row.y + row.height;
            int color = (i & 1) == 0 ? 0xB0FF4D8D : 0xB04DFF88;
            outline(row.x, row.y, row.x + row.width, rowBottom, color);
            drawInlineDebugBounds(layout, font, stack, row);

            String label = NfrTooltipAnchor.isAnchorLine(layout.lines.get(i))
                    ? layout.lines.get(i) : "row " + i + " " + row.height + "px";
            String rowDebug = label + " @" + row.x + "," + row.y;
            if (!NfrTooltipAnchor.isAnchorLine(layout.lines.get(i))) {
                float scale = layout.profile().textScale
                        * (layout.compactLines.get(i) ? 0.5F : 1.0F);
                TextVisualBounds bounds = row.text.bounds.scale(scale)
                        .translate(row.textX + layout.profile().offsetX, row.textY + layout.profile().offsetY);
                rowDebug += " text=" + row.textWidth + " x=" + row.textX + " y=" + row.textY
                        + " align=" + (row.title ? TooltipConfig.titleAlignment : TooltipConfig.bodyAlignment);
                int textLeft = (int) Math.floor(bounds.left);
                int textTop = (int) Math.floor(bounds.top);
                int textBottom = (int) Math.ceil(bounds.bottom);
                outline(textLeft, textTop, (int) Math.ceil(bounds.right), textBottom, 0x90FFDD44);
                rowDebug += " measuredY=" + textTop + ".." + textBottom;
            }
            debugLines.add(rowDebug);
            if (row.dividerY != Integer.MIN_VALUE) {
                Gui.drawRect(row.x, row.dividerY, row.x + row.width,
                        row.dividerY + 1, 0x604D7DFF);
            }
        }
        for (TooltipContentLayout.BlockPlacement placement : content.blocks) {
            TooltipVisualBlock block = placement.block;
            outline(placement.x, placement.y, placement.x + block.width(),
                    placement.y + block.height(), 0xE0FFE14D);
            debugLines.add(block.debugLabel() + " " + block.width() + "x" + block.height()
                    + " @" + placement.x + "," + placement.y);
            for (TooltipVisualBlock.DebugBounds nested : block.debugBounds()) {
                outline(placement.x + nested.x, placement.y + nested.y,
                        placement.x + nested.x + nested.width,
                        placement.y + nested.y + nested.height, 0xB0FFB14D);
                debugLines.add("  " + nested.label + " " + nested.width + "x" + nested.height);
            }
        }

        if (titleCount > 0) {
            int headerX = layout.x + sideWidth;
            int headerWidth = Math.max(1, layout.width - sideWidth);
            outline(headerX, content.textTop, headerX + headerWidth,
                    content.textTop + header.headerHeight, 0xE0FFFFFF);
            debugLines.add("header " + headerWidth + "x" + header.headerHeight);
            debugLines.add("header title alignment=" + TooltipConfig.titleAlignment
                    + " icon anchor=" + TooltipConfig.headerIconAlignment
                    + " inset=" + TooltipHeaderLayout.titleInset(stack));
        }
        drawDebugLegend(debugLines, font, layout, contentTop, contentBottom);
    }

    private static void drawDebugLegend(List<String> lines, FontRenderer font,
                                        TooltipLayout layout, int contentTop, int contentBottom) {
        if (lines.isEmpty() || font == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int lineHeight = 9;
        int padding = 4;
        int maxLines = Math.max(1,
                (resolution.getScaledHeight() - padding * 2 - 4) / lineHeight);
        if (lines.size() > maxLines) {
            int visibleCount = Math.max(0, maxLines - 1);
            List<String> visible = new ArrayList<>(lines.subList(0, visibleCount));
            visible.add("+" + (lines.size() - visibleCount) + " more");
            lines = visible;
        }
        int width = 0;
        for (String line : lines) width = Math.max(width, font.getStringWidth(line));
        width = Math.min(Math.max(86, width), 220);
        int height = padding * 2 + lines.size() * lineHeight;
        int panelLeft = layout.x + layout.width + 7;
        int panelTop = contentTop;
        if (panelLeft + width > resolution.getScaledWidth()) {
            panelLeft = layout.x - width - 7;
        }
        if (panelLeft < 2) {
            panelLeft = Math.max(2, layout.x);
            panelTop = contentBottom + 7;
            if (panelTop + height > resolution.getScaledHeight()) {
                panelTop = Math.max(2, contentTop - height - 7);
            }
        }
        panelTop = Math.max(2, Math.min(panelTop, resolution.getScaledHeight() - height - 2));
        Gui.drawRect(panelLeft, panelTop, panelLeft + width, panelTop + height, 0xD8101018);
        Gui.drawRect(panelLeft, panelTop, panelLeft + width, panelTop + 1, 0xF0FFCF66);
        Gui.drawRect(panelLeft, panelTop + height - 1, panelLeft + width, panelTop + height,
                0xF0FFCF66);
        Gui.drawRect(panelLeft, panelTop, panelLeft + 1, panelTop + height, 0xF0FFCF66);
        Gui.drawRect(panelLeft + width - 1, panelTop, panelLeft + width, panelTop + height,
                0xF0FFCF66);
        int y = panelTop + padding;
        for (String line : lines) {
            String clipped = clipDebugLabel(line, width - padding * 2, font::getStringWidth);
            font.drawString(clipped, panelLeft + padding, y, 0xFFFFE8A8, false);
            y += lineHeight;
        }
    }

    static String clipDebugLabel(String value, int maxWidth, ToIntFunction<String> width) {
        String text = value == null ? "" : value;
        int available = Math.max(0, maxWidth);
        if (width.applyAsInt(text) <= available) return text;
        String suffix = "...";
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (width.applyAsInt(text.substring(0, mid) + suffix) <= available) low = mid;
            else high = mid - 1;
        }
        return text.substring(0, low) + suffix;
    }

    private static void drawInlineDebugBounds(TooltipLayout tooltip, FontRenderer font,
                                              ItemStack stack, TooltipContentLayout.Row row) {
        int lineIndex = row.index;
        TextRenderRouteLayout line;
        try {
            line = TextRenderRouteApi.layout(font, tooltip.lines.get(lineIndex));
        } catch (RuntimeException ignored) {
            return;
        }
        if (!line.hasInlineContent()) return;

        boolean compact = tooltip.compactLines.get(lineIndex);
        float scale = tooltip.profile().textScale * (compact ? 0.5F : 1.0F);
        float originX = row.textX + tooltip.profile().offsetX;
        float originY = row.textY + tooltip.profile().offsetY;
        for (TextInlineBounds hit : line.inlineBounds()) {
            int left = Math.round(originX + hit.x() * scale);
            int top = Math.round(originY + hit.y() * scale);
            int right = Math.round(originX + (hit.x() + hit.width()) * scale);
            int bottom = Math.round(originY + (hit.y() + hit.height()) * scale);
            outline(left, top, right, bottom, 0xE0FFE14D);
        }
    }

    private static void outline(int left, int top, int right, int bottom, int color) {
        if (right <= left || bottom <= top) return;
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top, left + 1, bottom, color);
        Gui.drawRect(right - 1, top, right, bottom, color);
    }

    /** Matches Forge GuiUtils' GL contract while PostBackground/PostText subscribers render. */
    private static void beginTooltipExtensions() {
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

    private static void endTooltipExtensions() {
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
    }

    private static void drawBackground(TooltipLayout layout, int[] fill, int[] border, boolean spectrum) {
        TooltipPanelBounds panel = layout.panelBounds();
        float left = panel.left, top = panel.top, right = panel.right, bottom = panel.bottom;
        float radius = TooltipConfig.rounded ? TooltipConfig.cornerRadius : 0.01F;
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        // Inventory and several modded screens leave face culling enabled. The rounded fill is a
        // triangle fan in GUI (Y-down) coordinates, whose winding is otherwise culled while the
        // alternating border strip can still remain partially visible.
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        try {
            boolean analyticStyle = "modernui".equals(TooltipConfig.renderStyle)
                    || "mica".equals(TooltipConfig.renderStyle);
            boolean shaderDrawn = analyticStyle
                    && (TooltipConfig.rounded || "mica".equals(TooltipConfig.renderStyle))
                    && ModernUiTooltipShader.draw(left, top, right, bottom, radius, fill, border,
                    spectrum, "mica".equals(TooltipConfig.renderStyle));
            if (!shaderDrawn) {
                if (spectrum) applyFallbackSpectrum(border);
                drawShadow(left, top, right, bottom, radius);
                drawRoundedFill(left, top, right, bottom, radius, fill);
                drawRoundedBorder(left, top, right, bottom, radius,
                        Math.min(TooltipConfig.borderWidth, Math.max(0.5F, radius)), border);
            }

        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            if (cullEnabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            RenderHelper.enableGUIStandardItemLighting();
        }
    }

    /** Draws only NFR's panel/frame around a foreign renderer's already-computed bounds. */
    static void drawCompatibleBackground(int x, int y, int width, int height, ItemStack stack) {
        MicaBackdrop.captureUiIfEnabled();
        int[] fill = TooltipConfig.fillColors.clone();
        int[] border = TooltipConfig.borderColors.clone();
        boolean spectrum = false;
        if (TooltipConfig.adaptiveBorder && stack != null && !stack.isEmpty()) {
            AdaptiveBorderColors.Result adaptive = AdaptiveBorderColors.compute(stack, stack.getDisplayName(), border);
            border = adaptive.colors;
            spectrum = adaptive.spectrum;
        }
        spectrum |= "spectrum".equals(TooltipConfig.borderShading);
        applyBorderShading(border, TooltipConfig.borderShading);

        float left = x;
        float top = y;
        float right = x + width;
        float bottom = y + height;
        float radius = TooltipConfig.rounded ? TooltipConfig.cornerRadius : 0.01F;
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            boolean analyticStyle = "modernui".equals(TooltipConfig.renderStyle)
                    || "mica".equals(TooltipConfig.renderStyle);
            boolean shaderDrawn = analyticStyle
                    && (TooltipConfig.rounded || "mica".equals(TooltipConfig.renderStyle))
                    && ModernUiTooltipShader.draw(left, top, right, bottom, radius, fill, border,
                    spectrum, "mica".equals(TooltipConfig.renderStyle));
            if (!shaderDrawn) {
                if (spectrum) applyFallbackSpectrum(border);
                drawShadow(left, top, right, bottom, radius);
                drawRoundedFill(left, top, right, bottom, radius, fill);
                drawRoundedBorder(left, top, right, bottom, radius,
                        Math.min(TooltipConfig.borderWidth, Math.max(0.5F, radius)), border);
            }
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            if (cullEnabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.enableLighting();
            RenderHelper.enableGUIStandardItemLighting();
        }
    }

    /** Draws NFR's title divider inside a foreign tooltip while preserving its content renderer. */
    static void drawCompatibleDivider(int x, int y, int width, ItemStack stack) {
        if (!TooltipConfig.titleBreak || width <= 0) return;

        int[] border = TooltipConfig.borderColors.clone();
        boolean spectrum = false;
        if (TooltipConfig.adaptiveBorder && stack != null && !stack.isEmpty()) {
            AdaptiveBorderColors.Result adaptive = AdaptiveBorderColors.compute(stack, stack.getDisplayName(), border);
            border = adaptive.colors;
            spectrum = adaptive.spectrum;
        }
        spectrum |= "spectrum".equals(TooltipConfig.borderShading);
        applyBorderShading(border, TooltipConfig.borderShading);
        if (spectrum) applyFallbackSpectrum(border);

        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        GlStateManager.disableTexture2D();
        // GUI quads use a Y-down winding. If a screen left face culling enabled,
        // the divider is discarded even though the surrounding panel is visible.
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        try {
            drawQuad(x, y, x + width, y + 1.0F,
                    withAlpha(border[3], TooltipConfig.dividerAlpha),
                    withAlpha(border[2], TooltipConfig.dividerAlpha),
                    withAlpha(border[2], TooltipConfig.dividerAlpha),
                    withAlpha(border[3], TooltipConfig.dividerAlpha));
        } finally {
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            if (cullEnabled) GlStateManager.enableCull();
            else GlStateManager.disableCull();
        }
    }

    /** Shared retained layout for production and the settings preview. */
    static void drawContent(TooltipLayout layout, FontRenderer font, ItemStack stack) {
        if (layout != null && font != null) drawRetainedContent(layout, font, stack);
    }

    private static void drawRetainedContent(TooltipLayout layout, FontRenderer font, ItemStack stack) {
        TooltipContentLayout content = TooltipContentLayout.build(layout, font, stack);
        if (layout.visualPlan != null && content.sideWidth > 0) {
            layout.visualPlan.drawSide(layout.x, content.sideY, font, stack);
        }
        if (content.header.headerHeight > 0 && TooltipHeaderLayout.hasIcon(stack)) {
            TooltipHeaderLayout.drawIcon(stack, layout.x + content.sideWidth,
                    content.textTop + content.header.iconY);
        }
        for (TooltipContentLayout.Row row : content.rows) {
            String line = layout.lines.get(row.index);
            if (!NfrTooltipAnchor.isAnchorLine(line)) {
                boolean compact = layout.compactLines.get(row.index);
                float scale = layout.profile().textScale * (compact ? 0.5F : 1.0F);
                int color = row.title ? TooltipConfig.titleColor : TooltipConfig.textColor;
                float drawX = row.textX + layout.profile().offsetX;
                float drawY = row.textY + layout.profile().offsetY;
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.scale(scale, scale, 1.0F);
                    row.text.draw(font, drawX / scale, drawY / scale, color, TooltipConfig.textShadow);
                } finally {
                    GlStateManager.popMatrix();
                }
                if (row.title && row.index + 1 == layout.titleLines && content.rarity != null) {
                    content.rarity.draw(font, content.rarityX, content.rarityY,
                            TooltipHeaderLayout.rarityColor(stack), true);
                }
                if (row.dividerY != Integer.MIN_VALUE) {
                    drawCompatibleDivider(layout.x + content.sideWidth, row.dividerY,
                            Math.max(1, layout.width - content.sideWidth), stack);
                }
            }
            for (TooltipContentLayout.BlockPlacement placement : content.blocks) {
                if (placement.line == row.index) placement.block.draw(placement.x, placement.y, font);
            }
        }
    }

    private static void applyBorderShading(int[] colors, String mode) {
        if ("solid".equals(mode)) {
            colors[1] = colors[2] = colors[3] = colors[0];
        } else if ("horizontal".equals(mode)) {
            colors[3] = colors[0];
            colors[2] = colors[1];
        } else if ("vertical".equals(mode)) {
            colors[1] = colors[0];
            colors[3] = colors[2];
        }
    }

    private static void applyFallbackSpectrum(int[] colors) {
        int alpha = Color.alpha(colors[0]);
        colors[0] = withAlpha(0xFFFF5555, alpha);
        colors[1] = withAlpha(0xFFFFFF55, alpha);
        colors[2] = withAlpha(0xFF55FFFF, alpha);
        colors[3] = withAlpha(0xFFFF55FF, alpha);
    }

    static void drawRoundedFill(float left, float top, float right, float bottom,
                                float radius, int[] colors) {
        List<Point> points = perimeter(left, top, right, bottom, radius);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        float centerX = (left + right) * 0.5F;
        float centerY = (top + bottom) * 0.5F;
        vertex(buffer, centerX, centerY, colorAt(colors, 0.5F, 0.5F));
        for (Point point : points) {
            float tx = normalize(point.x, left, right);
            float ty = normalize(point.y, top, bottom);
            vertex(buffer, point.x, point.y, colorAt(colors, tx, ty));
        }
        Point first = points.get(0);
        vertex(buffer, first.x, first.y, colorAt(colors,
                normalize(first.x, left, right), normalize(first.y, top, bottom)));
        tessellator.draw();
    }

    static void drawRoundedBorder(float left, float top, float right, float bottom,
                                  float radius, float width, int[] colors) {
        List<Point> outer = perimeter(left, top, right, bottom, radius);
        List<Point> inner = perimeter(left + width, top + width, right - width, bottom - width,
                Math.max(0.01F, radius - width));
        drawStrip(outer, inner, left, top, right, bottom, colors, false);

        float aa = TooltipConfig.antialiasWidth;
        if (aa > 0.0F) {
            List<Point> fringe = perimeter(left - aa, top - aa, right + aa, bottom + aa, radius + aa);
            drawStrip(fringe, outer, left, top, right, bottom, colors, true);
        }
    }

    /** Draws non-overlapping rings whose vertex alpha follows a continuous Gaussian falloff. */
    private static void drawShadow(float left, float top, float right, float bottom, float radius) {
        float extent = TooltipConfig.shadowRadius;
        if (extent <= 0.0F || TooltipConfig.shadowAlpha <= 0) return;
        int steps = Math.max(2, TooltipConfig.shadowSteps);
        float offsetX = TooltipConfig.shadowOffsetX;
        float offsetY = TooltipConfig.shadowOffsetY;
        for (int i = steps - 1; i >= 0; i--) {
            float innerDistance = extent * i / steps;
            float outerDistance = extent * (i + 1) / steps;
            List<Point> inner = perimeter(left + offsetX - innerDistance,
                    top + offsetY - innerDistance, right + offsetX + innerDistance,
                    bottom + offsetY + innerDistance, radius + innerDistance);
            List<Point> outer = perimeter(left + offsetX - outerDistance,
                    top + offsetY - outerDistance, right + offsetX + outerDistance,
                    bottom + offsetY + outerDistance, radius + outerDistance);
            int innerAlpha = shadowAlpha(innerDistance, extent);
            int outerAlpha = shadowAlpha(outerDistance, extent);
            drawSolidStrip(outer, inner, withAlpha(TooltipConfig.shadowColor, outerAlpha),
                    withAlpha(TooltipConfig.shadowColor, innerAlpha));
        }
    }

    private static int shadowAlpha(float distance, float extent) {
        float normalized = distance / Math.max(0.001F, extent);
        return Math.round(TooltipConfig.shadowAlpha * (float) Math.exp(-3.0F * normalized * normalized));
    }

    private static void drawSolidStrip(List<Point> outer, List<Point> inner, int outerColor, int innerColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int count = Math.min(outer.size(), inner.size());
        for (int i = 0; i <= count; i++) {
            Point out = outer.get(i % count);
            Point in = inner.get(i % count);
            vertex(buffer, out.x, out.y, outerColor);
            vertex(buffer, in.x, in.y, innerColor);
        }
        tessellator.draw();
    }

    private static void drawStrip(List<Point> outer, List<Point> inner,
                                  float left, float top, float right, float bottom,
                                  int[] colors, boolean transparentOuter) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        int count = Math.min(outer.size(), inner.size());
        for (int i = 0; i <= count; i++) {
            Point out = outer.get(i % count);
            Point in = inner.get(i % count);
            int outColor = colorAt(colors, normalize(out.x, left, right), normalize(out.y, top, bottom));
            if (transparentOuter) outColor = withAlpha(outColor, 0);
            int inColor = colorAt(colors, normalize(in.x, left, right), normalize(in.y, top, bottom));
            vertex(buffer, out.x, out.y, outColor);
            vertex(buffer, in.x, in.y, inColor);
        }
        tessellator.draw();
    }

    private static List<Point> perimeter(float left, float top, float right, float bottom, float requestedRadius) {
        float radius = Math.max(0.01F, Math.min(requestedRadius,
                Math.min((right - left) * 0.5F, (bottom - top) * 0.5F)));
        int segments = Math.max(3, TooltipConfig.cornerSegments);
        List<Point> points = new ArrayList<>(segments * 4 + 4);
        appendCorner(points, right - radius, top + radius, radius, -90.0F, 0.0F);
        appendCorner(points, right - radius, bottom - radius, radius, 0.0F, 90.0F);
        appendCorner(points, left + radius, bottom - radius, radius, 90.0F, 180.0F);
        appendCorner(points, left + radius, top + radius, radius, 180.0F, 270.0F);
        return points;
    }

    private static void appendCorner(List<Point> points, float centerX, float centerY,
                                     float radius, float startDegrees, float endDegrees) {
        int segments = Math.max(3, TooltipConfig.cornerSegments);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(MathUtil.lerp(startDegrees, endDegrees,
                    i / (float) segments));
            points.add(new Point(centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius));
        }
    }

    private static void drawQuad(float left, float top, float right, float bottom,
                                 int upperLeft, int upperRight, int lowerRight, int lowerLeft) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, upperLeft);
        vertex(buffer, right, top, upperRight);
        vertex(buffer, right, bottom, lowerRight);
        vertex(buffer, left, bottom, lowerLeft);
        tessellator.draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, Z_LEVEL).color(
                Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)).endVertex();
    }

    private static int colorAt(int[] colors, float x, float y) {
        return lerpColor(lerpColor(colors[0], colors[1], x), lerpColor(colors[3], colors[2], x), y);
    }

    private static int lerpColor(int from, int to, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        return (Math.round(MathUtil.lerp(Color.alpha(from), Color.alpha(to), amount)) << 24)
                | (Math.round(MathUtil.lerp(Color.red(from), Color.red(to), amount)) << 16)
                | (Math.round(MathUtil.lerp(Color.green(from), Color.green(to), amount)) << 8)
                | Math.round(MathUtil.lerp(Color.blue(from), Color.blue(to), amount));
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static float normalize(float value, float start, float end) {
        if (end <= start) return 0.5F;
        return Math.max(0.0F, Math.min(1.0F, (value - start) / (end - start)));
    }

    /**
     * ModularUI publishes its tooltip Pre event before establishing the state used by its own
     * renderer. Cancelling that event must therefore leave the publisher's state untouched.
     * glPushAttrib restores the driver while the explicit setters also repair Minecraft's cached
     * GlStateManager view, which raw GL calls in third-party Post handlers can desynchronize.
     * Item lighting uses texture units 0, 1 and 2, so all three must be synchronized even though
     * only one of them is active when the tooltip event is posted.
     */
    static final class CallerGlState implements AutoCloseable {
        private final boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        private final boolean light0 = GL11.glIsEnabled(GL11.GL_LIGHT0);
        private final boolean light1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
        private final boolean colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        private final int alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        private final float alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        private final int cullFace = GL11.glGetInteger(GL11.GL_CULL_FACE_MODE);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
        private final boolean rescaleNormal = GL11.glIsEnabled(GL12.GL_RESCALE_NORMAL);
        private final int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        private final int blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        private final int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final TextureUnitState[] textureUnits = readTextureUnits(activeTexture);
        private final float[] color = readColor();
        private final boolean[] colorMask = readColorMask();
        private boolean closed;

        private CallerGlState() {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        }

        static CallerGlState capture() {
            return new CallerGlState();
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            GL11.glPopAttrib();

            GL20.glUseProgram(program);
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            restoreToggle(lighting, GL11.GL_LIGHTING,
                    GlStateManager::enableLighting, GlStateManager::disableLighting);
            restoreToggle(light0, GL11.GL_LIGHT0,
                    () -> GlStateManager.enableLight(0), () -> GlStateManager.disableLight(0));
            restoreToggle(light1, GL11.GL_LIGHT1,
                    () -> GlStateManager.enableLight(1), () -> GlStateManager.disableLight(1));
            restoreToggle(colorMaterial, GL11.GL_COLOR_MATERIAL,
                    GlStateManager::enableColorMaterial, GlStateManager::disableColorMaterial);
            restoreToggle(depth, GL11.GL_DEPTH_TEST,
                    GlStateManager::enableDepth, GlStateManager::disableDepth);
            GlStateManager.depthMask(depthMask);
            GL11.glDepthMask(depthMask);
            GlStateManager.depthFunc(depthFunc);
            GL11.glDepthFunc(depthFunc);
            GlStateManager.alphaFunc(alphaFunc, alphaRef);
            GL11.glAlphaFunc(alphaFunc, alphaRef);
            GlStateManager.cullFace(cullFace == GL11.GL_FRONT ? GlStateManager.CullFace.FRONT
                    : cullFace == GL11.GL_FRONT_AND_BACK ? GlStateManager.CullFace.FRONT_AND_BACK
                    : GlStateManager.CullFace.BACK);
            GL11.glCullFace(cullFace);
            restoreToggle(blend, GL11.GL_BLEND,
                    GlStateManager::enableBlend, GlStateManager::disableBlend);
            restoreToggle(alpha, GL11.GL_ALPHA_TEST,
                    GlStateManager::enableAlpha, GlStateManager::disableAlpha);
            restoreToggle(cull, GL11.GL_CULL_FACE,
                    GlStateManager::enableCull, GlStateManager::disableCull);
            restoreToggle(fog, GL11.GL_FOG,
                    GlStateManager::enableFog, GlStateManager::disableFog);
            restoreToggle(rescaleNormal, GL12.GL_RESCALE_NORMAL,
                    GlStateManager::enableRescaleNormal, GlStateManager::disableRescaleNormal);
            GlStateManager.shadeModel(shadeModel);
            GL11.glShadeModel(shadeModel);
            GlStateManager.colorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GL11.glColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
            GlStateManager.color(color[0], color[1], color[2], color[3]);
            GL11.glColor4f(color[0], color[1], color[2], color[3]);
            restoreTextureUnits(textureUnits, activeTexture);
        }

        private static void restoreToggle(boolean enabled, int capability,
                                          Runnable enable, Runnable disable) {
            if (enabled) enable.run();
            else disable.run();
            if (enabled) GL11.glEnable(capability);
            else GL11.glDisable(capability);
        }

        private static float[] readColor() {
            FloatBuffer values = BufferUtils.createFloatBuffer(4);
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, values);
            return new float[]{values.get(0), values.get(1), values.get(2), values.get(3)};
        }

        private static boolean[] readColorMask() {
            IntBuffer values = BufferUtils.createIntBuffer(4);
            GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK, values);
            return new boolean[]{values.get(0) != 0, values.get(1) != 0,
                    values.get(2) != 0, values.get(3) != 0};
        }

        private static TextureUnitState[] readTextureUnits(int originalActiveTexture) {
            boolean originalIsItemUnit = originalActiveTexture >= GL13.GL_TEXTURE0
                    && originalActiveTexture <= GL13.GL_TEXTURE2;
            TextureUnitState[] states = new TextureUnitState[originalIsItemUnit ? 3 : 4];
            for (int i = 0; i < 3; i++) {
                int unit = GL13.GL_TEXTURE0 + i;
                GL13.glActiveTexture(unit);
                states[i] = new TextureUnitState(unit,
                        GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            }
            if (!originalIsItemUnit) {
                GL13.glActiveTexture(originalActiveTexture);
                states[3] = new TextureUnitState(originalActiveTexture,
                        GL11.glIsEnabled(GL11.GL_TEXTURE_2D),
                        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            }
            GL13.glActiveTexture(originalActiveTexture);
            return states;
        }

        private static void restoreTextureUnits(TextureUnitState[] states,
                                                int originalActiveTexture) {
            for (TextureUnitState state : states) {
                GlStateManager.setActiveTexture(state.unit);
                GL13.glActiveTexture(state.unit);
                restoreToggle(state.enabled, GL11.GL_TEXTURE_2D,
                        GlStateManager::enableTexture2D, GlStateManager::disableTexture2D);
                GlStateManager.bindTexture(state.binding);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, state.binding);
            }
            GlStateManager.setActiveTexture(originalActiveTexture);
            GL13.glActiveTexture(originalActiveTexture);
        }
    }

    private static final class TextureUnitState {
        final int unit;
        final boolean enabled;
        final int binding;

        TextureUnitState(int unit, boolean enabled, int binding) {
            this.unit = unit;
            this.enabled = enabled;
            this.binding = binding;
        }
    }

    private static final class Point {
        final float x;
        final float y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
