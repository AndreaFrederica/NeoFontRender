package mnm.mods.tabbychat.gui;

import com.google.common.eventbus.Subscribe;
import mnm.mods.tabbychat.ChatManager;
import mnm.mods.tabbychat.TabbyChat;
import mnm.mods.tabbychat.api.gui.ChatInput;
import mnm.mods.tabbychat.core.GuiNewChatTC;
import mnm.mods.tabbychat.extra.spell.Spellcheck;
import mnm.mods.tabbychat.extra.spell.SpellingFormatter;
import mnm.mods.util.Color;
import mnm.mods.util.ILocation;
import mnm.mods.util.TexturedModal;
import mnm.mods.util.gui.GuiComponent;
import mnm.mods.util.gui.GuiText;
import mnm.mods.util.gui.events.GuiMouseEvent;
import mnm.mods.util.gui.events.GuiMouseEvent.MouseEvent;
import mnm.mods.util.gui.events.GuiKeyboardEvent;
import mnm.mods.util.text.FancyFontRenderer;
import mnm.mods.util.text.FancyTextComponent;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import neofontrender.addons.chat.ChatStyleConfig;
import neofontrender.addons.chat.ChatStyleRenderer;
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.chat.ChatContextMenu;
import neofontrender.addons.chat.ChatHudWindowController;
import neofontrender.addons.chat.CommandCompletionPresentation;
import neofontrender.addons.chat.CommandCompletionPresentation.ColoredRange;
import neofontrender.addons.chat.CommandCompletionPresentation.StyledLine;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import neofontrender.api.text.ModernTextApi;
import neofontrender.api.text.route.TextInlineBounds;
import neofontrender.api.text.route.TextRenderRouteApi;
import neofontrender.api.text.route.TextRenderRouteLayout;
import neofontrender.addons.inline.StructuredInlineContentAdapter;
import neofontrender.addons.chat.EnhancedChatFeatures;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.text.edit.SourceEditProjection;
import neofontrender.text.edit.SourceEditState;
import neofontrender.text.edit.SourceSpan;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class TextBox extends GuiComponent implements ChatInput {

    private static final TexturedModal MODAL = new TexturedModal(ChatBox.GUI_LOCATION, 0, 219, 254, 37);
    private static final int BLOCK_LEFT = 3;
    private static final int BLOCK_PADDING = 4;

    private FontRenderer fr = mc.fontRenderer;
    // Dummy textField
    private GuiText textField = new GuiText(new GuiTextField(0, fr, 0, 0, 0, 0) {
        @Override
        public void drawTextBox() {
            // noop
        }
    });
    private int cursorCounter;
    private Spellcheck spellcheck;
    private float animatedHeight = -1.0F;
    private long lastHeightUpdateNanos;
    private final IntBuffer oldScissor = BufferUtils.createIntBuffer(4);
    private boolean scissorWasEnabled;
    private String sourceProjectionText;
    private SourceEditProjection sourceProjection;
    private int sourceProjectionCursor = -1;
    private SourceEditState sourceProjectionState;

    TextBox() {
        textField.getTextField().setMaxStringLength(ChatManager.MAX_CHAT_LENGTH);
        textField.setFocused(true);
        textField.getTextField().setCanLoseFocus(false);

        spellcheck = TabbyChat.getInstance().getSpellcheck();
    }

    @Override
    public void onClosed() {
        super.onClosed();
    }

    @Override
    public void drawComponent(int mouseX, int mouseY) {
        float inputOffset = ChatAnimationController.inputOffset();
        boolean translated = Math.abs(inputOffset) > 0.001F;
        if (translated) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, inputOffset, 0.0F);
        }
        GlStateManager.enableBlend();
        if (ChatStyleConfig.enabled) {
            ChatStyleRenderer.panel(getBounds().width, getBounds().height,
                    ChatStyleConfig.inputBackground, ChatStyleConfig.border, mc.gameSettings.chatOpacity);
        } else {
            drawModalCorners(MODAL);
        }
        GlStateManager.disableBlend();

        boolean clipped = beginClip();
        drawPrivateCommandBlock(mouseX, mouseY);
        drawText();
        drawCursor();
        if (clipped) endClip();
        drawGlyphHover(mouseX, mouseY);

        if (translated) GlStateManager.popMatrix();

    }

    private void drawCursor() {
        GuiTextField textField = this.textField.getTextField();

        // keeps track of all the characters. Used to compensate for spaces
        int totalPos = 0;

        // The current pixel row. adds FONT_HEIGHT each iteration
        int line = 0;

        // The position of the cursor
        int pos = textField.getCursorPosition();
        // the position of the selection
        int sel = textField.getSelectionEnd();

        // make the position and selection in order
        int start = Math.min(pos, sel);
        int end = Math.max(pos, sel);

        for (String text : getWrappedLines()) {
            TextRenderRouteLayout layout = TextRenderRouteApi.layout(fr, text);
            int textLine = line + Math.max(0,
                    (int) Math.ceil(layout.height()) - fr.FONT_HEIGHT);

            // cursor drawing
            if (pos >= 0 && pos <= text.length()) {
                // cursor is on this line
                int c = sourceVisualWidth(text, totalPos, pos) + inputInset();
                boolean cursorBlink = this.cursorCounter / 6 % 3 != 0;
                if (cursorBlink) {
                    if (textField.getCursorPosition() < this.textField.getValue().length()) {
                        drawVerticalLine(c + 3, textLine - 2,
                                textLine + fr.FONT_HEIGHT + 1, 0xffd0d0d0);
                    } else {
                        fr.drawString("_", c + 2, textLine + 1,
                                getPrimaryColorProperty().getHex());
                    }

                }
            }

            // selection highlighting

            // the start of the highlight.
            int x = -1;
            // the end of the highlight.
            int w = -1;

            // test the start
            if (start >= 0 && start <= text.length()) {
                    x = TextRenderRouteApi.width(fr, text.substring(0, start)) + inputInset();
            }

            // test the end
            if (end >= 0 && end <= text.length()) {
                w = TextRenderRouteApi.width(fr, text.substring(start < 0 ? 0 : start, end)) + 2;
            }

            final int LINE_Y = textLine + fr.FONT_HEIGHT + 2;

            if (w != 0) {
                if (x >= 0 && w > 0) {
                    // start and end on same line
                    drawSelectionBox(x + 2, textLine, x + w, LINE_Y);
                } else {
                    if (x >= 0) {
                        // started on this line
                        drawSelectionBox(x + 2, textLine,
                                x + TextRenderRouteApi.width(fr, text.substring(start)) + 1, LINE_Y);
                    }
                    if (w >= 0) {
                        // ends on this line
                        drawSelectionBox(2 + inputInset(), textLine, w + inputInset(), LINE_Y);
                    }
                    if (start < 0 && end > text.length()) {
                        // full line
                        drawSelectionBox(1 + inputInset(), textLine,
                                TextRenderRouteApi.width(fr, text) + inputInset(), LINE_Y);
                    }
                }
            }

            // keep track of the lines
            totalPos += text.length();
            boolean space = getText().length() > totalPos && getText().charAt(totalPos) == ' ';

            // prepare all the markers for the next line.
            pos -= text.length();
            start -= text.length();
            end -= text.length();

            if (space) {
                // compensate for spaces
                pos--;
                start--;
                end--;
                totalPos++;
            }
            line += (int) Math.ceil(layout.height()) + 2;
        }

    }

    private void drawText() {
        FancyFontRenderer ffr = new FancyFontRenderer(fr);
        int yPos = 1;
        List<String> rawLines = getWrappedLines();
        List<ITextComponent> lines = getFormattedLines(rawLines);
        String fullText = textField.getTextField().getText();
        int sourceSearch = 0;
        int lastTextX = 3 + inputInset();
        int lastTextY = yPos;
        float lastTextAdvance = 0.0F;
        List<ColoredRange> commandColors =
                CommandCompletionPresentation.coloredRanges(textField.getTextField());
        for (int index = 0; index < lines.size(); index++) {
            ITextComponent line = lines.get(index);
            int color = ChatStyleConfig.enabled
                    ? ChatStyleRenderer.color(ChatStyleConfig.text, mc.gameSettings.chatOpacity)
                    : Color.WHITE.getHex();
            String raw = rawLines.get(index);
            TextRenderRouteLayout layout = TextRenderRouteApi.layout(fr, raw);
            int textX = 3 + inputInset();
            int lineStart = fullText.indexOf(raw, sourceSearch);
            if (lineStart < 0) lineStart = Math.min(sourceSearch, fullText.length());
            StyledLine styled = CommandCompletionPresentation.styleLine(
                    raw, lineStart, commandColors);
            if (!commandColors.isEmpty()) {
                lastTextAdvance = drawCommandLine(
                        raw, lineStart, layout, commandColors, styled, textX, yPos, color);
                drawSpellingDecorations(line, layout, textX, yPos);
            } else if (EnhancedChatConfigAccess.sourcePreviewEnabled()
                    && sourceIsActive(raw, lineStart, fullText)) {
                lastTextAdvance = drawSourceAwareLine(raw, lineStart, fullText,
                        layout, textX, yPos, color);
            } else if (layout.hasInlineContent() || !layout.structuredText().effects().isEmpty()) {
                // Structured effects (including TextAnimator's typewriter) must use the
                // same route as chat output. FancyFontRenderer only draws literal text and
                // silently drops angle-bracket effects from the input preview.
                TextRenderRouteApi.layout(fr, raw, color, false).draw(textX, yPos);
                drawSpellingDecorations(line, layout, textX, yPos);
                lastTextAdvance = layout.advance();
            } else {
                ffr.drawChat(line, textX, yPos, color, false);
                lastTextAdvance = layout.advance();
            }
            sourceSearch = Math.min(fullText.length(), lineStart + raw.length());
            lastTextX = textX;
            lastTextY = yPos;
            yPos += (int) Math.ceil(layout.height()) + 2;
        }
        drawCommandGhost(lastTextAdvance, lastTextX, lastTextY);

    }

    /** Shows source literally while the caret is editing any recognized syntax span. */
    private boolean sourceIsActive(String line, int lineStart, String fullText) {
        if (line == null || line.isEmpty()) return false;
        int cursor = textField.getTextField().getCursorPosition();
        SourceEditProjection projection = cachedSourceProjection(fullText);
        SourceEditState state = cachedSourceState(fullText, cursor);
        if (projection.at(state) == null) return false;
        int lineEnd = Math.min(fullText.length(), lineStart + line.length());
        for (neofontrender.text.edit.SourceSpan span : projection.spans()) {
            if (span.contains(state) && span.end() > lineStart && span.start() < lineEnd) return true;
        }
        return false;
    }

    /** Draws only the active syntax span literally; neighboring source stays previewed. */
    private float drawSourceAwareLine(String line, int lineStart, String fullText,
                                      TextRenderRouteLayout layout, int textX, int textY,
                                      int color) {
        int cursor = textField.getTextField().getCursorPosition();
        SourceEditProjection projection = cachedSourceProjection(fullText);
        SourceSpan active = projection.at(cachedSourceState(fullText, cursor));
        if (active == null) {
            layout.draw(textX, textY);
            return layout.advance();
        }

        int lineEnd = Math.min(fullText.length(), lineStart + line.length());
        int rawStart = Math.max(lineStart, active.start()) - lineStart;
        int rawEnd = Math.min(lineEnd, active.end()) - lineStart;
        if (rawStart >= rawEnd) {
            layout.draw(textX, textY);
            return layout.advance();
        }

        float advance = 0.0F;
        if (rawStart > 0) {
            String prefix = line.substring(0, rawStart);
            TextRenderRouteLayout prefixLayout = TextRenderRouteApi.layout(fr, prefix, color, false);
            prefixLayout.draw(textX, textY);
            advance += prefixLayout.advance();
        }
        String source = line.substring(rawStart, rawEnd);
        advance += drawLiteralSource(source, textX + Math.round(advance), textY, color);
        if (rawEnd < line.length()) {
            String suffix = line.substring(rawEnd);
            TextRenderRouteLayout suffixLayout = TextRenderRouteApi.layout(fr, suffix, color, false);
            suffixLayout.draw(textX + Math.round(advance), textY);
            advance += suffixLayout.advance();
        }
        return advance;
    }

    private SourceEditProjection cachedSourceProjection(String text) {
        if (sourceProjection == null || !text.equals(sourceProjectionText)) {
            sourceProjection = TextRenderRouteApi.sourceProjection(text);
            sourceProjectionText = text;
            sourceProjectionCursor = -1;
            sourceProjectionState = null;
        }
        return sourceProjection;
    }

    private SourceEditState cachedSourceState(String text, int cursor) {
        if (sourceProjectionState == null || sourceProjectionCursor != cursor
                || !text.equals(sourceProjectionText)) {
            sourceProjectionState = SourceEditState.of(text, cursor);
            sourceProjectionCursor = cursor;
        }
        return sourceProjectionState;
    }

    private float drawLiteralSource(String source, int x, int y, int color) {
        float advance = 0.0F;
        for (int index = 0; index < source.length();) {
            int codePoint = source.codePointAt(index);
            String character = new String(Character.toChars(codePoint));
            if (ModernTextApi.isAvailable()) {
                ModernTextApi.draw(character, x + advance, y,
                        NeofontrenderConfig.fontSize(), color);
                advance += TextRenderRouteApi.width(fr, character);
            } else {
                fr.drawString(character, x + Math.round(advance), y, color, false);
                advance += fr.getStringWidth(character);
            }
            index += Character.charCount(codePoint);
        }
        return advance;
    }

    private float drawCommandLine(String source, int lineStart, TextRenderRouteLayout layout,
                                  List<ColoredRange> colors, StyledLine styled,
                                  int textX, int textY, int baseColor) {
        if (!layout.hasInlineContent()) {
            return drawCommandText(styled, textX, textY, baseColor);
        }
        int cursor = 0;
        for (TextInlineBounds content : layout.inlineBounds()) {
            int start = content.sourceStart();
            if (cursor < start) {
                drawCommandText(CommandCompletionPresentation.styleLine(
                                source.substring(cursor, start), lineStart + cursor, colors),
                        textX + Math.round(layout.widthToSource(cursor)), textY, baseColor);
            }
            int contentColor = commandColorAt(
                    colors, lineStart + start, baseColor);
            new StructuredInlineContentAdapter(content.content()).draw(
                    textX + content.x(), textY + content.y(), contentColor, false);
            cursor = content.sourceEnd();
        }
        if (cursor < source.length()) {
            drawCommandText(CommandCompletionPresentation.styleLine(
                            source.substring(cursor), lineStart + cursor, colors),
                    textX + Math.round(layout.widthToSource(cursor)), textY, baseColor);
        }
        return layout.advance();
    }

    private float drawCommandText(StyledLine line, int textX, int textY, int color) {
        if (ModernTextApi.isAvailable()) {
            return ModernTextApi.drawFormatted(line.modernText(), textX, textY,
                    NeofontrenderConfig.fontSize(), color, false);
        }
        fr.drawString(line.legacyText(), textX, textY, color, false);
        return fr.getStringWidth(line.legacyText());
    }

    private static int commandColorAt(
            List<ColoredRange> colors, int sourceIndex, int baseColor) {
        for (ColoredRange range : colors) {
            if (sourceIndex >= range.start && sourceIndex < range.end) {
                return (baseColor & 0xFF000000) | (range.color & 0xFFFFFF);
            }
        }
        return baseColor;
    }

    private void drawCommandGhost(float lineAdvance, int textX, int textY) {
        String suffix = CommandCompletionPresentation.ghostSuffix(textField.getTextField());
        if (suffix.isEmpty()) return;
        float x = textX + lineAdvance;
        if (ModernTextApi.isAvailable()) {
            ModernTextApi.draw(suffix, x, textY, NeofontrenderConfig.fontSize(), 0xFF808080);
        } else {
            fr.drawString(suffix, x, textY, 0xFF808080, false);
        }
    }

    private void drawSpellingDecorations(ITextComponent line, TextRenderRouteLayout layout,
                                          int textX, int textY) {
        int sourceIndex = 0;
        for (ITextComponent component : line) {
            String segment = component.getUnformattedComponentText();
            int end = Math.min(layout.source().length(), sourceIndex + segment.length());
            if (component instanceof FancyTextComponent) {
                Color underline = ((FancyTextComponent) component)
                        .getFancyStyle().getUnderline();
                if (underline.getAlpha() > 0
                        && !layout.hasInlineContentInSourceRange(sourceIndex, end)) {
                    int left = textX + Math.round(layout.widthToSource(sourceIndex));
                    int right = textX + Math.round(layout.widthToSource(end));
                    if (right > left) {
                        drawHorizontalLine(left, right,
                                textY + (int) Math.ceil(layout.height()) - 1,
                                underline.getHex());
                    }
                }
            }
            sourceIndex = end;
        }
    }

    private void drawGlyphHover(int mouseX, int mouseY) {
        if (!EnhancedChatFeatures.imageGlyphHover()) return;
        int visualY = mouseY - Math.round(ChatAnimationController.inputOffset());
        List<String> lines = getWrappedLines();
        int row = rowAt(lines, visualY);
        if (row < 0) return;
        int rowTop = heightBefore(lines, row);
        int textX = 3 + inputInset();
        TextRenderRouteLayout layout = TextRenderRouteApi.layout(fr, lines.get(row));
        TextInlineBounds hit = layout.contentAt(mouseX - textX,
                visualY - rowTop - 1);
        if (hit == null) return;

        final int preview = 56;
        StructuredInlineContentAdapter content = new StructuredInlineContentAdapter(hit.content());
        String description = fr.trimStringToWidth(content.description(), 180);
        int panelWidth = Math.max(preview + 10, fr.getStringWidth(description) + 10);
        int panelHeight = preview + fr.FONT_HEIGHT + 13;
        int x = Math.max(2, Math.min(mouseX + 12, getBounds().width - panelWidth - 2));
        int y = -panelHeight - 5;
        drawRect(x, y, x + panelWidth, y + panelHeight, 0xF0181D24);
        drawRect(x + 1, y + 1, x + panelWidth - 1, y + panelHeight - 1, 0xF02B3440);
        content.drawPreview(x + (panelWidth - preview) / 2,
                y + 5, preview, 0xFFFFFFFF);
        fr.drawStringWithShadow(description, x + 5, y + preview + 8, 0xFFF2F5F7);
        GlStateManager.color(1, 1, 1, 1);
    }

    private boolean beginClip() {
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return false;
        ILocation location = getActualLocation();
        int factor = new ScaledResolution(mc).getScaleFactor();
        int x = Math.max(0, location.getXPos() * factor);
        int y = Math.max(0, mc.displayHeight
                - (location.getYPos() + location.getHeight()) * factor);
        int width = Math.min(mc.displayWidth - x,
                Math.max(0, location.getWidth() * factor));
        int height = Math.min(mc.displayHeight - y,
                Math.max(0, location.getHeight() * factor));
        if (width <= 0 || height <= 0) return false;
        scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (scissorWasEnabled) GL11.glGetInteger(GL11.GL_SCISSOR_BOX, oldScissor);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
        return true;
    }

    private void endClip() {
        if (scissorWasEnabled) {
            GL11.glScissor(oldScissor.get(0), oldScissor.get(1),
                    oldScissor.get(2), oldScissor.get(3));
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /**
     * Draws the blue selection box. Adapted from {@link GuiTextField#drawSelectionBox(int, int, int, int)}
     */
    private void drawSelectionBox(int x1, int y1, int x2, int y2) {
        if (x1 < x2) {
            int i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            int j = y1;
            y1 = y2;
            y2 = j;
        }

        x2 = Math.min(x2, this.getLocation().getXWidth());
        x1 = Math.min(x1, this.getLocation().getXWidth());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        GlStateManager.color(0.0F, 0.0F, 255.0F, 255.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.enableColorLogic();
        GlStateManager.colorLogicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        bufferbuilder.pos(x1, y2, 0.0D).endVertex();
        bufferbuilder.pos(x2, y2, 0.0D).endVertex();
        bufferbuilder.pos(x2, y1, 0.0D).endVertex();
        bufferbuilder.pos(x1, y1, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.disableColorLogic();
        GlStateManager.enableTexture2D();
    }


    @Override
    public void updateComponent() {
        this.cursorCounter++;
    }

    @Override
    public List<String> getWrappedLines() {
        if (!EnhancedChatConfigAccess.sourcePreviewEnabled()) {
            return fr.listFormattedStringToWidth(textField.getValue(),
                    Math.max(8, getBounds().width - inputInset()));
        }
        if (sourceEditingActive(textField.getTextField().getText())) {
            return TextRenderRouteApi.wrapEditable(fr, textField.getValue(),
                    Math.max(8, getBounds().width - inputInset()));
        }
        return TextRenderRouteApi.wrap(fr, textField.getValue(),
                Math.max(8, getBounds().width - inputInset()));
    }

    private boolean sourceEditingActive(String text) {
        if (text == null || text.isEmpty()) return false;
        return cachedSourceProjection(text).at(cachedSourceState(text,
                textField.getTextField().getCursorPosition())) != null;
    }

    private List<ITextComponent> getFormattedLines(List<String> lines) {
        if (TabbyChat.getInstance().settings.advanced.spelling.get()) {
            spellcheck.checkSpelling(textField.getValue());
            return lines.stream()
                    .map(new SpellingFormatter(spellcheck))
                    .collect(Collectors.toList());
        }
        return lines.stream()
                .map(TextComponentString::new)
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Dimension getMinimumSize() {
        int target = contentHeight(getWrappedLines());
        long now = System.nanoTime();
        if (animatedHeight < 0.0F) {
            animatedHeight = Math.min(target, fr.FONT_HEIGHT + 2);
            lastHeightUpdateNanos = now;
        }
        if (!EnhancedChatConfigAccess.inputAnimationEnabled()) {
            animatedHeight = target;
        } else {
            long elapsed = Math.max(0L, Math.min(50_000_000L, now - lastHeightUpdateNanos));
            float duration = Math.max(1,
                    EnhancedChatConfigAccess.inputAnimationDurationMillis()) * 1_000_000.0F;
            float amount = 1.0F - (float) Math.exp(-4.6F * elapsed / duration);
            animatedHeight += (target - animatedHeight) * amount;
            if (Math.abs(target - animatedHeight) < 0.1F) animatedHeight = target;
        }
        lastHeightUpdateNanos = now;
        return new Dimension(100, Math.max(fr.FONT_HEIGHT + 2,
                Math.round(animatedHeight)));
    }

    public GuiText getTextField() {
        return textField;
    }

    @Override
    public String getText() {
        return textField.getValue();
    }

    @Override
    public void setText(String text) {
        textField.setValue(text);
    }

    @Subscribe
    public void onMouseClick(GuiMouseEvent event) {
        if (event.getType() == MouseEvent.CLICK && event.getButton() == 0) {
            if (inputInset() > 0 && event.getMouseX() < inputInset()) {
                textField.getTextField().setCursorPositionZero();
                return;
            }
            setMousePosition(event.getMouseX(), event.getMouseY(), false);
        } else if (event.getType() == MouseEvent.DRAG && event.getButton() == 0) {
            setMousePosition(event.getMouseX(), event.getMouseY(), true);
        } else if (event.getType() == MouseEvent.CLICK && event.getButton() == 1) {
            ILocation actual = getActualLocation();
            float scale = getActualScale();
            ChatContextMenu.INSTANCE.openInput(textField.getTextField(),
                    actual.getXPos() + Math.round(event.getMouseX() * scale),
                    actual.getYPos() + Math.round(event.getMouseY() * scale));
        }
    }

    @Subscribe
    public void removePrivateCommandBlock(GuiKeyboardEvent event) {
        GuiTextField field = textField.getTextField();
        if (event.getKey() == Keyboard.KEY_BACK && Keyboard.isKeyDown(event.getKey())
                && field.getCursorPosition() == 0 && field.getSelectionEnd() == 0) {
            manager().removeActivePrivateCommandBlock();
        }
    }

    private void setMousePosition(int x, int y, boolean extendSelection) {
        Rectangle bounds = this.getBounds();
        int width = bounds.width - 1;
        int visualY = y - Math.round(ChatAnimationController.inputOffset());
        List<String> lines = getWrappedLines();
        int row = rowAt(lines, visualY);
        if (row < 0 || x < 0 || x > width) return;
        int index = 0;
        for (int i = 0; i < row; i++) {
            index += lines.get(i).length();
        }
        String line = lines.get(row);
        int localX = Math.max(0, x - 3 - inputInset());
        index += sourceIndexAt(line, index, localX);
        index = Math.max(0, Math.min(index, getText().length()));
        if (extendSelection) textField.getTextField().setSelectionPos(index);
        else textField.getTextField().setCursorPosition(index);
    }

    private int sourceIndexAt(String line, int lineStart, int x) {
        if (!EnhancedChatConfigAccess.sourcePreviewEnabled()) {
            return TextRenderRouteApi.layout(fr, line).sourceIndexAt(x);
        }
        String fullText = textField.getTextField().getText();
        SourceSpan active = cachedSourceProjection(fullText)
                .at(cachedSourceState(fullText, textField.getTextField().getCursorPosition()));
        if (active == null || active.end() <= lineStart || active.start() >= lineStart + line.length()) {
            return TextRenderRouteApi.layout(fr, line).sourceIndexAt(x);
        }
        int rawStart = Math.max(lineStart, active.start()) - lineStart;
        int rawEnd = Math.min(lineStart + line.length(), active.end()) - lineStart;
        float prefixWidth = rawStart == 0 ? 0 : TextRenderRouteApi.width(fr, line.substring(0, rawStart));
        if (x < prefixWidth) return TextRenderRouteApi.layout(fr, line.substring(0, rawStart)).sourceIndexAt(x);
        float rawWidth = literalWidth(line.substring(rawStart, rawEnd));
        if (x <= prefixWidth + rawWidth) {
            int offset = literalIndexAt(line.substring(rawStart, rawEnd),
                    Math.max(0, Math.round(x - prefixWidth)));
            return rawStart + offset;
        }
        int suffix = rawEnd + TextRenderRouteApi.layout(fr, line.substring(rawEnd))
                .sourceIndexAt(Math.max(0, Math.round(x - prefixWidth - rawWidth)));
        return suffix;
    }

    private int sourceVisualWidth(String line, int lineStart, int sourceOffset) {
        int local = Math.max(0, Math.min(line.length(), sourceOffset));
        if (!EnhancedChatConfigAccess.sourcePreviewEnabled()) return TextRenderRouteApi.width(fr, line.substring(0, local));
        String full = textField.getTextField().getText();
        SourceSpan active = cachedSourceProjection(full).at(cachedSourceState(full,
                textField.getTextField().getCursorPosition()));
        if (active == null || active.end() <= lineStart || active.start() >= lineStart + line.length()) {
            return TextRenderRouteApi.width(fr, line.substring(0, local));
        }
        int rawStart = Math.max(lineStart, active.start()) - lineStart;
        int rawEnd = Math.min(lineStart + line.length(), active.end()) - lineStart;
        if (local <= rawStart) return TextRenderRouteApi.width(fr, line.substring(0, local));
        int prefix = TextRenderRouteApi.width(fr, line.substring(0, rawStart));
        if (local <= rawEnd) return prefix + Math.round(literalWidth(line.substring(rawStart, local)));
        return prefix + Math.round(literalWidth(line.substring(rawStart, rawEnd)))
                + TextRenderRouteApi.width(fr, line.substring(rawEnd, local));
    }

    private float literalWidth(String source) {
        float width = 0;
        for (int i = 0; i < source.length();) {
            int cp = source.codePointAt(i);
            String s = new String(Character.toChars(cp));
            width += ModernTextApi.isAvailable() ? TextRenderRouteApi.width(fr, s) : fr.getStringWidth(s);
            i += Character.charCount(cp);
        }
        return width;
    }

    private int literalIndexAt(String source, int x) {
        int offset = 0;
        float advance = 0;
        while (offset < source.length()) {
            int cp = source.codePointAt(offset);
            String s = new String(Character.toChars(cp));
            float next = advance + (ModernTextApi.isAvailable() ? TextRenderRouteApi.width(fr, s) : fr.getStringWidth(s));
            if (x < (advance + next) / 2.0F) return offset;
            advance = next;
            offset += Character.charCount(cp);
        }
        return source.length();
    }

    private int contentHeight(List<String> lines) {
        int height = 0;
        for (String line : lines) {
            height += TextRenderRouteApi.layout(fr, line).height() + 2;
        }
        return Math.max(fr.FONT_HEIGHT + 2, height);
    }

    private int heightBefore(List<String> lines, int row) {
        int height = 0;
        for (int index = 0; index < row; index++) {
            height += TextRenderRouteApi.layout(fr, lines.get(index)).height() + 2;
        }
        return height;
    }

    private int rowAt(List<String> lines, int y) {
        if (y < 0) return -1;
        int top = 0;
        for (int row = 0; row < lines.size(); row++) {
            top += TextRenderRouteApi.layout(fr, lines.get(row)).height() + 2;
            if (y < top) return row;
        }
        return -1;
    }

    @Override
    public Rectangle getBounds() {
        return this.getLocation().asRectangle();
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && ChatHudWindowController.isChatExpanded();
    }

    private ChatManager manager() {
        return (ChatManager) TabbyChat.getInstance().getChat();
    }

    private int inputInset() {
        String prefix = manager().getActivePrivateCommandPrefix();
        return prefix.isEmpty() ? 0 : commandBlockWidth() + BLOCK_LEFT + 4;
    }

    private int commandBlockWidth() {
        String prefix = manager().getActivePrivateCommandPrefix();
        return prefix.isEmpty() ? 0 : BLOCK_PADDING + fr.getStringWidth(prefix)
                + BLOCK_PADDING;
    }

    private void drawPrivateCommandBlock(int mouseX, int mouseY) {
        String prefix = manager().getActivePrivateCommandPrefix();
        if (prefix.isEmpty()) return;
        int width = commandBlockWidth();
        int right = BLOCK_LEFT + width;
        int bottom = Math.min(getBounds().height - 1, fr.FONT_HEIGHT + 4);
        int alpha = Math.max(48, Math.min(255,
                Math.round(238.0F * mc.gameSettings.chatOpacity)));
        int border = alpha << 24 | 0x8295A8;
        int fill = alpha << 24 | 0x303A45;
        drawRect(BLOCK_LEFT, 1, right, bottom, border);
        drawRect(BLOCK_LEFT + 1, 2, right - 1, bottom - 1, fill);
        int textColor = alpha << 24 | 0xF2F5F7;
        int textY = Math.max(1, (bottom - fr.FONT_HEIGHT) / 2);
        fr.drawString(prefix, BLOCK_LEFT + BLOCK_PADDING, textY, textColor, false);
    }
}
