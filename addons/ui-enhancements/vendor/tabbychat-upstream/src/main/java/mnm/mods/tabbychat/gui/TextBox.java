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
                int c = TextRenderRouteApi.width(fr, text.substring(0, pos)) + inputInset();
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
            } else if (layout.hasInlineContent()) {
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
        return TextRenderRouteApi.wrap(fr, textField.getValue(),
                Math.max(8, getBounds().width - inputInset()));
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
            // listFormattedStringToWidth trims the wrapping space from the visual line.
            if (index < getText().length() && getText().charAt(index) == ' ') index++;
        }
        index += TextRenderRouteApi.layout(fr, lines.get(row)).sourceIndexAt(
                Math.max(0, x - 3 - inputInset()));
        index = Math.max(0, Math.min(index, getText().length()));
        if (extendSelection) textField.getTextField().setSelectionPos(index);
        else textField.getTextField().setCursorPosition(index);
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
