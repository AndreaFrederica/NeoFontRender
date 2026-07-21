package neofontrender.client.gui.views;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import neofontrender.core.font.FontManager;

import java.nio.charset.StandardCharsets;

/** Content view for the visual emoji rendering diagnostics page. */
public final class NfrEmojiTestView extends Widget<NfrEmojiTestView> implements Interactable {
    private static final String[] TEST_STRINGS = {
            "Hello World", "😀", "Hello 😀 World", "❤️", "Test 😀 ❤️ 🎉", "👨‍👩‍👧‍👦", "🙏 😥 😱 😎"
    };
    private static String userText = "Hello 😀 World ❤️";

    private GuiTextField inputField;

    @Override
    public void onInit() {
        super.onInit();
        Minecraft minecraft = Minecraft.getMinecraft();
        inputField = new GuiTextField(minecraft.fontRenderer, getArea().x() + 16, 0,
                getArea().w() - 32, 16);
        inputField.setMaxStringLength(256);
        inputField.setFocused(true);
        inputField.setText(userText);
        inputField.setCanLoseFocus(true);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        int x = getArea().x();
        int y = getArea().y();
        int width = getArea().w();
        int height = getArea().h();
        Gui.drawRect(x, y, x + width, y + height, 0xCC111111);

        Minecraft minecraft = Minecraft.getMinecraft();
        String engine = activeEngine();
        int cursorY = y + 8;
        minecraft.fontRenderer.drawString("Emoji Render Test (Active: " + engine + ")", x + 8, cursorY, 0xFFD700);
        cursorY += 14;
        minecraft.fontRenderer.drawString("Type emoji below:", x + 8, cursorY, 0xA9B5C5);
        cursorY += 12;

        if (inputField != null) {
            inputField.xPosition = x + 16;
            inputField.yPosition = cursorY;
            inputField.width = width - 32;
            userText = inputField.getText();
            inputField.drawTextBox();
        }
        cursorY += 22;
        cursorY = drawCodepoints(minecraft, x, y, width, height, cursorY);
        cursorY = drawCurrentResult(minecraft, engine, x, cursorY);

        Gui.drawRect(x + 8, cursorY, x + width - 8, cursorY + 1, 0x44FFFFFF);
        cursorY += 6;
        minecraft.fontRenderer.drawString("Preset tests (" + engine + "):", x + 8, cursorY, 0xA9B5C5);
        cursorY += 12;
        for (int index = 0; index < TEST_STRINGS.length && cursorY + 18 <= y + height; index++) {
            String test = TEST_STRINGS[index];
            int textWidth = minecraft.fontRenderer.getStringWidth(test);
            minecraft.fontRenderer.drawString("[" + index + "]", x + 8, cursorY + 2, 0x888888);
            minecraft.fontRenderer.drawString(test, x + 40, cursorY, 0xFFFFFF);
            minecraft.fontRenderer.drawString("w=" + textWidth, x + 48 + textWidth, cursorY + 2, 0x66FF66);
            cursorY += 16;
        }
    }

    private static int drawCodepoints(Minecraft minecraft, int x, int y, int width, int height, int cursorY) {
        if (userText == null || userText.isEmpty()) return cursorY;
        minecraft.fontRenderer.drawString("Codepoints:", x + 8, cursorY, 0xA9B5C5);
        cursorY += 10;
        StringBuilder codepoints = new StringBuilder();
        userText.codePoints().forEach(codepoint -> codepoints.append(String.format("U+%04X ", codepoint)));
        int charactersPerLine = Math.max(1, (width - 16) / 6);
        for (int start = 0; start < codepoints.length(); start += charactersPerLine) {
            if (cursorY + 10 > y + height) break;
            minecraft.fontRenderer.drawString(codepoints.substring(start,
                    Math.min(start + charactersPerLine, codepoints.length())), x + 8, cursorY, 0x888888);
            cursorY += 10;
        }
        cursorY += 2;
        minecraft.fontRenderer.drawString("length=" + userText.length() + " chars, bytes="
                + userText.getBytes(StandardCharsets.UTF_8).length, x + 8, cursorY, 0x666666);
        return cursorY + 12;
    }

    private static int drawCurrentResult(Minecraft minecraft, String engine, int x, int cursorY) {
        if (userText == null || userText.isEmpty()) return cursorY;
        minecraft.fontRenderer.drawString(engine + " result:", x + 8, cursorY, 0x88AAFF);
        cursorY += 12;
        int textWidth = minecraft.fontRenderer.getStringWidth(userText);
        minecraft.fontRenderer.drawString(userText, x + 16, cursorY, 0xFFFFFF);
        minecraft.fontRenderer.drawString("w=" + textWidth, x + 24 + textWidth, cursorY + 2, 0x66FF66);
        return cursorY + 18;
    }

    private static String activeEngine() {
        if (FontManager.INSTANCE.isCosmicActive()) return "Cosmic";
        if (FontManager.INSTANCE.isSkiaActive()) return "Skia";
        if (FontManager.INSTANCE.isSfrActive()) return "SFR";
        return "Vanilla";
    }

    @Override
    public Result onMousePressed(int mouseButton) {
        if (inputField != null) {
            inputField.mouseClicked(inputField.xPosition, inputField.yPosition, mouseButton);
        }
        return Result.ACCEPT;
    }

    @Override
    public Result onKeyPressed(char typedChar, int keyCode) {
        if (inputField != null) {
            inputField.textboxKeyTyped(typedChar, keyCode);
            userText = inputField.getText();
        }
        return Result.ACCEPT;
    }
}
